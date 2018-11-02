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

    final AtomicInteger incompleteScripts = new AtomicInteger(0);

    public QueuedProcessThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public QueuedProcessThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public QueuedProcessThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public QueuedProcessThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
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
    public Future<?> submit(Runnable task) {
        incompleteScripts.incrementAndGet();
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        incompleteScripts.incrementAndGet();
        return super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        incompleteScripts.incrementAndGet();
        return super.submit(task);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        int i = incompleteScripts.decrementAndGet();
        synchronized (incompleteScripts) {
            incompleteScripts.notifyAll();
        }
    }

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
            logger.error("ThreadPool wait threads to stop timeout", e);
        }
        shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return super.awaitTermination(timeout, unit);
    }
}
