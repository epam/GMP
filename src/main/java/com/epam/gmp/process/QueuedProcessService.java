/*
 *  /***************************************************************************
 *  Copyright (c) 2017, EPAM SYSTEMS INC
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ***************************************************************************
 */

package com.epam.gmp.process;

import com.epam.gmp.service.GMPContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service("QueuedProcessService")
@Scope(value = "singleton")
public class QueuedProcessService implements IQueuedProcessService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ExecutorService threadPool;
    private ArrayBlockingQueue<Runnable> queue;

    @Value("${groovy.thread.pool.size}")
    private int startPullers = 100;

    @Value("#{systemProperties['numThreads'] ?: ${groovy.thread.pool.core.size}}")
    private int corePullers = 100;

    @Value("${groovy.thread.timeout}")
    private int threadTimeout = 30;

    @Autowired
    private GMPContext gmpContext;

    @PostConstruct
    public void initialize() {
        queue = new ArrayBlockingQueue<>(startPullers);
        threadPool = new QueuedProcessThreadPoolExecutor(corePullers, startPullers, 30L, TimeUnit.SECONDS, queue, new QueuedProcessThreadPoolExecutor.GroovyThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

        if (logger.isInfoEnabled()) {
            logger.info("Prepared Groovy thread pool of {} pullers; concurrent: {}", startPullers, corePullers);
            logger.info("Initialized.");
        }
    }

    public synchronized void execute(IQueuedThread process) {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.execute(process);

            if (logger.isInfoEnabled()) {
                logger.info("Groovy pool size: " + queue.size());
            }
        }
    }

    @Override
    public <T extends IQueuedThread> void execute(Class<T> bean, Object... args) {
        execute(gmpContext.getApplicationContext().getBean(bean, args));
    }

    public void shutdown() {
        shutdown(threadTimeout);
    }

    public void shutdown(int timeout) {
        threadPool.shutdown();
        try {
            long start = System.currentTimeMillis();
            logger.info("Thread pool shutdown requested");
            if (!threadPool.awaitTermination(timeout, TimeUnit.MINUTES)) {
                logger.info("Thread pool shutdown timeout");
            }
            if (logger.isInfoEnabled()) {
                logger.info("Thread pool has been terminated in: {} millis.", System.currentTimeMillis() - start);
            }
        } catch (InterruptedException e) {
            logger.error("Unable to stop thread pool correctly.", e);
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getQueueCapacity() {
        return startPullers;
    }
}
