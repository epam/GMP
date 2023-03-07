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

import com.epam.gmp.ScriptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

import static com.epam.gmp.service.GroovyScriptEngineService.NULL_RESULT;

@Service("ExitCodeCalculator")
public class ExitCodeCalculator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Resource(name = "ResultMap")
    private Map<String, Object> resultCache;

    public Integer calculate(Integer initialCode) {
        Integer resultCode = initialCode;
        for (Map.Entry<String, Object> entry : resultCache.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (logger.isInfoEnabled()) logger.info(" Script: {} result={}", key, value);
            if (value != NULL_RESULT) {
                if (value instanceof Integer) {
                    resultCode += (Integer) value;
                } else if (value instanceof ScriptResult && ((ScriptResult<?>) value).getResult() instanceof Integer) {
                    resultCode += (Integer) ((ScriptResult<?>) value).getResult();
                }
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unique scripts executed: {}", resultCache.size());
            logger.info("Initial/Total = {}/{} ", initialCode, resultCode);
        }
        return resultCode;
    }
}
