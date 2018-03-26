package com.example.user.sociall2;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.twitter.sdk.android.core.models.Tweet;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import retrofit.client.Response;

public class CommentPostService extends Service {

    //tokens
    private AccessToken FBaccessToken;
    private String instagramSession;
    private TwitterSession twitterSession;

    private Messenger messenger;
    private SendParcel parcel;
    private String objectID;

    private SharedPreferences sharedPreferences;

    //binding to CommentRequestService
    private Messenger messengerRequestComments;

    public static final int FB_COMMENT = 0;
    public static final int FB_LIKE = 1;
    public static final int TWITTER_REPLY = 2; //retweets
    public static final int TWITTER_FAVOURITE = 3;
    public static final int TWITTER_RETWEET = 4;
    public static final int INSTAGRAM_COMMENT = 5;
    public static final int INSTAGRAM_LIKE = 6;

    private class MyHandler extends Handler {
        @Override
        //handle incoming messges
        public void handleMessage(Message message) {
            //get the ID from the parcel
            parcel = message.getData().getParcelable("id");
            objectID = parcel.id;
            String text;

            switch (message.what) {
                case FB_COMMENT:
                    //initialise sdk's
                    FacebookSdk.sdkInitialize(getApplicationContext());
                    FBaccessToken = AccessToken.getCurrentAccessToken();

                    //retrieve text for comment
                    text = parcel.text;

                    sendFBComment(text);
                    break;
                case FB_LIKE:
                    sendFBLike();
                    break;
                case TWITTER_REPLY:
                    //retrieve text for comment
                    text = parcel.text;
                    String twitterScreenName = parcel.twitterScreenName;
                    sendTwitterReply(text, twitterScreenName);
                    break;
                case TWITTER_FAVOURITE:
                    sendTwitterFavourite();
                    break;
                case TWITTER_RETWEET:
                    sendTwitterRetweet();
                    break;
                case INSTAGRAM_COMMENT:
                    //get instagram session
                    sharedPreferences = getApplicationContext().getSharedPreferences("instagramObject", 0);
                    instagramSession = sharedPreferences.getString("session", "");

                    //retrieve text for comment
                    text = parcel.text;

                    sendInstagramComment(text);
                    break;
                case INSTAGRAM_LIKE:
                    //get instagram session
                    sharedPreferences = getApplicationContext().getSharedPreferences("instagramObject", 0);
                    instagramSession = sharedPreferences.getString("session", "");

                    sendInstagramLike();
                    break;
            }
        }
    }

    private void sendFBComment(String text) {
        Bundle params = new Bundle();
        //attach text to bundle
        params.putString("message", text);
        // make the API call
        new GraphRequest(FBaccessToken, "/" + objectID + "/comments", params,
                HttpMethod.POST, new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        Toast.makeText(getApplicationContext(), "Your comment has been sent!", Toast.LENGTH_LONG).show();
                        finish("Facebook", "Comment");

                    }
                }
        ).executeAsync();
    }

    private void sendFBLike() {
        // make the API call
        new GraphRequest(FBaccessToken, "/" + objectID + "/likes", null, HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {

                        Toast.makeText(getApplicationContext(), "Your like has been sent!", Toast.LENGTH_LONG).show();
                        finish("Facebook", "Like");
                    }
                }
        ).executeAsync();
    }


    private void sendTwitterReply(String text, String twitterScreenName){
        //join together reply using screen name and text
        StringBuilder builder = new StringBuilder().append("@").append(twitterScreenName).append(" ").append(text);
        String finalText = builder.toString();

        //validate
        if (finalText.length() > 140){  //check the text is not longer than 140 characters
            Toast.makeText(getApplicationContext(), "Twitter posts must be less than 140 characters", Toast.LENGTH_LONG).show();
        } else {
            //create new client and call retweets service interface
            twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
            TwitterClient client = new TwitterClient(twitterSession);
            client.getPostReplyInterface().list(finalText, Long.parseLong(objectID), new Callback<Response>() {
                @Override
                public void success(Result<Response> result) {

                    Toast.makeText(getApplicationContext(), "Your reply has been sent!", Toast.LENGTH_LONG).show();
                    finish("Twitter", "Reply");
                }

                @Override
                public void failure(TwitterException e) {
                    Toast.makeText(getApplicationContext(), "Unable to post reply", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    private void sendTwitterFavourite(){
        //create new client and call corresponding interface
        twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
        TwitterClient client = new TwitterClient(twitterSession);
        client.getPostFavouriteInterface().list(Long.parseLong(objectID), new Callback<Response>() {

            @Override
            public void success(Result<Response> response) {
                Toast.makeText(getApplicationContext(), "Your favourite has been sent!", Toast.LENGTH_LONG).show();
                finish("Twitter", "Like");
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(getApplicationContext(), "Your account does not have these permissions", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendTwitterRetweet() {

        //send retweet using the id of the post
        Twitter.getApiClient().getStatusesService().retweet(Long.valueOf(objectID), null, new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {
                Toast.makeText(getApplicationContext(), "Your retweet has been sent!", Toast.LENGTH_LONG).show();
                finish("Twitter", "Retweet");
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(getApplicationContext(), "Your account does not have these permissions", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendInstagramComment(String text) {

        //must use thread or main thread exception is thrown
        class networkAccess implements Runnable {
            String text;

            networkAccess(String string) {
                text = string;
            }

            public void run() {

                try {
                    //instagram URI
                    URI url = new URI("https://api.instagram.com/v1/media/" + objectID + "/comments");

                    //create new client
                    HttpClient client = new DefaultHttpClient();

                    //specify post request
                    HttpPost post = new HttpPost(url);

                    //retrieve access token
                    //turn string into JSON so we can access access token
                    JSONObject object = new JSONObject(instagramSession);
                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(object.getJSONObject("nameValuePairs")));
                    String accessToken = user.getString("access_token");

                    //put text in form readable for curl by looping through each character
                    StringBuilder builder = new StringBuilder(text);
                    for (int i = 0; i < text.length(); i++) {
                        if (text.charAt(i) == ' ') {
                            builder.setCharAt(i, 'x');
                        }
                    }

                    text = builder.toString();

                    //-f in cURL is a form, thee are the contents of the form on submit
                    //add the additional information
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    nameValuePairs.add(new BasicNameValuePair("access_token", accessToken));
                    nameValuePairs.add(new BasicNameValuePair("text", text));
                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    //execute request
                    HttpResponse httpResponse = client.execute(post);
                    //read response and use reader to place into string format
                    InputStream inputStream = httpResponse.getEntity().getContent();
                    String response = streamToString(inputStream);

                } catch (Exception e) {

                }
            }
        }

        if (text.length() > 300){  //check the text is not longer than 300 characters
            Toast.makeText(getApplicationContext(), "Instagram comments must be less than 300 characters", Toast.LENGTH_LONG).show();
        } else {

            Thread thread = new Thread(new networkAccess(text));
            thread.start();

            try {
                //ensure main thread waits for other thread to finish executing
                thread.join();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Unable to post comment", Toast.LENGTH_LONG).show();
            } finally {
                Toast.makeText(getApplicationContext(), "Your comment has been sent!", Toast.LENGTH_LONG).show();
                finish("Instagram", "Comment");
            }
        }

    }

    private void sendInstagramLike(){

        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //instagram URI
                    URI url = new URI("https://api.instagram.com/v1/media/" + objectID + "/likes");

                    //create new client
                    HttpClient client = new DefaultHttpClient();

                    //specify post request
                    HttpPost post = new HttpPost(url);

                    //retrieve access token
                    //turn string into JSON so we can access access token
                    JSONObject object = new JSONObject(instagramSession);
                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(object.getJSONObject("nameValuePairs")));
                    String accessToken = user.getString("access_token");

                    //-f in cURL is a form, thee are the contents of the form on submit
                    //add the additional information
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    nameValuePairs.add(new BasicNameValuePair("access_token", accessToken));
                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    //execute request
                    HttpResponse httpResponse = client.execute(post);
                    //read response and use reader to place into string format
                    InputStream inputStream = httpResponse.getEntity().getContent();
                    String response = streamToString(inputStream);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        thread.start();
        try {
            //ensure main thread waits for other thread to finish executing
            thread.join();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Unable to send like", Toast.LENGTH_LONG).show();
        } finally {
            Toast.makeText(getApplicationContext(), "Your like has been sent!", Toast.LENGTH_LONG).show();
            finish("Instagram", "Like");

        }
    }

    //bind to CommentRequestService to refresh
    private void finish(String network, String type){

        Message message = null;

        SendParcel parcel = new SendParcel();
        parcel.id = objectID;

        Bundle bundle = new Bundle();
        bundle.putParcelable("id", parcel);

        switch (network){
            case "Facebook":
                if(type.equals("Comment")) {
                    message = Message.obtain(null, CommentRequestService.FB_COMMENTS, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerRequestComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                //facebook cannot retrieve likes
                break;

            case "Twitter":
                if(type.equals("Reply")){
                    message = Message.obtain(null, CommentRequestService.TWITTER_COMMENTS, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerRequestComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (type.equals("Like")){
                    //nothing as cannot retrieve twitter likes
                } else if (type.equals("Retweet")) {
                    message = Message.obtain(null, CommentRequestService.TWITTER_RETWEETS, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerRequestComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case "Instagram":
                if(type.equals("Comment")){
                    message = Message.obtain(null, CommentRequestService.INSTAGRAM_COMMENTS, 0, 0);
                } else {
                    message = Message.obtain(null, CommentRequestService.INSTAGRAM_LIKES, 0, 0);
                }

                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerRequestComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

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

    //////////////////////////// SERVICE CONNECTIONS ////////////////////////
    private ServiceConnection serviceConnectionRequestComments = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerRequestComments = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerRequestComments = null;
        }
    };

    /////////////////////life cycle//////////////
    @Override
    public void onCreate () {
        messenger = new Messenger(new MyHandler());
    }

    @Nullable
    @Override
    public IBinder onBind (Intent intent){

        //bind to service
        getApplicationContext().bindService(new Intent(getApplicationContext(), CommentRequestService.class), serviceConnectionRequestComments, Context.BIND_AUTO_CREATE);

        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }
}


