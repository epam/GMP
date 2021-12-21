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

package com.epam.dep.esp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OSRunner {

    protected static final Logger LOGGER = LoggerFactory.getLogger(OSRunner.class);
    public static final String INTERRUPTED = "Interrupted";
    private static final int THREAD_TIME_OUT = 10000;
    private int timeout;

    private final Process process;
    private List<String> result;
    private final StreamPumper errorPumper;
    private final StreamPumper stdPumper;

    OSRunner(Process process) {
        this(process, THREAD_TIME_OUT);
    }

    OSRunner(Process process, int timeout) {
        this.process = process;
        this.errorPumper = new StreamPumper(process.getErrorStream());
        this.stdPumper = new StreamPumper(process.getInputStream());
        this.timeout = timeout;
        result = new ArrayList<>();
        errorPumper.start();
        stdPumper.start();
    }

    @SuppressWarnings("squid:S3776")
    public Integer run(long processTimeout) {
        boolean error = false;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Wait for {}", process);
            }
            if (process.waitFor(processTimeout, TimeUnit.SECONDS)) {
                return Integer.valueOf(process.exitValue());
            } else {
                return null;
            }
        } catch (InterruptedException ignore) {
            LOGGER.error(INTERRUPTED, ignore);
            error = true;
        } finally {
            if (!error) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Wait for error stream processing.");
                    }
                    errorPumper.join(timeout);
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED, e);
                }
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Wait for std stream processing.");
                    }
                    stdPumper.join(timeout);
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED, e);
                }
            }

            if (errorPumper.isAlive()) {
                errorPumper.interrupt();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Kill Error Pumper {} {}}", process, errorPumper);
                }
            }

            if (stdPumper.isAlive()) {
                stdPumper.interrupt();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Kill Out Pumper {} {}", process, stdPumper);
                }
            }

            if (error) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Wait for error die.");
                    }
                    errorPumper.join(timeout);
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED, e);
                }
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Wait for std die.");
                    }
                    stdPumper.join(timeout);
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED, e);
                }
            }
            result.addAll(errorPumper.getOut());
            result.addAll(stdPumper.getOut());
        }
        return null;
    }

    public List<String> getOut() {
        return result;
    }

    private class StreamPumper extends Thread {
        private static final int SLEEP_TIME = 0;

        private boolean endOfStream = false;
        private final List<String> out = new ArrayList<>();
        private InputStream is;

        public StreamPumper(InputStream is) {
            this.is = is;
        }

        public List<String> getOut() {
            return out;
        }

        private void pumpStream(BufferedReader din) throws IOException {
            String line = din.readLine();
            if (line != null) {
                out.add(line);
                LOGGER.info(line);
            } else {
                endOfStream = true;
            }
        }

        @Override
        public void run() {
            try (BufferedReader din = new BufferedReader(new InputStreamReader(is))) {
                while (!endOfStream && !isInterrupted()) {
                    pumpStream(din);
                    sleep(SLEEP_TIME);
                }
            } catch (InterruptedException | IOException ie) {
                LOGGER.error("Stream pumper error: ", ie);
            }
        }
    }
}
