package com.android.alces.androidclass;

import org.json.JSONException;
import org.json.JSONObject;

public class User {

    public String name = null;
    //String id  = null;
    public double lon;
    public double lat;
    public String roomId = "-1";
    public boolean roomOwner = false;

    public User(String _name, double _lon, double _lat)
    {
        name = _name;
        lon = _lon;
        lat = _lat;
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("lat", lat);
            json.put("lon", lon);
            json.put("roomId", roomId);
        }
        catch(JSONException ex)
        {
        }

        return json;
    }
}
