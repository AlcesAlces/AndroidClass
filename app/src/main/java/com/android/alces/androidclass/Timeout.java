package com.android.alces.androidclass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class Timeout implements Runnable{

    int timeoutTime = 10000;
    Handler handler;

    public Timeout(Handler handle)
    {
        handler = handle;
    }

    public void run()
    {
        boolean outOfTime = false;

        long startTime = System.currentTimeMillis();

        while(!outOfTime)
        {
            long currentTime = System.currentTimeMillis();

            if((currentTime - startTime) >= timeoutTime)
            {
                outOfTime = true;
            }
        }

        Message msg = handler.obtainMessage();
        msg.what = 3;

        handler.sendMessage(msg);
    }
}
