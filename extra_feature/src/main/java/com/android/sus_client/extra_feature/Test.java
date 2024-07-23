package com.android.sus_client.extra_feature;

import android.content.Context;
import android.widget.Toast;

public class Test {

    private final Context context;

    public Test() {
        this.context = null;
    }

    public Test(Context context) {
        this.context = context;
    }

    public void start(String msg) {
        System.out.println("Test >> " + msg);
        if (context != null)
            Toast.makeText(context, "Test >> " + msg, Toast.LENGTH_SHORT).show();
    }

}