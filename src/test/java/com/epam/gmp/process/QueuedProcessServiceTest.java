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

package com.epam.gmp.process;

import com.epam.gmp.ScriptResult;
import com.epam.gmp.config.GMPConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GMPConfig.class})
public class QueuedProcessServiceTest {

    @Autowired
    QueuedProcessService testObj;

    @Test
    public void executor_01_Test() throws ExecutionException, InterruptedException {
        Future<ScriptResult<Integer>> future = testObj.execute(GroovyThread.class, "@test_03-executor/exec-01.groovy", Collections.EMPTY_LIST);
        Assert.assertEquals(Integer.valueOf(2), future.get().getResult());
    }

}