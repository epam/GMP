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

import com.epam.gmp.ScriptContext;
import com.epam.gmp.ScriptInitializationException;
import com.epam.gmp.config.GMPConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GMPConfig.class})

public class ScriptContextBuilderTest {

    @Autowired
    ScriptContextBuilder testObj;
    @Resource(name = "gmpHomeResource")
    org.springframework.core.io.Resource gmpHome;

    @Test
    public void resourcesTest() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[] resources = resolver.getResources(gmpHome.getURL().toString() + "*.*");
    }


    @Test
    //global-config: exists
    //common-config: is not exist
    //script-config: is not exist
    public void buildContextForTest_01_001() throws IOException, ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("@test_01/001-test.groovy", null);
        assertTrue(scriptContext.getParams().get("cmdLine") == Collections.emptyList());
        assertNotNull(scriptContext.getParams().get("logger"));
        assertEquals("001-test.groovy", scriptContext.getScriptName());
        assertEquals("@test_01/001-test.groovy", scriptContext.getScriptId());
        assertEquals("test-001-global", ((Map) scriptContext.getParams().get("gConfig")).get("testData_001"));
    }

    @Test
    //global-config: exists
    //common-config: is not exist
    //script-config: exists

    public void buildContextForTest_01_002() throws IOException, ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("@test_01/002-test.groovy", null);
        assertTrue(scriptContext.getParams().get("cmdLine") == Collections.emptyList());
        assertNotNull(scriptContext.getParams().get("logger"));
        assertEquals("002-test.groovy", scriptContext.getScriptName());
        assertEquals("@test_01/002-test.groovy", scriptContext.getScriptId());
        assertEquals("test_001", ((Map) scriptContext.getParams().get("gConfig")).get("testData_001"));
        assertEquals("test_002", ((Map) scriptContext.getParams().get("gConfig")).get("testData_002"));
    }

    @Test
    //global-config: exists
    //common-config: is not exist
    //script-config: exists

    public void buildContextForTest_01_003() throws IOException, ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("@test_01/003-test.config.groovy", null);
        assertTrue(scriptContext.getParams().get("cmdLine") == Collections.emptyList());
        assertNotNull(scriptContext.getParams().get("logger"));
        assertEquals("002-test.groovy", scriptContext.getScriptName());
        assertEquals("@test_01/003-test.config.groovy", scriptContext.getScriptId());
        assertEquals("test_001", ((Map) scriptContext.getParams().get("gConfig")).get("testData_001"));
        assertEquals("test_002", ((Map) scriptContext.getParams().get("gConfig")).get("testData_002"));
    }

    @Test
    //global-config: exists
    //common-config: is not exist
    //script-config: exists

    public void buildContextForTest_01_004() throws IOException, ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("@test_01/004-test.config.groovy", null);
        assertTrue(scriptContext.getParams().get("cmdLine") == Collections.emptyList());
        assertNotNull(scriptContext.getParams().get("logger"));
        assertEquals("002-test.groovy", scriptContext.getScriptName());
        assertEquals("@test_01/004-test.config.groovy", scriptContext.getScriptId());
        assertEquals("test_001", ((Map) scriptContext.getParams().get("gConfig")).get("testData_001"));
        assertEquals("test_002", ((Map) scriptContext.getParams().get("gConfig")).get("testData_002"));
        assertEquals("test_004", ((Map) scriptContext.getParams().get("gConfig")).get("testData_004"));
    }

    @Test
    //global-config: exists
    //common-config: exist
    //script-config: is not exist

    public void buildContextForTest_02_001() throws IOException, ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("env@test_02/001-test.groovy", null);
        assertEquals("env", scriptContext.getParams().get("gEnv"));
        assertTrue(scriptContext.getParams().get("cmdLine") == Collections.emptyList());
        assertNotNull(scriptContext.getParams().get("logger"));
        assertEquals("001-test.groovy", scriptContext.getScriptName());
        assertEquals("env@test_02/001-test.groovy", scriptContext.getScriptId());
        assertEquals("test_02-001-common", ((Map) scriptContext.getParams().get("gConfig")).get("testData_001"));

    }

    @Test
    //global-config: exists
    //common-config: exist
    //script-config: exist

    public void buildContextForTest_02_002() throws IOException, ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("@test_02/002-test.groovy", null);
        assertEquals("", scriptContext.getParams().get("gEnv"));
        assertTrue(scriptContext.getParams().get("cmdLine") == Collections.emptyList());
        assertNotNull(scriptContext.getParams().get("logger"));
        assertEquals("002-test.groovy", scriptContext.getScriptName());
        assertEquals("@test_02/002-test.groovy", scriptContext.getScriptId());
        assertEquals("test_02-002-script", ((Map) scriptContext.getParams().get("gConfig")).get("testData_001"));
    }

    @Test
    public void  buildContextForTest_04_001() throws ScriptInitializationException {
        ScriptContext scriptContext = testObj.buildContextFor("@test_04-env/001-test.groovy", null);
        assertEquals("var1", ((Map) scriptContext.getParams().get("gConfig")).get("var1"));
        assertEquals("var2=var1+1", ((Map) scriptContext.getParams().get("gConfig")).get("var2"));

    }

}