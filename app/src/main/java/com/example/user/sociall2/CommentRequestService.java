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
import com.twitter.sdk.android.core.models.Tweet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit.client.Response;

public class CommentRequestService extends Service {

    //tokens
    private AccessToken FBaccessToken;
    private String instagramSession;
    private TwitterSession twitterSession;

    //date format
    public SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    public SimpleDateFormat fbFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public SimpleDateFormat instaFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    private Messenger messenger;
    private SendParcel parcel;
    private String objectID;

    private SharedPreferences sharedPreferences;

    public static final int FB_COMMENTS = 0;
    public static final int TWITTER_COMMENTS = 1; //replies
    public static final int INSTAGRAM_COMMENTS = 2;
    public static final int TWITTER_RETWEETS = 3; //retweets
    public static final int INSTAGRAM_LIKES = 4;

    private class MyHandler extends Handler {
        @Override
        //handle incoming messages
        public void handleMessage(Message message) {

            //get the ID from the parcel
            parcel = message.getData().getParcelable("id");
            objectID = parcel.id;

            switch (message.what) {
                case FB_COMMENTS:
                    //initialise sdk's
                    FacebookSdk.sdkInitialize(getApplicationContext());
                    FBaccessToken = AccessToken.getCurrentAccessToken();
                    getFBNotes();
                    break;
                case TWITTER_COMMENTS:
                    //initialise
                    twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
                    getTwitterNotes();
                    break;
                case INSTAGRAM_COMMENTS:
                    //get instagram session
                    sharedPreferences = getApplicationContext().getSharedPreferences("instagramObject", 0);
                    instagramSession = sharedPreferences.getString("session", "");
                    getInstagramNotes();
                    break;
                case TWITTER_RETWEETS:
                    twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
                    getTwitterRetweets();
                    break;
                case INSTAGRAM_LIKES:
                    getInstagramLikes();
                default:
                    super.handleMessage(message);
            }
        }
    }

    /////////////////////REQUEST METHODS///////////////
    public void getFBNotes(){

        //create list of comment objects
        final ArrayList<CommentItem> listComments = new ArrayList();

        //compose url and send request
        new GraphRequest(FBaccessToken, "/" + objectID + "/comments", null, HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject object = response.getJSONObject();
                        try {
                            JSONArray array = object.getJSONArray("data");

                            for (int j = 0; j < array.length(); j++) {
                                //comment object
                                JSONObject comment = array.getJSONObject(j);
                                //get user object
                                JSONObject user = comment.getJSONObject("from");

                                //create friends object
                                CommentItem item = new CommentItem();

                                //get and set attributes
                                item.setUser(user.getString("name"));
                                item.setUserID(user.getString("id"));
                                item.setNetwork("Facebook");

                                item.setTextContent(comment.getString("message"));

                                Date date = null;
                                String timeCreated = comment.getString("created_time");
                                try {
                                    //facebooks date format is different to the one I want
                                    //therefore read in the date, format and reformat it
                                    Date oldDate = fbFormat.parse(timeCreated);
                                    String newDate = targetFormat.format(oldDate);
                                    date = targetFormat.parse(newDate);
                                    item.setTime(date);

                                    //add item to list
                                    listComments.add(item);

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }

                            sendBroadcastNotes(listComments);

                        } catch (Exception e) {
                            sendBroadcastNotes(listComments);
                        }
                    }
                }
        ).executeAsync();

    }

    //twitter replies
    //CAN ONLY DO THIS IF IT IS THE POST OF THE USER LOGGED IN!
    public void getTwitterNotes(){
        //create list of comment objects
        final ArrayList<CommentItem> listComments = new ArrayList();

        //get user mentions timeline
        Twitter.getApiClient().getStatusesService().mentionsTimeline(20, null, null, null, null,
                null, new Callback<List<Tweet>>() {
                    @Override
                    public void success(Result<List<Tweet>> result) {
                        List<Tweet> tweets = result.data;

                        for (Tweet t : tweets) {
                            //if in_reply_to_status id = original comment id
                            //then save it as it is a reply to the comment
                            if (String.valueOf(t.inReplyToStatusId).equals(objectID)) {
                                CommentItem item = new CommentItem();
                                item.setUser(t.user.name);
                                item.setTextContent(t.text);
                                item.setNetwork("Twitter");
                                item.setUser((t.user.name));
                                item.setUserID((String.valueOf(t.user.id)));

                                Date date = null;

                                String timeCreated = t.createdAt;
                                try {
                                    date = targetFormat.parse(timeCreated);
                                    item.setTime(date);

                                    //add item to list
                                    listComments.add(item);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        sendBroadcastNotes(listComments);
                    }

                    @Override
                    public void failure(TwitterException e) {
                        Toast.makeText(getApplicationContext(), "Unable to retrieve Twitter Comments", Toast.LENGTH_LONG).show();
                        sendBroadcastNotes(listComments);
                    }
                });
    }

    public void getTwitterRetweets() {
        //create list of likes
        final ArrayList<String> retweetNames = new ArrayList();

        //create new client and call retweets service interface
        TwitterClient client = new TwitterClient(twitterSession);
        client.getRetweetsInterface().list(Long.parseLong(objectID), new Callback<Response>() {

            @Override
            public void success(Result<Response> response) {

                try {
                    //turn the response into a string using the streamToString method
                    InputStream inputStream = response.response.getBody().in();
                    String data = streamToString(inputStream);

                    //turn into json array
                    JSONArray array = new JSONArray(data);

                    //Loop the Array and decompose each object into its attributes
                    for (int i = 0; i < array.length(); i++) {

                        //turn array value into JSON object
                        JSONObject retweet = array.getJSONObject(i);

                        //get the data we need
                        String stringRetweet = retweet.getString("text");
                        retweetNames.add(stringRetweet);
                    }

                    sendBroadcastLikes(retweetNames);

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Unable to retrieve all Twitter Retweets", Toast.LENGTH_LONG).show();
                    sendBroadcastLikes(retweetNames);
                }

            }

            @Override
            public void failure(TwitterException exception) {
                //a 403 status code error means that the account does not have access privileges to this data
                Toast.makeText(getApplicationContext(), "Your twitter account is unable to view this information", Toast.LENGTH_LONG).show();

            }
        });

    }

    public void getInstagramNotes () {
        //create list of comment objects
        final ArrayList<CommentItem> listComments = new ArrayList();

        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(new JSONObject(instagramSession).getJSONObject("nameValuePairs")));

                    String accessToken = user.getString("access_token");

                    //send request to instagram
                    URL url = new URL("https://api.instagram.com/v1/media/" + objectID + "/comments?access_token="
                            + accessToken);

                    //send url request
                    URLConnection connection = url.openConnection();

                    //get input
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));

                    //read input and put the input into a JSON object
                    JSONObject object = new JSONObject(input.readLine());
                    //get data attribute (this contains all the photos) and place into array
                    JSONArray array = object.getJSONArray("data");

                    for (int i = 0; i < array.length(); i++) {

                        //turn each photo in the array into its own object
                        JSONObject comment = array.getJSONObject(i); //objects: caption, likes, images

                        //get required objects from within object photo
                        JSONObject friend = comment.getJSONObject("from");

                        //create friends object
                        CommentItem item = new CommentItem();

                        //get and set attributes
                        item.setUser(friend.getString("full_name"));
                        item.setUserID(friend.getString("id"));
                        item.setNetwork("Instagram");

                        item.setTextContent(comment.getString("text"));

                        //DATE
                        try {
                            //instagrams date format is different to the one I want
                            //therefore read in the date, format and reformat it
                            //this is a UNIX time stamp
                            long unixTime = comment.getLong("created_time");
                            //parse into a date
                            Date oldDate = instaFormat.parse(String.valueOf(new Date(unixTime * 1000L)));
                            //convert into new date
                            String newDate = targetFormat.format(oldDate);
                            Date date = targetFormat.parse(newDate);
                            item.setTime(date);

                            //add item to list
                            listComments.add(item);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
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

        }

        sendBroadcastNotes(listComments);
    }

    public void getInstagramLikes () {
        //create list of likes
        final ArrayList<String> likeNames = new ArrayList();

        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(new JSONObject(instagramSession).getJSONObject("nameValuePairs")));

                    String accessToken = user.getString("access_token");

                    //send request to instagram
                    URL url = new URL("https://api.instagram.com/v1/media/" + objectID + "/likes?access_token="
                            + accessToken);

                    //send url request
                    URLConnection connection = url.openConnection();

                    //get input
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));

                    //read input and put the input into a JSON object
                    JSONObject object = new JSONObject(input.readLine());
                    //get data attribute (this contains all the photos) and place into array
                    JSONArray array = object.getJSONArray("data");

                    for (int i = 0; i < array.length(); i++) {

                        //turn each photo in the array into its own object
                        JSONObject instagramUser = array.getJSONObject(i);

                        String name = "";
                        //create friends object, but check if surname exists
                        if ((!instagramUser.isNull("full_name"))) {
                            name = instagramUser.getString("full_name") + " likes this";
                        } else {
                            name = instagramUser.getString("screen_name") + " likes this";
                        }
                        //add item to list
                        likeNames.add(name);
                    }
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

        }

        sendBroadcastLikes(likeNames);
    }

    public void sendBroadcastNotes (ArrayList < CommentItem > listComments) {
        //send list back when finished
        Intent broadcast = new Intent("com.example.user.sociall2.Comments");
        broadcast.putExtra("Type", "Notes");
        broadcast.putParcelableArrayListExtra("list", listComments);
        sendBroadcast(broadcast);
    }

    public void sendBroadcastLikes (ArrayList < String > likeNames) {
        //send list back when finished
        Intent broadcast = new Intent("com.example.user.sociall2.Comments");
        broadcast.putExtra("Type", "Likes");
        broadcast.putStringArrayListExtra("list", likeNames);
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


    /////////////////////life cycle//////////////
    @Override
    public void onCreate () {
        messenger = new Messenger(new MyHandler());
    }

    @Nullable
    @Override
    public IBinder onBind (Intent intent){
        return messenger.getBinder();
    }

}
