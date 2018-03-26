package com.example.user.sociall2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewPost extends BaseActivity {

    //create list of comment objects
    public List<CommentItem> list;
    public List<String> names;

    private ListView viewPost;
    private ListView viewComments;
    private ListView viewLikes;

    private Messenger messengerRequestComments;
    private Messenger messengerPostComments;
    private NewsFeedItem post;
    private Intent postIntent;

    private boolean requestConnected = false;
    private boolean postConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();
        
        setContentView(R.layout.activity_view_post);

        postIntent = getIntent();

        //find view of list
        viewComments = (ListView)findViewById(R.id.comments);
        viewPost = (ListView)findViewById(R.id.post);
        viewLikes = (ListView) findViewById(R.id.likesList);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles, navMenuIcons);

        //bind to service
        this.bindService(new Intent(this, CommentRequestService.class), serviceConnectionRequestComments, Context.BIND_AUTO_CREATE);
        this.bindService(new Intent(this, CommentPostService.class), serviceConnectionPostComments, Context.BIND_AUTO_CREATE);

    }

    //////////////////////////// SERVICE CONNECTIONS ////////////////////////
    private ServiceConnection serviceConnectionRequestComments = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerRequestComments = new Messenger(service);

            //ensure that both services have been bound to before continuing
            requestConnected = true;
            if(requestConnected && postConnected){
                retrievePost();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerRequestComments = null;
        }
    };

    private ServiceConnection serviceConnectionPostComments = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerPostComments = new Messenger(service);

            //ensure that both services have been bound to before continuing
            postConnected = true;
            if(requestConnected && postConnected){
                retrievePost();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerPostComments = null;
        }
    };

    //////////// 1) Deal with Intent and make calls for comments
    public void retrievePost(){
        //incoming intent means user clicked on a post
        //receive post object
        post = (NewsFeedItem) postIntent.getSerializableExtra("Post");

        //inflate adapter to show newsfeed item
        List<NewsFeedItem> tempPost = new ArrayList<>();
        tempPost.add(post);
        NewsFeedAdapter adapter = new NewsFeedAdapter(this, tempPost, R.layout.newsfeed_list_item);
        //place adapted list into view
        viewPost.setAdapter(adapter);

        //check network
        //send message to bound service to retrieve comments
        Message message = null;

        SendParcel parcel = new SendParcel();
        parcel.id = post.getGivenID();

        Bundle bundle = new Bundle();
        bundle.putParcelable("id", parcel);

        //decide which method to call based on type of social network
        switch(post.getNetwork()){
            case "Facebook":
                message = Message.obtain(null, CommentRequestService.FB_COMMENTS, 0, 0);
                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerRequestComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "Twitter":
                //need 3 broadcasts from twitter (favourites, retweets and replies)
                //only can retrieve twitter mentions/replies if the tweet is of the user logged in
                //so first check user ID of object, match to user logged in
                //and only send message if they match

                //make call for user ID
                getTwitterInfo();
                break;
            case "Instagram":
                message = Message.obtain(null, CommentRequestService.INSTAGRAM_COMMENTS, 0, 0);
                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerRequestComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }
    /////// 2) BROAD CAST RELIEVER WAITS FOR BROADCASTS
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //decide whether list returned is likes or retweets
            boolean retweets;

            //clear lists
            list = new ArrayList<>();
            names = new ArrayList<>();

            //retrieve list
            if(intent.getStringExtra("Type").equals("Notes")) {
                //if notes then inflate list using comment adapter
                for (Parcelable parcel : intent.getParcelableArrayListExtra("list")) {
                    CommentItem item = (CommentItem) parcel;
                    list.add(item);
                }

                //reverse order of lists so oldest ones are on top
                Collections.reverse(list);

                //place data into list using adapter
                CommentAdapter adapter = new CommentAdapter(getApplicationContext(), list, R.layout.comment_list_item);

                //place adapted list into view
                viewComments.setAdapter(adapter);

            } else if (intent.getStringExtra("Type").equals("Likes")){
                for(String name : intent.getStringArrayListExtra("list")){
                    names.add(name);
                }

                //show new list
                ListView likes = (ListView) findViewById(R.id.likesList);
                likes.setVisibility(View.VISIBLE);

                // parse like names into list
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.likes_list_item, R.id.likeName, names);
                viewLikes.setAdapter(adapter);
            } else if (intent.getStringExtra("Type").equals("LikesClicked")){
                //need to retrieve the likes
                retweets = false;
                requestLikes(retweets);
            } else if (intent.getStringExtra("Type").equals("RetweetsClicked")){
                //send likes method but notify that it is for twitter retweets
                retweets = true;
                requestLikes(retweets);
            }
        }
    };

    ////////////// 3) Likes are selected

    private void requestLikes (boolean retweets){

        //hide comments
        EditText text = (EditText) findViewById(R.id.userText);
        Button commentButton = (Button) findViewById(R.id.commentButton);
        viewComments.setVisibility(View.GONE);
        text.setVisibility(View.GONE);
        commentButton.setVisibility(View.GONE);

        //show everything else
        //show new like button and new list view, and back button
        Button likeButton = (Button) findViewById(R.id.like);
        Button goBack = (Button) findViewById(R.id.back);
        likeButton.setVisibility(View.VISIBLE);
        goBack.setVisibility(View.VISIBLE);

        //if post is twitter then give option to retweet also
        if(post.getNetwork().equals("Twitter")) {
            //make retweet button viewable
            Button retweet = (Button) findViewById(R.id.retweet);
            retweet.setVisibility(View.VISIBLE);
            likeButton.setText("Favourite");
        }

        //check network
        //send message to bound service to retrieve likes
        Message message = null;

        SendParcel parcel = new SendParcel();
        parcel.id = post.getGivenID();

        Bundle bundle = new Bundle();
        bundle.putParcelable("id", parcel);


        //decide which method to call based on type of social network
        switch(post.getNetwork()){
            case "Facebook":
                //cannot get names of likes so do not retrieve
                Toast.makeText(ViewPost.this, "Cannot retrieve Likes", Toast.LENGTH_LONG).show();
                break;

            case "Twitter":
                //either retweets or favourites
                if(retweets){
                    //send message
                    message = Message.obtain(null, CommentRequestService.TWITTER_RETWEETS, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerRequestComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    //cannot get names of favourites so do not retrieve
                    Toast.makeText(ViewPost.this, "Cannot retrieve Favourites", Toast.LENGTH_LONG).show();

                }
                break;

            case "Instagram":
                message = Message.obtain(null, CommentRequestService.INSTAGRAM_LIKES, 0, 0);

                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerRequestComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    ///////////////// 4) THE BUTTONS

    public void goBack(View v){
        //hide like button, back button and like list view
        //show new like button and new list view , and back button
        Button likeButton = (Button) findViewById(R.id.like);
        Button goBack = (Button) findViewById(R.id.back);
        ListView likes = (ListView) findViewById(R.id.likesList);
        Button retweet = (Button) findViewById(R.id.retweet);
        likeButton.setVisibility(View.GONE);
        goBack.setVisibility(View.GONE);
        likes.setVisibility(View.GONE);
        retweet.setVisibility(View.GONE);

        //show comments
        EditText text = (EditText) findViewById(R.id.userText);
        Button commentButton = (Button) findViewById(R.id.commentButton);
        viewComments.setVisibility(View.VISIBLE);
        text.setVisibility(View.VISIBLE);
        commentButton.setVisibility(View.VISIBLE);
    }
    public void sendLike(View v){

        //check network
        //send message to bound service to send a like
        Message message;

        SendParcel parcel = new SendParcel();
        parcel.id = post.getGivenID();

        Bundle bundle = new Bundle();
        bundle.putParcelable("id", parcel);

        //decide which method to call based on type of social network
        switch(post.getNetwork()){
            case "Facebook":
                message = Message.obtain(null, CommentPostService.FB_LIKE, 0, 0);

                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerPostComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "Twitter":
                message = Message.obtain(null, CommentPostService.TWITTER_FAVOURITE, 0, 0);

                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerPostComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "Instagram":
                message = Message.obtain(null, CommentPostService.INSTAGRAM_LIKE, 0, 0);

                //attach bundle
                message.setData(bundle);
                //send message
                try {
                    messengerPostComments.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void sendRetweet (View v){
        //construct message and send to service
        SendParcel parcel = new SendParcel();
        parcel.id = post.getGivenID();

        Bundle bundle = new Bundle();
        bundle.putParcelable("id", parcel);

        Message message = Message.obtain(null, CommentPostService.TWITTER_RETWEET, 0, 0);

        //attach bundle
        message.setData(bundle);
        //send message
        try {
            messengerPostComments.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////////SENDING A COMMENT
    public void postComment(View v){

        //extract text from user
        EditText textUpdate = (EditText) findViewById(R.id.userText);
        String text = textUpdate.getText().toString();

        //ensure text OR photo is provided
        if (text.matches("")){
            Toast.makeText(ViewPost.this, "You must write a comment", Toast.LENGTH_LONG).show();
        } else {
            //if null then post a toast notifying user that comment cannot be null

            //check network
            //send message to bound service to send a like
            Message message = null;

            SendParcel parcel = new SendParcel();
            parcel.id = post.getGivenID();
            parcel.text = text;

            //if it is twitter and sending a comment then we need the screen name of whom we are replying to
            if(post.getNetwork().equals("Twitter")){
                parcel.twitterScreenName = post.getTwitterScreenName();
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable("id", parcel);

            //decide which method to call based on type of social network
            switch (post.getNetwork()) {
                case "Facebook":
                    message = Message.obtain(null, CommentPostService.FB_COMMENT, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerPostComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "Twitter":
                    message = Message.obtain(null, CommentPostService.TWITTER_REPLY, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerPostComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case "Instagram":
                    message = Message.obtain(null, CommentPostService.INSTAGRAM_COMMENT, 0, 0);

                    //attach bundle
                    message.setData(bundle);
                    //send message
                    try {
                        messengerPostComments.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

        //clear the text
        EditText textbox = (EditText) findViewById(R.id.userText);
        textbox.getText().clear();
    }

    /////////////////////getting twitter ID
    private void getTwitterInfo() {
        TwitterSession twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
        Long userID = twitterSession.getUserId();

        if (post.getUserID().equals(String.valueOf(userID))) {
            Message message = Message.obtain(null, CommentRequestService.TWITTER_COMMENTS, 0, 0);
            SendParcel parcel = new SendParcel();
            parcel.id = post.getGivenID();

            Bundle bundle = new Bundle();
            bundle.putParcelable("id", parcel);
            message.setData(bundle);

            try {
                messengerRequestComments.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();

        //registering our receiver
        IntentFilter filter = new IntentFilter("com.example.user.sociall2.Comments");
        this.registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbind from services when activity is destroyed

        if(serviceConnectionRequestComments!=null) {
            unbindService(serviceConnectionRequestComments);
            serviceConnectionRequestComments = null;
        }

        if(serviceConnectionPostComments!=null) {
            unbindService(serviceConnectionPostComments);
            serviceConnectionPostComments = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unregister the receiver
        this.unregisterReceiver(this.receiver);
    }
}
