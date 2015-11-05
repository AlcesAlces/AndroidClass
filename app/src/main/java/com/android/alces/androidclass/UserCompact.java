package com.android.alces.androidclass;

import org.json.JSONException;
import org.json.JSONObject;

public class UserCompact {
    public String name;
    public String id;
    public boolean broadcasting = false;

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

    public UserCompact makeClone()
    {
        return new UserCompact(name, id);
    }

    @Override
    public String toString()
    {
        return this.name;
    }

}
