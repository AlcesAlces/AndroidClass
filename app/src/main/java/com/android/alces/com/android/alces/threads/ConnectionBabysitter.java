package com.android.alces.com.android.alces.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.alces.androidclass.Global;
import com.github.nkzawa.socketio.client.IO;

import java.net.URISyntaxException;

public class ConnectionBabysitter extends Thread implements Runnable{

    long startTimeout = 1000;
    Handler handler;

    public ConnectionBabysitter(Handler handle)
    {
        handler = handle;
    }

    public void run() {

        while(!this.isInterrupted()) {

            try {
                Thread.sleep(startTimeout);
            } catch (InterruptedException ex) {
                Log.d("SV", "interupt exception bad news bears");
            }

            if (!this.isInterrupted()) {

                if (Global.globalSocket != null) {
                    if (Global.globalSocket.connected()) {
                        Global.globalSocket.emit("ping");
                    } else {
                        Global.globalSocket.connect();
                    }
                } else {
                    try {
                        Global.globalSocket = IO.socket("http://MovieCatalog.cloudapp.net:8080/");
                        Global.globalSocket.connect();
                    } catch (URISyntaxException ex) {
                        Log.d("SV", "How did a URI Exception happen? Oh well, it did");
                    }
                }
            }
        }
    }

}
