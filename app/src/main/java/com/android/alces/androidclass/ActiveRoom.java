package com.android.alces.androidclass;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Socket;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ActiveRoom extends AppCompatActivity {

    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    ProgressDialog dialog;
    Timeout timerThread;
    String mFileName;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private ArrayAdapter<UserCompact> userAdapter;
    private ArrayAdapter<ChatMessage> messageAdapter;
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private ListView lvUsers;
    private ListView lvChat;
    Button pttButton;
    EditText etChat;

    /*TODO: Figure out how to design this. There's a good tutorial on how this could look
     *at https://github.com/nkzawa/socket.io-android-chat this integrates the chat aswell.
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_room);

        Global._currentHandler = handler;
        lvUsers = (ListView) findViewById(R.id.active_list_users);
        lvChat = (ListView) findViewById(R.id.active_list_chat);
        mFileName = getApplicationInfo().dataDir += "/audiorecordtest.mp4";
        fileChecks();

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
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        //Start action
                        startRecording();
                        pttButton.setBackgroundColor(Color.GREEN);
                        //pttButton.getBackground().setColorFilter(Color.parseColor("GREEN"), PorterDuff.Mode.MULTIPLY);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_CANCEL:
                        //Stop action
                        stopRecording();
                        pttButton.setBackgroundColor(Color.RED);
                        //pttButton.getBackground().setColorFilter(Color.parseColor("RED"), PorterDuff.Mode.MULTIPLY);
                        sendMessage();
                        break;
                }
                return true;
            }
        });

        etChat = (EditText)findViewById(R.id.active_editText_message);

        etChat.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendChat(etChat.getText().toString());
//                    InputMethodManager imm =
//                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(etChat.getWindowToken(), 0);
                    return true;
                } else {
                    int i = 0;
                }
                return false;
            }
        });

        mSocket.on("broadcast", recieveBroadcast);
        mSocket.on("room_users_change", roomContentChange);
        mSocket.on("new_message", roomNewMessage);
        mSocket.emit("request_all_users_room", "empty");

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
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

        mSocket.off("broadcast", recieveBroadcast);
        mSocket.off("room_users_change", roomContentChange);
        mSocket.off("new_message", roomNewMessage);

        Global._user.resetRoom();
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

    private void fileChecks()
    {
//        try {
//            File temp = new File(mFileName);
//            if (!temp.exists()) {
//                temp.createNewFile();
//            }
//        }
//        catch(IOException ex)
//        {
//            mFileName = Environment.getExternalStorageDirectory().toString() + "/audiorecordtest.mp4";
//            //Can't access that file. D:
//        }
        mFileName = Environment.getExternalStorageDirectory().toString() + "/audiorecordtest.mp4";
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Toast.makeText(ActiveRoom.this, "DEBUG" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording()
    {
        try {
            Thread.sleep(100);
        }
        catch(InterruptedException ex) {
            //TODO: D:
        }
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void startPlaying()
    {
        mPlayer = new MediaPlayer();
        try
        {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        }
        catch(IOException ex)
        {
            Toast.makeText(ActiveRoom.this, "IO Exception: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stoPlaying()
    {
        mPlayer.release();
        mPlayer = null;
    }

    private void sendMessage()
    {
        File outputFile = new File(mFileName);
        try {
            //byte[] toEncode = Files.toByteArray(outputFile);
            byte[] toEncode = Support.Files.toByteArray(outputFile);
            //This shit is REALLY slow
            //String encode = BaseEncoding.base64().encode(toEncode);
            String encode = Base64.encodeToString(toEncode, 0);
            mSocket.emit("broadcast", encode);
        }
        catch(IOException ex)
        {

        }
        catch(Exception ex)
        {
            int i = 0;
        }
    }

    private void sendChat(String message)
    {
        ChatMessage toSend = new ChatMessage(message, Global._user.name, "ohno");
        mSocket.emit("new_message", toSend.toJson());
        etChat.setText("");
    }

    private Emitter.Listener recieveBroadcast = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            String user;
            String payload;
            try {
                user = data.getString("user");
                payload = data.getString("payload");
            } catch (JSONException e) {
                return;
            }

            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = payload;
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener roomContentChange = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

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
    };

    private Emitter.Listener roomNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

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
    };

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            try {
                dialog.dismiss();
                timerThread.interrupt();
            } catch (Exception ex) {

            }
            if(msg.what == 0)
            {
                //TODO: Hack solution for testing
                String encoded = (String)msg.obj;
                encoded = encoded.trim().replaceAll("\n", "");
                byte[] decode = Base64.decode(encoded,0);
                //File outputFile = new File(mFileName);

                try {

                    //Files.write(decode, outputFile);
                    Support.Files.saveFileFromBytes(decode, mFileName);
                }
                catch(Exception ex)
                {

                }

                startPlaying();
            }
            if(msg.what == 1)
            {
                JSONArray tempJson = (JSONArray) msg.obj;
                ArrayList<UserCompact> listItems = new ArrayList<>();

                //Check to see if the list is empty. Why does this happen?
                if(tempJson != null) {
                    for (int i = 0; i < tempJson.length(); i++) {
                        try {
                            listItems.add(new UserCompact(tempJson.getJSONObject(i)));
                        } catch (JSONException ex) {

                        }
                    }
                }

                userAdapter = new ArrayAdapter<UserCompact>(getBaseContext(),
                        android.R.layout.simple_list_item_1,
                        listItems);

                lvUsers.setAdapter(userAdapter);
            }
            else if (msg.what == 2)
            {
                messages.add(new ChatMessage((JSONObject) msg.obj));
                messageAdapter = new ArrayAdapter<ChatMessage>(getBaseContext(),
                        android.R.layout.simple_list_item_1,
                        messages);

                lvChat.setAdapter(messageAdapter);
                lvChat.setSelection(messageAdapter.getCount() - 1);
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

                dialog = new ProgressDialog(ActiveRoom.this);
                dialog.setMessage("You lost connection. Reconnecting...");
                dialog.setIndeterminate(true);
                dialog.show();

                mSocket.emit("reauth", Global._user.toJson());
                mSocket.once("reauth_success", reauthRecover);
            }
            //Reauth recovery
            else if(msg.what == 253)
            {
                Toast.makeText(ActiveRoom.this, "Reauthed successfully.", Toast.LENGTH_LONG).show();
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
