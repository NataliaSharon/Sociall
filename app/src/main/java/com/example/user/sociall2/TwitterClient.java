package com.example.user.sociall2;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterSession;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public class TwitterClient extends TwitterApiClient {
    public TwitterClient(TwitterSession session) {
        super(session);
    }

    public Friends getFriendsInterface() {
        return getService(Friends.class);
    }

    public Retweets getRetweetsInterface() {
        return getService(Retweets.class);
    }

    public PostFavourite getPostFavouriteInterface() {
        return getService(PostFavourite.class);
    }

    public PostReply getPostReplyInterface() {
        return getService(PostReply.class);
    }

    public TwitterMessages getMessagesInterface() { return getService(TwitterMessages.class);}

    public TwitterSentMessages getSentMessagesInterface() { return getService(TwitterSentMessages.class);}

    public TwitterPostMessage getPostMessageInterface() { return getService(TwitterPostMessage.class);}
}


interface Friends {
    @GET("/1.1/friends/list.json")
    void list(@Query("user_id") long id, Callback<Response> cb);
}

interface Retweets {
    @GET("/1.1/statuses/retweets/{id}.json")
    void list(@Path("id") long objectID, Callback<Response> cb);
}

interface PostFavourite{
    @POST("/1.1/favorites/create.json")
    void list(@Query("id") long objectID, Callback<Response> cb);
}

interface PostReply {
    @POST("/1.1/statuses/update.json")
    void list(@Query("status") String text, @Query("in_reply_to_status_id") long objectID, Callback<Response> cb);
}

interface TwitterMessages{
    @GET("/1.1/direct_messages.json")
    void list(Callback<Response> cb);
}

interface TwitterSentMessages{
    @GET("/1.1/direct_messages/sent.json")
    void list(Callback<Response> cb);
}

interface TwitterPostMessage{
    @POST("/1.1/direct_messages/new.json")
    void list(@Query("user_id") String userID, @Query("text") String text, Callback<Response> cb);
}
