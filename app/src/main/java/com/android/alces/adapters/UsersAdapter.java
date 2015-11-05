package com.android.alces.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.alces.androidclass.R;
import com.android.alces.androidclass.Room;
import com.android.alces.androidclass.UserCompact;

import java.util.ArrayList;

public class UsersAdapter extends BaseAdapter{

    private Activity activity;
    public ArrayList<UserCompact> data;
    private static LayoutInflater inflater = null;

    public UsersAdapter(Activity a, ArrayList<UserCompact> d)
    {
        activity = a;
        data = d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public int getCount()
    {
        return data.size();
    }

    public Object getItem(int position)
    {
        return data.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View vi = convertView;
        if(convertView == null)
        {
            vi = inflater.inflate(R.layout.custom_list_users, null);
        }

        TextView tvName = (TextView) vi.findViewById(R.id.tvCustomUsersName);
        ImageView iv = (ImageView)vi.findViewById(R.id.clUsersImage);
        tvName.setText(data.get(position).name, TextView.BufferType.NORMAL);

        if(data.get(position).broadcasting)
        {
            iv.setImageResource(R.drawable.is_broadcast);
        }
        else
        {
            iv.setImageResource(R.drawable.not_broadcast);
        }
        return vi;
    }

}
