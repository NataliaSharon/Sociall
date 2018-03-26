package com.example.user.sociall2;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import retrofit.client.Header;
import retrofit.client.Response;

public class Messages extends BaseActivity {

    ListView view;

    TwitterSession twitterSession;

    boolean sortNewFirst;
    boolean twitterInboxFinished = false;
    boolean twitterOutboxFinished = false;

    //this variable notifies the adapter as to whether this is a message list or a conversation
    //if message list then we show the name of the opposite person
    //if conversation then we show name of sender
    boolean isConversation;

    //create list of objects
    //full list
    private List<MessageItem> list = new ArrayList<>();
    //partial list, only the ones which need to be viewed at that time
    private List<MessageItem> uniqueMessages = new ArrayList<>();

    //date format
    public SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    //message we are sending
    String text = "";
    String chosenUserID = "";
    String userName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateDrawer();

        setContentView(R.layout.activity_messages);

        //find view of list
        view = (ListView) findViewById(R.id.messageList);

        //create toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        //get titles and icons for navigation bar
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items); // load titles from strings.xml

        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);//load icons from strings.xml

        set(navMenuTitles, navMenuIcons);

        ////////////////////floating action button///////////////////
        findViewById(R.id.sendMessageFAB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Messages.this, SendMessage.class);
                startActivity(intent);
            }
        });

        //floating action button attributes
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.sendMessageFAB);
        fab.setSize(FloatingActionButton.SIZE_NORMAL);
        fab.setColorNormalResId(R.color.colorPrimary);
        fab.setColorPressedResId(R.color.colorPrimaryDark);
        fab.setIcon(R.drawable.ic_email_white_48dp);
        fab.setStrokeVisible(false);

        //listener for when a message is clicked on
        //meaning we need to display all messages by that person
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View clickView,
                                    int position, long id) {

                //find out who the conversation is with
                String name;
                //find which name was not the users (therefore the other person)
                if (uniqueMessages.get(position).getRecipientID().equals(String.valueOf(twitterSession.getUserId()))) {
                    name = uniqueMessages.get(position).getSender();
                    userName = uniqueMessages.get(position).getRecipient();
                    chosenUserID = uniqueMessages.get(position).getSenderID();
                } else {
                    name = uniqueMessages.get(position).getRecipient();
                    userName = uniqueMessages.get(position).getSender();
                    chosenUserID = uniqueMessages.get(position).getRecipientID();
                }

                //hide FAB
                fab.setVisibility(View.GONE);

                //display reply box and back button
                EditText text = (EditText) findViewById(R.id.enterMessage);
                text.setVisibility(View.VISIBLE);
                Button send = (Button) findViewById(R.id.sendButton);
                send.setVisibility(View.VISIBLE);
                Button goBack = (Button) findViewById(R.id.back);
                goBack.setVisibility(View.VISIBLE);

                //set list to stack from bottom (so newest messages appear at the bottom and you
                //scroll up to view the rest
                view.setStackFromBottom(true);

                sortNewFirst = true;

                uniqueMessages = new ArrayList<>();

                //find all messages between that person
                for (MessageItem message : list){
                    if (message.getRecipient().equals(name)){
                        uniqueMessages.add(message);
                    } else if (message.getSender().equals(name)){
                        uniqueMessages.add(message);
                    }
                }

                //display
                isConversation = true;
               finish();
            }
        });
    }

    private void getTwitterMessages(){

        //initialise
        list = null;
        list = new ArrayList<>();

        //make api call using interface
        //get received messages
        TwitterClient client = new TwitterClient(twitterSession);
        client.getMessagesInterface().list(new Callback<Response>() {

            @Override
            public void success(Result<Response> response) {

                try {
                    //get response stream
                    InputStream inputStream = response.response.getBody().in();
                    //convert to string
                    String data = streamToString(inputStream);

                    //create array of objects
                    JSONArray array = new JSONArray(data);

                    responseToObjects(array);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                twitterInboxFinished = true;

                //sort list if both have finished
                if(twitterInboxFinished && twitterOutboxFinished){
                    sortList();
                }
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(getApplicationContext(), "Unable to retrieve Twitter messages", Toast.LENGTH_LONG).show();

                //do not display any messages if half of them could not be retrieved
                // show error message
                ImageView errorImage = (ImageView) findViewById(R.id.errorImage);
                TextView errorText = (TextView) findViewById(R.id.errorText);
                errorText.setVisibility(View.VISIBLE);
                errorImage.setVisibility(View.VISIBLE);
            }

        });

        //get received messages
        client.getSentMessagesInterface().list(new Callback<Response>() {

            @Override
            public void success(Result<Response> response) {

                try {
                    //get response stream
                    InputStream inputStream = response.response.getBody().in();
                    //convert to string
                    String data = streamToString(inputStream);

                    //create array of objects
                    JSONArray array = new JSONArray(data);

                    responseToObjects(array);


                } catch (Exception e) {

                }

                twitterOutboxFinished = true;

                //sort list if both have finished
                if(twitterInboxFinished && twitterOutboxFinished){
                    sortList();
                }
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(getApplicationContext(), "Unable to retrieve Twitter messages", Toast.LENGTH_LONG).show();

                //do not display any messages if half of them could not be retrieved
                //show error message
                ImageView errorImage = (ImageView) findViewById(R.id.errorImage);
                TextView errorText = (TextView) findViewById(R.id.errorText);
                errorText.setVisibility(View.VISIBLE);
                errorImage.setVisibility(View.VISIBLE);
            }
        });
    }

    //takes response from twitter and creates objects
    private void responseToObjects(JSONArray array){
        try {
            //Loop the Array and decompose each object into its attributes
            for (int i = 0; i < array.length(); i++) {

                //retrieve objects
                JSONObject sender = array.getJSONObject(i).getJSONObject("sender");
                JSONObject recipient = array.getJSONObject(i).getJSONObject("recipient");

                //create new item
                MessageItem message = new MessageItem();

                //get data for each message and place into object
                message.setSender(sender.getString("name"));
                message.setSenderID(array.getJSONObject(i).getString("sender_id_str"));
                message.setTextContent(array.getJSONObject(i).getString("text"));
                message.setGivenID(array.getJSONObject(i).getString("id"));
                message.setRecipient(recipient.getString("name"));
                message.setRecipientID(recipient.getString("id"));
                message.setNetwork("Twitter");

                //time
                Date date = null;

                String timeCreated = array.getJSONObject(i).getString("created_at");
                try {
                    date = targetFormat.parse(timeCreated);
                    message.setTime(date);
                    list.add(message);
                } catch (ParseException e) {
                }
            }
        } catch (Exception e ){

        }

    }

    //extracts unique conversations
    private void sortList(){
        List<String> ids = new ArrayList<>();
        uniqueMessages = null;
        uniqueMessages = new ArrayList<>();

        //this bit is to show the single unique conversations on the message page
        //so we only need the latest message from each different person
        //and have their name, despite whoever was the last person to send a message

        //sort list
        //sort by time (need newest first so they go to bottom of list)
        Collections.sort(list, new Comparator<MessageItem>() {
            public int compare(MessageItem item1, MessageItem item2) {
                return item2.getTime().compareTo(item1.getTime());
            }
        });

        //compare
        for (MessageItem message : list) {
            //find which name was not the users' and add to the list
            if (message.getRecipientID().equals(String.valueOf(twitterSession.getUserId()))){
                //ensure we haven't already collected the name to ensure unique list
                if(!ids.contains(message.getSenderID())) {
                    ids.add(message.getSenderID());
                }
            } else {
                //ensure we haven't already collected the name to ensure unique list
                if(!ids.contains(message.getRecipientID())) {
                    ids.add(message.getRecipientID());
                }
            }
        }

        //iterate the same size as the list and add the newest messages with the name in it
        for(String id : ids) {
            loop:
            for (MessageItem message : list) {
                //find which name was not the users and add to the list
                if (message.getRecipientID().equals(id) || message.getSenderID().equals(id)){
                    uniqueMessages.add(message);
                     break loop;
                }
            }
        }

        isConversation = false;
        finish();
    }

    //uses adapter to inflate list
    public void finish() {

        if(sortNewFirst) {
            //sort by time (need oldest first so they go to top of list)
            Collections.sort(uniqueMessages, new Comparator<MessageItem>() {
                public int compare(MessageItem item1, MessageItem item2) {
                    return item1.getTime().compareTo(item2.getTime());
                }
            });
        } else {
            //sort by time (need newest first so they go to bottom of list)
            Collections.sort(uniqueMessages, new Comparator<MessageItem>() {
                public int compare(MessageItem item1, MessageItem item2) {
                    return item2.getTime().compareTo(item1.getTime());
                }
            });
        }

        //only put list through to adapter if list is not empty
        if (uniqueMessages.size() > 0) {

            //place data into list using adapter
            MessageAdapter adapter = new MessageAdapter(this, uniqueMessages, R.layout.message_list_item, isConversation);

            //place adapted list into view
            view.setAdapter(adapter);

        } else {
            //else show error message
            ImageView errorImage = (ImageView) findViewById(R.id.errorImage);
            TextView errorText = (TextView) findViewById(R.id.errorText);
            errorText.setVisibility(View.VISIBLE);
            errorImage.setVisibility(View.VISIBLE);
        }
    }

    ////// BUTTON CLICKS
    public void goBack(View v){

        //show all messages
        try {
            twitterSession= Twitter.getInstance().core.getSessionManager().getActiveSession();
        }catch (NullPointerException e) {
            twitterSession = null;
        }

        //initialise variables ready to use again
        uniqueMessages = new ArrayList<>();
        twitterOutboxFinished = false;
        twitterInboxFinished = false;

        //call messages again
        getTwitterMessages();

        //hide box and button
        EditText text = (EditText) findViewById(R.id.enterMessage);
        text.setVisibility(View.GONE);
        Button send = (Button) findViewById(R.id.sendButton);
        send.setVisibility(View.GONE);
        Button goBack = (Button) findViewById(R.id.back);
        goBack.setVisibility(View.GONE);

        //show FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.sendMessageFAB);
        fab.setVisibility(View.VISIBLE);

        //set list to stack from top again
        //scroll down to view rest
        view.setStackFromBottom(false);

        sortNewFirst = false;
    }

    //validates message and decides how to send
    public void validate(View v){

        //get the text
        TextView textMessage = (TextView) findViewById(R.id.enterMessage);
        text = textMessage.getText().toString();

        //check that message is not empty
        if(text.matches("")){
            Toast.makeText(Messages.this, "Your message cannot be empty", Toast.LENGTH_LONG).show();
            //check that a friend was selected
        } else {
            sendTwitter();

        }

    }

    //the sending methods
    private void sendTwitter() {

        if (text.length() > 140) {  //check the text is not longer than 140 characters
            Toast.makeText(Messages.this, "Twitter messages must be less than 140 characters", Toast.LENGTH_LONG).show();
        } else {

            //make api call using interface
            //get received messages
            TwitterClient client = new TwitterClient(twitterSession);
            client.getPostMessageInterface().list(chosenUserID, text, new Callback<Response>() {

                @Override
                public void success(Result<Response> response) {
                    //clear the text box
                    TextView textMessage = (TextView) findViewById(R.id.enterMessage);
                    textMessage.setText("");

                    //create message object
                    MessageItem justSent = new MessageItem();
                    justSent.setNetwork("Twitter");
                    justSent.setTextContent(text);
                    justSent.setSenderID(String.valueOf(twitterSession.getUserId()));
                    justSent.setSender(userName);

                   try{
                       //retrieve the date from the response
                       Response responseResponse = response.response;
                       List<Header> headers = responseResponse.getHeaders();
                       Header headerDate = headers.get(4);
                       String stringDate = headerDate.getValue();

                       //format of given date
                       SimpleDateFormat givenFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

                       //parse current time into own format
                       SimpleDateFormat targetFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

                       Date oldDate = givenFormat.parse(stringDate);
                       String newDateString = targetFormat.format(oldDate);
                       Date newDate = targetFormat.parse(newDateString);

                       justSent.setTime(newDate);

                        //add to list and refresh
                        uniqueMessages.add(justSent);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }

                @Override
                public void failure(TwitterException e) {
                    Toast.makeText(Messages.this, "Your message could not be sent", Toast.LENGTH_LONG).show();

                }

            });
        }
    }


    //method for turning http response into string
    //note this is the same method used in InstagramApp
    // and therefore originates from the instagram tutorials and therefore NOT MY OWN CODE
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


    ////////////////////////////LIFE CYCLE/////////////////////////////
    //check keys on resume in case user changed settings
    @Override
    protected void onResume() {
        super.onResume();

        try {
            twitterSession= Twitter.getInstance().core.getSessionManager().getActiveSession();
            getTwitterMessages();
        }catch (Exception e) {
            twitterSession = null;
        }
    }
}
