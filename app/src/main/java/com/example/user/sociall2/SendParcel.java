package com.example.user.sociall2;


import android.os.Parcel;
import android.os.Parcelable;

public class SendParcel implements Parcelable{

    public String id;
    public int extraInfo;
    public String text;
    public String twitterScreenName;

    public SendParcel(Parcel parcel)
    {
        readFromParcel(parcel);
    }

    public SendParcel() {

    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(this.id);
        out.writeInt(this.extraInfo);
        out.writeString(this.text);
        out.writeString(this.twitterScreenName);
    }

    private void readFromParcel(Parcel parcel)
    {
        this.id = parcel.readString();
        this.extraInfo = parcel.readInt();
        this.text = parcel.readString();
        this.twitterScreenName = parcel.readString();
    }

    public static final Parcelable.Creator<SendParcel> CREATOR = new Parcelable.Creator<SendParcel>() {

        public SendParcel createFromParcel(Parcel parcel) {
            return new SendParcel(parcel);
        }

        public SendParcel[] newArray(int size) {
            return new SendParcel[size];
        }

    };



}

