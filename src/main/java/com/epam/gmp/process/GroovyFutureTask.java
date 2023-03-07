package com.epam.gmp.process;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * This class was designed as a wrapper for FutureTask to provide
 * custom hash code based on script name and parameters.
 *
 * @param <V> Result Class
 */
public class GroovyFutureTask<V> extends FutureTask<V> {
    private final int hashCode;
    private final String str;

    public GroovyFutureTask(Callable<V> callable) {
        super(callable);
        hashCode = callable.hashCode();
        str = callable.toString();
    }

    public GroovyFutureTask(Runnable runnable, V result) {
        super(runnable, result);
        hashCode = Objects.hash(runnable.hashCode(), result.hashCode());
        str = runnable.toString();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "GroovyFutureTask(" + hashCode + "):" + str;
    }
}
