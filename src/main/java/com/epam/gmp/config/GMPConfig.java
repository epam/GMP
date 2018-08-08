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

package com.epam.gmp.config;

import com.epam.gmp.ExportBinding;
import com.epam.gmp.ScriptContextException;
import com.epam.gmp.service.GMPContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ComponentScan({"com.epam.gmp.service", "com.epam.gmp.process", "com.epam.gmp.config.custom"})
@EnableAspectJAutoProxy

public class GMPConfig {
    private static final Logger logger = LoggerFactory.getLogger(GMPConfig.class);
    private static final String GMP_PROPERTIES = "gmp.properties";

    @Autowired
    private GMPContext gmpContext;

    @Bean(name = "gmpHome")
    public static String gmpHome() {
        return System.getProperty("gmp.home");
    }

    @Bean(name = "GMPProperties")
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        Resource[] resources;
        String pesHome = gmpHome();
        if (pesHome != null) {
            File gmpProperties = new File(pesHome + File.pathSeparator + GMP_PROPERTIES);
            if (gmpProperties.exists()) {
                resources = new Resource[]{new FileSystemResource(gmpProperties)};
                logger.info("Loading config from file system...");
            } else {
                logger.info("Loading config from classpath...");
                resources = new ClassPathResource[]{new ClassPathResource(GMP_PROPERTIES)};
            }
        } else {
            throw new ScriptContextException("gmp.home is not set!");
        }
        pspc.setLocations(resources);
        pspc.setIgnoreUnresolvablePlaceholders(true);
        pspc.setIgnoreResourceNotFound(true);
        return pspc;
    }

    @Bean(name = "ResultMap")
    public static Map<String, Object> resultMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "ExportBindings")
    public static Map<String, Object> computeExportBeans(ListableBeanFactory beanFactory) {
        Map<String, Object> beans = beanFactory.getBeansWithAnnotation(ExportBinding.class);
        Map<String, Object> bindings = new HashMap<>();
        for (Map.Entry<String, Object> bean : beans.entrySet()) {
            ExportBinding annotationBinding = bean.getValue().getClass().getAnnotation(ExportBinding.class);
            String key = annotationBinding.name().length() == 0 ? bean.getKey() : annotationBinding.name();
            bindings.put(key, bean.getValue());
            if (logger.isDebugEnabled()) logger.debug("Export binding: {}.", key);
        }
        return Collections.unmodifiableMap(bindings);
    }
}
