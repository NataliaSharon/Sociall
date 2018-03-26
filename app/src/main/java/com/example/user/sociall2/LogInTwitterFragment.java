package com.example.user.sociall2;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import io.fabric.sdk.android.Fabric;

//for all twitter code I have used the official developers documentation to assist me
//https://dev.twitter.com/overview/documentation

public class LogInTwitterFragment extends Fragment{

    public LogInTwitterFragment(){}

    // keys
    private String TWITTER_KEY;
    private String TWITTER_SECRET;


    //use this when printing to console to be able to filter relevant messages
    private static final String log = "logOutput: ";

    private TwitterLoginButton twitterLoginButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialise
        TWITTER_KEY = getString(R.string.twitter_key);
        TWITTER_SECRET = getString(R.string.twitter_secret);

        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(getActivity(), new Twitter(authConfig));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.activity_log_in_twitter_fragment, container, false);
        return rootview;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        twitterLoginButton = (TwitterLoginButton) view.findViewById(R.id.twitter_login_button);
        Callback twitterCallback = new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {

            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(getContext(), "Unable to log in via Twitter", Toast.LENGTH_LONG).show();
            }
        };
        twitterLoginButton.setCallback(twitterCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        twitterLoginButton.onActivityResult(requestCode, resultCode, data);
    }
}
