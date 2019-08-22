package com.yu.chainsimple;

public interface Interceptor {

    String interceptor(Chain chain);

    interface Chain{
        String request();
        String proceed(String request);
    }

}
