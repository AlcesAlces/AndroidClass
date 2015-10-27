package com.android.alces.androidclass;

import org.json.JSONException;
import org.json.JSONObject;

public class RangeInfo {
    //Origin info
    public double lat;
    public double lon;
    public boolean isRanged;
    public double range;

    public RangeInfo(Boolean _isRanged, double _lat, double _lon, double _range)
    {
        if(isRanged)
        {
            lat = _lat;
            lon = _lon;
            range = _range;
        }
        isRanged = _isRanged;
    }

    public RangeInfo(JSONObject json)
    {
        try {
            try {
                lat = (double) json.get("originLat");
                lon = (double) json.get("originLon");
            }
            catch(ClassCastException ex)
            {
                lat = 0.0;
                lon = 0.0;
            }



            range = (int)json.get("range");
            isRanged = (range != -1);

        }
        catch(JSONException exception)
        {

        }
    }

    @Override
    public String toString()
    {
        return this.lat + "," + this.lon;
    }
}
