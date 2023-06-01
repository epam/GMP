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

import com.epam.gmp.GmpResourceUtils;
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
    public static final String G_ENV = "gEnv";

    @Resource(name = "ExportBindings")
    private Map<String, Object> bindingBeans;

    //Because of @CconfigSlurper memory leaks we need to minimize config loading
    private Map<Class, Map<String, ConfigObject>> configCache = new ConcurrentHashMap<>();

    @Resource(name = "gmpHomeResource")
    private org.springframework.core.io.Resource gmpHome;

    @Autowired
    private IGroovyScriptEngineService groovyScriptEngineService;

    @Autowired
    private YamlLoader yamlLoader;

    public ScriptContext buildContextFor(String scriptPath, Map<String, Object> params) throws ScriptInitializationException {
        Matcher pathMatcher = SCRIPT_PATTERN.matcher(scriptPath);

        if (!pathMatcher.matches()) {
            throw new ScriptContextException("Unable to build context for: " + scriptPath);
        }
        org.springframework.core.io.Resource scriptGroupPath = GmpResourceUtils.getRelativeResource(gmpHome, "/" + SCRIPTS_FOLDER + "/" + pathMatcher.group(3) + "/");

        String scriptName = pathMatcher.group(4);
        String environment = pathMatcher.group(1);


        if (!scriptGroupPath.exists()) {
            throw new ScriptContextException("Script group folder doesn't exist: " + scriptGroupPath);
        }

        try {

            ConfigStackBuilder confBuilder = new ConfigStackBuilder();

            //Global config
            confBuilder.addLayer(new ConfigLayer(scriptGroupPath, GLOBAL_CONFIG));
            //Common config
            confBuilder.addLayer(new ConfigLayer(scriptGroupPath, COMMON_CONFIG));
            //Script config
            confBuilder.addLayer(new ConfigLayer(scriptGroupPath, scriptName));
            Map<String, Object> scriptConfigParameters = new HashMap<>();
            scriptConfigParameters.put(G_ENV, environment);
            ConfigObject scriptConfig = buildConfig(confBuilder.build(), environment, scriptConfigParameters);

            scriptName = ((Map) scriptConfig.get(EXECUTOR_FIELD)).get(SCRIPT_TO_RUN).toString();
            scriptConfig.remove(EXECUTOR_FIELD);

            //ADD logger
            HashMap<String, Object> paramMap = new HashMap<>();
            Logger scriptLogger = LoggerFactory.getLogger(scriptName.replaceAll("[.]", "_"));
            if (params != null) {
                paramMap.putAll(params);
            }
            paramMap.computeIfAbsent("cmdLine", (key -> Collections.emptyList()));
            paramMap.put("logger", scriptLogger);
            paramMap.put("gConfig", scriptConfig);
            paramMap.put(G_ENV, environment);

            if (logger.isInfoEnabled()) {
                logger.info("Groovy based script configuration:\n" + scriptName + ":" + configToString(scriptConfig));
            }
            return new ScriptContext(scriptPath, paramMap, scriptGroupPath, scriptName);

        } catch (ScriptInitializationException e) {
            throw new ScriptContextException("Unable to build context for: " + scriptPath, e);
        }
    }

    private void updateWithDefaultExecutor(ConfigObject scriptConfig, ConfigLayer layer) {
        @SuppressWarnings("unchecked")
        Map<String, Object> executorConfig = (Map<String, Object>) scriptConfig.computeIfAbsent(EXECUTOR_FIELD, (key -> new HashMap<String, Object>()));
        executorConfig.putIfAbsent(SCRIPT_TO_RUN, layer.getScriptToRun());
        executorConfig.put(SCRIPT_CONFIG, layer.getScriptConfig());
    }

    protected ConfigObject preProcessConfig(Deque<ConfigLayer> configStack, ConfigLayer layer, String environment, Map<String, Object> bindingMap) {

        ConfigObject scriptConfig;

        if (layer.getScript() != null) {
            scriptConfig = fillParamMapFromGroovy(layer.getScript(), environment, bindingMap);
            updateWithDefaultExecutor(scriptConfig, layer);
            Map<String, Object> executorConfig = (Map<String, Object>) scriptConfig.get(EXECUTOR_FIELD);

            GmpConfigObject yamlCfg;
            String yamlToInclude = null;

            if (!StringUtils.isEmpty(executorConfig.get(INCLUDE_CONFIG_FIELD))) {
                configStack.addFirst(new ConfigLayer(layer.getRoot(), executorConfig.get(INCLUDE_CONFIG_FIELD).toString()));
            }

            if (!StringUtils.isEmpty(executorConfig.get(INCLUDE_YAML_FIELD))) {
                yamlToInclude = executorConfig.get(INCLUDE_YAML_FIELD).toString();
            }

            if (!StringUtils.isEmpty(yamlToInclude)) {
                Map<String, Object> yamlObject = yamlLoader.getObject(scriptConfig, layer.getRoot(), yamlToInclude);
                yamlCfg = new GmpConfigObject();
                yamlCfg.mapMerge(yamlObject);
                yamlCfg.mapMerge(scriptConfig);
                scriptConfig = yamlCfg;
            }
        } else {
            scriptConfig = new ConfigObject();
            updateWithDefaultExecutor(scriptConfig, layer);
        }
        return scriptConfig;
    }


    protected ConfigObject assembleConfigForLayer(Deque<ConfigLayer> configStack, ConfigLayer layer, String environment, Map<String, Object> additionalBindings) throws ScriptInitializationException {
        Matcher fileNameMatcher = SCRIPT_FILE_PATTERN.matcher(layer.getScriptName());
        ConfigObject cfgObj = null;

        if (fileNameMatcher.matches()) {
            if (COMMON_CONFIG.equals(layer.getScriptName()) || GLOBAL_CONFIG.equals(layer.getScriptName())) {
                layer.setScriptConfig(layer.getScriptName());
                layer.setScriptToRun(null);
            } else {
                if (fileNameMatcher.group(3) == null) {
                    layer.setScriptConfig(fileNameMatcher.group(2) + CONFIG_SUFFIX);
                    layer.setScriptToRun(layer.getScriptName());

                } else {
                    layer.setScriptConfig(layer.getScriptName());
                    layer.setScriptToRun(fileNameMatcher.group(2) + SCRIPT_SUFFIX);
                }
            }
            layer.setScript(groovyScriptEngineService.createScript(layer.getRoot(), layer.getScriptConfig(), new Binding(bindingBeans)));

            Map<String, Object> bindingMap = new LinkedHashMap<>(bindingBeans);

            if (additionalBindings != null) {
                bindingMap.putAll(additionalBindings);
            }
            cfgObj = preProcessConfig(configStack, layer, environment, bindingMap);
        }
        return cfgObj;
    }


    protected ConfigObject buildConfig(Deque<ConfigLayer> configStack, String environment, Map additionalBindings) throws ScriptInitializationException {
        Set<String> processedConfigs = new HashSet<>();
        Deque<ConfigObject> cfgObjStack = new LinkedList<>();
        ConfigObject resultConfig = new ConfigObject();

        while (!configStack.isEmpty()) {
            ConfigLayer cfgLayer = configStack.removeFirst();
            // Skip parsing if current config already parsed
            String curConfigPath = cfgLayer.getRoot().toString() + cfgLayer.getScriptName();
            if (processedConfigs.contains(curConfigPath)) continue;
            processedConfigs.add(curConfigPath);

            ConfigObject cfgObj = assembleConfigForLayer(configStack, cfgLayer, environment, additionalBindings);
            if (cfgObj != null) cfgObjStack.addFirst(cfgObj);
        }

        do {
            ConfigObject cfgItem = cfgObjStack.removeFirst();
            resultConfig.merge(cfgItem);
        } while (!cfgObjStack.isEmpty());
        return resultConfig;
    }

    private ConfigObject fillParamMapFromGroovy(Script cfgScript, String environment, Map bindings) {
        if (cfgScript == null) return null;

        Map<String, ConfigObject> scriptConfigs = configCache.computeIfAbsent(cfgScript.getClass(), key -> new ConcurrentHashMap<>());
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
                } else {
                    if (configEntry instanceof Map && !((Map) configEntry).isEmpty() && value instanceof Map) {
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
