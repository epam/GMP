package com.epam.gmp.service;

import com.epam.gmp.ScriptResult;
import com.epam.gmp.config.GMPConfig;
import com.epam.gmp.process.GroovyThread;
import com.epam.gmp.process.QueuedProcessService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GMPConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GroovyScriptEngineServiceTest {
    @Autowired
    QueuedProcessService testObj;

    @Test(expected = ExecutionException.class)
    public void test_01_004_Test() throws ExecutionException, InterruptedException {
        Future<ScriptResult<String>> future = testObj.execute(GroovyThread.class, "fe1@test_01/004-exception-test.groovy");
        Assert.assertEquals("fe1/file.txt", future.get().getResult());
    }
}