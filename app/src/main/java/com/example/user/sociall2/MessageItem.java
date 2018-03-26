package com.example.user.sociall2;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageItem implements Serializable, Parcelable {

    private String network; //network e.g. facebook, twitter, instagram etc
    private String textContent; //the text
    private Date time;  //time is used to order the posts
    private String sender; //user is the person who the message is from
    private String senderID; //and their user id
    private String recipient;
    private String recipientID;
    private String givenID; //id of the message

    public MessageItem(){

    }

    public String getNetwork() { return this.network; }
    public void setNetwork (String network) { this.network = network; }

    public String getTextContent() { return this.textContent; }
    public void setTextContent (String textContent) { this.textContent = textContent; }

    public Date getTime() { return this.time; }
    public void setTime (Date time) { this.time = time;}

    public String getSender() { return this.sender; }
    public void setSender (String sender) { this.sender = sender; }

    public String getSenderID() { return this.senderID; }
    public void setSenderID (String senderID) { this.senderID = senderID; }

    public String getRecipient() { return this.recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getRecipientID() { return this.recipientID; }
    public void setRecipientID(String recipientID) { this.recipientID = recipientID; }

    public String getGivenID() {return this.givenID;}
    public void setGivenID(String givenID){this.givenID = givenID;}

    public int describeContents(){
        return 0;
    }

    public MessageItem(Parcel parcel){

        this.network = parcel.readString();
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
        this.sender = parcel.readString();
        this.senderID = parcel.readString();
        this.recipient = parcel.readString();
        this.recipientID = parcel.readString();
        this.givenID = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {

        parcel.writeString(this.network);
        parcel.writeString(this.textContent);
        parcel.writeString(String.valueOf(this.time));
        parcel.writeString(this.sender);
        parcel.writeString(this.senderID);
        parcel.writeString(this.recipient);
        parcel.writeString(this.recipientID);
        parcel.writeString(this.givenID);

    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public MessageItem createFromParcel(Parcel parcel) {
            return new MessageItem(parcel);
        }

        public MessageItem[] newArray(int size) {
            return new MessageItem[size];
        }
    };
}