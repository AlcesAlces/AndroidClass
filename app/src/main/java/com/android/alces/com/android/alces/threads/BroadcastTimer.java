package com.android.alces.com.android.alces.threads;

import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;

public class BroadcastTimer extends Thread implements Runnable{

    public int goal = 0;
    public AudioTrack track;
    Handler handler;
    public Boolean running = false;

    public BroadcastTimer(Handler handle)
    {
        handler = handle;
    }

    public void run()
    {
        running = true;
        while(goal != track.getPlaybackHeadPosition())
        {
            //spin
        }

        if(!this.isInterrupted()) {

            Message msg = handler.obtainMessage();
            msg.what = 4;
            handler.sendMessage(msg);
        }

        running = false;
    }
}
