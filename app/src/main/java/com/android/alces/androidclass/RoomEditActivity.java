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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class RoomEditActivity extends Activity {
    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    /*TODO: Figure out how to design this. There's a good tutorial on how this could look
     *at https://github.com/nkzawa/socket.io-android-chat this integrates the chat aswell.
    */

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
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
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

    public void disbandFrequency()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(RoomEditActivity.this);
        builder.setMessage("Remove this frequency? This cannot be undone.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        //TODO: Delete the room

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

    public void updateLatLng()
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

        //TODO: Update server with new information

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Updating Frequency Settings...");
        dialog.setIndeterminate(true);
        dialog.show();
    }
}
