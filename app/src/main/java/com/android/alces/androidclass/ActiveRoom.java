package com.android.alces.androidclass;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.alces.adapters.ChatAdapter;
import com.android.alces.adapters.UsersAdapter;
import com.android.alces.com.android.alces.threads.BroadcastTimer;
import com.android.alces.com.android.alces.threads.Timeout;
import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ActiveRoom extends AppCompatActivity {

    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    ProgressDialog dialog;
    Timeout timerThread;
    String mFileName;
    private UsersAdapter userAdapter;
    private ChatAdapter messageAdapter;
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private ListView lvUsers;
    private ListView lvChat;
    Button pttButton;
    EditText etChat;
    Button sendButton;
    private BroadcastTimer broadcastTimer;
    private Boolean canBroadcast = true;

    int byteCounter = 0;

    /*TODO: Figure out how to design this. There's a good tutorial on how this could look
     *at https://github.com/nkzawa/socket.io-android-chat this integrates the chat aswell.
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_room);
        setupRecieve();
        Global._currentHandler = handler;
        lvUsers = (ListView) findViewById(R.id.active_list_users);
        lvChat = (ListView) findViewById(R.id.active_list_chat);

        broadcastTimer = new BroadcastTimer(handler);
        broadcastTimer.track = audioTrack;

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

        /*
        Button settingsButton = (Button) findViewById(R.id.active_button_edit);

        settingsButton.setVisibility(Global._user.roomOwner ? View.VISIBLE : View.INVISIBLE);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activity = new Intent(ActiveRoom.this, RoomEditActivity.class);
                activity.putExtra("payload", new Gson().toJson(thisRoom));
                startActivityForResult(activity, 1);
            }
        });
        */

        pttButton = (Button) findViewById(R.id.active_button_ptt);
        pttButton.getBackground().setColorFilter(Color.parseColor("RED"), PorterDuff.Mode.MULTIPLY);
        pttButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            if (isSpeakerBroadcasting()) {
                                //Start action
                                status = true;
                                setSelfBroadcasting(true);
                                startStreaming();
                                //startRecording();
                                pttButton.setBackgroundColor(Color.GREEN);
                            }
                            //pttButton.getBackground().setColorFilter(Color.parseColor("GREEN"), PorterDuff.Mode.MULTIPLY);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_OUTSIDE:
                        case MotionEvent.ACTION_CANCEL:
                            if (isSpeakerBroadcasting()) {
                                //Stop action
                                status = false;
                                status2 = false;
                                record.release();
                                setSelfBroadcasting(false);
                                //stopRecording();
                                pttButton.setBackgroundColor(Color.RED);
                                //pttButton.getBackground().setColorFilter(Color.parseColor("RED"), PorterDuff.Mode.MULTIPLY);
                                //sendMessage();
                            }
                            break;
                    }
                }
                catch(Exception ex)
                {
                    Log.d("WT", "Unexpected exception: " + ex.getMessage());
                }

                return true;
            }
        });

        etChat = (EditText)findViewById(R.id.active_editText_message);

        etChat.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                try {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        sendChat(etChat.getText().toString());
//                    InputMethodManager imm =
//                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(etChat.getWindowToken(), 0);
                        return true;
                    } else {
                        int i = 0;
                    }
                }
                catch(Exception ex)
                {
                    Log.d("WT", "Unexpected exception: " + ex.getMessage());
                }
                    return false;
            }
        });


        sendButton = (Button) findViewById(R.id.active_button_send);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendChat(etChat.getText().toString());
                }
                catch(Exception ex)
                {
                    Log.d("WT", "Unexpected exception: " + ex.getMessage());
                }

            }});


        mSocket.on("broadcast", getBroadcastPart);
        mSocket.on("room_users_change", roomContentChange);
        mSocket.on("new_message", roomNewMessage);
        mSocket.emit("request_all_users_room", "empty");

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //Debug statement that may be useful.
//        pttButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                mSocket.emit("reauth", Global._user.toJson());
//            }
//        });
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

        mSocket.off("broadcast", getBroadcastPart);
        mSocket.off("room_users_change", roomContentChange);
        mSocket.off("new_message", roomNewMessage);

        //Global._user.resetRoom();
        mSocket.emit("leave_room", json);

        Intent intent = new Intent();
        intent.putExtra("payload", "this is pointless kek");
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        try {
            if (requestCode == 1) {
                if (resultCode == RESULT_OK) {
                    Intent intent2 = new Intent();
                    intent2.putExtra("payload", "something");
                    setResult(RESULT_OK, intent2);
                    finish();
                }
            }
        }
        catch(Exception ex)
        {
            Log.d("WT", "Unexpected exception: " + ex.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_active_room, menu);

        menu.findItem(R.id.action_edit).setVisible((Global._user.roomOwner ? true : false));

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

            case R.id.action_edit:
            {
                Intent activity = new Intent(ActiveRoom.this, RoomEditActivity.class);
                activity.putExtra("payload", new Gson().toJson(thisRoom));
                startActivityForResult(activity, 1);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Purpose here is to set your broadcast button based on your ability to broadcast
    private void setBroadcastButtonStatus()
    {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                if(canBroadcast)
//                {
//                    //set reds & enable
//                    pttButton.setBackgroundColor(Color.RED);
//                    pttButton.setEnabled(true);
//                }
//                else
//                {
//                    //set gray and disable
//                    pttButton.setBackgroundColor(Color.GRAY);
//                    pttButton.setEnabled(false);
//                }
//            }+
//        });

    }

    private boolean isSpeakerBroadcasting()
    {
        try {
            //In state playing.
            if (audioTrack.getPlaybackHeadPosition() != byteCounter) {
                return false;
            } else {
                return true;
            }
        }
        catch(Exception ex)
        {
            Log.d("WT", "Unexpected exception: " + ex.getMessage());
        }
        return false;
    }

    private void setUserBroadcasting(String id)
    {
        try {
            canBroadcast = false;
            Support.Users.setBroadcasterById(userAdapter.data, id);
            setBroadcastButtonStatus();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userAdapter.notifyDataSetChanged();
                }
            });
//
//        if(broadcastTimer == null)
//        {
//            broadcastTimer = new BroadcastTimer(handler);
//            broadcastTimer.run();
//        }
//        else if(!broadcastTimer.finished)
//        {
//            broadcastTimer.interrupt();
//            //broadcastTimer = new BroadcastTimer(handler);
//            broadcastTimer.run();
//        }
//        else
//        {
//            //broadcastTimer = new BroadcastTimer(handler);
//            broadcastTimer.run();
//        }
        }
        catch(Exception ex)
        {
            Log.d("WT", "Unexpected exception: " + ex.getMessage());
        }
    }

    private void setSelfBroadcasting(boolean set)
    {
        try {
            //broadcasting
            if (set) {
                ArrayList<UserCompact> users = userAdapter.data;

                for (UserCompact uc : users) {
                    if (uc.name.equals(Global._user.name)) {
                        uc.broadcasting = true;
                    }
                }

                userAdapter = new UsersAdapter(ActiveRoom.this, users);
                lvUsers.setAdapter(userAdapter);
            } else {
                ArrayList<UserCompact> users = userAdapter.data;

                for (UserCompact uc : users) {
                    if (uc.name.equals(Global._user.name)) {
                        uc.broadcasting = false;
                    }
                }
                userAdapter = new UsersAdapter(ActiveRoom.this, users);

                lvUsers.setAdapter(userAdapter);
            }
        }
        catch(Exception ex)
        {
            Log.d("WT", "Unexpected exception: " + ex.getMessage());
        }
    }

    private void sendChat(String message)
    {
        try {
            ChatMessage toSend = new ChatMessage(message, Global._user.name, "ohno");
            mSocket.emit("new_message", toSend.toJson());
            etChat.setText("");
        }
        catch(Exception ex)
        {
            Log.d("WT", "Unexpected exception: " + ex.getMessage());
        }
    }

    private Emitter.Listener roomContentChange = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            try {

                JSONArray data = (JSONArray) args[0];
//            String user;
//            String payload;
//            try {
//                user = data.getString("user");
//                payload = data.getString("payload");
//            } catch (JSONException e) {
//                return;
//            }

                Message msg = handler.obtainMessage();
                msg.what = 1;
                msg.obj = data;
                handler.sendMessage(msg);
            }
            catch(Exception ex)
            {
                Log.d("WT", "Unexpected exception: " + ex.getMessage());
            }
        }
    };

    private Emitter.Listener roomNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            try {
                JSONObject data = (JSONObject) args[0];
//            String user;
//            String payload;
//            try {
//                user = data.getString("user");
//                payload = data.getString("payload");
//            } catch (JSONException e) {
//                return;
//            }

                Message msg = handler.obtainMessage();
                msg.what = 2;
                msg.obj = data;
                handler.sendMessage(msg);
            }
            catch(Exception ex)
            {
                Log.d("WT", "Unexpected exception: " + ex.getMessage());
            }
        }
    };

    //Settings
    AudioRecord record;
    boolean status = false;
    private int sampleRate = 8000;
    private int channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    public void startStreaming() {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                    byte[] buffer = new byte[minBufSize];
                    record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);

                    record.startRecording();


                    while (status == true) {


                        //reading data from MIC into buffer
                        minBufSize = record.read(buffer, 0, buffer.length);

                        //String toSend = Base64.encodeToString(buffer, 0);
                        if (isSpeakerBroadcasting()) {
                            mSocket.emit("broadcast", buffer);
                        }
                    }
                }
                catch(Exception ex)
                {
                    Log.d("WT", "Unexpected exception: " + ex.getMessage());
                }
            }

        });
        streamThread.start();
    }

    private boolean status2 = true;
    private AudioTrack audioTrack;
    byte[] spBuffer = new byte[256];
    int minbufsize;

    public void setupRecieve()
    {
        try {
            minbufsize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, minbufsize, AudioTrack.MODE_STREAM);

            audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    //Reenable the UI.
                    Support.Users.setAllNotBroadcasting(userAdapter.data);
                    userAdapter.notifyDataSetChanged();
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {
                    //Nothing to do tbh
                }
            });

            audioTrack.play();
        }
        catch(Exception ex)
        {
            Log.d("WT", "Unexpected exception: " + ex.getMessage());
        }
    }

    private Emitter.Listener getBroadcastPart = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try {
                JSONObject data = (JSONObject) args[0];

                //String encoded;
                byte[] encoded;
                String id;
                try {
                    //encoded  = data.getString("payload");
                    encoded = (byte[]) data.get("payload");
                    id = data.getString("id");
                } catch (JSONException e) {
                    return;
                }

                //We're getting a broadcast that means we need to set the components properly.
                setUserBroadcasting(id);


                Message msg = handler.obtainMessage();
                msg.obj = encoded;
                msg.what = 0;
                handler.sendMessage(msg);
            }
            catch(Exception ex)
            {
                Log.d("WT", "Unexpected exception: " + ex.getMessage());
            }
        }
    };

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            try {
                try {
                    dialog.dismiss();
                    timerThread.interrupt();
                } catch (Exception ex) {

                }
                //Get broadcast.
                if (msg.what == 0) {
                    spBuffer = (byte[]) msg.obj;
                    audioTrack.write(spBuffer, 0, minbufsize);
                    byteCounter += (minbufsize / 2);

                    audioTrack.setNotificationMarkerPosition(byteCounter);

                }
                if (msg.what == 1) {
                    JSONArray tempJson = (JSONArray) msg.obj;
                    ArrayList<UserCompact> listItems = new ArrayList<>();

                    //Check to see if the list is empty. Why does this happen?
                    if (tempJson != null) {
                        for (int i = 0; i < tempJson.length(); i++) {
                            try {
                                listItems.add(new UserCompact(tempJson.getJSONObject(i)));
                            } catch (JSONException ex) {
                            }
                        }
                    }

                    userAdapter = new UsersAdapter(ActiveRoom.this, listItems);

                    lvUsers.setAdapter(userAdapter);
                } else if (msg.what == 2) {
                    messages.add(new ChatMessage((JSONObject) msg.obj));
                    messageAdapter = new ChatAdapter(ActiveRoom.this, messages);

                    lvChat.setAdapter(messageAdapter);
                    lvChat.setSelection(messageAdapter.getCount() - 1);
                }
                //Broadcast timer ran out.
                else if (msg.what == 4) {
//                Support.Users.setAllNotBroadcasting(userAdapter.data);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        userAdapter.notifyDataSetChanged();
//                    }
//                });

                    canBroadcast = true;
                    //setBroadcastButtonStatus();
                }
                //Reauth needed
                else if (msg.what == 254) {
//                Thread thread = new Thread(new Timeout(handler), "timeout_thread");
//                thread.start();
                    timerThread = new Timeout(handler);
                    timerThread.start();
                    //TODO: Fix this variable.
                    //done = false;

                    dialog = new ProgressDialog(ActiveRoom.this);
                    dialog.setMessage("You lost connection. Reconnecting...");
                    dialog.setIndeterminate(true);
                    dialog.show();

                    mSocket.emit("reauth", Global._user.toJson());
                    mSocket.once("reauth_success", reauthRecover);
                }
                //Reauth recovery
                else if (msg.what == 253) {
                    Toast.makeText(ActiveRoom.this, "Reauthed successfully.", Toast.LENGTH_LONG).show();
                }
            }
            catch(Exception e)
            {
                Log.d("WT", "Unexpected exception: " + e.getMessage());
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
