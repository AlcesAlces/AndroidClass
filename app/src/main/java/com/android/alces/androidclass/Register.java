package com.android.alces.androidclass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class Register extends Activity {

    EditText etName;
    EditText etPass;
    EditText etEmail;
    TextView tvMessage;
    ProgressDialog dialog;
    private Socket mSocket;
    private Boolean authCycle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = (EditText) findViewById(R.id.etUsernameRegister);
        etPass = (EditText) findViewById(R.id.etPasswordRegister);
        etEmail = (EditText) findViewById(R.id.etEmailRegister);
        Button btnRegister = (Button) findViewById(R.id.btnCreateRegister);
        tvMessage = (TextView) findViewById(R.id.tvRegisterMessages);
        //Clear the TV. This way we can have a message in designer mode to check formatting.
        tvMessage.setText("");

        mSocket = Global.globalSocket;

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryCreate();
            }});

        mSocket.on("refuse_reg", onRefuse);
        mSocket.on("createsuccess", onSuccess);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //TODO: Remove all socket listeners.
        mSocket.off("refuse", onRefuse);
        mSocket.off("createsuccess", onSuccess);
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

    private Emitter.Listener onSuccess = new Emitter.Listener() {
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
            msg.what = 2;
            msg.obj = message;
            handler.sendMessage(msg);
        }
    };

    private void tryCreate()
    {
        JSONObject json = new JSONObject();
        try {

            //TODO: Perform preprocessing on this stuff.
            String name = etName.getText().toString();
            String pass = etPass.getText().toString();
            String email = etEmail.getText().toString();

            json.put("name", name);
            json.put("pass", pass);
            json.put("email", email);
        }
        catch(JSONException ex)
        {

        }

        dialog = new ProgressDialog(this);
        dialog.setMessage("Attempting to create user...");
        dialog.setIndeterminate(true);
        dialog.show();

        //Send some information to the server.
        mSocket.emit("create", json);
        Thread thread = new Thread(new Timeout(10000,handler), "timeout_thread");
        thread.start();
    }

    final private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            if(dialog != null)
            {
                dialog.dismiss();
            }

            if(msg.what==0){
                //Refused
                tvMessage.setText(((String)msg.obj));
            }
            else if(msg.what==2)
            {
                authCycle = true;
                //Success
                AlertDialog.Builder builder = new AlertDialog.Builder(Register.this);
                builder.setMessage((String)msg.obj)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();

                alert.show();
            }
            else if(msg.what == 3)
            {
                //timeout. authCycle prevents us from stepping on the toes of authentication
                if(!authCycle) {
                    tvMessage.setText("Connection timed out from the server");
                }
            }
            super.handleMessage(msg);
        }
    };
}
