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

package com.epam.gmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class ScriptClassloader extends URLClassLoader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public ScriptClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public ScriptClassloader(URL[] urls) {
        super(urls);
    }

    public ScriptClassloader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> result = super.loadClass(name);
        if (logger.isInfoEnabled()) {
            logger.debug("Loading class: " + name + (result == null ? " not found" : " loaded!"));
        }
        return result;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (logger.isDebugEnabled() && resolve) logger.debug("Loading class: " + name);
        return super.loadClass(name, resolve);
    }
}
