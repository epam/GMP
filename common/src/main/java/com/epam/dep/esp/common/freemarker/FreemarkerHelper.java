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

package com.epam.dep.esp.common.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class FreemarkerHelper {
    private Configuration config;

    /**
     * @param classLoader class loader
     * @param path        base path to templates
     */
    public FreemarkerHelper(ClassLoader classLoader, String path) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassLoaderForTemplateLoading(classLoader, path);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        config = cfg;
    }

    /**
     * @param templateName template name
     * @param variables    variables map
     * @return processed template as a string
     * @throws IOException
     * @throws TemplateException
     */
    public String processTemplate(String templateName, Map<String, Object> variables) throws IOException, TemplateException {
        Template template = config.getTemplate(templateName);
        StringWriter out = new StringWriter();
        template.process(variables, out);
        return out.toString();
    }
}
