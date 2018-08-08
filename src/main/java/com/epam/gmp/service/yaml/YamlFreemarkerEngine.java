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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;


public class YamlFreemarkerEngine {
    private Configuration config;

    public YamlFreemarkerEngine(ClassLoader classLoader, String path, String baseFolder) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        TemplateLoader[] loaders = {new ClassTemplateLoader(classLoader, path), new FileTemplateLoader(new File(baseFolder))};
        cfg.setTemplateLoader(new MultiTemplateLoader(loaders));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        config = cfg;
    }

    public String loadYaml(String templateName, YamlLoaderContext context) throws IOException, TemplateException {
        Template template = config.getTemplate(templateName);
        StringWriter out = new StringWriter();
        template.process(context.getParams(), out);
        return out.toString();
    }
}