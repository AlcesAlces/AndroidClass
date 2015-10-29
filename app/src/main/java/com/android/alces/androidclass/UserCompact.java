package com.android.alces.androidclass;

import org.json.JSONException;
import org.json.JSONObject;

public class UserCompact {
    private String name;
    private String id;

    public UserCompact(String _name, String _id)
    {
        name = _name;
        id = _id;
    }

    public UserCompact(JSONObject obj)
    {
        try {
            name = obj.getString("name");
            id = obj.getString("user");
        }
        catch(JSONException ex)
        {

        }
    }

    @Override
    public String toString()
    {
        return this.name;
    }

}
