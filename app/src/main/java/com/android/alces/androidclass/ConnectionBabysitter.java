package com.android.alces.androidclass;

import android.os.Handler;
import android.os.Message;

public class ConnectionBabysitter extends Thread implements Runnable{

    long startTimeout = 30000;
    Handler handler;
    boolean connecting = false;

    public ConnectionBabysitter(Handler handle)
    {
        handler = handle;
    }

    public void run()
    {
//        boolean outOfTime = false;
//
//        long startTime = System.currentTimeMillis();
//
//        while(!outOfTime && !this.isInterrupted())
//        {
//            long currentTime = System.currentTimeMillis();
//
//            if((currentTime - startTime) >= startTimeout)
//            {
//                outOfTime = true;
//            }
//        }
        try {
            Thread.sleep(startTimeout);
        }
        catch(InterruptedException ex)
        {
            //TODO: Something something.
        }

        if(!this.isInterrupted()) {

            if (!Global.globalSocket.connected() && !connecting) {
                Global.globalSocket.connect();
                connecting = true;
                if(Global._user != null)
                {
                    Global.globalSocket.emit("reauth", Global._user.toJson());
                }
                startTimeout = 10000;
            } else if (Global.globalSocket.connected()){
                startTimeout = 5000;
                connecting = false;
            }
            else{
                startTimeout = 5000;
            }

            run();
        }
    }

}
