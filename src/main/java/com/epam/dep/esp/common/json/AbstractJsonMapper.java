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

package com.epam.dep.esp.common.json;

import com.epam.dep.esp.common.AbstractObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractJsonMapper extends AbstractObjectMapper<String, Object> {
    protected abstract ObjectMapper getObjectMapper();

    @Override
    public String map(boolean pretty, boolean printClassName, Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return (printClassName ? (obj.getClass().getSimpleName() + ":") : "") + (pretty ?
                    getObjectMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(obj) :
                    getObjectMapper().writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
        }
    }
}
