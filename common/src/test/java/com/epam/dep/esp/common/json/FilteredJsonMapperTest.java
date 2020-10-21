package com.epam.dep.esp.common.json;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilteredJsonMapperTest {

    public class TestObj1 {
        public String field;
        public String password;

        public TestObj1(String field, String password) {
            this.field = field;
            this.password = password;
        }
    }

    public class TestObj2 {
        public String field;
        public String pwd;

        public TestObj2(String field, String pwd) {
            this.field = field;
            this.pwd = pwd;
        }
    }

    private FilteredJsonMapper testObj;

    @Before
    public void init() {
        testObj = FilteredJsonMapper.getInstance();
    }

    @Test
    public void test() {
        assertFalse(testObj.map(new TestObj1("test", "pwd")).contains("password"));
        assertTrue(testObj.map(new TestObj2("test", "pwd")).contains("pwd"));
    }
}