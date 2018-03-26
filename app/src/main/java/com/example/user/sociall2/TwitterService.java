package com.example.user.sociall2;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TwitterService extends Service{

    //tokens
    private TwitterSession twitterSession;

    //create list of objects
    private ArrayList list = new ArrayList();

    //date format
    public SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    private Messenger messenger;

    public static final int REQUEST = 0;

    private class MyHandler extends Handler {
        @Override
        //handle incoming messages
        public void handleMessage(Message message) {

            //make list empty to avoid duplicates
            for (int i = 0; i < list.size(); i++) {
                list.remove(i);
            }

            twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();

           makeRequest();

        }
    }

    public void makeRequest() {
        TwitterCore.getInstance().getApiClient(twitterSession).getStatusesService().userTimeline(twitterSession.getUserId(),
                twitterSession.getUserName(), 20, null, null, null, null, null, null, new Callback<List<Tweet>>() {
                    @Override
                    public void success(Result<List<Tweet>> result) {
                        for (Tweet t : result.data) {
                            NewsFeedItem item = new NewsFeedItem();

                            item.setGivenID(String.valueOf(t.id));
                            item.setUser(t.user.name);
                            item.setTextContent(t.text);

                            if (t.entities.media != null)
                                item.setPhotoContent(t.entities.media.get(0).mediaUrl);
                            else
                                item.setPhotoContent(null);

                            item.setNetwork("Twitter");
                            item.setLikes(String.valueOf(t.favoriteCount));
                            item.setNotes(String.valueOf(t.retweetCount));
                            item.setUserID((String.valueOf(t.user.id)));
                            item.setTwitterScreenName(String.valueOf(t.user.screenName));

                            Date date = null;

                            String timeCreated = t.createdAt;
                            try {
                                date = targetFormat.parse(timeCreated);
                                item.setTime(date);
                                list.add(item);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        //send list back
                        Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
                        broadcast.putExtra("Network", "Twitter");
                        broadcast.putParcelableArrayListExtra("list", list);
                        sendBroadcast(broadcast);
                    }

                    @Override
                    public void failure(TwitterException e) {
                        Toast.makeText(getApplicationContext(), "Unable to retrieve Twitter posts", Toast.LENGTH_LONG).show();

                        Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
                        broadcast.putExtra("Network", "Twitter");
                        broadcast.putParcelableArrayListExtra("list", list);
                        sendBroadcast(broadcast);

                    }
                });
    }

    @Override
    public void onCreate() {

        messenger = new Messenger(new MyHandler());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}