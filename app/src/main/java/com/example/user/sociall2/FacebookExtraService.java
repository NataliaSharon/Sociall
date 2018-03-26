package com.example.user.sociall2;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONObject;

public class FacebookExtraService extends Service {

    public static final int REQUEST = 0;

    private Messenger messenger;
    private SendParcel parcel;

    //tokens
    private AccessToken FBaccessToken;

    //initialise
    final String url[] = new String[1];

    private class MyHandler extends Handler {
        @Override
        //handle incoming messages
        public void handleMessage(Message message) {

            //get the ID from the parcel
            parcel = message.getData().getParcelable("id");
            final String id = parcel.id;

            //initialise sdk's
            FacebookSdk.sdkInitialize(getApplicationContext());
            FBaccessToken = AccessToken.getCurrentAccessToken();

            //get URL
            new GraphRequest(FBaccessToken, "/" + id + "/attachments", null, HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            //convert response into json object
                            JSONObject media = response.getJSONObject();
                            try {


                                if(media.getJSONArray("data").getJSONObject(0).getString("type").equals("video_inline")){
                                    //do nothing as this doesn't work
                                } else {
                                    //get the URL of the image
                                    url[0] = media.getJSONArray("data").getJSONObject(0).getJSONObject("media")
                                            .getJSONObject("image").getString("src");

                                    //send url back when finished
                                    Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
                                    broadcast.putExtra("id", id);
                                    broadcast.putExtra("url", url[0]);
                                    broadcast.putExtra("Network", "ExtraFacebook");
                                    sendBroadcast(broadcast);
                                }

                            } catch (Exception e) {
                                //if exception then url will be null as initialized
                                url[0] = null;
                                //send url back when finished
                                Intent broadcast = new Intent("com.example.user.sociall2.Broadcast");
                                broadcast.putExtra("id", id);
                                broadcast.putExtra("url", url[0]);
                                broadcast.putExtra("Network", "ExtraFacebook");
                                sendBroadcast(broadcast);
                            }
                        }
                    }).executeAsync();

        }
    }

    /////////////////////life cycle//////////////
    @Override
    public void onCreate() {
        messenger = new Messenger(new MyHandler());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}
