package com.example.user.sociall2;

//object to represent an item in the newsfeed

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewsFeedItem implements Serializable, Parcelable{

    private String network; //network e.g. facebook, twitter, instagram etc
    private String textContent; //the text
    private String photoContent;  //content is the actual content of the photo if there is one
    private Date time;  //time is used to order the posts
    private String user; //user is the person who posted the item
    private String userID; //user id
    private String notes; //notes: e.g. comments, replies
    private String likes; //likes: e.g. likes, hearts, favourites
    private String givenID; //their given id
    private String twitterScreenName = ""; //twitter screen name of person who posted - need this for replies
    private boolean isVideo = false;

    public NewsFeedItem(){

    }

    public String getNetwork() { return this.network; }
    public void setNetwork (String network) { this.network = network; }

    public String getTextContent() { return this.textContent; }
    public void setTextContent (String textContent) { this.textContent = textContent; }

    public String getPhotoContent() { return this.photoContent; }
    public void setPhotoContent (String photoContent) { this.photoContent = photoContent; }

    public Date getTime() { return this.time; }
    public void setTime (Date time) { this.time = time;}

    public String getUser() { return this.user; }
    public void setUser (String user) { this.user = user; }

    public String getUserID() { return this.userID; }
    public void setUserID (String userID) { this.userID = userID; }

    public String getNotes() { return this.notes; }
    public void setNotes (String notes) { this.notes = notes; }

    public String getLikes() { return this.likes; }
    public void setLikes (String likes) { this.likes = likes;}

    public String getGivenID() {return this.givenID;}
    public void setGivenID(String givenID){this.givenID = givenID;}

    public String getTwitterScreenName() {return this.twitterScreenName;}
    public void setTwitterScreenName(String twitterScreenName){this.twitterScreenName = twitterScreenName;}

    public boolean getVideo() {return this.isVideo;}
    public void setVideo(boolean isVideo){this.isVideo = isVideo;}

    public int describeContents(){
        return 0;
    }

    public NewsFeedItem(Parcel parcel){

        this.network = parcel.readString();
        this.photoContent = parcel.readString();
        this.textContent = parcel.readString();

        String tempDate = parcel.readString();
        //convert date from string to date
        Date date = null;
        SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        try {
            date = targetFormat.parse(tempDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        this.time = date;
        this.user = parcel.readString();
        this.userID = parcel.readString();
        this.notes = parcel.readString();
        this.likes = parcel.readString();
        this.givenID = parcel.readString();
        this.twitterScreenName = parcel.readString();
        this.isVideo = Boolean.parseBoolean(parcel.readString());
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {

        parcel.writeString(this.network);
        parcel.writeString(this.photoContent);
        parcel.writeString(this.textContent);
        parcel.writeString(String.valueOf(this.time));
        parcel.writeString(this.user);
        parcel.writeString(this.userID);
        parcel.writeString(this.notes);
        parcel.writeString(this.likes);
        parcel.writeString(this.givenID);
        parcel.writeString(this.twitterScreenName);
        parcel.writeString(String.valueOf(this.isVideo));

    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public NewsFeedItem createFromParcel(Parcel parcel) {
            return new NewsFeedItem(parcel);
        }

        public NewsFeedItem[] newArray(int size) {
            return new NewsFeedItem[size];
        }
    };
}