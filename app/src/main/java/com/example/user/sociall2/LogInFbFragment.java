package com.example.user.sociall2;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

//I have used the official facebook guide to the SDK write this code
//https://developers.facebook.com/docs/facebook-login/android
//short code samples and explanations to aid step by step instructions are given

//this fragment connects to facebook
public class LogInFbFragment extends Fragment {

    public LogInFbFragment() {}

    private LoginButton fb_login_button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initalise facebook
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());

        //create callback
        callbackManager = CallbackManager.Factory.create();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_log_in_fb_fragment, container, false);
        return rootView;
    }

    //handle callback (response to attempt log in)
    private CallbackManager callbackManager;
    private FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            //if successful log in, then go to newsfeed
            Intent intent = new Intent(getActivity(), NewsFeed.class);
            getActivity().startActivity(intent);
        }

        @Override
        public void onCancel() {
            Toast.makeText(getContext(), "Log in canceled", Toast.LENGTH_SHORT);
        }

        @Override
        public void onError(FacebookException e) {
            Toast.makeText(getContext(), "Unable to log in via Facebook", Toast.LENGTH_SHORT);
        }
    };

    //To get a person's data, app needs to use person's access token
    //To get access token, that person must authorize app (through Facebook Login)
    //And accept permissions
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fb_login_button = (LoginButton) view.findViewById(R.id.fb_login_button);

        //Gain permissions from user
        fb_login_button.setFragment(this);
        fb_login_button.setReadPermissions(Arrays.asList("public_profile, user_friends, user_posts, publish_actions"));
        fb_login_button.registerCallback(callbackManager, callback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}