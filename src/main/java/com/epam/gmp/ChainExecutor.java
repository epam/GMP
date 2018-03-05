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

package com.epam.gmp;

import com.epam.gmp.config.GMPConfig;
import com.epam.gmp.process.GroovyThread;
import com.epam.gmp.process.QueuedProcessService;
import com.epam.gmp.service.ExitCodeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.epam.gmp.service.ScriptContextBuilder.SCRIPT_PATTERN;

public class ChainExecutor {
    private final static Logger logger = LoggerFactory.getLogger(ChainExecutor.class);

    private AnnotationConfigApplicationContext context;

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0) {
            Integer initialExitCode = 0;
            ChainExecutor chainExecutor = new ChainExecutor();
            chainExecutor.initContext();

            QueuedProcessService processService = chainExecutor.context.getBean(QueuedProcessService.class);
            long start = System.currentTimeMillis();
            for (int i = 0; i < args.length; ) {
                String arg = args[i];
                Matcher pathMatcher = SCRIPT_PATTERN.matcher(arg);
                if (pathMatcher.matches()) {
                    List<String> cmdLineParams = new ArrayList<>();
                    int j = i + 1;
                    while (j < args.length) {
                        Matcher nextMatcher = SCRIPT_PATTERN.matcher(args[j]);
                        if (!nextMatcher.matches()) {
                            cmdLineParams.add(args[j++]);
                        } else {
                            break;
                        }
                    }
                    i += cmdLineParams.size() + 1;
                    try {
                        processService.execute(GroovyThread.class, arg, cmdLineParams);
                    } catch (BeansException e) {
                        logger.error("Unable to run script", e);
                        initialExitCode++;
                    }
                } else {
                    i++;
                }
            }
            processService.shutdown();
            logger.info("Took: " + (System.currentTimeMillis() - start));
            ExitCodeCalculator exitCodeCalculator = chainExecutor.context.getBean(ExitCodeCalculator.class);
            int exitCode = exitCodeCalculator.calculate(initialExitCode);
            chainExecutor.context.close();
            System.exit(exitCode);
        } else {
            logger.error("Parameter script name is absent!");
            System.exit(1);
        }
    }

    private void initContext() {
        context = new AnnotationConfigApplicationContext();
        context.register(GMPConfig.class);
        context.refresh();
        if (logger.isInfoEnabled()) {
            for (String name : context.getBeanDefinitionNames()) {
                logger.info("<bean name='" + name + "'/>" + " - READY.");
            }
        }
    }
}
