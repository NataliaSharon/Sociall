package com.example.user.sociall2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterSession;

import io.fabric.sdk.android.Fabric;

public class Decision extends AppCompatActivity {

    // keys, tokens, sessions
    private String TWITTER_KEY;
    private String TWITTER_SECRET;
    private TwitterSession twitterSession;

    private AccessToken fbAccessToken;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialise
        TWITTER_KEY = getString(R.string.twitter_key);
        TWITTER_SECRET = getString(R.string.twitter_secret);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        try {
            twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();
        }catch (NullPointerException e){
            twitterSession = null;
        }

        context = this.getApplicationContext();

        FacebookSdk.sdkInitialize(context);

        setContentView(R.layout.activity_decision);

        //creates intent to start next activity, but with 2 second delay
        //so this activity acts as a splash screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //get fb access token to check if user is logged in via facebook
                fbAccessToken = AccessToken.getCurrentAccessToken();

                //check to see if there is an instagram session
                SharedPreferences sharedPreferences = context.getSharedPreferences("instagramObject", 0);
                String instagramSession = sharedPreferences.getString("session", "");

                //check to see if there is a google session
                sharedPreferences = context.getSharedPreferences("Google", 0);
                String googleSession = sharedPreferences.getString("session", "");

                if (fbAccessToken == null && twitterSession == null && instagramSession == "" && googleSession == "") {
                    //not connected, log the user in
                    Intent logInIntent = new Intent(Decision.this, LogIn.class);
                    //start next activity
                    Decision.this.startActivity(logInIntent);
                } else {
                    //already connected, launch activity
                    Intent mainIntent = new Intent(Decision.this, NewsFeed.class);
                    //start next activity
                    Decision.this.startActivity(mainIntent);
                }

                //finish this activity
                Decision.this.finish();
            }
        }, 1500); //1.5 second delay
    }

}
