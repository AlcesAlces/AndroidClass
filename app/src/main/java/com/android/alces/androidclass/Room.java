package com.android.alces.androidclass;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Each room needs to have enough information to identify them from the
 * array list.
 */
public class Room {

    public String roomId;
    public String creator;
    public String name;
    public boolean isPrivate;
    public RangeInfo rangeInfo;
    public int numUsers;
    public boolean isHeader = false;

    //TODO: Going to add another property to check for who is in the room

    //Create the room object from a JSONObject. Ezpz
    public Room(JSONObject inputJson)
    {
        try {
            roomId = inputJson.get("_id").toString();
            creator = inputJson.get("creator").toString();
            name = inputJson.get("room").toString();
            isPrivate = (Integer.parseInt((inputJson.get("isPrivate").toString()))
                         == 0 ? false : true);
            rangeInfo = new RangeInfo(inputJson);
            numUsers = inputJson.getInt("numUsers");
        }
        catch(JSONException exception)
        {

        }
    }

    //Dummy
    public Room()
    {
        isHeader = true;
    }

    @Override
    public String toString()
    {
        return this.name + " : " + numUsers;
    }

    public void updateRangeLatLon(double lat, double lon)
    {
        rangeInfo.lat = lat;
        rangeInfo.lon = lon;
    }

    public void updateRoomName (String newName)
    {
        name = newName;
    }

    public void updateIsPrivate(Boolean toggle)
    {
        isPrivate = toggle;
    }

    public void updateIsRanged (Boolean toggle)
    {
        rangeInfo.isRanged = toggle;
    }

    public void updateRange (double newRange)
    {
        rangeInfo.range = newRange;
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        try {
            json.put("_id", roomId);
            json.put("creator", creator);
            json.put("room", name);
            json.put("isPrivate", isPrivate ? 1 : 0);
            json.put("range", rangeInfo.range);
            json.put("originLat", rangeInfo.lat);
            json.put("originLon", rangeInfo.lon);

        }
        catch(JSONException ex)
        {

        }

        return json;
    }

}
