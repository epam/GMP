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
import com.epam.gmp.ScriptClassloader;
import com.epam.gmp.ScriptContext;
import com.epam.gmp.ScriptInitializationException;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service("GroovyScriptEngineService")
@Scope(value = "singleton")
public class GroovyScriptEngineService implements IGroovyScriptEngineService {
    public static final String SCRIPTS_FOLDER = "scripts";
    public static final String GROOVY_ROOT_FOLDER = "groovyRoot";
    public static final String LIB_FOLDER = "lib";
    public static final String NULL_RESULT = ":null";
    private static final Logger logger = LoggerFactory.getLogger(GroovyScriptEngineService.class);
    private Map<String, GroovyScriptEngine> gseCache;

    @Resource(name = "gmpHome")
    private String gmpHome;

    @Resource(name = "ResultMap")
    private Map<String, Object> resultMap;

    private String groovyRoot;

    @PostConstruct
    private void init() {
        gseCache = new HashMap<>();
        groovyRoot = gmpHome + File.separator + SCRIPTS_FOLDER + File.separator + GROOVY_ROOT_FOLDER;
    }

    /**
     * @param rootFolder - script group root folder
     * @return GroovyScriptEngine instance
     * @throws ScriptInitializationException in case if it is not possible to load script
     */
    protected GroovyScriptEngine getEngine(String rootFolder) throws ScriptInitializationException {
        GroovyScriptEngine engine = gseCache.get(rootFolder);
        try {
            if (engine == null) {
                String[] roots = new String[]{groovyRoot, rootFolder};
                ClassLoader scriptClassLoader = buildClassloader(rootFolder);
                if (scriptClassLoader != null) {
                    engine = new GroovyScriptEngine(roots, scriptClassLoader);
                } else {
                    engine = new GroovyScriptEngine(roots, this.getClass().getClassLoader());
                }
                gseCache.put(rootFolder, engine);
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

    public Script createScript(String rootFolder, String scriptName, Binding binding) throws ScriptInitializationException {
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

    protected ClassLoader buildClassloader(String sPath) {
        File libs = new File(sPath + File.separator + LIB_FOLDER);
        ClassLoader scriptClassLoader = null;
        if (libs.exists()) {
            try {
                String[] jars = libs.list(new LibFilter());
                List<URL> urls = new ArrayList<>();
                for (String jar : jars) {
                    File jarFile = new File(libs.getAbsolutePath() + File.separator + jar);
                    urls.add(jarFile.toURI().toURL());
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

    protected void putResult(Object result, ScriptContext scriptContext) {
        if (resultMap != null) {
            if (scriptContext.getScriptId() != null) {
                if (result != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Internal cache put into: " + scriptContext.getScriptId());
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
    public void runScript(ScriptContext scriptContext) {
        try {
            // fix issue with jackson caching
            JsonMapper.getInstance().cleanCache();

            Script script = createScript(scriptContext);
            if (script != null) {
                Object result = script.run();
                if (result instanceof Map) {
                    putResult(Collections.unmodifiableMap((Map) result), scriptContext);
                } else {
                    putResult(result, scriptContext);
                }

                if (logger.isInfoEnabled()) {
                    if (result == null) {
                        logger.info("Script doesn't return anything, converting to " + NULL_RESULT);
                    } else {
                        logger.info(result.toString());
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Unable to run: " + scriptContext.getScriptName() + " message: " + e.getMessage());
                putResult(1, scriptContext);
            }
        }
    }

    private class LibFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jar");
        }
    }
}
