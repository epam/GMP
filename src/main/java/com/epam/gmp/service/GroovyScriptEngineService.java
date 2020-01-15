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

import com.epam.dep.esp.common.json.JsonMapper;
import com.epam.gmp.*;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("GroovyScriptEngineService")
@Scope(value = "singleton")
//Should support multithreading
public class GroovyScriptEngineService implements IGroovyScriptEngineService {
    public static final String SCRIPTS_FOLDER = "scripts";
    public static final String GROOVY_ROOT_FOLDER = "groovyRoot";
    public static final String LIB_FOLDER = "lib";
    public static final String NULL_RESULT = ":null";

    private static final Logger logger = LoggerFactory.getLogger(GroovyScriptEngineService.class);
    private Map<String, GroovyScriptEngine> gseCache;

    @Resource(name = "gmpHomeResource")
    private org.springframework.core.io.Resource gmpHomeResource;

    @Resource(name = "ResultMap")
    private Map<String, Object> resultMap;

    private org.springframework.core.io.Resource groovyRoot;

    @PostConstruct
    private void init() {
        gseCache = new ConcurrentHashMap<>();
        try {
            groovyRoot = GmpResourceUtils.getRelativeResource(gmpHomeResource, "/" + SCRIPTS_FOLDER + "/" + GROOVY_ROOT_FOLDER + "/");
        } catch (ScriptInitializationException e) {
            //TODO
            groovyRoot = null;
        }
    }

    /**
     * @param rootFolder - script group root folder
     * @return GroovyScriptEngine instance
     * @throws ScriptInitializationException in case if it is not possible to load script
     */
    public GroovyScriptEngine getEngine(org.springframework.core.io.Resource rootFolder) throws ScriptInitializationException {
        GroovyScriptEngine engine = gseCache.get(rootFolder.toString());
        try {
            if (engine == null) {
                URL[] roots = new URL[]{groovyRoot.exists() ? groovyRoot.getURL() : gmpHomeResource.getURL(), rootFolder.getURL()};
                ClassLoader scriptClassLoader = buildClassloader(rootFolder);
                if (scriptClassLoader != null) {
                    engine = new GroovyScriptEngine(roots, scriptClassLoader);
                } else {
                    engine = new GroovyScriptEngine(roots, this.getClass().getClassLoader());
                }
                gseCache.put(rootFolder.toString(), engine);
            }
            return engine;
        } catch (IOException e) {
            throw new ScriptInitializationException("Unable to initialize groovy root at: " + rootFolder, e);
        }
    }

    @Override
    public Script createScript(ScriptContext scriptContext) throws ScriptInitializationException {
        return createScript(scriptContext.getRoot(), scriptContext.getScriptName(), new Binding(scriptContext.getParams()));
    }

    public Script createScript(org.springframework.core.io.Resource rootFolder, String scriptName, Binding binding) throws ScriptInitializationException {
        GroovyScriptEngine gse = getEngine(rootFolder);
        try {
            return gse.createScript(scriptName, binding);
        } catch (ResourceException e) {
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("File not found for script: %s/%s ", rootFolder, scriptName));
            }
            return null;
        } catch (ScriptException e) {
            throw new ScriptInitializationException("Unable to initialize groovy script: " + scriptName, e);
        }
    }


    protected ClassLoader buildClassloader(org.springframework.core.io.Resource scriptPath) {
        ClassLoader scriptClassLoader = null;

        org.springframework.core.io.Resource libPath = null;
        try {
            libPath = scriptPath.createRelative(LIB_FOLDER);
            if (libPath.exists()) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                org.springframework.core.io.Resource[] jarResources = resolver.getResources(libPath.getURL().toString() + "/*.jar");
                List<URL> urls = new ArrayList<>();
                for (org.springframework.core.io.Resource resource : jarResources) {
                    try {
                        urls.add(resource.getURL());
                    } catch (IOException ex) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unable to resolve url for " + resource.toString(), ex);
                        }
                    }
                }
                URL[] aUrls = new URL[urls.size()];
                scriptClassLoader = new ScriptClassloader(urls.toArray(aUrls), Thread.currentThread().getContextClassLoader());
            } else {
                scriptClassLoader = new ScriptClassloader(new URL[0], Thread.currentThread().getContextClassLoader());
            }

        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("unable to create relative LIB path.");
            }
        }
        return scriptClassLoader;
    }

    protected <T> void putResult(T result, ScriptContext scriptContext) {
        if (resultMap != null) {
            if (scriptContext.getScriptId() != null) {
                if (result != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Internal cache put into: {}", scriptContext.getScriptId());
                    }
                    resultMap.put(scriptContext.getScriptId(), result);
                } else {
                    //logger.info("Internal cache delete from: " + resultKey);
                    //resultMap.remove(resultKey);
                    resultMap.put(scriptContext.getScriptId(), NULL_RESULT);
                }
            } else {
                logger.warn("Result key should not be NULL_RESULT!");
            }
        } else logger.warn("Result storage should not be NULL_RESULT!");
    }

    @Override
    public <R> ScriptResult<R> runScript(ScriptContext scriptContext) {
        try {
            // fix issue with jackson caching
            JsonMapper.getInstance().cleanCache();
            Script script = createScript(scriptContext);
            if (script != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Start script: <{}>", scriptContext.getScriptId());
                }
                Object scriptResult = script.run();
                ScriptResult<R> result;

                if (logger.isInfoEnabled()) {
                    logger.info("Script finished: <{}>={}", scriptContext.getScriptId(), scriptResult == null ? NULL_RESULT : scriptResult.toString());
                }
                if (scriptResult instanceof ScriptResult) {
                    result = (ScriptResult) scriptResult;

                } else {
                    result = new ScriptResult(scriptResult);
                }
                putResult(result, scriptContext);
                return result;
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Unable to run: {}  message: {}", scriptContext.getScriptName(), e.getMessage());
                putResult(1, scriptContext);
            }
        }
        return null;
    }
}
