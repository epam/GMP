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

import java.util.Map;

public class ScriptContext {
    private ClassLoader classLoader;
    private Map<String, Object> params;
    private String root;
    private String scriptName;
    private String scriptId;

    public ScriptContext(String scriptId, ClassLoader classLoader, Map<String, Object> params, String root, String scriptName) {
        this.scriptId = scriptId;
        this.classLoader = classLoader;
        this.params = params;
        this.root = root;
        this.scriptName = scriptName;
    }

    public String getScriptId() {
        return scriptId;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getRoot() {
        return root;
    }

    public String getScriptName() {
        return scriptName;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
