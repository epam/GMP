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

package com.epam.dep.esp.common;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;


public class OSTest {
    OS testObj;

    @Test
    public void testOs() {
        System.out.println("OS TEST: " + testObj.name());
        List<String> result = new ArrayList<>();
        Map<String, String> envVar = new HashMap<>();
        envVar.put("testVar", "testOs.var");

        if (testObj == OS.win) {
            assertEquals(0, (int) testObj.execCommandLine(Arrays.asList(new String[]{"cmd", "/c", "echo %testVar%"}), result, ".", 10, envVar));
        } else {
            assertEquals(0, (int) testObj.execCommandLine(Arrays.asList(new String[]{"/bin/bash", "-c", "echo $testVar"}), result, ".", 10, envVar));
        }

        assertEquals(1, result.size());
        assertEquals("testOs.var", result.get(0));
    }

    @Before
    public void setUp() {
        testObj = OS.getOs();
    }

}