package com.example.user.sociall2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class InstagramService extends Service{

    //tokens
    private String instagramSession;

    //create list of objects
    private ArrayList listProfile = new ArrayList();
    private ArrayList listNewsFeed = new ArrayList();

    //date format
    public SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    public SimpleDateFormat instaFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    private Messenger messenger;
    private SendParcel parcel;

    public static final int PROFILE = 0;
    public static final int NEWSFEED = 1;

    private class MyHandler extends Handler {
        @Override
        //handle incoming messages
        public void handleMessage(Message message) {
            //get instagram session
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("instagramObject", 0);
            instagramSession = sharedPreferences.getString("session", "");

            //handle message
            switch (message.what) {
                case PROFILE:

                    //make list empty to avoid duplicates
                    for (int i = 0; i < listProfile.size(); i++) {
                        listProfile.remove(i);
                    }

                    getProfile();
                    break;
                case NEWSFEED:

                    //make list empty to avoid duplicates
                    for (int i = 0; i < listNewsFeed.size(); i++) {
                        listNewsFeed.remove(i);
                    }

                    //get the ID from the parcel
                    parcel = message.getData().getParcelable("info");
                    final String id = parcel.id;
                    final int postsPerPerson = parcel.extraInfo;
                    getNewsFeed(id, postsPerPerson);
                    break;
            }
        }

    }

    public void getProfile(){
        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(new JSONObject(instagramSession).getJSONObject("nameValuePairs")));

                    String accessToken = user.getString("access_token");

                    //send request to instagram
                    URL url = new URL("https://api.instagram.com/v1/users/self/media/recent/?access_token="
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
                        JSONObject photo = array.getJSONObject(i); //objects: caption, likes, images

                        //find out if is a video
                        String type = photo.getString("type");

                        //get required objects from within object photo
                        JSONObject friend = photo.getJSONObject("user");
                        JSONObject likes = photo.getJSONObject("likes");//count
                        JSONObject comments = photo.getJSONObject("comments"); //count

                        JSONObject media = null;

                        //create friends object
                        NewsFeedItem item = new NewsFeedItem();

                        //get video or image url
                        if(type.equals("image")) {
                            JSONObject images = photo.getJSONObject("images");  //objects:standard resolution
                            media = images.getJSONObject("standard_resolution"); //url

                            item.setVideo(false);
                        } else if(type.equals("video")){
                            JSONObject videos = photo.getJSONObject("videos");  //objects:standard resolution
                            media = videos.getJSONObject("standard_resolution"); //url

                            item.setVideo(true);
                        }

                        item.setPhotoContent(media.getString("url"));

                        //get and set attributes
                        item.setGivenID(photo.getString("id"));
                        item.setNetwork("Instagram");
                        item.setUser(friend.getString("full_name"));
                        item.setUserID(friend.getString("id"));

                        try {
                            item.setTextContent(photo.getString("text"));
                        }catch(Exception e){
                            item.setTextContent("");
                        }

                        //DATE
                        try {
                            //instagrams date format is different to the one I want
                            //therefore read in the date, format and reformat it
                            //this is a UNIX time stamp
                            long unixTime = photo.getLong("created_time");
                            //parse into a date
                            Date oldDate = instaFormat.parse(String.valueOf(new Date(unixTime * 1000L)));
                            //convert into new date
                            String newDate = targetFormat.format(oldDate);
                            Date date = targetFormat.parse(newDate);
                            item.setTime(date);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        item.setNotes(comments.getString("count"));
                        item.setLikes(likes.getString("count"));

                        //add item to list
                        listProfile.add(item);
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
            e.printStackTrace();
        }

        //send broadcast with list back
        Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
        broadcast.putExtra("Network", "Instagram");
        broadcast.putParcelableArrayListExtra("list", listProfile);
        sendBroadcast(broadcast);
    }

    public void getNewsFeed(final String id, final int postsPerPerson){

        //use ID to make call to instagram and retrieve the posts
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //get Instagram Session
                    SharedPreferences sharedPreferences = getSharedPreferences("instagramObject", 0);
                    String instagramSession = sharedPreferences.getString("session", "");

                    //retrieve the user object
                    JSONObject user = new JSONObject(String.valueOf(new JSONObject(instagramSession).getJSONObject("nameValuePairs")));

                    String accessToken = user.getString("access_token");

                    //send request to instagram
                    URL url = new URL("https://api.instagram.com/v1/users/" + id + "/media/recent/?access_token="
                            + accessToken + "&count=" + postsPerPerson);

                    //send url request
                    URLConnection connection = url.openConnection();

                    //read input and put the input into a JSON object
                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));

                    StringBuilder responseStrBuilder = new StringBuilder();

                    String inputStr;
                    while ((inputStr = streamReader.readLine()) != null)
                        responseStrBuilder.append(inputStr);

                    JSONObject object = new JSONObject(responseStrBuilder.toString());

                    //get data attribute (this contains all the photos) and place into array
                    JSONArray array = object.getJSONArray("data");

                    for (int i = 0; i < array.length(); i++) {

                        //turn each photo in the array into its own object
                        JSONObject photo = array.getJSONObject(i); //objects: caption, likes, images

                        //find out if is a video
                        String type = photo.getString("type");

                        //get required objects from within object photo
                        JSONObject friend = photo.getJSONObject("user");
                        JSONObject likes = photo.getJSONObject("likes");//count
                        JSONObject comments = photo.getJSONObject("comments"); //count

                        JSONObject media = null;

                        //create friends object
                        NewsFeedItem item = new NewsFeedItem();

                        //get video or image url
                        if(type.equals("image")) {
                            JSONObject images = photo.getJSONObject("images");  //objects:standard resolution
                            media = images.getJSONObject("standard_resolution"); //url

                            item.setVideo(false);
                        } else if(type.equals("video")){
                            JSONObject videos = photo.getJSONObject("videos");  //objects:standard resolution
                            media = videos.getJSONObject("standard_resolution"); //url

                            item.setVideo(true);
                        }

                        item.setPhotoContent(media.getString("url"));

                        //get and set attributes
                        item.setGivenID(photo.getString("id"));
                        item.setNetwork("Instagram");
                        item.setUser(friend.getString("full_name"));
                        item.setUserID(friend.getString("id"));


                        //instagrams date format is different to the one I want
                        //therefore read in the date, format and reformat it
                        //this is a UNIX time stamp
                        long unixTime = photo.getLong("created_time");
                        //parse into a date
                        Date oldDate = instaFormat.parse(String.valueOf(new Date(unixTime * 1000L)));
                        //convert into new date
                        String newDate = targetFormat.format(oldDate);
                        Date date = targetFormat.parse(newDate);
                        item.setTime(date);

                        item.setNotes(comments.getString("count"));
                        item.setLikes(likes.getString("count"));

                        //add item to list
                        listNewsFeed.add(item);

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
            e.printStackTrace();
        }

        //send a broadcast back on completion, with the objects attached
        Intent broadcast = new Intent("com.example.user.sociall2.InstagramFeed");
        broadcast.putExtra("Network", "Instagram");
        broadcast.putParcelableArrayListExtra("list", listNewsFeed);
        sendBroadcast(broadcast);
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