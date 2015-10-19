package com.android.alces.androidclass;

public class RangeInfo {
    //Origin info
    double lat;
    double lon;
    public boolean isRanged;
    double range;

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
}
