package com.yu.chainsimple;

public class RetryAndFollowInterceptor implements Interceptor {

    @Override
    public String interceptor(Chain chain) {
        System.out.println("excuter befor RetryAndFollowInterceptor");
        String reuest = chain.proceed(chain.request());
        System.out.println("excuter after RetryAndFollowInterceptor " + reuest);
        return reuest;
    }
}
