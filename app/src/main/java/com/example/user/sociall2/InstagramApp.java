package com.example.user.sociall2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/////////////////////////PLEAES NOTE
////////////////////////I USED THE FOLLOWING SOURCES FOR THE INSTAGRAM APP AND DIALOG CLASSES
////////////////////////http://grishma102.blogspot.co.uk/2014/01/instagram-api-integration-in-android.html
////////////////////////http://stackoverflow.com/questions/20290309/how-to-get-instagram-data-from-a-user-on-android?rq=1

public class InstagramApp {

    private InstagramDialog dialog;
    private OAuthAuthenticationListener listener;
    private ProgressDialog progress;
    private String auth_url;

    private String clientID;
    private String clientSecret;

    public Context context;

    //error codes
    private static int WHAT_ERROR = 1;
    private static int WHAT_FETCH_INFO = 2;

    public static String callbackURL = "";
    private static final String AUTH_URL = "https://api.instagram.com/oauth/authorize/";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";

    public InstagramApp(Context context, String clientId, String clientSecret,
                        String callbackUrl) {

        this.clientID = clientId;
        this.clientSecret = clientSecret;
        this.callbackURL = callbackUrl;
        this.context = context;

        auth_url = AUTH_URL
                + "?client_id="
                + clientId
                + "&redirect_uri="
                + callbackUrl
                + "&response_type=code&display=touch&scope=likes+comments+relationships+public_content+follower_list";

        //the above scope= are the permission requests

        InstagramDialog.OAuthDialogListener listener = new InstagramDialog.OAuthDialogListener() {
            @Override
            public void onComplete(String code) {
                //if code was recieved from instagram, then get access token
                getAccessToken(code);
            }

            @Override
            public void onError(String error) {
                InstagramApp.this.listener.onFail("Log In failed");
            }
        };

        dialog = new InstagramDialog(context, auth_url, listener);
        progress = new ProgressDialog(context);
        progress.setCancelable(false);
    }

    private void getAccessToken(final String code) {

        new Thread() {
            @Override
            public void run() {
                int what = WHAT_FETCH_INFO;
                try {
                    //send url request for access token
                    URL url = new URL(TOKEN_URL);
                    HttpURLConnection urlConnection = (HttpURLConnection) url
                            .openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);

                    OutputStreamWriter writer = new OutputStreamWriter(
                            urlConnection.getOutputStream());
                    writer.write("client_id=" + clientID + "&client_secret="
                            + clientSecret + "&grant_type=authorization_code"
                            + "&redirect_uri=" + callbackURL + "&code=" + code);
                    writer.flush();

                    //get response and put into string, then into a json object
                    String response = streamToString(urlConnection.getInputStream());
                    JSONObject instagramObject = (JSONObject) new JSONTokener(response)
                            .nextValue();

                    //Save the user object in shared preferences
                    //this way I can check if the user is logged in (if there is an object) even if the app restarted
                    //when the user logs out, delete the object

                    //store object using sharedpreference and gson
                    SharedPreferences sharedPreferences = context.getSharedPreferences("instagramObject", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    //gson translates an object into gson object, which can be put into sharedPreferences
                    Gson gson = new Gson();
                    String json = gson.toJson(instagramObject);
                    editor.putString("session", json);
                    editor.commit();

                } catch (Exception e) {
                    what = WHAT_ERROR;
                    e.printStackTrace();
                }

                mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
            }
        }.start();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == WHAT_ERROR) {
                progress.dismiss();
                listener.onFail("Unable to Log In");
            } else {
                progress.dismiss();
                listener.onSuccess();
            }
        }
    };

    public void setListener(OAuthAuthenticationListener listener) {
        this.listener = listener;
    }


    public void authorize() {
        //start InstagramDialog (creates the instagram log in box and shows loading)
        dialog.show();
    }

    //appends data into a single string
    private String streamToString(InputStream input) throws IOException {
        String string = "";

        if (input != null) {
            StringBuilder builder = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input));

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                reader.close();
            } finally {
                input.close();
            }

            string = builder.toString();
        }

        return string;
    }


    public interface OAuthAuthenticationListener {
        public abstract void onSuccess();

        public abstract void onFail(String error);
    }
}
