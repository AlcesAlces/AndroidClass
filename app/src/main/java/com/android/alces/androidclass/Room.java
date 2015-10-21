package com.android.alces.androidclass;

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
    //TODO: Going to add another property to check for who is in the room

    //Create the room object from a JSONObject. Ezpz
    public Room(JSONObject inputJson)
    {
        try {
            roomId = inputJson.get("_id").toString();
            creator = inputJson.get("_creator").toString();
            name = inputJson.get("_room").toString();
            isPrivate = (Integer.parseInt((inputJson.get("_isPrivate").toString()))
                         == 0 ? false : true);
            rangeInfo.range = (Double.parseDouble(inputJson.get("_range").toString()));
            rangeInfo.isRanged = (Integer.parseInt((inputJson.get("_isRanged").toString()))
                                == 0 ? false : true);
        }
        catch(JSONException exception)
        {

        }
    }

    @Override
    public String toString()
    {
        return this.name;
    }

}
