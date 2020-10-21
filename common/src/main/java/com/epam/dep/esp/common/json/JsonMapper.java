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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

public class JsonMapper extends AbstractJsonMapper {
    private static final ThreadLocal<ObjectMapper> objectMapperThreadLocal = ThreadLocal.withInitial(() -> new ObjectMapper());

    private static JsonMapper serializer = new JsonMapper();

    private JsonMapper() {
    }

    public static JsonMapper getInstance() {
        return serializer;
    }

    @Override
    protected ObjectMapper getObjectMapper() {
        return objectMapperThreadLocal.get();
    }

    public void cleanCache() {
        SerializerProvider serializerProvider = getObjectMapper().getSerializerProvider();
        if (serializerProvider instanceof DefaultSerializerProvider) {
            ((DefaultSerializerProvider) serializerProvider).flushCachedSerializers();
        }
    }
}
