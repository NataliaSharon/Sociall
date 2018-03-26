package com.example.user.sociall2;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class CommentAdapter extends BaseAdapter {

    private Context context;
    private List<CommentItem> list;

    private int layout;

    public CommentAdapter(Context context, List<CommentItem> list, int layout) {
        this.context = context;
        this.list = list;
        this.layout = layout;
    }

    @Override
    public int getCount() {

        return list.size();
    }

    @Override
    public Object getItem(int position) {

        return list.get(position);
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
            convertView = inflater.inflate(layout, null);
        }

        //set user name
        TextView userName = (TextView) convertView.findViewById(R.id.userName);
        userName.setText(list.get(position).getUser());

        //set comment
        TextView comment = (TextView) convertView.findViewById(R.id.comment);
        comment.setText(list.get(position).getTextContent());

        //set time, get rid of last part
        TextView timeStamp = (TextView) convertView.findViewById(R.id.timeStamp);
        String wholeDate = list.get(position).getTime().toString();
        String[] splitDate = wholeDate.split("(?= )");
        timeStamp.setText(splitDate[0] + splitDate[2] + splitDate[1] + splitDate[5] + ", " + splitDate[3]);

        return convertView;
    }

}
