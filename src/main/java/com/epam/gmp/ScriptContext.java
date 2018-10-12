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

import com.epam.dep.esp.common.json.JsonMapper;
import org.springframework.core.io.Resource;

import java.util.Map;

public class ScriptContext {
    private Map<String, Object> params;
    private Resource root;
    private String scriptName;
    private String scriptId;

    public ScriptContext(String scriptId, Map<String, Object> params, Resource root, String scriptName) {
        this.scriptId = scriptId;
        this.params = params;
        this.root = root;
        this.scriptName = scriptName;
    }

    public String getScriptId() {
        return scriptId;
    }

    public Resource getRoot() {
        return root;
    }

    public String getScriptName() {
        return scriptName;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
