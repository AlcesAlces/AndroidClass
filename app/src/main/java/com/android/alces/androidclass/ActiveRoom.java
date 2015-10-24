package com.android.alces.androidclass;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.nkzawa.engineio.client.Socket;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class ActiveRoom extends Activity {

    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    /*TODO: Figure out how to design this. There's a good tutorial on how this could look
     *at https://github.com/nkzawa/socket.io-android-chat this integrates the chat aswell.
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_room);

        Bundle extras = getIntent().getExtras();
        //The bundle is a serialized json object with the Gson code.
        if(extras != null)
        {
            //Deserialize
            String jsonObject = extras.getString("payload");
            thisRoom = new Gson().fromJson(jsonObject, Room.class);

            ((TextView) findViewById(R.id.tvRoomFrequencyName)).setText(thisRoom.name);
        }
        else
        {
            //TODO: Handle this. Someone didn't pass information correctly.
            finish();
        }

        Button settingsButton = (Button) findViewById(R.id.active_button_edit);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activity = new Intent(ActiveRoom.this, RoomEditActivity.class);
                activity.putExtra("payload", new Gson().toJson(thisRoom));
                startActivityForResult(activity, 1);
            }
        });

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        JSONObject json = new JSONObject();
        try {
            json.put("roomId", thisRoom.roomId);
        }
        catch(JSONException ex)
        {
            //TODO: handle error
        }

        mSocket.emit("leave_room", json);

        Intent intent = new Intent();
        intent.putExtra("payload", "this is pointless kek");
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == 1)
        {
            if(resultCode == RESULT_OK) {
                Intent intent2 = new Intent();
                intent2.putExtra("payload", "something");
                setResult(RESULT_OK, intent2);
                finish();
            }
        }
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
}
