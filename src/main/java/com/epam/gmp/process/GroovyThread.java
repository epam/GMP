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

import com.epam.dep.esp.common.json.JsonMapper;
import com.epam.gmp.ScriptContext;
import com.epam.gmp.ScriptContextException;
import com.epam.gmp.service.ScriptContextBuilder;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@Component("GroovyThread")
@Scope(value = "prototype")
public class GroovyThread implements IQueuedThread {

    public static final String NULL_RESULT = ":null";
    private static int execCounter = 0;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired
    private ScriptContextBuilder scriptContextBuilder;
    private ScriptContext scriptContext;
    private String resultKey;

    @Resource(name = "ResultMap")
    private Map<String, Object> resultMap;
    private String[] roots;
    private String scriptPath;
    private List<String> cmdLineParams;

    public GroovyThread(String scriptPath) {
        this.scriptPath = scriptPath;
        this.cmdLineParams = Collections.EMPTY_LIST;
    }

    public GroovyThread(String scriptPath, List<String> cmdLineParams) {
        this.scriptPath = scriptPath;
        this.cmdLineParams = cmdLineParams;
    }

    @PostConstruct
    public void init() throws ScriptContextException {
        this.scriptContext = scriptContextBuilder.buildContextFor(scriptPath, cmdLineParams);
        this.resultKey = scriptContext.getScriptId();
        this.roots = new String[]{scriptContext.getRoot()};
    }

    private void putResult(Object result) {
        if (resultMap != null) {
            if (resultKey != null) {
                if (result != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Internal cache put into: " + resultKey);
                    }
                    resultMap.put(resultKey, result);
                } else {
                    //logger.info("Internal cache delete from: " + resultKey);
                    //resultMap.remove(resultKey);
                    resultMap.put(resultKey, NULL_RESULT);
                }
            } else {
                logger.warn("Result key should not be NULL_RESULT!");
            }
        } else logger.warn("Result storage should not be NULL_RESULT!");
    }

    public void run() {
        try {
            // fix issue with jakson caching
            JsonMapper.getInstance().cleanCache();

            GroovyScriptEngine gse;
            if (scriptContext.getClassLoader() != null) {
                gse = new GroovyScriptEngine(roots, scriptContext.getClassLoader());
            } else {
                gse = new GroovyScriptEngine(roots, this.getClass().getClassLoader());
            }

            Binding binding = new Binding();

            for (Map.Entry<String, Object> entry : scriptContext.getParams().entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }


            Script script = gse.createScript(scriptContext.getScriptName(), binding);
            try {
                Object result = script.run();
                execCounter++;
                if (result instanceof Map) {
                    putResult(Collections.unmodifiableMap((Map) result));
                } else {
                    putResult(result);
                }

                if (logger.isInfoEnabled()) {
                    if (result == null) {
                        logger.info("Script doesn't return anything, converting to " + NULL_RESULT);
                    } else {
                        logger.info(result.toString());
                    }
                }
            } finally {
                InvokerHelper.removeClass(script.getClass());
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("ERROR!!!! " + execCounter);
                logger.error("Unable to run: " + scriptContext.getScriptName() + " message: " + e.getMessage());
                putResult(1);
            }
        }
    }

    public String getKey() {
        return resultKey;
    }
}
