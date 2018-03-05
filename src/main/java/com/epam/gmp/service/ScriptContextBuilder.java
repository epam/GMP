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

package com.epam.gmp.service;

import com.epam.gmp.ExportBinding;
import com.epam.gmp.ScriptClassloader;
import com.epam.gmp.ScriptContext;
import com.epam.gmp.ScriptContextException;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ScriptContextBuilder")
@Scope(value = "singleton")
public class ScriptContextBuilder {
    public static final String CONFIG_SUFFIX = ".config.groovy";
    public static final String SCRIPT_SUFFIX = ".groovy";
    public static final String INCLUDE_CONFIG_FIELD = "includeConfig";
    public static final String SCRIPT_CONFIG = "@scriptConfig";
    public static final String SCRIPT_TO_RUN = "script";
    public static final String EXECUTOR_FIELD = "EXECUTOR";
    private final static Logger logger = LoggerFactory.getLogger(ScriptContextBuilder.class);
    private static final String LIB_FOLDER = "lib";
    private static final String COMMON_CONFIG = "common-config.groovy";
    private static final String GLOBAL_CONFIG = "global-config.groovy";
    private static final String SCRIPTS = "scripts";
    public static Pattern SCRIPT_PATTERN = Pattern.compile("([^@]*)[@](([^/]*)/((.*)[.]groovy))");
    public static Pattern SCRIPT_FILE_PATTERN = Pattern.compile("(([^.]*)([.]config)?([.]groovy))");

    private Map<String, Object> bindingBeans;

    @Resource(name = "gmpHome")
    private String gmpHome;

    @Autowired
    private GMPContext GMPContext;

    public ScriptContext buildContextFor(String scriptPath, List<String> cmdLineParams) throws ScriptContextException {
        Matcher pathMatcher = SCRIPT_PATTERN.matcher(scriptPath);
        if (pathMatcher.matches()) {
            String scriptGroupFolder = gmpHome + File.separator + SCRIPTS + File.separator + pathMatcher.group(3);
            String scriptName = pathMatcher.group(4);
            String environment = pathMatcher.group(1);
            File fScriptGroupFolder = new File(scriptGroupFolder);
            if (fScriptGroupFolder.exists()) {
                ClassLoader scriptClassLoader = buildClassloader(fScriptGroupFolder);

                ConfigObject groovyConfig = new ConfigObject();
                //Global config
                File global = new File(gmpHome + File.separator + GLOBAL_CONFIG);
                ConfigObject globalConfig = fillParamMapFromGroovy(global, getBindingBeans(), environment);
                if (globalConfig != null) groovyConfig.merge(globalConfig);

                //Common config
                File properties = new File(scriptGroupFolder + File.separator + COMMON_CONFIG);
                ConfigObject commonConfig = fillParamMapFromGroovy(properties, getBindingBeans(), environment);
                if (commonConfig != null) groovyConfig.merge(commonConfig);

                ConfigObject scriptConfig = buildConfig(scriptGroupFolder, scriptName, environment, null);
                if (scriptConfig != null) {
                    groovyConfig.merge(scriptConfig);
                    scriptName = ((Map) groovyConfig.get(EXECUTOR_FIELD)).get(SCRIPT_TO_RUN).toString();
                    groovyConfig.remove(EXECUTOR_FIELD);
                }

                //ADD logger
                HashMap<String, Object> paramMap = new HashMap<>();
                Logger scriptLogger = LoggerFactory.getLogger(scriptName.replaceAll("[.]", "_"));
                paramMap.put("logger", scriptLogger);

                paramMap.put("gConfig", groovyConfig);
                paramMap.put("cmdLine", cmdLineParams);
                if (logger.isInfoEnabled()) {
                    logger.info("Groovy based script configuration:\n" + scriptName + ":" + configToString(groovyConfig));
                }
                return new ScriptContext(scriptPath, scriptClassLoader, paramMap, scriptGroupFolder, scriptName);
            }
        }
        throw new ScriptContextException("Unable to build context for: " + scriptPath);
    }

    protected ClassLoader buildClassloader(File sPath) {
        File libs = new File(sPath + File.separator + LIB_FOLDER);
        ClassLoader scriptClassLoader = null;
        if (libs.exists()) {
            try {
                String[] jars = libs.list(new LibFilter());
                List<URL> urls = new ArrayList<>();
                for (String jar : jars) {
                    urls.add(new URL("file", "", libs.getAbsolutePath() + File.separator + jar));
                }
                URL[] aUrls = new URL[urls.size()];
                scriptClassLoader = new ScriptClassloader(urls.toArray(aUrls), Thread.currentThread().getContextClassLoader());

            } catch (MalformedURLException e) {
                logger.error("Unable to setup classloader for: " + libs.getAbsolutePath());
            }
        } else {
            scriptClassLoader = new ScriptClassloader(new URL[0], Thread.currentThread().getContextClassLoader());
        }
        return scriptClassLoader;
    }

    protected ConfigObject buildConfig(String scriptGroupFolder, String scriptName, String environment, Set<String> configs) {
        Matcher fileNameMatcher = SCRIPT_FILE_PATTERN.matcher(scriptName);
        ConfigObject scriptConfig = null;
        if (fileNameMatcher.matches()) {

            if (configs == null) {
                configs = new LinkedHashSet<>();
            }

            String configFileName = scriptName;
            String scriptToRun;

            if (!scriptName.endsWith(CONFIG_SUFFIX)) {
                configFileName = fileNameMatcher.group(2) + CONFIG_SUFFIX;
                scriptToRun = scriptName;
            } else {
                scriptToRun = fileNameMatcher.group(2) + SCRIPT_SUFFIX;
            }

            if (configs.contains(configFileName)) return null; // we parsed it already

            File gConfigFile = new File(scriptGroupFolder + File.separator + configFileName);
            scriptConfig = fillParamMapFromGroovy(gConfigFile, getBindingBeans(), environment);
            if (logger.isInfoEnabled()) logger.info("File " + gConfigFile.getAbsolutePath() + " loaded.");
            configs.add(configFileName);
            if (scriptConfig != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parentConfig = (Map) scriptConfig.get(EXECUTOR_FIELD);

                if (parentConfig == null) {
                    parentConfig = new HashMap<>();
                    scriptConfig.put(EXECUTOR_FIELD, parentConfig);
                }

                if (!parentConfig.containsKey(SCRIPT_TO_RUN)) {
                    parentConfig.put(SCRIPT_TO_RUN, scriptToRun);
                }

                parentConfig.put(SCRIPT_CONFIG, scriptName);

                if (!StringUtils.isEmpty(parentConfig.get(INCLUDE_CONFIG_FIELD))) {
                    File fParentConfig = new File(scriptGroupFolder + File.separator + parentConfig.get(INCLUDE_CONFIG_FIELD));
                    if (fParentConfig.exists()) {
                        ConfigObject parentConfigObject = buildConfig(scriptGroupFolder, (String) parentConfig.get(INCLUDE_CONFIG_FIELD), environment, configs);
                        if (parentConfigObject != null) {
                            parentConfigObject.merge(scriptConfig);
                            if (logger.isInfoEnabled()) {
                                logger.info("File " + scriptGroupFolder + "/" + configFileName + " merged.");
                            }
                            return parentConfigObject;
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("File " + scriptGroupFolder + "/" + parentConfig + " doesn't exist.");
                        }
                    }
                }
            }
        }
        return scriptConfig;
    }

    private ConfigObject fillParamMapFromGroovy(File file, Map<String, Object> params, String environment) {
        if (!file.exists()) {
            if (logger.isInfoEnabled()) logger.info("File doesn't exist " + file.getAbsolutePath());
            return null;
        }
        try {
            ConfigSlurper configSlurper = new ConfigSlurper(environment);
            configSlurper.setBinding(params);
            return configSlurper.parse(file.toURI().toURL());
        } catch (MalformedURLException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Unable to load " + file.getAbsolutePath());
            }
        }
        return null;
    }

    private void fillParamMapFromProperties(File file, Map<String, Object> params) {
        Properties scriptProperties;
        if (!file.exists()) {
            if (logger.isWarnEnabled()) {
                logger.warn("File doesn't exist " + file.getAbsolutePath());
            }
            return;
        }
        try {
            scriptProperties = new Properties();
            scriptProperties.load(Files.newInputStream(Paths.get(file.toURI())));
            Enumeration e = scriptProperties.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = scriptProperties.getProperty(key);
                params.put(key, value);
            }
        } catch (IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Unable to load " + file.getAbsolutePath());
            }
        }
    }

    private Map<String, Object> getBindingBeans() {
        if (bindingBeans == null) {
            Map<String, Object> beans = GMPContext.getApplicationContext().getBeansWithAnnotation(ExportBinding.class);
            Map<String, Object> bindings = new HashMap<>();
            for (Map.Entry<String, Object> bean : beans.entrySet()) {
                ExportBinding annotationBinding = bean.getValue().getClass().getAnnotation(ExportBinding.class);
                bindings.put(annotationBinding.name().length() == 0 ? bean.getKey() : annotationBinding.name(), bean.getValue());
            }
            bindingBeans = bindings;
        }
        return bindingBeans;
    }

    private String configToString(ConfigObject cfg) {
        return "hidden";
        //todo return config with password sanitizing
        //cfg.toString();
        //FilteredJsonMapper.getInstance().map(true, cfg);
    }

    private class LibFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jar");
        }
    }
}
