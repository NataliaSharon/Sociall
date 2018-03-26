package com.example.user.sociall2;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;

public class DeleteService extends Service {

    public static final int FB_DELETE = 0;
    public static final int TWITTER_DELETE = 1;

    Messenger messenger;
    SendParcel parcel;
    String objectID;

    private class MyHandler extends Handler {
        @Override
        //handle incoming messages
        public void handleMessage(Message message) {
            //get the ID from the parcel
            parcel = message.getData().getParcelable("id");
            objectID = parcel.id;

            switch (message.what) {
                case FB_DELETE:
                    facebookDelete();
                    break;

                case TWITTER_DELETE:
                    twitterDelete();
                    break;

            }
        }
    }

    //////////////////// delete methods ///////////
    private void facebookDelete(){
        FacebookSdk.sdkInitialize(getApplicationContext());
        AccessToken FBaccessToken = AccessToken.getCurrentAccessToken();

        new GraphRequest(FBaccessToken, "/" + objectID, null, HttpMethod.DELETE,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Toast.makeText(getApplicationContext(), "Your post has been deleted",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        ).executeAsync();
    }

    private void twitterDelete(){
        TwitterSession twitterSession = Twitter.getInstance().core.getSessionManager().getActiveSession();

        //call destroy and pass through id
        TwitterCore.getInstance().getApiClient(twitterSession).getStatusesService()
                .destroy(Long.valueOf(objectID), null, new Callback<Tweet>() {

                    @Override
                    public void success(Result result) {
                        Toast.makeText(getApplicationContext(), "Your post has been deleted",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void failure(TwitterException e) {
                        Toast.makeText(getApplicationContext(), "Post could not be deleted",
                                Toast.LENGTH_LONG).show();
                    }
                });

    }

    private void finish(){
        //send broadcast back to notify Profile to refresh
        Intent broadcast = new Intent("com.example.user.sociall2.Refresh");
        sendBroadcast(broadcast);
    }

    /////////////////////life cycle//////////////
    @Override
    public void onCreate () {
        messenger = new Messenger(new MyHandler());
    }

    @Nullable
    @Override
    public IBinder onBind (Intent intent){

        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }
}

