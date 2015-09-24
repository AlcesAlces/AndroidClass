package com.android.alces.androidclass;

import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class SocketHandler extends AsyncTask<String,String,String> {

    private static com.github.nkzawa.socketio.client.Socket msocket;
    boolean connected = false;
    boolean connecting = false;
    enum commands {connect, auth, none}
    commands inputCmd = commands.none;
    Handler msgHandler;

    String user = "";
    String pass = "";

    List<Button> buttons = new ArrayList<>();
    List<TextView> textViews = new ArrayList<>();
    List<EditText> editTexts = new ArrayList<>();

    //You need to recycle this object after every use.
    public SocketHandler(List<Button> btns, List<TextView> tvs, List<EditText> ets,
                         Handler hndl, String usr, String ps)
    {
        user = usr;
        pass = ps;
        msocket = Global.globalSocket;

        msgHandler = hndl;

        if(btns != null)
            buttons.addAll(btns);
        if(tvs != null)
            textViews.addAll(tvs);
        if(ets != null)
            editTexts.addAll(ets);

        try {
            if(msocket == null)
            {
                msocket = IO.socket("http://MovieCatalog.cloudapp.net:80/");
            }
        }
        catch(URISyntaxException ex) {
            return;
        }
    }

    @Override
    protected String doInBackground(String... params)
    {
        switch(inputCmd)
        {
            case connect:
                Connect();
                break;
            case auth:
                Auth();
            default:
                break;
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result)
    {
        for(Button btn : buttons)
        {
            btn.setEnabled(true);
        }

        for(TextView tv : textViews)
        {
            tv.setEnabled(true);
        }

        for(EditText et : editTexts)
        {
            et.setEnabled(true);
        }

        msgHandler.sendEmptyMessage(0);
    }

    public void Connect()
    {
        connecting = true;
        if(!msocket.connected())
        {
            msocket.connect();
            if(msocket.connected())
            {
                connected = true;
            }
            else {
                connected = false;
            }
        }
        connecting = false;
    }

    boolean auth = false;
    boolean refused = false;

    public void Auth()
    {
        auth = false;
        refused = false;

        if(!msocket.connected())
        {
            Connect();
        }


        msocket.once("disconnect", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Ack ack = (Ack) args[args.length - 1];
                ack.call();
                JSONObject obj = (JSONObject) args[0];
                refused = true;
            }
        });

        msocket.once("authenticate", onNewMessage);

        JSONObject json = new JSONObject();
        try {
            json.put("name", user);
            json.put("pass", pass);
        }
        catch(JSONException ex)
        {

        }
        //Send some information to the server.
        msocket.emit("authenticate", json);

        long startTime = System.currentTimeMillis();

        while(!auth && !refused)
        {
            long currentTime = System.currentTimeMillis();

            if((currentTime - startTime) >= 5000)
            {
                refused = true;
            }
        }

        if(auth)
        {
            int i = 0;
        }
        else
        {
            int i = 0;
        }
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            String username;
            String message;
            try {
                username = data.getString("username");
                message = data.getString("message");
            } catch (JSONException e) {
                return;
            }

        }
    };

    public void Disconnect()
    {
        if(msocket.connected())
        {
            msocket.disconnect();
        }
    }

    public void UpdateCommand(String parse)
    {
        if(parse == "connect")
        {
            inputCmd = commands.connect;
        }
        else if(parse == "auth")
        {
            inputCmd = commands.auth;
        }
    }
}
