package com.android.alces.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.alces.androidclass.R;
import com.android.alces.androidclass.Room;

import java.util.ArrayList;
import java.util.HashMap;

public class RoomsAdapter extends BaseAdapter{

    private Activity activity;
    private ArrayList<Room> data;
    private static LayoutInflater inflater = null;

    public RoomsAdapter(Activity a, ArrayList<Room> d)
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
            vi = inflater.inflate(R.layout.custom_list_item, null);
        }

        TextView tvName = (TextView) vi.findViewById(R.id.tvCustomListName);
        TextView tvNumber = (TextView) vi.findViewById(R.id.tvCustomListNumber);

        if((data.get(position).isHeader))
        {
            tvName.setText("Room Name", TextView.BufferType.NORMAL);
            tvNumber.setText("Users", TextView.BufferType.NORMAL);
        }
        else
        {
            tvName.setText(data.get(position).name, TextView.BufferType.NORMAL);
            tvNumber.setText("" + data.get(position).numUsers, TextView.BufferType.NORMAL);
        }
        return vi;
    }
}
