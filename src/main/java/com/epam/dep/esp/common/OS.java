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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S00115")
public enum OS {
    win {},
    linux {},
    SunOs {},
    macOsX {},
    unknown {};

    protected static final Logger LOGGER = LoggerFactory.getLogger(OS.class);
    public static final String arch = System.getProperty("os.arch").replaceFirst("amd64", "x64");
    public static final String osName = System.getProperty("os.name").toLowerCase();
    public static final String version = System.getProperty("os.version");
    public static final String patch = System.getProperty("sun.os.patch.level");

    OS() {

    }

    public static OS getOs() {
        LOGGER.debug(osName);
        if (osName.startsWith("windows")) return win;
        else if (osName.startsWith("linux")) return linux;
        else if (osName.startsWith("sunos")) return SunOs;
        else if (osName.startsWith("mac os x")) return macOsX;
        else return unknown;
    }

    public Integer execCommandLine(List<String> command, List<String> out, String homeFolder, int timeout) {
        return execCommandLine(command, out, homeFolder, timeout, null);
    }

    public Integer execCommandLine(List<String> command, List<String> out, String homeFolder, int timeout, Map<String, String> envVars) {
        Integer result = null;
        if (command != null && !command.isEmpty() && out != null) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Start command: {}", command.stream().collect(Collectors.joining(" ", "\"", "\"")));
                }
                List<String> processOut;
                ProcessBuilder builder = new ProcessBuilder(command);
                // put additional environment variables if needed
                if (envVars != null) {
                    builder.environment().putAll(envVars);
                }
                if (homeFolder != null) builder.directory(new File(homeFolder));

                Process process = builder.start();
                OSRunner runner = new OSRunner(process);

                result = runner.run(timeout * 1000l);
                process.destroy();

                LOGGER.debug("Pumpers finished.");
                processOut = runner.getOut();
                if (LOGGER.isDebugEnabled()) {
                    for (String item : processOut) {
                        LOGGER.debug(item);
                    }
                    LOGGER.debug("Exit code: {} {}%\n\r", result, command.stream().collect(Collectors.joining(" ", "\"", "\"")));
                }
                out.addAll(processOut);

            } catch (IOException e) {
                LOGGER.error("IOError", e);
            }
        }
        return result;
    }
}