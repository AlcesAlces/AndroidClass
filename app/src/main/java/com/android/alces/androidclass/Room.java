package com.android.alces.androidclass;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

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
    public ArrayList<UserCompact> approvedUsers;

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

            //Parse permitted users
            JSONArray tempArray = (JSONArray)inputJson.get("permittedUsers");
            approvedUsers = new ArrayList<>();
            for(int i = 0; i < tempArray.length(); i++)
            {
                try
                {
                    String name = tempArray.getJSONObject(i).getString("userName");
                    String id = tempArray.getJSONObject(i).getString("userID");
                    approvedUsers.add(new UserCompact(name,id));
                }
                catch(JSONException arrayEx)
                {
                    Log.d("VS", "Rooms threw error: " + arrayEx.getMessage());
                }
            }

        }
        catch(JSONException exception)
        {
            Log.d("VS", "Rooms threw error: " + exception.getMessage());
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

            //Build permitted users:
            JSONArray permittedUsers = new JSONArray();
            //Basically permittedUSers is an array full of JSONObjects.
            for(UserCompact uc : approvedUsers)
            {
                JSONObject toAppend = new JSONObject();
                toAppend.put("userID", uc.id);
                toAppend.put("userName", uc.name);
                permittedUsers.put(toAppend);
            }

            json.put("permittedUsers", permittedUsers);
        }
        catch(JSONException ex)
        {
            Log.d("VS", "Tojson in Room threw error: " + ex.getMessage());
        }

        return json;
    }

    public void removeUser(UserCompact uc)
    {
        //Found this method online the iterator removes the need to check for going out of bounds.
        for(Iterator<UserCompact> it = approvedUsers.iterator(); it.hasNext(); )
        {
            UserCompact user = it.next();
            if(user.id.equals(uc.id))
            {
                it.remove();
            }
        }
    }

    public boolean isDuplicate(UserCompact uc)
    {
        boolean returnValue = false;
        //Found this method online the iterator removes the need to check for going out of bounds.
        for(Iterator<UserCompact> it = approvedUsers.iterator(); it.hasNext(); )
        {
            UserCompact user = it.next();
            if(user.id.equals(uc.id))
            {
                returnValue = true;
            }
        }

        return returnValue;
    }

}
