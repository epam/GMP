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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class QueuedProcessThreadPoolExecutor extends ThreadPoolExecutor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected final AtomicInteger incompleteScripts = new AtomicInteger(0);

    public QueuedProcessThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new QueuedProcessThreadPoolExecutor.GroovyThreadFactory(), new QueuedProcessThreadPoolExecutor.QPSRejectedExecutionHandler());
    }

    public static class GroovyThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        GroovyThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "QueuedProcess(" + poolNumber.getAndIncrement() + ")-thread-";
        }

        public Thread newThread(Runnable r) {
            String name = namePrefix + threadNumber.getAndIncrement();
            Thread t = new Thread(group, r, name, 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    @Override
    public void execute(Runnable command) {
        incompleteScripts.incrementAndGet(); //Should be before execute.
        super.execute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        decScriptCounter();
    }

    protected void decScriptCounter() {
        incompleteScripts.decrementAndGet();
        synchronized (incompleteScripts) {
            incompleteScripts.notifyAll();
        }
    }

    @SuppressWarnings("squid:S2142")
    public void shutdown(int timeout) {
        if (logger.isInfoEnabled()) {
            logger.info("ThreadPool shutdown requested");
        }
        try {
            while (incompleteScripts.get() != 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Scripts still running=({}) Await for termination...", incompleteScripts.get());
                }
                synchronized (incompleteScripts) {
                    incompleteScripts.wait(TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES));
                }
            }
            logger.info("No active Scripts. Shutting down...", incompleteScripts.get());
        } catch (InterruptedException e) {
            logger.error("Shutdown wait timeout. Force shutdown.", e);
        }
        shutdown();
    }

    protected static class QPSRejectedExecutionHandler implements RejectedExecutionHandler {
        private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            QueuedProcessThreadPoolExecutor qpExecutor = ((QueuedProcessThreadPoolExecutor) executor);
            qpExecutor.decScriptCounter();
            logger.info("RejectedExecution for № {}. Run in the same thread.", qpExecutor.incompleteScripts);
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
