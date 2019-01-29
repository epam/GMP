/*
 *  /***************************************************************************
 *  Copyright (c) 2019, EPAM SYSTEMS INC
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

import com.google.common.base.Objects;
import groovy.lang.Script;
import org.springframework.core.io.Resource;

public class ConfigLayer {
    private Resource root;
    private String scriptName;
    private Script script;

    private String scriptToRun;
    private String scriptConfig;

    public Resource getRoot() {
        return root;
    }

    public String getScriptName() {
        return scriptName;
    }

    public ConfigLayer(Resource root, String scriptName) {
        this.root = root;
        this.scriptName = scriptName;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public String getScriptToRun() {
        return scriptToRun;
    }

    public void setScriptToRun(String scriptToRun) {
        this.scriptToRun = scriptToRun;
    }

    public String getScriptConfig() {
        return scriptConfig;
    }

    public void setScriptConfig(String scriptConfig) {
        this.scriptConfig = scriptConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigLayer that = (ConfigLayer) o;
        return Objects.equal(root, that.root) &&
                Objects.equal(scriptName, that.scriptName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root, scriptName);
    }
}
