package com.yu.chainsimple;

public class CacheInterceptor implements Interceptor {

    @Override
    public String interceptor(Chain chain) {
        System.out.println("excuter CacheInterceptor");
        return "success";
    }
}
