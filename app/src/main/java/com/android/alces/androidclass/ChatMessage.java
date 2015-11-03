package com.android.alces.androidclass;


import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {

    public String message;
    public String user;
    String id;
    public Boolean isHeader = false;

    @Override
    public String toString()
    {
        return this.user + ": " + this.message;
    }

    public ChatMessage(String msg, String usr, String _id)
    {
        message = msg;
        user = usr;
        id = _id;
    }

    public ChatMessage(JSONObject messageObject)
    {
        try
        {
            message = messageObject.getString("message");
            user = messageObject.getString("user");
            id = messageObject.getString("id");
        }
        catch(JSONException ex) {

        }
    }

    public ChatMessage()
    {
        isHeader = true;
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put("message", message);
            json.put("user", user);
            json.put("id", id);
        }
        catch(JSONException ex) {

        }
        return json;
    }


}
