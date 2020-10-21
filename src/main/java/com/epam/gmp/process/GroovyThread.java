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

import com.epam.gmp.ScriptContext;
import com.epam.gmp.ScriptInitializationException;
import com.epam.gmp.ScriptResult;
import com.epam.gmp.service.IGroovyScriptEngineService;
import com.epam.gmp.service.ScriptContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@Component("GroovyThread")
@Scope(value = "prototype")
public class GroovyThread<T> implements IQueuedThread<T> {

    @Autowired
    private ScriptContextBuilder scriptContextBuilder;

    @Autowired
    private IGroovyScriptEngineService groovyScriptEngineService;

    private ScriptContext scriptContext;
    private String resultKey;


    private String scriptPath;
    private Map<String, Object> params = new HashMap<>();

    public GroovyThread(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public GroovyThread(String scriptPath, List<String> params) {
        this.scriptPath = scriptPath;
        this.params.put("cmdLine", params);
    }

    public GroovyThread(String scriptPath, Map<String, Object> params) {
        this.scriptPath = scriptPath;
        this.params = params;
    }

    @PostConstruct
    public void init() throws ScriptInitializationException {
        this.scriptContext = scriptContextBuilder.buildContextFor(scriptPath, params);
        this.resultKey = scriptContext.getScriptId();
    }

    public ScriptResult<T> call() {
        return groovyScriptEngineService.runScript(scriptContext);
    }

    public String getKey() {
        return resultKey;
    }
}
