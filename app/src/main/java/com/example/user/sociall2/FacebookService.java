package com.example.user.sociall2;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FacebookService extends Service{

    //tokens
    private AccessToken FBaccessToken;

    //create list of objects
    private ArrayList<NewsFeedItem> list = new ArrayList();

    //date format
    public SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    public SimpleDateFormat fbFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

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

            //initialise sdk's
            FacebookSdk.sdkInitialize(getApplicationContext());
            FBaccessToken = AccessToken.getCurrentAccessToken();

            GraphRequest request = GraphRequest.newGraphPathRequest(FBaccessToken, "/v2.3/me/posts", new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    //convert response into json object
                    JSONObject jsonObject = response.getJSONObject();
                    try {
                        //Get the element that holds the objects
                        JSONArray array = jsonObject.getJSONArray("data");

                        //Loop the Array and decompose each object into its attributes
                        for (int i = 0; i < array.length(); i++) {

                            JSONObject object = array.getJSONObject(i);
                            //create friends object
                            NewsFeedItem item = new NewsFeedItem();

                            item.setNetwork("Facebook");
                            //initialise photo content to null
                            item.setPhotoContent(null);

                            // if there is no message, set text content ""
                            try {
                                item.setTextContent(object.getString("message"));
                            } catch (Exception e) {
                                item.setTextContent("");
                            }

                            Date date = null;
                            String timeCreated = object.getString("created_time");
                            try {
                                //facebooks date format is different to the one I want
                                //therefore read in the date, format and reformat it
                                Date oldDate = fbFormat.parse(timeCreated);
                                String newDate = targetFormat.format(oldDate);
                                date = targetFormat.parse(newDate);
                                item.setTime(date);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            item.setGivenID(object.getString("id"));
                            item.setUser(Profile.fbName);
                            item.setUserID(Profile.fbID);
                            //facebook do not supply likes and comments without making additional requests
                            item.setNotes("-");
                            item.setLikes("-");

                            //add object to list
                            list.add(item);
                        }

                        //send list back when finished
                        Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
                        broadcast.putExtra("Network", "Facebook");
                        broadcast.putParcelableArrayListExtra("list", list);
                        sendBroadcast(broadcast);

                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Unable to retrieve Facebook posts", Toast.LENGTH_LONG).show();

                        //send list back when finished
                        Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
                        broadcast.putExtra("Network", "Facebook");
                        broadcast.putParcelableArrayListExtra("list", list);
                        sendBroadcast(broadcast);

                    }
                }
            });
            request.executeAsync();
        }
    }


    /////////////////////life cycle//////////////
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
