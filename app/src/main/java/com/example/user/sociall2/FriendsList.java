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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class FriendsList extends BaseActivity implements Serializable {

    private TwitterSession twitterSession;

    private AccessToken FBaccessToken;

    private String instagramSession;
    private boolean calledFriends = false;

    private ListView view;

    private Messenger messenger;

    //create list of objects from json objects
    public List<FriendsListItem> list = new ArrayList<FriendsListItem>();

    //booleans of received broadcasts
    boolean facebookBC = false;
    boolean twitterBC = false;
    boolean instagramBC = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();

        //initialise facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_friends_list);

        //get access tokens
        FBaccessToken = AccessToken.getCurrentAccessToken();

        //find view of list
        view = (ListView) findViewById(R.id.friendsList);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles, navMenuIcons);

        //listener for when a friend is clicked on
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View clickView,
                                    int position, long id) {
                Intent intent = new Intent(FriendsList.this, FriendProfile.class);
                intent.putExtra("FriendObject", (Serializable) FriendsListAdapter.filteredList.get(position));
                startActivity(intent);
            }
        });

        //bind to services
        this.bindService(new Intent(this, FriendsListService.class), serviceConnection, Context.BIND_AUTO_CREATE);

    }

        private ServiceConnection serviceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);

            //get the friends if not already done so
            if(!calledFriends) {
                calledFriends = true;
                getFriends();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
        }
    };

    public void getFriends(){

        //display error message if user is not logged in
        if(FBaccessToken == null && twitterSession == null && instagramSession.equals("")){
            //else show error message
            ImageView errorImage = (ImageView)findViewById(R.id.errorImage);
            TextView errorText = (TextView)findViewById(R.id.errorText);
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }

        //start intents to services
        if(FBaccessToken != null) {
            //send a message to the service
            Message message = Message.obtain(null, FriendsListService.FB, 0, 0);

            try {
                messenger.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            facebookBC = true;
        }

        if(twitterSession != null) {
            Message message = Message.obtain(null, FriendsListService.TWITTER, 0, 0);

            try {
                messenger.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            twitterBC = true;
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("instagramObject", 0);
        instagramSession = sharedPreferences.getString("session", "");

        if(instagramSession != "") {
            Message message = Message.obtain(null, FriendsListService.INSTAGRAM, 0, 0);

            try {
                messenger.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            instagramBC = true;
        }

    }

    ////////BROAD CAST RECIEVER WAITS FOR BROADCASTS
    private BroadcastReceiver receiver = new BroadcastReceiver()  {
        @Override
        public void onReceive(Context context, Intent intent) {

        //extract objects and append
        for(Parcelable parcel : intent.getParcelableArrayListExtra("list")) {
            FriendsListItem item = (FriendsListItem) parcel;
            list.add(item);
        }

        finish();

        }
    };


    ///////////////////////////sort and adapt/////////////////////

    public void finish(){

        //remove duplicates using iterator
        final List<String> usedNames = new ArrayList<>();
        Iterator<FriendsListItem> iterate = list.iterator();
        while (iterate.hasNext()) {
            FriendsListItem friend = iterate.next();
            String name = friend.getName();
            if (usedNames.contains(name)) {
                iterate.remove();
            } else {
                usedNames.add(friend.getName());
            }
        }

        //sort list  alphabetically
        Collections.sort(list, new Comparator<FriendsListItem>() {
            @Override
            public int compare(final FriendsListItem one, final FriendsListItem two) {
                return one.getName().compareTo(two.getName());
            }
        });


        //only put list through to adapter if list is not empty
        if (list.size() > 0) {

            //place data into list using adapter
            final FriendsListAdapter friendsListAdapter = new FriendsListAdapter(this, list);

            //place adapted list into view
            view.setAdapter(friendsListAdapter);

            //listener that watches text being entered into search bar
            EditText search = (EditText) findViewById(R.id.enterSearch);
            view.setTextFilterEnabled(true);
            search.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence filter, int arg1, int arg2, int arg3) {
                    //when text changes, call adapter with text to filter
                    //place data into list using adapter
                    friendsListAdapter.getFilter().filter(filter.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    friendsListAdapter.notifyDataSetChanged();
                }
            });

        } else {
            //else show error message
            ImageView errorImage = (ImageView)findViewById(R.id.errorImage);
            TextView errorText = (TextView)findViewById(R.id.errorText);
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }
    }

    //////////////////LIFE CYCLE /////////////////////////
    //check keys on resume in case user changed settings
    @Override
    protected void onResume() {
        super.onResume();

        //keys/tokens
        FBaccessToken = AccessToken.getCurrentAccessToken();

        try {
            twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
        }catch (NullPointerException e){
            twitterSession = null;
        }

        //check to see if there is an instagram session
        SharedPreferences sharedPreferences = this.getSharedPreferences("instagramObject", 0);
        instagramSession = sharedPreferences.getString("session", "");


        //registering our receiver
        IntentFilter filter = new IntentFilter("com.example.user.sociall2.FriendsBroadcast");
        this.registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbind from services when activiy is destroyed
        if(serviceConnection!=null) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unregister the receiver
        this.unregisterReceiver(this.receiver);
    }
}
