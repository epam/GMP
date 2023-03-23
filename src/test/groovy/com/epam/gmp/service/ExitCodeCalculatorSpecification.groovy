package com.epam.gmp.service

import com.epam.gmp.ScriptResult
import com.epam.gmp.config.GMPConfig
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = GMPConfig)
class ExitCodeCalculatorSpecification extends Specification {

    @SpringBean(name = "ResultMap")
    Map resultMap = new HashMap()

    @Autowired
    ExitCodeCalculator exitCodeCalculator

    def setup() {
        resultMap.clear()
    }

    def "Integer values test Test"() {
        resultMap.put("test", Integer.valueOf(1))
        resultMap.put("test1", Integer.valueOf(2))
        def result = exitCodeCalculator.calculate(0)
        expect:
        result == 3
    }

    def "ScriptResult values Test"() {
        resultMap.put("test", new ScriptResult<>(new Integer(1)))
        resultMap.put("test1", new ScriptResult<>(new Integer(2)))
        def result = exitCodeCalculator.calculate(0)
        expect:
        result == 3
    }

    def "Mixed values Test"() {
        resultMap.put("test", Integer.valueOf(1))
        resultMap.put("test1", new ScriptResult<>(new Integer(2)))
        def result = exitCodeCalculator.calculate(1)
        expect:
        result == 4
    }

    def "Null values Test"() {
        resultMap.put("test", Integer.valueOf(1))
        resultMap.put("test1", null)
        def result = exitCodeCalculator.calculate(1)
        expect:
        result == 2
    }

    def "Object value test"() {
        resultMap.put("test", Integer.valueOf(1))
        resultMap.put("test1", [value: "value1"])
        def result = exitCodeCalculator.calculate(1)
        expect:
        result == 2
    }
}
