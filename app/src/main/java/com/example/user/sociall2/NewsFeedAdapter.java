package com.example.user.sociall2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.InputStream;
import java.util.List;

//adapter for friends list
public class NewsFeedAdapter extends BaseAdapter {

    private Context context;
    private List<NewsFeedItem> list;

    private int layout;

    public NewsFeedAdapter(Context context, List<NewsFeedItem> list, int layout) {
        this.context = context;
        this.list = list;
        this.layout = layout;
    }

    @Override
    public int getCount() {

        return list.size();
    }

    @Override
    public Object getItem(int position) {

        return list.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, null);
        }

        //set user name
        TextView userName = (TextView) convertView.findViewById(R.id.userName);
        userName.setText(list.get(position).getUser());

        //set network image
        ImageView networkImage = (ImageView) convertView.findViewById(R.id.networkImage);

        //depending on where the post has come from, show corresponding social network logo
        Drawable networkIcon = null;
        switch (list.get(position).getNetwork()) {

            case "Facebook":
                networkIcon = ContextCompat.getDrawable(context, R.drawable.ic_facebook);
                break;
            case "Twitter":
                networkIcon = ContextCompat.getDrawable(context, R.drawable.ic_twitter);
                break;
            case "Instagram":
                networkIcon = ContextCompat.getDrawable(context, R.drawable.ic_instagram);
                break;

        }
        networkImage.setImageDrawable(networkIcon);

        //set status
        TextView status = (TextView) convertView.findViewById(R.id.status);
        status.setText(list.get(position).getTextContent());

        //find out if needs image or video
        ImageView image = (ImageView) convertView.findViewById(R.id.image);
        final VideoView video = (VideoView) convertView.findViewById(R.id.video);

        //find screen width
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;

        //set params
        ViewGroup.LayoutParams imageParams = image.getLayoutParams();
        ViewGroup.LayoutParams videoParams = video.getLayoutParams();

        //likes and comments
        TextView likes = (TextView) convertView.findViewById(R.id.likes);
        TextView notes = (TextView) convertView.findViewById(R.id.notes);
        RelativeLayout.LayoutParams likeParams = (RelativeLayout.LayoutParams) likes.getLayoutParams();
        RelativeLayout.LayoutParams noteParams = (RelativeLayout.LayoutParams) notes.getLayoutParams();


        if(!list.get(position).getVideo()){ //is not a video
            //hide video
            video.setVisibility(View.GONE);

            //set likes and comments below image
            likeParams.addRule(RelativeLayout.BELOW, R.id.image);
            noteParams.addRule(RelativeLayout.BELOW, R.id.image);

            if (list.get(position).getPhotoContent() != null){
                new DownloadImageTask(image).execute(list.get(position).getPhotoContent());
                image.setVisibility(View.VISIBLE);

                imageParams.height = (int) (displayWidth / 1.5);
                imageParams.width = (int) (displayWidth / 1.5);
            } else {
                imageParams.height = 1;
                image.setVisibility(View.INVISIBLE);
            }
        } else if (list.get(position).getVideo()) { //else if it is a video
            image.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);

            //set likes and comments below image
            likeParams.addRule(RelativeLayout.BELOW, R.id.video);
            noteParams.addRule(RelativeLayout.BELOW, R.id.video);

            videoParams.height = (int) (displayWidth / 1.5);
            videoParams.width = (int) (displayWidth / 1.5);

            //video uri
            Uri uri =  Uri.parse(list.get(position).getPhotoContent());
            //stream
            video.setVideoURI(uri);
            video.start();

        }

        //set tags for if video is clicked on
        video.setTag(list.get(position));
        //if like is selected
        video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //play if is paused, and pause if is playing
                if (video.isPlaying()) {
                    video.pause();
                } else {
                    //video uri
                    Uri uri = Uri.parse(list.get(position).getPhotoContent());
                    //stream
                    video.setVideoURI(uri);
                    video.start();
                }
                return true;
            }
        });

        //set likes
        switch (list.get(position).getNetwork()) {

            case "Facebook":
                likes.setText(list.get(position).getLikes() + " Likes  ");
                break;
            case "Twitter":
                likes.setText(list.get(position).getLikes() + " Favorites  ");
                break;
            case "Instagram":
                likes.setText(list.get(position).getLikes() + " Likes  ");
                break;
        }

        //set tags for if likes are clicked on (for comment section only)
        likes.setTag(list.get(position));
        //if like is selected
        likes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(context.getClass().equals(ViewPost.class)) {
                    //if context is ViewPost then send broadcast back to ViewPost to run likes, else ignore
                    Intent broadcast = new Intent("com.example.user.sociall2.Comments");
                    broadcast.putExtra("Type", "LikesClicked");
                    context.sendBroadcast(broadcast);
                }
            }
        });

        //set notes
        switch (list.get(position).getNetwork()) {

            case "Facebook":
                likes.setText(list.get(position).getNotes() + " Likes  ");
            case "Instagram":
                notes.setText(list.get(position).getNotes() + " Comments");
                break;
            case "Twitter":
                notes.setText(list.get(position).getNotes() + " Re-tweets");
                break;
        }

        //set tags for viewing retweets (twitter only)
        notes.setTag(list.get(position));
        //if notes is selected
        notes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //only for twitter posts as only twitter has retweets
                if(context.getClass().equals(ViewPost.class) && list.get(position).getNetwork().equals("Twitter")) {
                    //if context is ViewPost then send broadcast back to ViewPost to run likes, else ignore
                    Intent broadcast = new Intent("com.example.user.sociall2.Comments");
                    broadcast.putExtra("Type", "RetweetsClicked");
                    context.sendBroadcast(broadcast);
                }
            }
        });

        //set time, get rid of last part
        TextView timeStamp = (TextView) convertView.findViewById(R.id.timeStamp);
        String wholeDate = list.get(position).getTime().toString();
        String[] splitDate = wholeDate.split("(?= )");
        timeStamp.setText(splitDate[0] + splitDate[2] + splitDate[1] + splitDate[5] + ", " + splitDate[3]);

        //if the context is not from the users personal profile then hide the delete post tag as they are
        //unable to delete other peoples posts
        //set tag for if delete button is selected
        TextView delete = (TextView) convertView.findViewById(R.id.delete);
        if(context.getClass().equals(Profile.class)){
            delete.setVisibility(View.VISIBLE);
        } else {
            delete.setVisibility(View.GONE);
        }

        delete.setTag(list.get(position));
        //if delete post is selected
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //send broadcast to Profile with Network and ID
                Intent broadcast = new Intent("com.example.user.sociall2.Delete");
                broadcast.putExtra("Network", list.get(position).getNetwork());
                broadcast.putExtra("id", list.get(position).getGivenID());
                context.sendBroadcast(broadcast);
            }
        });

        return convertView;
    }

    ///////////////DOWNLOAD IMAGE//////////////////////////////////////////////////////
    //////////////taken from http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
    ////////////// not my own code///////////////////
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView image;

        public DownloadImageTask(ImageView image) {
            this.image = image;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {

            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            image.setImageBitmap(result);
        }
    }
}