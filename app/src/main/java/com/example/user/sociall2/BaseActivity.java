package com.example.user.sociall2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/////////////////////////////// IMPORTANT - A lot of the content of this activity is based on an
////////////////////////////// OPEN SOURCE BASE DRAWER/NAVIGATION TUTORIAL
/////////////////////////////// http://naddydroid.blogspot.co.uk/2014/05/implementing-android-navigation-drawer.html.

//other activities inherit from this activity to have the navigation drawer
public class BaseActivity extends AppCompatActivity {

    private DrawerLayout layout;
    private ListView list;
    private ActionBarDrawerToggle toggle;
    protected Toolbar toolbar;

    // nav drawer title and colour
    private CharSequence title;

    private String titleCol = "<font color=\"#ffffff\">";

    private ArrayList<NavDrawerItem> items;
    private NavDrawerListAdapter adapter;

    //tokens
    private String TWITTER_KEY;
    private String TWITTER_SECRET;
    private TwitterSession twitterSession;

    private AccessToken FBaccessToken;

    private String instagramSession;

    private String googleSession;

    protected void onCreateDrawer() {
        setContentView(R.layout.navigation_layout);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //initialise sdk's
        FacebookSdk.sdkInitialize(getApplicationContext());

        TWITTER_KEY = getString(R.string.twitter_key);
        TWITTER_SECRET = getString(R.string.twitter_secret);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

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

        //check to see if there is a Google session
        sharedPreferences = this.getSharedPreferences("Google", 0);
        googleSession = sharedPreferences.getString("session", "");

        //set user information
        setHeaderInfo();
    }

    //retrieves the menu titles and icons and places them in arrays
    public void set(String[] navMenuTitles, TypedArray navMenuIcons) {

        title = getTitle();

        layout = (DrawerLayout) findViewById(R.id.drawer_layout);
        list = (ListView) findViewById(R.id.left_drawer);

        createHeader();

        items = new ArrayList<>();

        if (navMenuIcons == null) {
            for (int i = 0; i < navMenuTitles.length; i++) {
                items.add(new NavDrawerItem(navMenuTitles[i]));
            }
        } else {
            for (int i = 0; i < navMenuTitles.length; i++) {
                items.add(new NavDrawerItem(navMenuTitles[i], navMenuIcons.getResourceId(i, -1)));
            }
        }

        list.setOnItemClickListener(new SlideMenuClickListener());

        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(),
                items);
        list.setAdapter(adapter);

        // enabling action bar settings
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setTitle(title);

        //toggling movement
        toggle = new ActionBarDrawerToggle(
                this,
                layout,
                toolbar,
                R.string.app_name,
                R.string.app_name
                ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(title);
                // calling onPrepareOptionsMenu() to show action bar icons
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(title);
                // calling onPrepareOptionsMenu() to hide action bar icons
                supportInvalidateOptionsMenu();
            }
        };
        layout.setDrawerListener(toggle);

    }

    //////////////////////////////HERE WE RETRIEVE DATA ABOUT THE USER FROM SOCIAL NETWORKS//////////////////////////////
    public void createHeader(){
        LayoutInflater inflater = (LayoutInflater)
                this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.header, null);
        list.addHeaderView(header);
    }

    public void setHeaderInfo(){
        //if facebook is used then use their facebook details
        if (FBaccessToken != null) {
            getFBinfo();
        } else if (twitterSession != null) {
            getTwitterInfo();
        } else if (instagramSession != "") {
            getInstagramInfo();
        } else {
            //if user is not logged into a social network, set photo to unavailable
            ImageView photoUnavailable = (ImageView)findViewById(R.id.headerPhoto);
            photoUnavailable.setImageResource(R.drawable.image_unavailable);
        }
    }

    //////////////////////////////// GET SOCIAL NETWORK INFO METHODS /////////////////////////////////////////////////

    private void getFBinfo() {
        try {
            //FB graph requests, making API call
            GraphRequest request = GraphRequest.newMeRequest(FBaccessToken, new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {

                    //convert response into json object
                    JSONObject jsonObject1 = response.getJSONObject();
                    try {
                        //set name
                        TextView text = (TextView) findViewById(R.id.headerName);
                        text.setText(jsonObject1.getString("name"));

                        //get profile picture from url
                        String imageURL = "https://graph.facebook.com/" + jsonObject1.getString("id") + "/picture?type=large";
                        ImageView picture = (ImageView) findViewById(R.id.headerPhoto);
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
                    TextView text = (TextView) findViewById(R.id.headerName);
                    text.setText(result.data.name);

                    //get profile picture from url
                    String imageURL = result.data.profileImageUrl.replace("_normal", "");
                    ImageView picture = (ImageView) findViewById(R.id.headerPhoto);
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
            TextView text = (TextView) findViewById(R.id.headerName);
            text.setText(session.getString("full_name"));

            //get profile picture from url
            String imageURL = session.getString("profile_picture");

            //.getString("nameValuePairs.full_name");
            ImageView picture = (ImageView) findViewById(R.id.headerPhoto);
            new DownloadImageTask(picture).execute(imageURL);
            picture.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Unable to retrieve profile information", Toast.LENGTH_SHORT);
        }

    }

    ///////////////////////////////////////////////SELECTING MENU ITEMS//////////////////////////////////
    private class SlideMenuClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // display view for selected nav drawer item
            displayView(position);
        }
    }


    //changes layout on decision if drawer is to be open or not
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            if (layout.isDrawerOpen(list)) {
                layout.closeDrawer(list);
            } else {
                layout.openDrawer(list);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Called when invalidateOptionsMenu() is triggered
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setTitle(title);
        return super.onPrepareOptionsMenu(menu);
    }


     // What to do if item in list is selected

    private void displayView(int position) {
        switch (position - 1) { //need to do -1 as header is counted as an item
            case 0: //news feed
                Intent intent = new Intent(this, NewsFeed.class);
                startActivity(intent);
                finish();
                break;
            case 1: //profile
                Intent intent1 = new Intent(this, Profile.class);
                startActivity(intent1);
                finish();
                break;
            case 2: //notifications
              //  Intent intent2 = new Intent(this, third.class);
              //  startActivity(intent2);
                finish();
                break;
            case 3: //messages
                Intent intent3 = new Intent(this, Messages.class);
                startActivity(intent3);
                finish();
                break;
            case 4: //friends list
                Intent intent4 = new Intent(this, FriendsList.class);
                startActivity(intent4);
                finish();
                break;
            case 5: //options
                Intent intent5 = new Intent(this, Options.class);
                startActivity(intent5);
                finish();
                break;
            default:
                break;
        }

        // update selected item and title, then close the drawer
        list.setItemChecked(position, true);
        list.setSelection(position);
        layout.closeDrawer(list);
    }


    public void setTitle(CharSequence title2) {
        title = title2;
        getSupportActionBar().setTitle((Html.fromHtml(titleCol + title)));
    }

    //When using the ActionBarDrawerToggle
    // you must call it during onPostCreate() and onConfigurationChanged()...
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        toggle.onConfigurationChanged(newConfig);
    }

    ///////////////////////////////download image//////////////////////////////////////////////////////
    ///////////////////////////taken from http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android

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

