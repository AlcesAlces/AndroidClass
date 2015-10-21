package com.android.alces.androidclass;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class CreateRoomActivity extends Activity {

    private EditText etFrequencyRange;
    private EditText etRoomName;
    private Button btnCreate;
    private Boolean done = false;
    private Socket mSocket = Global.globalSocket;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        etFrequencyRange = (EditText) findViewById(R.id.create_editText_frange);
        etRoomName = (EditText) findViewById(R.id.create_editText_fname);

        final CheckBox cbVisRange = (CheckBox) findViewById(R.id.create_checkBox_frange);
        final CheckBox cbPrivate = (CheckBox) findViewById(R.id.create_checkBox_fprivate);
        btnCreate = (Button) findViewById(R.id.create_button_create);
        //Ternary operator set visibility
        etFrequencyRange.setVisibility(cbVisRange.isChecked() ? View.VISIBLE : View.INVISIBLE);


        cbVisRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                etFrequencyRange.setVisibility(cbVisRange.isChecked() ? View.VISIBLE : View.INVISIBLE);
            }
        });

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryCreateRoom(cbVisRange.isChecked(), cbPrivate.isChecked());
            }
        });

        mSocket.on("room_create_success", onSuccess);
        mSocket.on("refuse_room_create", onRefuse);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        mSocket.off("room_create_success", onSuccess);
        mSocket.off("refuse_room_create", onRefuse);
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

    public void tryCreateRoom(Boolean isRanged, Boolean isPrivate)
    {
        //TODO: something something preprocess data
        String name = etRoomName.getText().toString();
        //Default in "Anonymous User" in case somehow the user got in here without their name.
        String creator = Global._user == null ? "Anonymous User" : Global._user.name;

        String rangeToProcess = etFrequencyRange.getText().toString();
        rangeToProcess = rangeToProcess.trim();

        int range = -1;

        if(isRanged) {

            if (rangeToProcess.matches("[0-9]+")) {

                //String is in correct format
                try {
                    range = Integer.parseInt(rangeToProcess);
                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse " + nfe);
                }
            } else {
                //Throw some kind of error. Maybe an okay box?
                return;
            }
        }
        else
        {
            range = -1;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("_creator", creator);
            json.put("_room", name);
            json.put("_isPrivate", isPrivate ? 1 : 0);
            json.put("_range", range);
        }
        catch(JSONException ex)
        {
            //TODO: handle error
            return;
        }

        mSocket.emit("create_room", json);
        Thread thread = new Thread(new Timeout(10000,handler), "timeout_thread");
        thread.start();
        done = false;

        dialog = new ProgressDialog(this);
        dialog.setMessage("Attempting to create frequency...");
        dialog.setIndeterminate(true);
        dialog.show();

    }

    private Emitter.Listener onSuccess = new Emitter.Listener() {
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
            String message = (String) args[0];

            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = message;
            handler.sendMessage(msg);
        }
    };

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

            Message msg = handler.obtainMessage();
            msg.what = 1;
            handler.sendMessage(msg);
        }
    };

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            try {
                dialog.dismiss();
            }
            catch(Exception ex)
            {

            }

            if(msg.what==0){
                done = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(CreateRoomActivity.this);
                builder.setMessage((String)msg.obj)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //TODO: Reload the RoomsActivity listview so it displays the new thing
                                Intent intent = new Intent();
                                intent.putExtra("payload", "something");
                                setResult(RESULT_OK, intent);
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();

                alert.show();
            }
            else if(msg.what == 1)
            {
                //Refused for some reason.
                done = true;
                //TODO: Show refusal message somehow
            }
            else if(msg.what == 3)
            {
                //timeout. authCycle prevents us from stepping on the toes of authentication
                if(!done) {
                    //TODO: Create timeout display code.
                }
            }
            return true;
        }
    });

    /*
    //depreciated stuff. historically significant.
    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            try {
                dialog.dismiss();
            }
            catch(Exception ex)
            {

            }

            if(msg.what==0){
                done = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(CreateRoomActivity.this);
                builder.setMessage((String)msg.obj)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //TODO: Reload the RoomsActivity listview so it displays the new thing
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();

                alert.show();
            }
            else if(msg.what == 1)
            {
                //Refused for some reason.
                done = true;
                //TODO: Show refusal message somehow
            }
            else if(msg.what == 3)
            {
                //timeout. authCycle prevents us from stepping on the toes of authentication
                if(!done) {
                    //TODO: Create timeout display code.
                }
            }

            super.handleMessage(msg);
        }
    };
    */
}
