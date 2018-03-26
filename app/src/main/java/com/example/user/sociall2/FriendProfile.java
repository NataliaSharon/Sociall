package com.example.user.sociall2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class FriendProfile extends BaseActivity {

    private FriendsListItem friend;
    private ListView view;

    //tokens
    private TwitterSession twitterSession;

    private boolean unableToRetrieve = false;

    //create list of objects
    private List<NewsFeedItem> list = new ArrayList<NewsFeedItem>();

    //date format
    public SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    public SimpleDateFormat instaFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialise sdk's
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_profile);

        //find view of list
        view = (ListView) findViewById(R.id.profileItems);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles, navMenuIcons);

        //listener for when a post is clicked on
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View clickView,
                                    int position, long id) {

                Intent intent = new Intent(FriendProfile.this, ViewPost.class);
                intent.putExtra("Post", (Serializable) list.get(position));
                startActivity(intent);
            }
        });

    }

    public void setInfo(){

        //set name
        TextView text = (TextView) findViewById(R.id.name);
        text.setText(friend.getName());

        //set profile picture
        ImageView profilePhoto = (ImageView) findViewById(R.id.profilePic);

        if(!friend.getProfileURL().equals("")) {
            //check if their is a photo to download (as facebook do not supply this)
            new DownloadImageTask(profilePhoto).execute(friend.getProfileURL());
        } else { //else set default to image unavailable
            ImageView photoUnavailable = (ImageView)findViewById(R.id.profilePic);
            photoUnavailable.setImageResource(R.drawable.image_unavailable);
        }
    }

    private void getTwitterFeed(){
        try {
            Twitter.getApiClient().getStatusesService().userTimeline(Long.parseLong(friend.getTwitterID()), null, null, null, null, null, null,
            null, true, new Callback<List<Tweet>>() {
               @Override
               public void success(Result<List<Tweet>> result) {

                   List<Tweet> tweets = result.data;

                   for (Tweet t :  tweets) {

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

                    getInstagramFeed();
               }

                @Override
                public void failure(TwitterException e) {
                            Toast.makeText(FriendProfile.this, "Unable to retrieve Twitter posts", Toast.LENGTH_LONG).show();
                            getInstagramFeed();
                }
            });
        }catch(Exception e){
            getInstagramFeed();
        }

    }

    private void getInstagramFeed(){
        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //check if user is logged in, else do not bother continuing
                    if (friend.getHasInstagram()) {

                        //get Instagram Session
                        SharedPreferences sharedPreferences = getSharedPreferences("instagramObject", 0);
                        String instagramSession = sharedPreferences.getString("session", "");

                        //turn string into JSON so we can access access token
                        JSONObject object1 = new JSONObject(instagramSession);

                        //retrieve the user object
                        JSONObject user = new JSONObject(String.valueOf(object1.getJSONObject("nameValuePairs")));

                        String accessToken = user.getString("access_token");

                        //send request to instagram
                        URL url = new URL("https://api.instagram.com/v1/users/" + friend.getInstagramID() + "/media/recent/?access_token="
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
                            try {

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
                                list.add(item);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (Exception e) {
                    unableToRetrieve = true;
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            //ensure main thread waits for other thread to finish executing
            thread.join();
        } catch (InterruptedException e) {

        }

        if(unableToRetrieve){
            Toast.makeText(FriendProfile.this, "Unable to retrieve Instagram posts", Toast.LENGTH_LONG).show();
        }

        finish();
    }


    /////////////////////////////////sort and adapt////////////////////////////////////

    //uses adapter to inflate list
    public void finish(){

        //iterator
        final List<String> ids = new ArrayList<>();
        Iterator<NewsFeedItem> iterate = list.iterator();
        while (iterate.hasNext()) {
            NewsFeedItem item = iterate.next();

            String id = item.getGivenID();
            if (ids.contains(id)) {
                iterate.remove();
            } else {
                ids.add(item.getGivenID());
            }
        }

        //sort by time
        Collections.sort(list, new Comparator<NewsFeedItem>() {
            public int compare(NewsFeedItem item1, NewsFeedItem item2) {
                return item2.getTime().compareTo(item1.getTime());
            }
        });

        //only put list through to adapter if list is not empty
        if (list.size() > 0) {

            //place data into list using adapter
            NewsFeedAdapter adapter = new NewsFeedAdapter(this, list, R.layout.newsfeed_list_item);

            //place adapted list into view
            view.setAdapter(adapter);

        } else {
            //else show error message
            ImageView errorImage = (ImageView)findViewById(R.id.errorImage);
            TextView errorText = (TextView)findViewById(R.id.errorText);
            errorText.setText("No posts available");
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //receive friend object
        Intent intent = getIntent();
        friend = (FriendsListItem) intent.getSerializableExtra("FriendObject");

        //twitter session
        try {
            twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
        }catch (NullPointerException e){
            twitterSession = null;
        }

        //set user information
        setInfo();

        //icons, am using drawable instead of resource as may have to greyscale the images
        ImageView fbIcon = (ImageView) findViewById(R.id.facebookConnected);
        ImageView  twitterIcon = (ImageView) findViewById(R.id.twitterConnected);
        ImageView instagramIcon = (ImageView) findViewById(R.id.instagramConnected);

        //check which social networks are logged into, if logged in then get the feed
        if (!friend.getHasFacebook()) {
            //set icon to greyed out
            fbIcon.setImageResource(R.drawable.ic_facebook_grey);
        } else {
            fbIcon.setImageResource(R.drawable.ic_facebook);
        }

        if (!friend.getHasTwitter()) {
            //set icon to greyed out
            twitterIcon.setImageResource(R.drawable.ic_twitter_grey);
        } else {
            twitterIcon.setImageResource(R.drawable.ic_twitter);
        }

        if (!friend.getHasInstagram()){
            //set icon to greyed out
            instagramIcon.setImageResource(R.drawable.ic_instagram_grey);
        } else {
            instagramIcon.setImageResource(R.drawable.ic_instagram);
        }

        //get the feeds
        getTwitterFeed();


    }

    ///////////DOWNLOAD IMAGE//////////////////////////////////////////////////////
    //////////taken from http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
    ///////// not my own code///////////////////////
    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}
