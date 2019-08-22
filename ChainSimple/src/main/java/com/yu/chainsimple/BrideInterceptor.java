package com.yu.chainsimple;

public class BrideInterceptor implements Interceptor {
    @Override
    public String interceptor(Chain chain) {
        System.out.println("excuter befor brideginterceptor");
        String reuest = chain.proceed(chain.request());
        System.out.println("excuter after brideginterceptor " + reuest);
        return reuest;
    }
}
