package com.android.sus_client.commonutility.basic;

public interface Function3<T, P1, P2, P3> {
    T invoke(Invoker<P1> var1, Invoker<P2> var2, Invoker<P3> var3);
}