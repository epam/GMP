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

package com.epam.gmp.service.yaml;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;


@Component
@Scope(value = "prototype")
public class YamlLoaderContext {

    @Resource(name = "ExportBindings")
    private Map<String, Object> bindingBeans;

    private Map<String, Object> params = new HashMap<>();

    public Map<String, Object> getParams() {
        return params;
    }

    public Object putParam(String key, Object value) {
        return params.put(key, value);
    }

    public void putParams(Map<String, Object> params) {
        this.params.putAll(params);
    }

    public YamlLoaderContext(Map<String, Object> params) {
        this.putParams(params);
    }

    public YamlLoaderContext(String key, Object value) {
        this.putParam(key, value);
    }

    public Map<String, Object> getBindingBeans() {
        return bindingBeans;
    }
}
