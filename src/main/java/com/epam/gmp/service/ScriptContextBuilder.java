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

import com.epam.gmp.ScriptContext;
import com.epam.gmp.ScriptContextException;
import com.epam.gmp.ScriptInitializationException;
import com.epam.gmp.service.yaml.YamlLoader;
import groovy.lang.Binding;
import groovy.lang.Script;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ScriptContextBuilder")
@Scope(value = "singleton")
public class ScriptContextBuilder {
    public static final String CONFIG_SUFFIX = ".config.groovy";
    public static final String SCRIPT_SUFFIX = ".groovy";
    public static final String INCLUDE_CONFIG_FIELD = "includeConfig";
    public static final String INCLUDE_YAML_FIELD = "includeYaml";
    public static final String SCRIPT_CONFIG = "@scriptConfig";
    public static final String SCRIPT_TO_RUN = "script";
    public static final String EXECUTOR_FIELD = "EXECUTOR";
    private static final Logger logger = LoggerFactory.getLogger(ScriptContextBuilder.class);
    private static final String COMMON_CONFIG = "common-config.groovy";
    private static final String GLOBAL_CONFIG = "global-config.groovy";
    private static final String SCRIPTS_FOLDER = "scripts";
    public static final Pattern SCRIPT_PATTERN = Pattern.compile("([^@]*)[@](([^/]*)/((.*)[.]groovy))");
    public static final Pattern SCRIPT_FILE_PATTERN = Pattern.compile("(([^.]*)([.]config)?([.]groovy))");

    @Resource(name = "ExportBindings")
    private Map<String, Object> bindingBeans;

    //Because of @CconfigSlurper memory leaks we need to minimize config loading
    private Map<Class, Map<String, ConfigObject>> configCache = new ConcurrentHashMap<>();

    @Resource(name = "gmpHome")
    private String gmpHome;

    @Autowired
    private IGroovyScriptEngineService groovyScriptEngineService;

    @Autowired
    private YamlLoader yamlLoader;

    public ScriptContext buildContextFor(String scriptPath, List<String> cmdLineParams) {
        Matcher pathMatcher = SCRIPT_PATTERN.matcher(scriptPath);

        if (!pathMatcher.matches()) {
            throw new ScriptContextException("Unable to build context for: " + scriptPath);
        }

        String scriptGroupFolder = gmpHome + File.separator + SCRIPTS_FOLDER + File.separator + pathMatcher.group(3);
        String scriptName = pathMatcher.group(4);
        String environment = pathMatcher.group(1);
        File fScriptGroupFolder = new File(scriptGroupFolder);

        if (!fScriptGroupFolder.exists()) {
            throw new ScriptContextException("Script group folder doesn't exist: " + fScriptGroupFolder.getAbsolutePath());
        }

        try {
            ConfigObject groovyConfig = new ConfigObject();
            //Global config
            Script globalConfigScript = groovyScriptEngineService.createScript(scriptGroupFolder, GLOBAL_CONFIG, new Binding(bindingBeans));
            ConfigObject globalConfig = fillParamMapFromGroovy(globalConfigScript, environment, bindingBeans);
            if (globalConfig != null) {
                groovyConfig.merge(globalConfig);
            }

            //Common config
            Script commonConfigScript = groovyScriptEngineService.createScript(scriptGroupFolder, COMMON_CONFIG, new Binding(bindingBeans));
            ConfigObject commonConfig = fillParamMapFromGroovy(commonConfigScript, environment, bindingBeans);
            if (commonConfig != null) {
                groovyConfig.merge(commonConfig);
            }

            //Script config
            ConfigObject scriptConfig = buildConfig(scriptGroupFolder, scriptName, environment, null, null);
            if (scriptConfig != null) {
                groovyConfig.merge(scriptConfig);
            }

            scriptName = ((Map) groovyConfig.get(EXECUTOR_FIELD)).get(SCRIPT_TO_RUN).toString();
            groovyConfig.remove(EXECUTOR_FIELD);

            //ADD logger
            HashMap<String, Object> paramMap = new HashMap<>();
            Logger scriptLogger = LoggerFactory.getLogger(scriptName.replaceAll("[.]", "_"));
            paramMap.put("logger", scriptLogger);

            paramMap.put("gConfig", groovyConfig);
            paramMap.put("cmdLine", cmdLineParams);
            if (logger.isInfoEnabled()) {
                logger.info("Groovy based script configuration:\n" + scriptName + ":" + configToString(groovyConfig));
            }
            return new ScriptContext(scriptPath, paramMap, scriptGroupFolder, scriptName);

        } catch (ScriptInitializationException e) {
            throw new ScriptContextException("Unable to build context for: " + scriptPath);
        }
    }


    /**
     * @param scriptGroupFolder  - group folder
     * @param scriptName         - script name
     * @param environment        - environment key
     * @param configs            - set if already applied configs
     * @param additionalBindings - additional buinding for inscript substitution
     * @return ConfigObject for a given script
     * @throws ScriptInitializationException in case of error
     */
    protected ConfigObject buildConfig(String scriptGroupFolder, String scriptName, String environment, Set<String> configs, Map additionalBindings) throws ScriptInitializationException {
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

            // Skip parsing if current config already parsed
            if (configs.contains(configFileName)) return null;
            Map<String, Object> bindingMap = new LinkedHashMap<>();
            bindingMap.putAll(bindingBeans);

            if (additionalBindings != null) {
                bindingMap.putAll(additionalBindings);
            }
            Binding bindings = new Binding(bindingMap);

            Script configScript = groovyScriptEngineService.createScript(scriptGroupFolder, configFileName, bindings);

            if (configScript != null) {
                scriptConfig = fillParamMapFromGroovy(configScript, environment, bindingMap);
                if (logger.isInfoEnabled()) logger.info("Load: {}", configFileName);
                configs.add(configFileName);

                @SuppressWarnings("unchecked")
                Map<String, Object> parentConfig = (Map<String, Object>) scriptConfig.computeIfAbsent(EXECUTOR_FIELD, (key -> new HashMap<String, Object>()));
                parentConfig.putIfAbsent(SCRIPT_TO_RUN, scriptToRun);
                parentConfig.put(SCRIPT_CONFIG, scriptName);
                GmpConfigObject yamlCfg = null;
                String yamlToInclude = null;

                ConfigObject parentConfigObject = null;
                if (!StringUtils.isEmpty(parentConfig.get(INCLUDE_CONFIG_FIELD))) {
                    File fParentConfig = new File(scriptGroupFolder + File.separator + parentConfig.get(INCLUDE_CONFIG_FIELD));
                    if (fParentConfig.exists()) {
                        parentConfigObject = buildConfig(scriptGroupFolder, (String) parentConfig.get(INCLUDE_CONFIG_FIELD), environment, configs, scriptConfig);

                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("File {} doesn't exist.", parentConfig);
                        }
                    }
                }

                if (!StringUtils.isEmpty(parentConfig.get(INCLUDE_YAML_FIELD))) {
                    yamlToInclude = parentConfig.get(INCLUDE_YAML_FIELD).toString();
                }

                if (parentConfigObject != null) {
                    parentConfigObject.merge(scriptConfig);
                    scriptConfig = parentConfigObject;
                    if (logger.isInfoEnabled()) {
                        logger.info("Merge: {}", configFileName);
                    }
                }

                if (!StringUtils.isEmpty(yamlToInclude)) {
                    Map<String, Object> yamlObject = yamlLoader.getObject(scriptConfig, scriptGroupFolder, yamlToInclude);
                    yamlCfg = new GmpConfigObject();
                    yamlCfg.mapMerge(yamlObject);
                    yamlCfg.mapMerge(scriptConfig);
                    scriptConfig = yamlCfg;
                }
            }
        }
        return scriptConfig;
    }

    private ConfigObject fillParamMapFromGroovy(Script cfgScript, String environment, Map bindings) {
        if (cfgScript == null) return null;

        Map<String, ConfigObject> scriptConfigs = configCache.computeIfAbsent(cfgScript.getClass(), key -> new ConcurrentHashMap<String, ConfigObject>());
        ConfigObject configForEnvironment = scriptConfigs.computeIfAbsent(environment, key -> {
            ConfigSlurper configSlurper = new ConfigSlurper(key);
            configSlurper.setBinding(bindings);
            return configSlurper.parse(cfgScript);
        });
        return copyConfigObject(configForEnvironment);
    }

    /**
     * @param origin - ConfigObject to clone
     * @return - deep copy of config object
     */
    private ConfigObject copyConfigObject(ConfigObject origin) {
        ConfigObject copy = new ConfigObject();
        origin.forEach((key, value) -> {
            if (value instanceof ConfigObject) {
                value = copyConfigObject((ConfigObject) value);
            }
            copy.put(key, value);
        });
        return copy;
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

    private String configToString(ConfigObject cfg) {
        if (logger.isDebugEnabled()) {
            return cfg.toString();
        } else {
            return "Hidden, enable DEBUG level for component to see the data.";
        }
    }

    class GmpConfigObject extends ConfigObject {
        public Map mapMerge(Map other) {
            return doMerge(this, other);
        }

        private Map doMerge(Map config, Map other) {
            for (Object o : other.entrySet()) {
                Map.Entry next = (Map.Entry) o;
                Object key = next.getKey();
                Object value = next.getValue();

                Object configEntry = config.get(key);

                if (configEntry == null) {
                    config.put(key, value);

                    continue;
                } else {
                    if (configEntry instanceof Map && !((Map) configEntry).isEmpty() && value instanceof Map) {
                        // recur
                        doMerge((Map) configEntry, (Map) value);
                    } else {
                        config.put(key, value);
                    }
                }
            }
            return config;
        }
    }
}
