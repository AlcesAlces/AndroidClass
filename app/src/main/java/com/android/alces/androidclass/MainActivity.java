package com.android.alces.androidclass;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    enum status {APPROVE, REFUSE};

    EditText editTextUser;
    EditText editTextPass;
    TextView tvMessages;
    Button btnLogin;
    Button btnRegister;
    ProgressDialog dialog;

    Timeout timerThread;
    ConnectionBabysitter babysitter;

    private Socket mSocket;
    {
        try{
            mSocket = IO.socket("http://MovieCatalog.cloudapp.net:8080/");
            //Keep track of the socket in global space.
            Global.globalSocket = mSocket;
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

        tvMessages = (TextView) findViewById(R.id.tvLoginMessages);
        mSocket.connect();
        babysitter = new ConnectionBabysitter(handler);
        babysitter.start();

        //TODO: Handle a non-connection situation.
        //Something like if(mSocket.isConnected()....

        mSocket.on("refuse", onRefuse);
        mSocket.on("approve", onApprove);
        mSocket.on("reauth", onReauth);

        btnLogin.setOnClickListener(new View.OnClickListener() {


            //When you click the button we wait for the server response.

            //Establish event listeners for the socket object.
            @Override
            public void onClick(View v) {
                setComponentsEnabled(false);
                tryAuth();
            }

        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, Register.class);
                MainActivity.this.startActivity(myIntent);
            }});

        editTextPass.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    tryAuth();
                    return true;
                }
                return false;
            }
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
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

        switch (id)
        {
            case R.id.action_back:
            {
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void success() {
        Intent myIntent = new Intent(MainActivity.this, RoomsActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void creationSuccess()
    {
        tvMessages.setText("User Created!");
        setComponentsEnabled(true);
    }

    private void tryAuth()
    {
        JSONObject json = new JSONObject();
        try {
            dialog = new ProgressDialog(this);
            dialog.setMessage("Logging in. Please wait....");
            dialog.setIndeterminate(true);
            dialog.show();

            json.put("name", editTextUser.getText().toString());
            json.put("pass", editTextPass.getText().toString());

            //TODO: Need to check for more cases here.
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            try {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                json.put("lat", latitude);
                json.put("lon", longitude);
                Global._user = new User(editTextUser.getText().toString(), latitude, longitude);
            }
            catch(NullPointerException nptr)
            {
                //Default location.
                double lat = 0.0;
                double lon = 0.0;
                json.put("lat", lat);
                json.put("lon", lon);
                Global._user = new User(editTextUser.getText().toString(), lat, lon);
            }
        }
        catch(JSONException ex)
        {

        }
        catch(SecurityException ex)
        {

        }
        //Send some information to the server.
        mSocket.emit("authenticate", json);

        //Create a timeout thread which will sit in the background and verify that everything is Kosher.

        timerThread = new Timeout(handler);
        timerThread.start();

        //Thread thread = new Thread(new Timeout(handler), "timeout_thread");
        //thread.start();
    }

    private Emitter.Listener onRefuse = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            String message = (String) args[0];
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
            msg.obj = message;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onReauth = new Emitter.Listener() {
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
            msg.what = 254;
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

    //Basically this is the ONLY place where we can interact with the UI thread once we've spun off.
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            try {
                dialog.dismiss();
                timerThread.interrupt();
            }
            catch(Exception ex)
            {

            }

            if(msg.what==0){
                tvMessages.setText((String)msg.obj);
            }
            else if(msg.what == 1)
            {
                success();
            }
            else if(msg.what == 2)
            {
                creationSuccess();
            }
            else if(msg.what == 3)
            {
                //timeout. authCycle prevents us from stepping on the toes of authentication
//                if(!authCycle) {
//                    tvMessages.setText("Connection timed out from the server");
//                }

                Toast.makeText(MainActivity.this, "Timed out from the server!", Toast.LENGTH_LONG).show();
            }
            else if(msg.what == 254)
            {
                //Reauth message.
                //TODO: Ensure that this logic is executing correctly.
                if(Global._currentHandler != null)
                {
                    Message toSend = Global._currentHandler.obtainMessage();
                    toSend.what = 254;
                    Global._currentHandler.sendMessage(toSend);
                }
                //Toast.makeText(MainActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
            }
            setComponentsEnabled(true);
            return true;
        }
    });
}
