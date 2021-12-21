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

import com.epam.dep.esp.common.json.filter.PasswordFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class FilteredJsonMapper extends AbstractJsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static SimpleFilterProvider filters = new SimpleFilterProvider().addFilter(PasswordFilter.PASSWORD, new PasswordFilter());
    private static FilteredJsonMapper serializer = new FilteredJsonMapper();

    static {
        filters.setFailOnUnknownId(false);
        mapper.setFilterProvider(filters);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public Object findFilterId(Annotated a) {
                if (Object.class.isAssignableFrom(a.getRawType())) {
                    return PasswordFilter.PASSWORD;
                }
                return super.findFilterId(a);
            }
        });
    }

    private FilteredJsonMapper() {
    }

    public static FilteredJsonMapper getInstance() {
        return serializer;
    }

    @Override
    protected ObjectMapper getObjectMapper() {
        return mapper;
    }
}
