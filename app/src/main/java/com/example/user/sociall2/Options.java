package com.example.user.sociall2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterSession;

public class Options extends BaseActivity {

    private TwitterSession twitterSession;

    private InstagramApp instagramApp;

    private String CLIENT_ID;
    private String CLIENT_SECRET;
    private String CALLBACK_URL;

    //buttons
    Button twitterLogOut;
    Button instagramLogOut;
    Button instagramLogIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();

        //initialise
        FacebookSdk.sdkInitialize(getApplicationContext());

        //initialise instagram
        CLIENT_ID = getString(R.string.instagram_id);
        CLIENT_SECRET = getString(R.string.instagram_secret);
        CALLBACK_URL  = getString(R.string.instagram_callback);

        setContentView(R.layout.activity_options);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles,navMenuIcons);

        //buttons
        twitterLogOut = (Button) findViewById(R.id.twitterLogOut);
        instagramLogOut = (Button) findViewById(R.id.instagramLogOut);
        instagramLogIn = (Button) findViewById(R.id.instagramLogIn);

    }

    public void TwitterLogOut(View v){
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();
        Twitter.getSessionManager().clearActiveSession();
        Twitter.logOut();

        //change buttons
        twitterLogOut.setVisibility(View.GONE);
        this.findViewById(R.id.twitter_login_button).setVisibility(View.VISIBLE);

    }

    public void InstagramLogOut(View v){

       //clear session by removing the shared preference
        //as this stores the session object
        //so when we check if it exists and it doesn't, it means the user is not logged in
        SharedPreferences sharedPreferences = this.getSharedPreferences("instagramObject", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("session");
        editor.apply();

        //change buttons
        instagramLogIn.setVisibility(View.VISIBLE);
        instagramLogOut.setVisibility(View.GONE);

    }

    public void InstagramLogIn(View v){
        instagramApp = new InstagramApp(this, CLIENT_ID, CLIENT_SECRET, CALLBACK_URL);
        instagramApp.authorize();  //add this in your button click or wherever you need to call the instagram api
        instagramApp.setListener(new InstagramApp.OAuthAuthenticationListener() {

            @Override
            public void onSuccess() {
                //change buttons
                instagramLogIn.setVisibility(View.GONE);
                instagramLogOut.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFail(String error) {
                Toast.makeText(Options.this, "Unable to log in via Instagram", Toast.LENGTH_LONG).show();
            }
        });
    }

    //on resume find twitter and instagram tokens
    @Override
    protected void onResume() {
        super.onResume();

        //get twitter session
        twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();

        //if sesison is active then user is logged in so display log out (default)
        if(twitterSession != null) {
            twitterLogOut.setVisibility(View.VISIBLE);
            this.findViewById(R.id.twitter_login_button).setVisibility(View.GONE);
        } else {
            //if null then user is not logged in so display log in fragment
            twitterLogOut.setVisibility(View.GONE);
            this.findViewById(R.id.twitter_login_button).setVisibility(View.VISIBLE);
        }

        //check to see if there is an instagram session
        SharedPreferences sharedPreferences = this.getSharedPreferences("instagramObject", 0);
        String instagramSession = sharedPreferences.getString("session", "");

        //if sesison is active then user is logged in so display log out (default)
        if(instagramSession != "") {
            instagramLogOut.setVisibility(View.VISIBLE);
            instagramLogIn.setVisibility(View.GONE);
        } else {
            //if null then user is not logged in so display log in fragment
            instagramLogOut.setVisibility(View.GONE);
            instagramLogIn.setVisibility(View.VISIBLE);
        }

    }

    //twitter fragment result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.twitterFragment);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
            this.findViewById(R.id.twitter_login_button).setVisibility(View.GONE);
            twitterLogOut.setVisibility(View.VISIBLE);
        }
    }
}
