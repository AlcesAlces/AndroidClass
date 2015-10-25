package com.android.alces.androidclass;

import android.os.Handler;

import com.github.nkzawa.socketio.client.Socket;

public final class Global {

    public static com.github.nkzawa.socketio.client.Socket globalSocket;
    public static User _user = null;
    //This handler is going to be recycled at every view.
    public static Handler _currentHandler;
}
