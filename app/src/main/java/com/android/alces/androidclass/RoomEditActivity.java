package com.android.alces.androidclass;

import android.app.Activity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class RoomEditActivity extends Activity {
    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    Boolean cycle = false;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_room);

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

        mSocket.on("success_update_room", onUpdateSuccess);
        mSocket.on("refuse_update_room", onEditActivityFailure);
        mSocket.on("refuse_delete_room", onEditActivityFailure);
        mSocket.on("success_delete_room", onDeleteSuccess);
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
        dialog.setMessage("Logging in. Please wait....");
        dialog.setIndeterminate(true);
        dialog.show();
        mSocket.emit("delete_room", thisRoom.toJson());
        //Use timeout class and handler to stop this from going forever.
        Thread thread = new Thread(new Timeout(10000,handler), "timeout_thread");
        thread.start();
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
        thisRoom.updateRange(Double.parseDouble(((EditText) findViewById(R.id.edit_editText_frange)).getText().toString()));

        setComponentsByRoom(thisRoom);

        doUpdate();
    }

    private void doUpdate()
    {
        dialog = new ProgressDialog(RoomEditActivity.this);
        dialog.setMessage("Updating room. Please wait....");
        dialog.setIndeterminate(true);
        dialog.show();
        //TODO: Build/Emit data.


        mSocket.emit("update_room", thisRoom.toJson());
        //Use timeout class and handler to stop this from going forever.
        Thread thread = new Thread(new Timeout(10000,handler), "timeout_thread");
        thread.start();
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
            } catch (Exception ex) {

            }
            if (msg.what == 0) {
                //Update Success
                cycle = true;
                Toast.makeText(RoomEditActivity.this,"Successfully updated room!",Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.putExtra("payload", "something");
                setResult(RESULT_OK, intent);
                finish();
            }
            else if(msg.what == 1)
            {
                //Delete Success
                Toast.makeText(RoomEditActivity.this,"Successfully deleted room!",Toast.LENGTH_LONG).show();
                cycle = true;
                Intent intent = new Intent();
                intent.putExtra("payload", "something");
                setResult(RESULT_OK, intent);
                finish();
            }
            else if(msg.what == 2)
            {
                cycle = true;
                //Whatever failure message the server sent.
                Toast.makeText(RoomEditActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
            }
            else if (msg.what == 3) {
                if (!cycle) {
                    Toast.makeText(RoomEditActivity.this,"Connection timed out!",Toast.LENGTH_LONG).show();
                    //TODO: Mike: Investigate why the connection isn't working.
                }
            }
            return true;
        }
    });
}
