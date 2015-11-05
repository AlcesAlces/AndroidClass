package com.android.alces.com.android.alces.threads;

import android.os.Handler;
import android.os.Message;

public class BroadcastTimer extends Thread implements Runnable{

    int timeoutTime = 1500;
    Handler handler;
    public Boolean finished = false;

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
            msg.what = 4;
            handler.sendMessage(msg);
            finished = true;
        }
    }
}
