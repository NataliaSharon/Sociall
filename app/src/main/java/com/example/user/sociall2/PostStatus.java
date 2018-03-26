package com.example.user.sociall2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.share.ShareApi;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;

import io.fabric.sdk.android.Fabric;

public class PostStatus extends BaseActivity {

    //keys and tokens
    private String TWITTER_KEY;
    private String TWITTER_SECRET;
    private TwitterSession twitterSession;

    private AccessToken FBaccessToken;

    private String instagramSession;

    //image and text views
    private TextView textUpdate;
    private ImageView imageUpdate;
    private Boolean imageSelected = false;
    private Bitmap image = null;
    private String picturePath;

    String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();

        //initialise facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

        //initialise twitter keys and fabric
        TWITTER_KEY = getString(R.string.twitter_key);
        TWITTER_SECRET = getString(R.string.twitter_secret);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new TwitterCore(authConfig), new TweetComposer());

        setContentView(R.layout.activity_post_status);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles, navMenuIcons);

        //initialise views
        textUpdate = (TextView) findViewById(R.id.textUpdate);

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

    }

    public void onSubmit(View v){

        //checkboxes
        CheckBox checkboxFB = (CheckBox) findViewById(R.id.checkBoxFB);
        CheckBox checkboxTwitter = (CheckBox) findViewById(R.id.checkBoxTwitter);
        CheckBox checkboxInstagram = (CheckBox) findViewById(R.id.checkBoxInstagram);

        //booleans
        boolean facebookValidated;
        boolean twitterValidated;
        boolean instagramValidated;
        boolean facebookPosted = true;
        boolean twitterPosted = true;
        boolean instagramPosted = true;
        boolean networkSelected = true;

        //user input
        text = textUpdate.getText().toString();

        //if no social networks have been selected, tell user to select one
        if(!checkboxFB.isChecked() && !checkboxTwitter.isChecked() && !checkboxInstagram.isChecked()){
            Toast.makeText(PostStatus.this, "Please select a social network", Toast.LENGTH_LONG).show();
            networkSelected = false;
        }

        //ensure text OR photo is provided
        if (text.matches("") && !imageSelected){
            Toast.makeText(PostStatus.this, "You must write an update or select a photo", Toast.LENGTH_LONG).show();
        } else {
            //check which boxes were selected
            //call corresponding validation methods
            if (checkboxFB.isChecked())
                facebookValidated = validateFacebook();
            else
                facebookValidated = false;

            if (checkboxTwitter.isChecked())
                twitterValidated = validateTwitter();
            else
                twitterValidated = false;

            if (checkboxInstagram.isChecked())
                instagramValidated = validateInstagram();
            else
                instagramValidated = false;

            //post to those social networks which validated
            if(facebookValidated)
                facebookPosted = postFacebook();

            if(twitterValidated)
                twitterPosted = postTwitter();

            if(instagramValidated)
                instagramPosted =  postInstagram();

            //if true is returned from the post methods then close
            if(facebookPosted && twitterPosted && instagramPosted && networkSelected){
                this.finish();
            }
        }
    }

    public void getPhoto(View v){
        //open external media
        Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 2);
    }


    //used this tutorial
    //http://www.c-sharpcorner.com/UploadFile/e14021/capture-image-from-camera-and-selecting-image-from-gallery-o/
    //for getting phone images
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        imageUpdate = (ImageView) findViewById(R.id.imageView);

        if (resultCode == RESULT_OK) {
          if (requestCode == 2) {
              //get data of selected image
              Uri selectedImage = data.getData();
              //find path
              String[] filePath = { MediaStore.Images.Media.DATA };
              //use a cursor to navigate to the image using the path
              Cursor cursor = getContentResolver().query(selectedImage, filePath, null, null, null);
              cursor.moveToFirst();
              int columnIndex = cursor.getColumnIndex(filePath[0]);
              picturePath = cursor.getString(columnIndex);
              cursor.close();

              image = (BitmapFactory.decodeFile(picturePath));

              imageUpdate.setImageBitmap(image);
              imageUpdate.setVisibility(View.VISIBLE);
              imageSelected = true;

            } else {
              Toast.makeText(PostStatus.this, "Could not load image", Toast.LENGTH_LONG).show();
          }
        }
    }

    private boolean validateFacebook(){
        //check that user is connected to facebook
        if (FBaccessToken == null) {
            Toast.makeText(PostStatus.this, "You are not connected to Facebook", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean postFacebook(){

        if (imageSelected) {

            //posts photo to facebook using facebook API
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(image)
                    .setCaption(text)
                    .build();

            SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhoto(photo)
                    .build();

            ShareApi.share(content, null);

        } else {

            //use graph API to make API call and post status
            Bundle params = new Bundle();
            params.putString("message", text);
            new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/feed", params, HttpMethod.POST,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            Toast.makeText(PostStatus.this, "Status posted successfully to Facebook!", Toast.LENGTH_LONG).show();
                        }
                    }
            ).executeAsync();

        }

        return true;
    }


    private boolean validateTwitter(){
        //check user is connceted to twitter
        if (twitterSession == null) {
            Toast.makeText(PostStatus.this, "You are not connected to Twitter", Toast.LENGTH_LONG).show();
            return false;
        } else if (text.length() > 140){  //check the text is not longer than 140 characters
            Toast.makeText(PostStatus.this, "Twitter posts must be less than 140 characters", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean postTwitter(){

        TweetComposer.Builder builder = null;

        //get URI of image if exists
        if(imageSelected) {
            File image = new File(picturePath);
            Uri uri = Uri.fromFile(image);

            //compose and send tweet using builder
            builder = new TweetComposer.Builder(this)
                    .text(text)
                    .image(uri);
        } else {
            builder = new TweetComposer.Builder(this)
                    .text(text);
        }

        builder.show();
        //If the Twitter app is not installed, the intent will launch twitter.com in a browser, but the specified image will be ignored.

        return true;
    }

    private boolean validateInstagram(){
        //check user is connected to instagram
        if (instagramSession == "") {
            Toast.makeText(PostStatus.this, "You are not connected to Instagram", Toast.LENGTH_LONG).show();
            return false;
        } else if (!imageSelected){
            //check an image is attatched
            Toast.makeText(PostStatus.this, "You must select an image for Instagram", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean postInstagram(){

        //get URI of image
        File image = new File(picturePath);
        Uri uri = Uri.fromFile(image);

        //create intent for opening instagram
        //first check if instagram is installed
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (intent != null)
        {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            //where to make intent to
            shareIntent.setPackage("com.instagram.android");
            //add the image and text
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            //add type
            shareIntent.setType("image/jpeg");
            //send intent
            startActivity(shareIntent);
        }
        else
        {
            // bring user to the market to download the app
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //intent to download app
            intent.setData(Uri.parse("market://details?id="+"com.instagram.android"));
            startActivity(intent);
        }

        return true;
    }

}
