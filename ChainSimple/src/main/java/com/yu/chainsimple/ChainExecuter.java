package com.yu.chainsimple;

import java.util.ArrayList;
import java.util.List;

public class ChainExecuter {

    public static final void main(String[] args){
        List<Interceptor> interceptors = new ArrayList<>();
        interceptors.add(new BrideInterceptor());
        interceptors.add(new RetryAndFollowInterceptor());
        interceptors.add(new CacheInterceptor());

        RealInterceptorChain request = new RealInterceptorChain(0, interceptors, "request");
        request.proceed("request");
    }

}
