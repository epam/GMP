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
import com.epam.gmp.ScriptExecutionException;
import com.epam.gmp.ScriptInitializationException;
import com.epam.gmp.ScriptResult;
import groovy.lang.Binding;
import groovy.lang.Script;
import org.springframework.core.io.Resource;

public interface IGroovyScriptEngineService {
    Script createScript(ScriptContext scriptContext) throws ScriptInitializationException;

    Script createScript(Resource rootFolder, String scriptName, Binding binding) throws ScriptInitializationException;

    <R> ScriptResult<R> runScript(ScriptContext scriptContext) throws ScriptExecutionException;
}
