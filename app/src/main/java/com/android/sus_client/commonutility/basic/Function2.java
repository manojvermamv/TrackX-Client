package com.android.sus_client.commonutility.basic;

public interface Function2<T, P1, P2> {
    T invoke(Invoker<P1> var1, Invoker<P2> var2);
}