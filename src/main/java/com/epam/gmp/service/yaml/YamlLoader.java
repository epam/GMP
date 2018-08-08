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

import com.epam.gmp.ScriptInitializationException;
import com.epam.gmp.service.GMPContext;
import com.epam.gmp.service.GroovyScriptEngineService;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YamlLoader {
    private static final Logger logger = LoggerFactory.getLogger(YamlLoader.class);
    @Autowired
    private List<IYamlPreProcessor> yamlPreProcessors;

    @Autowired
    private List<IYamlPreLoader> yamlPreLoaders;

    //TODO use real cahe ?
    Map<String, YamlFreemarkerEngine> yamlEngineCache;

    @PostConstruct
    private void init() {
        yamlEngineCache = new HashMap<>();
    }

    @Autowired
    GroovyScriptEngineService groovyScriptEngineService;

    public Map<String, Object> getObject(Map<String, Object> params, String baseFolder, String yamlFileName) {
        Map<String, Object> result = Collections.emptyMap();

        YamlFreemarkerEngine yamlFreemarkerEngine = yamlEngineCache.computeIfAbsent(baseFolder,
                (key -> {
                    try {
                        return new YamlFreemarkerEngine(groovyScriptEngineService.getEngine(baseFolder).getParentClassLoader(), "", baseFolder);
                    } catch (ScriptInitializationException | IOException e) {
                        logger.error("Unable to create YamlFreemarkerEngine for {} ", baseFolder);
                    }
                    return null;
                })
        );

        if (yamlFreemarkerEngine != null) {
            try {
                YamlLoaderContext context = GMPContext.getApplicationContext().getBean(YamlLoaderContext.class, params);

                for (IYamlPreLoader preLoader : yamlPreLoaders) {
                    preLoader.preload(context);
                }

                String yamlStr = yamlFreemarkerEngine.loadYaml(yamlFileName, context);
                for (IYamlPreProcessor preProcessor : yamlPreProcessors) {
                    yamlStr = preProcessor.process(yamlStr, context);
                }

                YamlMapFactoryBean factory = new YamlMapFactoryBean();
                factory.setResources(new ByteArrayResource(yamlStr.getBytes()));
                result = factory.getObject();
            } catch (TemplateException | IOException e) {
                logger.error("Unable to load " + yamlFileName, e);
            }
        }
        return result;
    }
}
