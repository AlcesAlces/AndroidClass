package com.android.alces.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.alces.androidclass.ChatMessage;
import com.android.alces.androidclass.R;
import com.android.alces.androidclass.Room;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatAdapter extends BaseAdapter{

    private Activity activity;
    private ArrayList<ChatMessage> data;
    private static LayoutInflater inflater = null;

    public ChatAdapter(Activity a, ArrayList<ChatMessage> d)
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
            vi = inflater.inflate(R.layout.custom_list_chat, null);
        }

        TextView tvName = (TextView) vi.findViewById(R.id.tvCustomChatName);
        TextView tvMsg = (TextView) vi.findViewById(R.id.tvCustomChatMsg);

        if((data.get(position)).isHeader)
        {
            tvName.setText("Chat", TextView.BufferType.NORMAL);
            tvMsg.setText("", TextView.BufferType.NORMAL);
        }
        else
        {
            tvName.setText(data.get(position).user + ": ", TextView.BufferType.NORMAL);
            tvMsg.setText(data.get(position).message, TextView.BufferType.NORMAL);
        }
        return vi;
    }
}
