package com.android.alces.androidclass;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    enum status {APPROVE, REFUSE};

    EditText editTextUser;
    EditText editTextPass;
    Button btnLogin;
    Button btnRegister;

    boolean auth = false;
    boolean refused = false;
    String userName = "";

//    public Handler msgHandler = new Handler()
//    {
//        public void handleMessage(Message msg)
//        {
//            super.handleMessage(msg);
//            success();
//        }
//    };
    private Socket mSocket;
    {
        try{
            mSocket = IO.socket("http://MovieCatalog.cloudapp.net:8080/");
        }
        catch(URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogin = (Button) findViewById(R.id.button);
        btnRegister = (Button) findViewById(R.id.button2);
        editTextUser = (EditText) findViewById(R.id.editText);
        editTextPass = (EditText) findViewById(R.id.editText2);

        mSocket.connect();

        mSocket.on("refuse", onRefuse);
        mSocket.on("approve", onApprove);
        mSocket.on("createsuccess", onCreateSuccess);

        btnLogin.setOnClickListener(new View.OnClickListener() {


            //When you click the button we wait for the server response.

            //Establish event listeners for the socket object.
            @Override
            public void onClick(View v) {
                setComponentsEnabled(false);
                tryAuth();
                //Intent myIntent = new Intent(MainActivity.this, Testbed.class);
                //MainActivity.this.startActivity(myIntent);
            }

            });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryCreate();
            }});
    }


    private void setComponentsEnabled(boolean set)
    {
        btnLogin.setEnabled(set);
        btnRegister.setEnabled(set);
        editTextUser.setEnabled(set);
        editTextPass.setEnabled(set);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        mSocket.off("refuse", onRefuse);
        mSocket.off("approve", onApprove);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void success()
    {

        Intent myIntent = new Intent(MainActivity.this, Testbed.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void failure()
    {
        editTextUser.setText("FAIL!");
        setComponentsEnabled(true);
    }

    public void creationSuccess()
    {
        editTextUser.setText("User Created!");
        setComponentsEnabled(true);
    }

    private void tryAuth()
    {
        JSONObject json = new JSONObject();
        try {
            json.put("name", editTextUser.getText().toString());
            json.put("pass", editTextPass.getText().toString());
        }
        catch(JSONException ex)
        {

        }
        //Send some information to the server.
        mSocket.emit("authenticate", json);
    }

    private void tryCreate()
    {
        JSONObject json = new JSONObject();
        try {
            json.put("name", editTextUser.getText().toString());
            json.put("pass", editTextPass.getText().toString());
        }
        catch(JSONException ex)
        {

        }
        //Send some information to the server.
        mSocket.emit("create", json);
    }

    private Emitter.Listener onRefuse = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args[0];
//
//            int numUsers;
//            try {
//                numUsers = data.getInt("numUsers");
//            } catch (JSONException e) {
//                return;
//            }
            //SO! Basically at this point we need to set up a messenger to
            //communicate with the main thread. I suggest looking at:
            //https://github.com/nkzawa/socket.io-android-chat/blob/master/app/src/main/java/com/github/nkzawa/socketio/androidchat/MainFragment.java
            Message msg = handler.obtainMessage();
            msg.what = 0;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onApprove = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args[0];
//
//            int numUsers;
//            try {
//                numUsers = data.getInt("numUsers");
//            } catch (JSONException e) {
//                return;
//            }

            Message msg = handler.obtainMessage();
            msg.what = 1;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onCreateSuccess = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args[0];
//
//            int numUsers;
//            try {
//                numUsers = data.getInt("numUsers");
//            } catch (JSONException e) {
//                return;
//            }

            Message msg = handler.obtainMessage();
            msg.what = 2;
            handler.sendMessage(msg);
        }
    };

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0){
                editTextUser.setText("FAIL!");
            }
            else if(msg.what == 1)
            {
                success();
            }
            else if(msg.what == 2)
            {
                creationSuccess();
            }
            super.handleMessage(msg);
        }
    };
}
