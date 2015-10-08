package com.android.alces.androidclass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class RoomsActivity extends Activity {

    private Socket mSocket = Global.globalSocket;
    private ListView lv;
    private ArrayAdapter<Room> adapter;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        lv = (ListView) findViewById(R.id.roomsListView);

        //TODO: Check for non-connected socket.
        mSocket.on("server error", serverError);

        //TODO: More general solution for this
        dialog = new ProgressDialog(this);
        dialog.setMessage("Retrieving frequency information");
        dialog.setIndeterminate(true);
        dialog.show();

        mSocket.on("all rooms", displayAllRooms);
        mSocket.on("reauth", reAuth);
        //TODO: Watch for timeout
        mSocket.emit("get all rooms", "nothing");

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int i = 0;
                //Dereference
                Room lvi = (Room)lv.getItemAtPosition(position);

                JSONObject json = new JSONObject();
                try {
                    json.put("roomId", lvi.roomId);
                }
                catch(JSONException ex)
                {

                }

                //TODO: Handle this fully.
                mSocket.emit("join room", json);

                //TODO: Create a context menu about joining.
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mSocket.off("server error", serverError);
        mSocket.off("all rooms", displayAllRooms);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rooms, menu);
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

    private Emitter.Listener displayAllRooms = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            JSONArray data = (JSONArray) args[0];
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
            //TODO: Parse this in a more intelligent way. Parse it into a self-contained object.
            msg.obj = data;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener reAuth = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            String data = (String) args[0];
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
            //TODO: Parse this in a more intelligent way. Parse it into a self-contained object.
            msg.obj = data;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener serverError = new Emitter.Listener() {
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
            msg.what = 1;
            handler.sendMessage(msg);
        }
    };


    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0) {
                //TODO: This is a crappy way to do this. Switch to a custom list adapter.
                JSONArray tempJson = (JSONArray) msg.obj;
                ArrayList<Room> listItems = new ArrayList<>();

                dialog.dismiss();
                for(int i = 0; i < tempJson.length(); i++)
                {
                    try
                    {
                    listItems.add(new Room(tempJson.getJSONObject(i)));
                    }
                    catch(JSONException ex)
                    {

                    }
                }

                adapter = new ArrayAdapter<Room>(getBaseContext(),
                        android.R.layout.simple_list_item_1,
                        listItems);

                lv.setAdapter(adapter);
            }
            //Reauth message
            else if(msg.what == 1)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(RoomsActivity.this);
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
            super.handleMessage(msg);
        }
    };
}
