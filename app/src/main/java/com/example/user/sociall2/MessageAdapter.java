package com.example.user.sociall2;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.List;

/**
 * Created by User on 03/02/2016.
 */
public class MessageAdapter extends BaseAdapter {

    private Context context;
    private List<MessageItem> list;
    private Boolean isConversation;

    private int layout;

    public MessageAdapter(Context context, List<MessageItem> list, int layout, Boolean isConversation) {
        this.context = context;
        this.list = list;
        this.layout = layout;
        this.isConversation = isConversation;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, null);
        }

        //set name of sender/receiver that is not the user
        TextView name = (TextView) convertView.findViewById(R.id.name);

        TwitterSession twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();

        if(isConversation){
            //as this is a conversation we display the name of the sender on each message
            name.setText(list.get(position).getSender());
        } else {
            //else this is the messages list where we want to display the opposing recipient
            //so this user knows whom the conversation is with
            //check against the userID so the latest message is that of the other person, and not the user
            if(list.get(position).getSenderID().equals(String.valueOf(twitterSession.getUserId()))){
                name.setText(list.get(position).getRecipient());
            } else {
                name.setText(list.get(position).getSender());
            }
        }

        //set network image
        ImageView networkImage = (ImageView) convertView.findViewById(R.id.networkImage);

        //depending on where the post has come from, show corresponding social network logo
        Drawable networkIcon = null;
        switch (list.get(position).getNetwork()) {

            case "Facebook":
                networkIcon = ContextCompat.getDrawable(context, R.drawable.ic_facebook);
                break;
            case "Twitter":
                networkIcon = ContextCompat.getDrawable(context, R.drawable.ic_twitter);
                break;
            case "Instagram":
                networkIcon = ContextCompat.getDrawable(context, R.drawable.ic_instagram);
                break;
        }
        networkImage.setImageDrawable(networkIcon);

        //set text
        TextView message = (TextView) convertView.findViewById(R.id.message);
        message.setText(list.get(position).getTextContent());

        //set time, get rid of last part
        TextView timeStamp = (TextView) convertView.findViewById(R.id.timeStamp);
        String wholeDate = list.get(position).getTime().toString();
        String[] splitDate = wholeDate.split("(?= )");
        timeStamp.setText(splitDate[0] + splitDate[2] + splitDate[1] + splitDate[5] + ", " + splitDate[3]);

        return convertView;

    }
}
