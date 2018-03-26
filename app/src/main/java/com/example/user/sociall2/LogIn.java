package com.example.user.sociall2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

//this class allows user to log into facebook by using the facebook log in button as a fragment
public class LogIn extends AppCompatActivity {

    private InstagramApp instagramApp;

    private String CLIENT_ID;
    private String CLIENT_SECRET;
    private String CALLBACK_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialise instagram
        CLIENT_ID = getString(R.string.instagram_id);
        CLIENT_SECRET = getString(R.string.instagram_secret);
        CALLBACK_URL  = getString(R.string.instagram_callback);

        //set layout of the UI
        setContentView(R.layout.activity_log_in);

    }


    //twitter fragment result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.twitterFragment);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);

            //if successful log in, then go to newsfeed
            Intent intent = new Intent(getApplicationContext(), NewsFeed.class);
            startActivity(intent);
        }
    }

    //instagram uses a button instead of a fragment as the code is more complicated
    //and instagram does not provide direct code for logging in
    public void InstagramLogIn(View v) {

        instagramApp = new InstagramApp(this, CLIENT_ID, CLIENT_SECRET, CALLBACK_URL);
        instagramApp.authorize();  //add this in your button click or wherever you need to call the instagram api
        instagramApp.setListener(new InstagramApp.OAuthAuthenticationListener() {

            @Override
            public void onSuccess() {
                //if successful log in, then go to newsfeed
                Intent intent = new Intent(getApplicationContext(), NewsFeed.class);
                startActivity(intent);
            }

            @Override
            public void onFail(String error) {
                Toast.makeText(LogIn.this, "Unable to retrieve Instagram posts", Toast.LENGTH_LONG).show();
            }
        });

    }
}

