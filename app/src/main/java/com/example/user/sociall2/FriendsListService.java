package com.example.user.sociall2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.facebook.HttpMethod;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import retrofit.client.Response;

public class FriendsListService extends Service {

    //tokens
    private AccessToken FBaccessToken;
    private TwitterSession twitterSession;

    private Messenger messenger;
    public static final int FB = 0;
    public static final int TWITTER = 1;
    public static final int INSTAGRAM = 2;

    //wait booleans for list
    private boolean facebookWait = false;
    private boolean twitterWait = false;
    private boolean instagramWait = false;

    //create list of objects
    private ArrayList<FriendsListItem> list = new ArrayList();

    private class MyHandler extends Handler {
        @Override
        //handle incoming messages
        public void handleMessage(Message message) {
            switch (message.what) {
                case FB:
                    facebookWait = true;
                    getFB();
                    break;
                case TWITTER:
                    twitterWait = true;
                    getTwitter();
                    break;
                case INSTAGRAM:
                    instagramWait = true;
                    getInstagram();
                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }


    private void getFB() {

        //initialise sdk's
        FacebookSdk.sdkInitialize(getApplicationContext());
        FBaccessToken = AccessToken.getCurrentAccessToken();

        //FB graph requests, making API call
        GraphRequest request = new GraphRequest(FBaccessToken, "/me/taggable_friends", null, HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        //convert response into json object
                        JSONObject jsonObject = response.getJSONObject();

                        try {
                            //Get the element that holds the objects
                            JSONArray array = jsonObject.getJSONArray("data");
                            //Loop the Array and decompose each object into its attributes
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject friend = array.getJSONObject(i);

                                //check if friend is already an object
                                boolean exists = false;
                                for (FriendsListItem checkFriend : list){
                                    if(checkFriend.getName().equals(friend.getString("name"))){
                                        exists = true;
                                        checkFriend.setHasFacebook(true);
                                        checkFriend.setFacebookID(friend.getString("id"));
                                    }
                                }

                                if(!exists) {
                                    //create friends object
                                    FriendsListItem item = new FriendsListItem();
                                    item.setName(friend.getString("name"));
                                    item.setHasFacebook(true);
                                    item.setFacebookID(friend.getString("id"));
                                    list.add(item);
                                }
                            }

                            facebookWait = false;
                            if(!facebookWait && !twitterWait && !instagramWait){
                                sendBC();
                            }

                        } catch (Exception e) {
                            facebookWait = false;
                            if(!facebookWait && !twitterWait && !instagramWait){
                                sendBC();
                            }
                        }

                    }

                });
        request.executeAsync();
    }

    private void getTwitter() {
        twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();

        //create new client and call friends service interface
        TwitterClient client = new TwitterClient(twitterSession);
        client.getFriendsInterface().list(twitterSession.getUserId(), new Callback<Response>() {

            @Override
            public void success(Result<Response> response) {
                try {
                    //get stream
                    InputStream inputStream = response.response.getBody().in();
                    //convert to string
                    String data = streamToString(inputStream);

                    //create array of objects
                    JSONArray array = new JSONObject(data).getJSONArray("users");

                    //Loop the Array and decompose each object into its attributes
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);

                        //check if friend is already an object
                        boolean exists = false;
                        for (FriendsListItem checkFriend : list) {
                            if (checkFriend.getName().equals(object.getString("name"))) {
                                exists = true;
                                checkFriend.setHasTwitter(true);
                                checkFriend.setTwitterID(object.getString("id"));
                                if (checkFriend.getProfileURL().equals("")) {
                                    checkFriend.setProfileURL(object.getString("profile_image_url"));
                                }
                            }
                        }

                        if (!exists) {
                            //create friends object
                            FriendsListItem item = new FriendsListItem();
                            item.setName(object.getString("name"));
                            item.setHasTwitter(true);
                            item.setTwitterID(object.getString("id"));
                            item.setProfileURL(object.getString("profile_image_url"));
                            list.add(item);
                        }
                    }


                    twitterWait = false;
                    if(!facebookWait && !twitterWait && !instagramWait){
                        sendBC();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(getApplicationContext(), "Unable to retrieve Twitter friends", Toast.LENGTH_LONG).show();

                twitterWait = false;
                if(!facebookWait && !twitterWait && !instagramWait) {
                    sendBC();
                }
            }

        });

    }

    private void getInstagram(){
        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //check to see if there is an instagram session
                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("instagramObject", 0);
                    String instagramSession = sharedPreferences.getString("session", "");


                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(new JSONObject(instagramSession).getJSONObject("nameValuePairs")));

                    String accessToken = user.getString("access_token");

                    //send request to instagram
                    URL url = new URL("https://api.instagram.com/v1/users/self/follows?access_token="
                            + accessToken);

                    //send url request
                    URLConnection connection = url.openConnection();

                    //get input
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));

                    //read input and get data attribute (this contains all the friends) and place into array
                    JSONArray array = new JSONObject(input.readLine()).getJSONArray("data");

                    for (int i = 0; i < array.length(); i++) {

                        //turn each friend in the array into its own object
                        JSONObject friend = array.getJSONObject(i); //objects: caption, likes, images

                        //check if friend is already an object
                        boolean exists = false;
                        for (FriendsListItem checkFriend : list){ //iterate through existing list
                            //check if there is a name match
                            if(checkFriend.getName().equals(friend.getString("full_name"))){
                                exists = true; //if true then set the Instagram variables
                                checkFriend.setHasInstagram(true);
                                checkFriend.setInstagramID(friend.getString("id"));
                                //if there is no photo url existing then use this one
                                if(checkFriend.getProfileURL().equals("")){
                                    checkFriend.setProfileURL(friend.getString("profile_picture"));
                                }
                            }
                        }

                        if(!exists){ //if does not exist then create new object
                            //create friends object
                            FriendsListItem item = new FriendsListItem();
                            //get and set attributes
                            item.setName(friend.getString("full_name"));
                            item.setHasInstagram(true);
                            item.setInstagramID(friend.getString("id"));
                            item.setProfileURL(friend.getString("profile_picture"));
                            //add item to list
                            list.add(item);
                        }
                    }


                    instagramWait = false;
                    if(!facebookWait && !twitterWait && !instagramWait){
                        sendBC();
                    }

                } catch (Exception e) {
                    instagramWait = false;
                    if(!facebookWait && !twitterWait && !instagramWait){
                        sendBC();
                    }
                }
            }
        });

        thread.start();
        try {
            //ensure main thread waits for other thread to finish executing
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendBC() {
    //send list back when finished
        Intent broadcast = new Intent("com.example.user.sociall2.FriendsBroadcast");
        broadcast.putParcelableArrayListExtra("list", list);
        sendBroadcast(broadcast);
    }

    //method for turning http response into string
    //note this is the same method used in InstagramApp
    // and therefore originates from the instagram tutorials and therefore NOT MY OWN CODE
    //appends data into a single string
    private String streamToString(InputStream input) throws IOException {
        String string = "";

        if (input != null) {
            StringBuilder builder = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input));

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                reader.close();
            } finally {
                input.close();
            }

            string = builder.toString();
        }

        return string;
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
