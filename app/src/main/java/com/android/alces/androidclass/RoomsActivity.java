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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;

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
    private Room attempt = null;
    boolean done = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        Global._currentHandler = handler;

        lv = (ListView) findViewById(R.id.roomsListView);

        getAllRooms();

        mSocket.on("all rooms", displayAllRooms);
        Button createButton = (Button) findViewById(R.id.btnCreateRooms);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(RoomsActivity.this, CreateRoomActivity.class);
                RoomsActivity.this.startActivityForResult(myIntent, 1);
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                tryJoin(position);
            }
        });


        mSocket.on("join_success", joinSuccess);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mSocket.off("server error", serverError);
        mSocket.off("all rooms", displayAllRooms);
        mSocket.off("join_success", joinSuccess);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == 1)
        {
            if(resultCode == RESULT_OK) {
                getAllRooms();
            }
        }
    }

    private void getAllRooms()
    {
        done = false;
        dialog = new ProgressDialog(this);
        dialog.setMessage("Retrieving frequency information");
        dialog.setIndeterminate(true);
        dialog.show();
        mSocket.emit("get all rooms", "nothing");
        Thread thread = new Thread(new Timeout(10000,handler), "timeout_thread");
        thread.start();
    }

    private void tryJoin(int position)
    {
        done = false;
        Room lvi = (Room)lv.getItemAtPosition(position);
        attempt = lvi;
        JSONObject json = new JSONObject();
        try {
            json.put("roomId", lvi.roomId);
        }
        catch(JSONException ex)
        {
            //TODO: handle error
        }

        //TODO: Handle this fully.
        mSocket.emit("join_room", json);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Joining. Please wait....");
        dialog.setIndeterminate(true);
        dialog.show();
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

    private Emitter.Listener joinSuccess = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
             JSONObject data = (JSONObject) args[0];

            boolean owner = false;
            try {
                owner  = data.getBoolean("perms");
            } catch (JSONException e) {
                return;
            }
            Message msg = handler.obtainMessage();
            msg.obj = owner;
            msg.what = 2;
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

            if (msg.what == 0) {
                done = true;
                JSONArray tempJson = (JSONArray) msg.obj;
                ArrayList<Room> listItems = new ArrayList<>();

                dialog.dismiss();
                for (int i = 0; i < tempJson.length(); i++) {
                    try {
                        listItems.add(new Room(tempJson.getJSONObject(i)));
                    } catch (JSONException ex) {

                    }
                }

                //TODO: Sort listItems. Java doesn't have built in stuff so may have to write custom.

                adapter = new ArrayAdapter<Room>(getBaseContext(),
                        android.R.layout.simple_list_item_1,
                        listItems);

                lv.setAdapter(adapter);
            }
            //Reauth message
            else if (msg.what == 1) {
                done = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(RoomsActivity.this);
                builder.setMessage((String) msg.obj)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //TODO: This is not really valid. Create an intelligent reauth system
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();

                alert.show();
            }
            else if (msg.what == 2)
            {
                done = true;
                if(attempt != null)
                {
                    Global._user.roomId = attempt.roomId;
                    Global._user.roomOwner = (Boolean)msg.obj;
                    Intent activity = new Intent(RoomsActivity.this, ActiveRoom.class);
                    activity.putExtra("payload", new Gson().toJson(attempt));
                    startActivityForResult(activity, 1);
                }
            }
            else if(msg.what == 3)
            {
                if(!done)
                {
                    //TODO: Add timeout to this section
                }
            }
            //Reauth needed
            else if(msg.what == 254)
            {
                done = true;
                Thread thread = new Thread(new Timeout(10000,handler), "timeout_thread");
                thread.start();
                //TODO: Fix this variable.
                //done = false;

                dialog = new ProgressDialog(RoomsActivity.this);
                dialog.setMessage("You lost connection. Reconnecting...");
                dialog.setIndeterminate(true);
                dialog.show();

                mSocket.emit("reauth", Global._user.toJson());
                mSocket.once("reauth_success", reauthRecover);
            }
            //Reauth recovery
            else if(msg.what == 253)
            {
                Toast.makeText(RoomsActivity.this, "Reauthed successfully.", Toast.LENGTH_LONG).show();
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
