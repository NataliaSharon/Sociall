package com.example.user.sociall2;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//adapter for the navigation drawer

public class NavDrawerListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItems;

    public NavDrawerListAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItems){
        this.context = context;
        this.navDrawerItems = navDrawerItems;
    }

    @Override
    public int getCount() {

        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {

        return navDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.drawer_list_item, null);

        }

        //set icon to image view
        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.icon);

        imgIcon.setImageResource(navDrawerItems.get(position).getIcon());


        //set text
        TextView txtTitle = (TextView) convertView.findViewById(R.id.title);

        txtTitle.setText(navDrawerItems.get(position).getTitle());

        return convertView;
    }

}