package com.yu.chainsimple;

import java.util.List;

public class RealInterceptorChain implements Interceptor.Chain {

    private int index;
    private List<Interceptor> interceptors;
    private String request;

    public RealInterceptorChain(int index, List<Interceptor> interceptors, String request) {
        this.index = index;
        this.interceptors = interceptors;
        this.request = request;
    }

    @Override
    public String request() {
        return request;
    }

    @Override
    public String proceed(String request) {
        if (index >= interceptors.size()){
            return "";
        }
        RealInterceptorChain next = new RealInterceptorChain(index + 1, interceptors, request);
        Interceptor interceptor = interceptors.get(index);
        return interceptor.interceptor(next);
    }
}
