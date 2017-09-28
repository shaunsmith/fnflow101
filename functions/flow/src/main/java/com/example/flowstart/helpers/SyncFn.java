package com.example.flowstart.helpers;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.api.flow.HttpMethod;

import java.io.Serializable;

/**
 * Created on 22/09/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class SyncFn {

    public static <I> void invokeJsonFunction(String name, I val) {
        Flows.currentFlow().invokeFunction(name, val).get();
    }

    public static String invokeFunction(String name, String in) {
        return invokeFunction(name, in, Headers.emptyHeaders());
    }

    public static String invokeFunction(String name, String in, Headers headers) {
        return new String(Flows.currentFlow().invokeFunction(name, HttpMethod.POST, headers, in.getBytes()).get().getBodyAsBytes());
    }

    public static <R extends Serializable, I> R invokeJsonFunction(String name, I val, Class<R> returnType) {
        return Flows.currentFlow().invokeFunction(name, val, returnType).get();
    }
}
