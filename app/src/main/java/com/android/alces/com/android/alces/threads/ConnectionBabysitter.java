package com.android.alces.com.android.alces.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.alces.androidclass.Global;
import com.github.nkzawa.socketio.client.IO;

import java.net.URISyntaxException;

public class ConnectionBabysitter extends Thread implements Runnable{

    long startTimeout = 10000;
    Handler handler;
    boolean connecting = false;

    public ConnectionBabysitter(Handler handle)
    {
        handler = handle;
    }

    public void run() {

        try {
            Thread.sleep(startTimeout);
        }
        catch(InterruptedException ex)
        {
            Log.d("SV", "interupt exception bad news bears");
        }

        if(!this.isInterrupted()) {

            if(Global.globalSocket != null) {
                if (!Global.globalSocket.connected() && !connecting) {
                    Global.globalSocket.connect();
                    connecting = true;
                    if (Global._user != null) {
                        Global.globalSocket.emit("reauth", Global._user.toJson());
                    }
                    startTimeout = 10000;
                } else if (Global.globalSocket.connected()) {
                    startTimeout = 5000;
                    connecting = false;
                } else {
                    startTimeout = 5000;
                }
            }
            else
            {
                try {
                    Global.globalSocket = IO.socket("http://MovieCatalog.cloudapp.net:8080/");
                }
                catch(URISyntaxException ex){
                    Log.d("SV", "How did a URI Exception happen? OH well, it did");
                }
            }

            run();
        }
    }

}
