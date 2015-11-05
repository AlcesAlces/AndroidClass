package com.android.alces.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.alces.androidclass.R;
import com.android.alces.androidclass.UserCompact;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteUserAdapter extends BaseAdapter implements Filterable{

    private Activity activity;
    public ArrayList<UserCompact> backup;
    public ArrayList<UserCompact> data;
    private static LayoutInflater inflater = null;
    private ItemFilter mFilter = new ItemFilter();

    public AutocompleteUserAdapter(Activity a, ArrayList<UserCompact> d)
    {
        activity = a;
        backup = (ArrayList<UserCompact>)d.clone();
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
            vi = inflater.inflate(R.layout.custom_list_autocomplete_user, null);
        }

        TextView tvName = (TextView) vi.findViewById(R.id.tvCustomUsersNameAuto);

        tvName.setText(data.get(position).name);
        return vi;
    }

    public Filter getFilter()
    {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        @SuppressLint("DefaultLocale")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults results = new FilterResults();
            if(constraint != null) {
                String filterString = constraint.toString().toLowerCase();

                final List<UserCompact> list = backup;

                int count = list.size();
                final ArrayList<UserCompact> nlist = new ArrayList<UserCompact>(count);

                String filterableString;

                for (int i = 0; i < count; i++) {
                    filterableString = "" + list.get(i).toString();
                    if (filterableString.toLowerCase().contains(filterString)) {
                        UserCompact mYourCustomData = list.get(i);
                        nlist.add(mYourCustomData);
                    }
                }

                results.values = nlist;
                results.count = nlist.size();

            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<UserCompact> filteredData = (ArrayList<UserCompact>) results.values;

            if(results.count != 0) {
                data = filteredData;
                notifyDataSetChanged();
            }
            else
            {
                notifyDataSetInvalidated();
            }
        }

    }


}
