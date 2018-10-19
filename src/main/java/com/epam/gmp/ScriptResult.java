package com.epam.gmp;

import com.epam.dep.esp.common.json.JsonMapper;

public class ScriptResult<T> {
    private T result;

    public T getResult() {
        return result;
    }

    public ScriptResult(T result) {
        this.result = result;
    }

    public ScriptResult() {
        result = null;
    }

    @Override
    public String toString() {
        return JsonMapper.getInstance().map(result);
    }
}
