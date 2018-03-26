package com.example.user.sociall2;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentItem implements Parcelable {

    private String textContent;  //text
    private Date time;  //time is used to order the posts
    private String user; //user is the person who posted the item
    private String userID;
    private String network;

    public CommentItem(){

    }

    public String getTextContent() { return this.textContent; }
    public void setTextContent (String textContent) { this.textContent = textContent; }

    public Date getTime() { return this.time; }
    public void setTime (Date time) { this.time = time;}

    public String getUser() { return this.user; }
    public void setUser (String user) { this.user = user; }

    public String getUserID(){return this.userID;}
    public void setUserID(String userID){this.userID = userID;}

    public String getNetwork(){return this.network;}
    public void setNetwork(String network){this.network = network;}


    public int describeContents(){
        return 0;
    }

    public CommentItem(Parcel parcel){

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
        this.network = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {

        parcel.writeString(this.textContent);
        parcel.writeString(String.valueOf(this.time));
        parcel.writeString(this.user);
        parcel.writeString(this.userID);
        parcel.writeString(this.network);
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public CommentItem createFromParcel(Parcel parcel) {
            return new CommentItem(parcel);
        }

        public CommentItem[] newArray(int size) {
            return new CommentItem[size];
        }
    };
}
