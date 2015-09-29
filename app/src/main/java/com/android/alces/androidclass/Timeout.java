package com.android.alces.androidclass;

import android.os.Handler;
import android.os.Message;

/**
 * Created by Echdah on 9/29/2015.
 */
public class Timeout implements Runnable{

    int timeoutTime = 10000;
    Handler handler;

    public Timeout(int timeout, Handler handle)
    {
        timeoutTime = timeout;
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
