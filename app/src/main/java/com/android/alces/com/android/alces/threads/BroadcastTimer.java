package com.android.alces.com.android.alces.threads;

import android.os.Handler;
import android.os.Message;

public class BroadcastTimer extends Thread implements Runnable{

    int timeoutTime = 10000;
    Handler handler;

    public BroadcastTimer(Handler handle)
    {
        handler = handle;
    }

    public void run()
    {
        boolean outOfTime = false;

        long startTime = System.currentTimeMillis();

        while(!outOfTime && !this.isInterrupted())
        {
            long currentTime = System.currentTimeMillis();

            if((currentTime - startTime) >= timeoutTime)
            {
                outOfTime = true;
            }
        }

        if(!this.isInterrupted()) {

            Message msg = handler.obtainMessage();
            msg.what = 3;
            handler.sendMessage(msg);
        }
    }
}
