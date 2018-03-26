package com.example.user.sociall2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class Profile extends BaseActivity {

    //tokens
    private TwitterSession twitterSession;
    public static String fbName; //need this for knowing the name of the user for the newsfeed objects
    public static String fbID;

    private AccessToken FBaccessToken;

    private String instagramSession;

    //booleans of relieved broadcasts
    private boolean facebookBC = false;
    private boolean twitterBC = false;
    private boolean instagramBC = false;
    private boolean extraFacebookBC = false;
    private boolean executedAlready = false;

    //booleans for finished binding
    private boolean facebookBind = false;
    private boolean twitterBind = false;
    private boolean instagramBind = false;
    private boolean facebookExtraBind = false;
    private boolean deleteBind = false;

    //messengers
    public Messenger messengerFB;
    public Messenger messengerTwitter;
    public Messenger messengerInstagram;
    public Messenger messengerFBExtra;
    public Messenger messengerDelete;
    private int counter;

    //create list of objects
    private  List<NewsFeedItem> list = new ArrayList<>();

    private ListView view;

    //id of chosen object to delete
    String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();

        counter = 0;

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

                Intent intent = new Intent(Profile.this, ViewPost.class);
                intent.putExtra("Post", (Serializable) list.get(position));
                startActivity(intent);
            }
        });

        //bind to services
        this.bindService(new Intent(this, FacebookService.class), serviceConnectionFB, Context.BIND_AUTO_CREATE);
        this.bindService(new Intent(this, TwitterService.class), serviceConnectionTwitter, Context.BIND_AUTO_CREATE);
        this.bindService(new Intent(this, InstagramService.class), serviceConnectionInstagram, Context.BIND_AUTO_CREATE);
        this.bindService(new Intent(this, FacebookExtraService.class), serviceConnectionFBExtra, Context.BIND_AUTO_CREATE);
        this.bindService(new Intent(this, DeleteService.class), serviceConnectionDelete, Context.BIND_AUTO_CREATE);

        //listeners for if profile icons are clicked on (get corresponding information)
        ImageView facebookListener = (ImageView) findViewById(R.id.facebookConnected);
        ImageView twitterListener = (ImageView) findViewById(R.id.twitterConnected);
        ImageView instagramListener = (ImageView) findViewById(R.id.instagramConnected);

        facebookListener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFBinfo();
            }
        });

        twitterListener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTwitterInfo();
            }
        });

        instagramListener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInstagramInfo();
            }
        });


    }

    //////////////////////////// SERVICE CONNECTIONS ////////////////////////
    //note, need a separate serviceConnection and messenger for each service
    private ServiceConnection serviceConnectionFB = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerFB = new Messenger(service);

            facebookBind = true;
            //check to see if all services have bound, if so get feeds
            if(facebookBind && twitterBind && instagramBind && facebookExtraBind && deleteBind){
                getFeeds();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerFB = null;
        }
    };

    private ServiceConnection serviceConnectionTwitter = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerTwitter = new Messenger(service);

            twitterBind = true;
            //check to see if all services have bound, if so get feeds
            if(facebookBind && twitterBind && instagramBind && facebookExtraBind && deleteBind){
                getFeeds();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerTwitter = null;
        }
    };
    private ServiceConnection serviceConnectionInstagram = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerInstagram = new Messenger(service);

            instagramBind = true;
            //check to see if all services have bound, if so get feeds
            if(facebookBind && twitterBind && instagramBind && facebookExtraBind && deleteBind){
                getFeeds();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerInstagram = null;
        }
    };

    private ServiceConnection serviceConnectionFBExtra = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerFBExtra = new Messenger(service);

            facebookExtraBind = true;
            //check to see if all services have bound, if so get feeds
            if(facebookBind && twitterBind && instagramBind && facebookExtraBind && deleteBind){
                getFeeds();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerFBExtra = null;
        }
    };

    private ServiceConnection serviceConnectionDelete = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            messengerDelete = new Messenger(service);

            deleteBind = true;
            //check to see if all services have bound, if so get feeds
            if(facebookBind && twitterBind && instagramBind && facebookExtraBind && deleteBind){
                getFeeds();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messengerDelete = null;
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////
    private void setInfo() {

        if (FBaccessToken != null) {        //if FB is logged in then use FB details
            getFBinfo();
        } else if (twitterSession != null) {    //else get twitter
            getTwitterInfo();
        } else if (instagramSession != "") {    //else get instagram
            getInstagramInfo();
        } else {
            //if user is not logged into a social network, set photo to unavailable
            ImageView photoUnavailable = (ImageView)findViewById(R.id.profilePic);
            photoUnavailable.setImageResource(R.drawable.image_unavailable);

            //and show error message
            ImageView errorImage = (ImageView)findViewById(R.id.errorImage);
            TextView errorText = (TextView)findViewById(R.id.errorText);
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }
    }

    //////////////////////////////// GET SOCIAL NETWORK INFO METHODS ///////////////////////////


    private void getFBinfo() {
        try {
            //FB graph requests, making API call
            GraphRequest request = GraphRequest.newMeRequest(FBaccessToken, new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    //convert response into json object
                    JSONObject userInfo = response.getJSONObject();
                    try {
                        //set name
                        TextView text = (TextView) findViewById(R.id.name);
                        //store name as facebook does not return name of user with posts
                        fbName = userInfo.getString("name");
                        fbID = userInfo.getString("id");
                        text.setText(fbName);

                        //get profile picture from url
                        String imageURL = "https://graph.facebook.com/" + fbID + "/picture?type=large";
                        ImageView picture = (ImageView) findViewById(R.id.profilePic);
                        new DownloadImageTask(picture).execute(imageURL);
                        picture.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        //in case of failure, attempt to get info from Twitter
                        getTwitterInfo();
                    }
                }
            });
            request.executeAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void getTwitterInfo() {
        try {
            TwitterCore.getInstance().getApiClient(twitterSession).getAccountService().verifyCredentials(true, false, new Callback<User>() {
                @Override
                public void success(Result<User> result) {
                    //set name
                    TextView text = (TextView) findViewById(R.id.name);
                    text.setText(result.data.name);

                    //get profile picture from url
                    String imageURL = result.data.profileImageUrl.replace("_normal", "");
                    ImageView picture = (ImageView) findViewById(R.id.profilePic);
                    //download image and set
                    new DownloadImageTask(picture).execute(imageURL);
                    picture.setVisibility(View.VISIBLE);

                }

                @Override
                public void failure(TwitterException e) {
                    //in case of failure, attempt to get instagram info
                    getInstagramInfo();
                }
            });
        } catch (Exception e){
            //in case of failure, attempt to get instagram info
            getInstagramInfo();
        }
    }

    public void getInstagramInfo() {
        try {
            //turn string into JSON
            JSONObject object1 = new JSONObject(instagramSession);
            //retrieve the user object
            JSONObject user = new JSONObject(String.valueOf(object1.getJSONObject("nameValuePairs")));
            //There are two objects, one with access token and one with details, retrieve details
            JSONObject details = new JSONObject(String.valueOf(user.getJSONObject("user")));
            //get json for details
            JSONObject session = new JSONObject(String.valueOf(details.getJSONObject("nameValuePairs")));

            //retrieve key value pairs and display
            TextView text = (TextView) findViewById(R.id.name);
            text.setText(session.getString("full_name"));

            //get profile picture from url
            String imageURL = session.getString("profile_picture");

            //.getString("nameValuePairs.full_name");
            ImageView picture = (ImageView) findViewById(R.id.profilePic);
            new DownloadImageTask(picture).execute(imageURL);
            picture.setVisibility(View.VISIBLE);

        } catch (Throwable t) {

        }

    }


    //////////////////////////////////// GET FEEDS METHODS /////////////////////////////////////////////


    public void getFeeds(){

        //start intents to services
        if(FBaccessToken != null) {
            //send a message to the service
            Message message = Message.obtain(null, FacebookService.REQUEST, 0, 0);

            try {
                messengerFB.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } else {
            facebookBC = true;
            extraFacebookBC = true;
        }


        if(twitterSession != null) {
            Message message = Message.obtain(null, TwitterService.REQUEST, 0, 0);

            try {
                messengerTwitter.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            twitterBC = true;
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("instagramObject", 0);
        instagramSession = sharedPreferences.getString("session", "");

        if(instagramSession != "") {
            Message message = Message.obtain(null, InstagramService.PROFILE, 0, 0);

            try {
                messengerInstagram.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            instagramBC = true;
        }
    }

    ////////BROAD CAST RELIEVER WAITS FOR BROADCASTS
    private BroadcastReceiver receiver = new BroadcastReceiver()  {
        @Override
        public void onReceive(Context context, Intent intent) {

            String network = intent.getStringExtra("Network");

            //set whichever social network has returned to true
            //and retrieve the list of objects
            //add to the list here
            switch (network) {
                case "Facebook":
                    facebookBC = true;
                    //extract objects and append
                    for (Parcelable parcel : intent.getParcelableArrayListExtra("list")) {
                        NewsFeedItem item = (NewsFeedItem) parcel;
                        list.add(item);
                    }
                    break;
                case "Twitter":
                    twitterBC = true;
                    //extract objects and append
                    for (Parcelable parcel : intent.getParcelableArrayListExtra("list")) {
                        NewsFeedItem item = (NewsFeedItem) parcel;
                        list.add(item);
                    }
                    break;
                case "Instagram":
                    instagramBC = true;
                    //extract objects and append
                    for (Parcelable parcel : intent.getParcelableArrayListExtra("list")) {
                        NewsFeedItem item = (NewsFeedItem) parcel;
                        list.add(item);
                    }
                    break;
                case "ExtraFacebook":
                    //decrement this each time there is a result
                    counter--;
                    if (counter == 0)
                        extraFacebookBC = true;

                    //retrieve the info here from the BC and find the corresponding item
                    String id = intent.getStringExtra("id");
                    String url = intent.getStringExtra("url");

                    for (NewsFeedItem item : list) {
                        if (item.getGivenID().equals(id)) {
                            item.setPhotoContent(url);
                        }
                    }
                    break;
            }

            //when the entire list has been returned, and this is the first time this is being executed
            //find facebook posts and make additional reuqests for extra information
            if(facebookBC && twitterBC && instagramBC && !executedAlready){
                executedAlready = true;
                for (NewsFeedItem item : list) {
                    //check if facebook item
                    if (item.getNetwork().equals("Facebook")){
                        //increase to tell the amount of BC's we need back
                        counter++;
                        //send a message to the service with the facebook id
                        Message message = Message.obtain(null, FacebookExtraService.REQUEST, 0, 0);
                        SendParcel parcel = new SendParcel();
                        parcel.id = item.getGivenID();

                        Bundle bundle = new Bundle();
                        bundle.putParcelable("id", parcel);
                        message.setData(bundle);

                        try {
                            messengerFBExtra.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            //check to see if all broadcasts have returned to finish
            if (extraFacebookBC){
                finish();
            }
        }
    };

    //receiver that listens out for post to be deleted
    private BroadcastReceiver deleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //get the network and id
            id = intent.getStringExtra("id");
            final String network = intent.getStringExtra("Network");

            //pop up dialog box asking the user for confirmation
            DialogInterface.OnClickListener confirmationListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            //send message to correct delete service method

                            Message message = null;
                            SendParcel parcel;
                            Bundle bundle;

                            switch (network){
                                case "Facebook":
                                    message = Message.obtain(null, DeleteService.FB_DELETE, 0, 0);

                                    parcel = new SendParcel();
                                    parcel.id = id;

                                    bundle = new Bundle();
                                    bundle.putParcelable("id", parcel);
                                    message.setData(bundle);

                                    try {
                                        messengerDelete.send(message);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                break;

                                case "Twitter":
                                    message = Message.obtain(null, DeleteService.TWITTER_DELETE, 0, 0);

                                     parcel = new SendParcel();
                                    parcel.id = id;

                                    bundle = new Bundle();
                                    bundle.putParcelable("id", parcel);
                                    message.setData(bundle);

                                    try {
                                        messengerDelete.send(message);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                break;

                                case "Instagram":
                                    //cannot delete instagram posts
                                    Toast.makeText(Profile.this, "Unable to delete this post", Toast.LENGTH_LONG).show();
                                    break;
                            }

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            //no further actions
                            break;
                    }
                }
            };

            //using builder to create dialog box
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage("Are you sure you wish to delete this post?").setPositiveButton("Yes", confirmationListener)
                    .setNegativeButton("No", confirmationListener).show();

        }
    };

    //receiver that listens out for refresh
    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            //iterate through list
            //delete the object so it disappears from view (this works better than refreshing
            //because it takes a moment for social network API to process and update data

            //use an iterator to avoid ConcurrentModificationException

            for (Iterator<NewsFeedItem> iterator = list.iterator(); iterator.hasNext();) {
                String string = iterator.next().getGivenID();
                if (string.equals(id)) {
                    // Remove the current element from the iterator and the list.
                    iterator.remove();
                }
            }

            finish();

        }
    };

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
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }
    }

    //check keys on resume in case user changed settings
    @Override
    protected void onResume() {
        super.onResume();

        //get access tokens and sessions
        FBaccessToken = AccessToken.getCurrentAccessToken();

        try {
            twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
        }catch (NullPointerException e){
            twitterSession = null;
        }

        //check to see if there is an instagram session
        SharedPreferences sharedPreferences = this.getSharedPreferences("instagramObject", 0);
        instagramSession = sharedPreferences.getString("session", "");

        //set user information
        setInfo();

        //icons, am using drawable instead of resource as may have to greyscale the images
        ImageView fbIcon = (ImageView) findViewById(R.id.facebookConnected);
        ImageView  twitterIcon = (ImageView) findViewById(R.id.twitterConnected);
        ImageView instagramIcon = (ImageView) findViewById(R.id.instagramConnected);

        //check which social networks are logged into, if logged in then get the feed
        if (FBaccessToken == null) {
            //set icon to greyed out
            fbIcon.setImageResource(R.drawable.ic_facebook_grey);
        } else {
            fbIcon.setImageResource(R.drawable.ic_facebook);
        }

        if (twitterSession == null) {
            //set icon to greyed out
            twitterIcon.setImageResource(R.drawable.ic_twitter_grey);
        } else {
            twitterIcon.setImageResource(R.drawable.ic_twitter);
        }

        if (instagramSession == ""){
            //set icon to greyed out
            instagramIcon.setImageResource(R.drawable.ic_instagram_grey);
        } else {
            instagramIcon.setImageResource(R.drawable.ic_instagram);
        }


        //registering our receivers
        IntentFilter filter = new IntentFilter("com.example.user.sociall2.Broadcast");
        this.registerReceiver(receiver, filter);

        filter = new IntentFilter("com.example.user.sociall2.Delete");
        this.registerReceiver(deleteReceiver, filter);

        filter = new IntentFilter("com.example.user.sociall2.Refresh");
        this.registerReceiver(refreshReceiver, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbind from services when activity is destroyed
        if(serviceConnectionFB!=null) {
            unbindService(serviceConnectionFB);
            serviceConnectionFB = null;
        }

        if(serviceConnectionTwitter!=null) {
            unbindService(serviceConnectionTwitter);
            serviceConnectionTwitter = null;
        }

        if(serviceConnectionInstagram!=null) {
            unbindService(serviceConnectionInstagram);
            serviceConnectionInstagram = null;
        }

        if(serviceConnectionFBExtra!=null) {
            unbindService(serviceConnectionFBExtra);
            serviceConnectionFBExtra = null;
        }

        if(serviceConnectionDelete!=null) {
            unbindService(serviceConnectionDelete);
            serviceConnectionDelete = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unregister the receiver
        this.unregisterReceiver(this.receiver);
        this.unregisterReceiver(this.deleteReceiver);
        this.unregisterReceiver(this.refreshReceiver);
    }

    ///////////////DOWNLOAD IMAGE//////////////////////////////////////////////////////
    //////////////taken from http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
    ////////////// not my own code///////////////////
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
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}

