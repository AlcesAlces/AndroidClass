package com.android.alces.androidclass;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Socket;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ActiveRoom extends Activity {

    Room thisRoom = null;
    private com.github.nkzawa.socketio.client.Socket mSocket = Global.globalSocket;
    ProgressDialog dialog;
    Timeout timerThread;
    String mFileName;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    /*TODO: Figure out how to design this. There's a good tutorial on how this could look
     *at https://github.com/nkzawa/socket.io-android-chat this integrates the chat aswell.
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_room);

        Global._currentHandler = handler;

        mFileName = getApplicationInfo().dataDir += "/audiorecordtest.mp4";

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

        settingsButton.setVisibility(Global._user.roomOwner ? View.VISIBLE : View.INVISIBLE);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activity = new Intent(ActiveRoom.this, RoomEditActivity.class);
                activity.putExtra("payload", new Gson().toJson(thisRoom));
                startActivityForResult(activity, 1);
            }
        });

        Button pttButton = (Button) findViewById(R.id.active_button_ptt);

        pttButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        //Start action
                        startRecording();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_CANCEL:
                        //Stop action
                        stopRecording();
                        sendMessage();
                        break;
                }
                return true;
            }
        });

        mSocket.on("broadcast", recieveBroadcast);
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

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
        }

        mRecorder.start();
    }

    private void stopRecording()
    {
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
            byte[] toEncode = Files.toByteArray(outputFile);
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
                byte[] decode = BaseEncoding.base64().decode(encoded);
                File outputFile = new File(mFileName);

                try {

                    Files.write(decode, outputFile);
                }
                catch(Exception ex)
                {

                }

                startPlaying();
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

    public static byte[] convertFileToByteArray(File f)
        {
               byte[] byteArray = null;
              try
              {
                            InputStream inputStream = new FileInputStream(f);
                   ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024*8];
                    int bytesRead = 0;

                           while((bytesRead = inputStream.read(b)) != -1)
                        {
                                   bos.write(b,0,bytesRead);
                    }

                            byteArray = bos.toByteArray();
                }
                catch(IOException e)
                {
                            e.printStackTrace();
                }
                return byteArray;
            }
}
