package com.example.user.sociall2;

//object for an item in the friends drawer

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class FriendsListItem implements Serializable, Parcelable{

    private String name;
    private String facebookID;
    private String twitterID;
    private String instagramID;
    private boolean hasFacebook;
    private boolean hasTwitter;
    private boolean hasInstagram;
    private String profileURL = "";

    public FriendsListItem(){
    }

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name;}

    public boolean getHasFacebook () { return this.hasFacebook; }
    public void setHasFacebook (boolean hasFacebook) { this.hasFacebook = hasFacebook;}

    public boolean getHasTwitter () { return this.hasTwitter; }
    public void setHasTwitter (boolean hasTwitter) { this.hasTwitter = hasTwitter;}

    public boolean getHasInstagram () { return this.hasInstagram; }
    public void setHasInstagram (boolean hasInstagram) { this.hasInstagram = hasInstagram;}

    public String getFacebookID () { return this.facebookID; }
    public void setFacebookID (String facebookID) { this.facebookID = facebookID;}

    public String getTwitterID () { return this.twitterID; }
    public void setTwitterID (String twitterID) { this.twitterID = twitterID;}

    public String getInstagramID () { return this.instagramID; }
    public void setInstagramID (String instagramID) { this.instagramID = instagramID;}

    public String getProfileURL() {return this.profileURL;}
    public void setProfileURL(String profileURL) {this.profileURL = profileURL;}

    public int describeContents(){
        return 0;
    }

    public FriendsListItem(Parcel parcel){

        this.name = parcel.readString();
        this.hasFacebook = Boolean.parseBoolean(parcel.readString());
        this.facebookID = parcel.readString();
        this.hasTwitter = Boolean.parseBoolean(parcel.readString());
        this.twitterID = parcel.readString();
        this.hasInstagram = Boolean.parseBoolean(parcel.readString());
        this.instagramID = parcel.readString();
        this.profileURL = parcel.readString();
    }


    @Override
    public void writeToParcel(Parcel parcel, int flags) {

        parcel.writeString(this.name);
        parcel.writeString(String.valueOf(this.hasFacebook));
        parcel.writeString(this.facebookID);
        parcel.writeString(String.valueOf(this.hasTwitter));
        parcel.writeString(this.twitterID);
        parcel.writeString(String.valueOf(this.hasInstagram));
        parcel.writeString(this.instagramID);
        parcel.writeString(this.profileURL);

    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public FriendsListItem createFromParcel(Parcel parcel) {
            return new FriendsListItem(parcel);
        }

        public FriendsListItem[] newArray(int size) {
            return new FriendsListItem[size];
        }
    };

}
