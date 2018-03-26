package com.example.user.sociall2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NewsFeed extends BaseActivity {

    //keys
    private TwitterSession twitterSession;

    private String instagramSession;
    public Messenger messengerInstagram;

    private Boolean twitterFinished = false;
    private Boolean instagramFinished = false;

    //create list of newsfeed objects
    public List<NewsFeedItem> list = new ArrayList<>();

    //date format
    private SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");

    //for instagram friends
    private String[] instagramIDs = new String[30];
    private int instagramCounter = 0;

    private ListView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();

        setContentView(R.layout.activity_news_feed);

        //find view of list
        view = (ListView)findViewById(R.id.newsFeed);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles,navMenuIcons);

        ////////////////////floating action button///////////////////
        findViewById(R.id.postUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewsFeed.this, PostStatus.class);
                startActivity(intent);
            }
        });

        //floating action button attributes
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.postUpdate);
        fab.setSize(FloatingActionButton.SIZE_NORMAL);
        fab.setColorNormalResId(R.color.colorPrimary);
        fab.setColorPressedResId(R.color.colorPrimaryDark);
        fab.setIcon(R.drawable.ic_plus_white_48dp);
        fab.setStrokeVisible(false);

        //listener for when a post is clicked on
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View clickView,
                                    int position, long id) {

                Intent intent = new Intent(NewsFeed.this, ViewPost.class);
                intent.putExtra("Post", (Serializable) list.get(position));
                startActivity(intent);
            }
        });

        //bind to service
        this.bindService(new Intent(this, InstagramService.class), serviceConnectionInstagram, Context.BIND_AUTO_CREATE);

    }

    /////////////////////SERVICE connection/////////////////////
    private ServiceConnection serviceConnectionInstagram = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerInstagram = new Messenger(service);

            if(twitterSession != null){
                getTwitterFeed();
            } else {
                twitterFinished = true;
            }

            if(instagramSession != ""){
                getInstagramFeed();
            } else {
                instagramFinished = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerInstagram = null;
        }
    };

    ///////////////////////////////////////////// feed methods /////////////////////////////////////////

    public void getInstagramFeed(){
        //must use thread or main thread exception is thrown
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {

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

                    //read input and put the input into a JSON object
                    JSONObject objectInput = new JSONObject(input.readLine());
                    //get data attribute (this contains all the friends) and place into array
                    JSONArray array = objectInput.getJSONArray("data");

                    for (int i = 0; i < array.length(); i++) {
                        //turn each friend in the array into its own object
                        JSONObject friend = array.getJSONObject(i); //objects: caption, likes, images

                        //store the id in an array
                        instagramIDs[i] = friend.getString("id");
                        instagramCounter++;
                    }

                    //add the authenticated user to the list and increment data
                    //so that their posts also appear
                    instagramIDs[instagramCounter] = user.getJSONObject("user").getJSONObject("nameValuePairs").getString("id");
                    instagramCounter++;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            //ensure main thread waits for other thread to finish executing
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //decide how many posts to get
        int postPerPerson = 1;
        boolean noFriends = false;

        //if no friends then set instagram to finished
        if (instagramCounter == 0){
            noFriends = true;
            instagramFinished = true;

            //check to see if instagram and twitter have finished, if so then call finish()
            if (twitterFinished && instagramFinished){
                finish();
            }
            //if less than 20 then calculate how many posts per person to retrieve
        } else if (instagramCounter < 20) {
            postPerPerson = 20 / instagramCounter;
        }

        //if there are no friends then do not execute
        if (!noFriends) {

            //loop through list and send a message to InstagramService with the ID and number of posts
            //send a message to the service with the facebook id
            for (int j = 0; j < instagramCounter; j++) {
                String id = instagramIDs[j];
                Message message = Message.obtain(null, InstagramService.NEWSFEED, 0, 0);
                SendParcel parcel = new SendParcel();
                parcel.id = id;
                parcel.extraInfo = postPerPerson;

                Bundle bundle = new Bundle();
                bundle.putParcelable("info", parcel);
                message.setData(bundle);

                try {
                    messengerInstagram.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void getTwitterFeed(){
        TwitterCore.getInstance().getApiClient(twitterSession).getStatusesService().homeTimeline(20,
                null, null, null, null, null, null, new Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {
                for (Tweet t : result.data) {
                    NewsFeedItem item = new NewsFeedItem();

                    item.setGivenID(String.valueOf(t.id));
                    item.setUser(t.user.name);
                    item.setUserID(String.valueOf(t.user.id));
                    item.setTwitterScreenName(String.valueOf(t.user.screenName));
                    item.setTextContent(t.text);

                    if (t.entities.media != null)
                        item.setPhotoContent(t.entities.media.get(0).mediaUrl);
                    else
                        item.setPhotoContent(null);


                    item.setNetwork("Twitter");
                    item.setLikes(String.valueOf(t.favoriteCount));
                    item.setNotes(String.valueOf(t.retweetCount));

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

                //twitter has finished
                twitterFinished = true;
                //check to see if instagram and twitter have finished, if so then call finish()
                if (twitterFinished && instagramFinished){
                    finish();
                }
            }

            @Override
            public void failure(TwitterException e) {
                Toast.makeText(NewsFeed.this, "Unable to retrieve Twitter posts", Toast.LENGTH_LONG).show();

                //twitter has finished
                twitterFinished = true;
                //check to see if instagram and twitter have finished, if so then call finish()
                if (twitterFinished && instagramFinished){
                    finish();
                }
            }
        });

    }

    ////////BROAD CAST RECIEVER WAITS FOR BROADCASTS
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //retrieve the newsfeed object
            for(Parcelable parcel : intent.getParcelableArrayListExtra("list")){
                NewsFeedItem item = (NewsFeedItem) parcel;
                //append object to list
                list.add(item);

            }

            //decrement counter, if counter == 0 then finish
            instagramCounter--;

            if(instagramCounter == 0){
                instagramFinished = true;
            }
            //check if instagram and twitter have both finished
            if(instagramFinished && twitterFinished){
                finish();
            }

        }
    };

            /////////////////////////////////sort and adapt //////////////////////////////////////

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

        if (list.size() > 0) {
            //place data into list using adapter
            NewsFeedAdapter adapter = new NewsFeedAdapter(this, list, R.layout.newsfeed_list_item);

            //place adapted list into view
            view.setAdapter(adapter);
        }  else {
            //else show error message
            ImageView errorImage = (ImageView)findViewById(R.id.errorImage);
            TextView errorText = (TextView)findViewById(R.id.errorText);
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }
    }

    ////////////////////////////LIFE CYCLE/////////////////////////////
    //check keys on resume in case user changed settings
    @Override
    protected void onResume() {
        super.onResume();

        try {
            twitterSession= Twitter.getInstance().core.getSessionManager().getActiveSession();
        }catch (NullPointerException e){
            twitterSession = null;
        }

        //get instagram session
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("instagramObject", 0);
        instagramSession = sharedPreferences.getString("session", "");

        //registering our receiver
        IntentFilter filter = new IntentFilter("com.example.user.sociall2.InstagramFeed");
        this.registerReceiver(receiver, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbind from services when activiy is destroyed
        if(serviceConnectionInstagram!=null) {
            unbindService(serviceConnectionInstagram);
            serviceConnectionInstagram = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unregister the receiver
        this.unregisterReceiver(this.receiver);
    }
}
