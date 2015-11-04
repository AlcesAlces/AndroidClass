package com.android.alces.androidclass;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.alces.com.android.alces.threads.Timeout;
import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson;

public class RoomEditActivity extends AppCompatActivity {
    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    ProgressDialog dialog;
    Timeout timerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_room);

        Global._currentHandler = handler;

        Bundle extras = getIntent().getExtras();
        //The bundle is a serialized json object with the Gson code.
        if(extras != null)
        {
            //Deserialize
            String jsonObject = extras.getString("payload");
            thisRoom = new Gson().fromJson(jsonObject, Room.class);

            setComponentsByRoom(thisRoom);
        }
        else
        {
            //TODO: Handle this. Someone didn't pass information correctly.
            finish();
        }

        // Button Listeners

        Button btnResetOrigin = (Button) findViewById(R.id.edit_button_reset);
        btnResetOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLatLng();
            }
        });

        Button btnUpdate = (Button) findViewById(R.id.edit_button_update);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFrequency();
            }
        });

        Button btnDisband = (Button) findViewById(R.id.edit_button_disband);
        btnDisband.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disbandFrequency();
            }
        });

        final CheckBox cbRange = (CheckBox) findViewById(R.id.edit_checkBox_frange);
        cbRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!cbRange.isChecked())
                {
                    thisRoom.updateRange(-1);
                    thisRoom.updateIsRanged(false);
                    setComponentsByRoom(thisRoom);
                }
                if (cbRange.isChecked() && thisRoom.rangeInfo.range <= 0)
                {
                    thisRoom.updateRange(1);
                    thisRoom.updateIsRanged(true);
                    setComponentsByRoom(thisRoom);
                }
            }
        });

        mSocket.on("success_update_room", onUpdateSuccess);
        mSocket.on("refuse_update_room", onEditActivityFailure);
        mSocket.on("refuse_delete_room", onEditActivityFailure);
        mSocket.on("success_delete_room", onDeleteSuccess);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //Remove listeners.
        mSocket.off("success_update_room", onUpdateSuccess);
        mSocket.off("refuse_update_room", onEditActivityFailure);
        mSocket.off("refuse_delete_room", onEditActivityFailure);
        mSocket.off("success_delete_room", onDeleteSuccess);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_active_room, menu);

        menu.findItem(R.id.action_edit).setVisible(false);

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

    /*
    Note: The way to update this is to pass the object in and it will edit all of the values.
    The object will NOT be updated into the database until the button is pressed.
     */
    public void setComponentsByRoom(Room toSet)
    {
        //Components
        //Check boxes
        CheckBox cbPrivate = (CheckBox) findViewById(R.id.edit_checkBox_fprivate);
        CheckBox cbRange = (CheckBox) findViewById(R.id.edit_checkBox_frange);
        //Edit Texts
        EditText etName = (EditText) findViewById(R.id.edit_editText_fname);
        EditText etAddUser = (EditText) findViewById(R.id.edit_editText_addUser);
        EditText etRemoveUser = (EditText) findViewById(R.id.edit_editText_removeUser);
        EditText etRange = (EditText) findViewById(R.id.edit_editText_frange);
        //Text View
        TextView tvOrigin = (TextView) findViewById(R.id.edit_tv_originValue);
        //Buttons
        Button btnResetOrigin = (Button) findViewById(R.id.edit_button_reset);
        Button btnDisband = (Button) findViewById(R.id.edit_button_disband);
        Button btnUpdate = (Button) findViewById(R.id.edit_button_update);

        //Range Sets
        cbRange.setChecked(toSet.rangeInfo.isRanged);
        etRange.setText(Double.toString(toSet.rangeInfo.range));

        //Privacy settings
        cbPrivate.setChecked(toSet.isPrivate);

        //Text box settings
        etName.setText(toSet.name);
        tvOrigin.setText(toSet.rangeInfo.toString());
    }

    private void disbandFrequency()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomEditActivity.this);
        builder.setMessage("Remove this frequency? This cannot be undone.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        doDisband();

                        return;
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
        AlertDialog alert = builder.create();

        alert.show();
    }

    private void doDisband()
    {
        dialog = new ProgressDialog(RoomEditActivity.this);
        dialog.setMessage("Disbanding frequency. Please wait....");
        dialog.setIndeterminate(true);
        dialog.show();
        mSocket.emit("delete_room", thisRoom.toJson());
        //Use timeout class and handler to stop this from going forever.
//        Thread thread = new Thread(new Timeout(handler), "timeout_thread");
//        thread.start();
        timerThread = new Timeout(handler);
        timerThread.start();
    }

    private void updateLatLng()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomEditActivity.this);
        builder.setMessage("Reset the origin of the frequency to your current location?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        try {
                            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            double longitude = location.getLongitude();
                            double latitude = location.getLatitude();
                            thisRoom.updateRangeLatLon(latitude, longitude);
                            setComponentsByRoom(thisRoom);
                        }
                        catch(SecurityException ex)
                        {

                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
        AlertDialog alert = builder.create();

        alert.show();
    }

    public void updateFrequency()
    {
        thisRoom.updateRoomName(((EditText) findViewById(R.id.edit_editText_fname)).getText().toString());
        thisRoom.updateIsPrivate(((CheckBox) findViewById(R.id.edit_checkBox_fprivate)).isChecked());
        thisRoom.updateIsRanged(((CheckBox) findViewById(R.id.edit_checkBox_frange)).isChecked());

        //TODO: Better blank range handle?
        if (((EditText) findViewById(R.id.edit_editText_frange)).getText().toString().trim().isEmpty())
        {
            if (thisRoom.rangeInfo.isRanged)
            {
                thisRoom.updateRange(1);
            }
            else
            {
                thisRoom.updateRange(-1);
            }
        }
        else
        {
            if (thisRoom.rangeInfo.isRanged)
            {
                if (Double.parseDouble(((EditText) findViewById(R.id.edit_editText_frange)).getText().toString()) <= 0)
                {
                    thisRoom.updateRange(1);
                }
                else
                {
                    thisRoom.updateRange(Double.parseDouble(((EditText) findViewById(R.id.edit_editText_frange)).getText().toString()));
                }
            }
            else
            {
                thisRoom.updateRange(-1);
            }
        }

        setComponentsByRoom(thisRoom);

        doUpdate();
    }

    private void doUpdate()
    {
        dialog = new ProgressDialog(RoomEditActivity.this);
        dialog.setMessage("Updating frequency. Please wait....");
        dialog.setIndeterminate(true);
        dialog.show();
        //TODO: Build/Emit data.


        mSocket.emit("update_room", thisRoom.toJson());
        //Use timeout class and handler to stop this from going forever.
//        Thread thread = new Thread(new Timeout(handler), "timeout_thread");
//        thread.start();
        timerThread = new Timeout(handler);
        timerThread.start();
    }

    //Use to signify update success.
    private Emitter.Listener onUpdateSuccess = new Emitter.Listener() {
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
            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = message;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onDeleteSuccess = new Emitter.Listener() {
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
            msg.what = 1;
            msg.obj = message;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onEditActivityFailure = new Emitter.Listener() {
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

    //Basically this is the ONLY place where we can interact with the UI thread once we've spun off.
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            try {
                dialog.dismiss();
                timerThread.interrupt();
            } catch (Exception ex) {

            }
            if (msg.what == 0) {
                //Update Success
                Toast.makeText(RoomEditActivity.this,"Successfully updated frequency!",Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.putExtra("payload", "something");
                setResult(RESULT_OK, intent);
                finish();
            }
            else if(msg.what == 1)
            {
                //Delete Success
                Toast.makeText(RoomEditActivity.this,"Successfully deleted frequency!",Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.putExtra("payload", "something");
                setResult(RESULT_OK, intent);
                finish();
            }
            else if(msg.what == 2)
            {
                //Whatever failure message the server sent.
                Toast.makeText(RoomEditActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
            }
            else if (msg.what == 3) {
                Toast.makeText(RoomEditActivity.this,"Connection timed out!",Toast.LENGTH_LONG).show();
            }
            //Reauth needed
            else if(msg.what == 254)
            {
//                Thread thread = new Thread(new Timeout(handler), "timeout_thread");
//                thread.start();
                timerThread = new Timeout(handler);
                timerThread.start();
                //TODO: Fix this variable.
                //done = false;

                dialog = new ProgressDialog(RoomEditActivity.this);
                dialog.setMessage("You lost connection. Reconnecting...");
                dialog.setIndeterminate(true);
                dialog.show();

                mSocket.emit("reauth", Global._user.toJson());
                mSocket.once("reauth_success", reauthRecover);
            }
            //Reauth recovery
            else if(msg.what == 253)
            {
                Toast.makeText(RoomEditActivity.this, "Reauthed successfully.", Toast.LENGTH_LONG).show();
            }
            return true;
        }
    });

    private Emitter.Listener reauthRecover = new Emitter.Listener() {
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
            msg.what = 253;
            handler.sendMessage(msg);
        }
    };
}
