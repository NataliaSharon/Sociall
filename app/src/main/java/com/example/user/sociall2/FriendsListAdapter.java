package com.example.user.sociall2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

//adapter for friends list
public class FriendsListAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private List<FriendsListItem> list = null;
    static public List<FriendsListItem> filteredList;

    public FriendsListAdapter(Context context, List<FriendsListItem> list) {
        this.context = context;
        this.list = list;
        this.filteredList = list;
    }

    @Override
    public int getCount() {

        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {

        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    //view holder
    protected static class ViewHolder
    {
        private TextView txtTitle;
        private ImageView fbIcon;
        private ImageView twitterIcon;
        private ImageView instagramIcon;
        private ImageView profilePhoto;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.friends_list_item, null);

            holder = new ViewHolder();
            //find views
            holder.txtTitle = (TextView) convertView.findViewById(R.id.name);
            holder.fbIcon = (ImageView) convertView.findViewById(R.id.facebookConnected);
            holder.twitterIcon = (ImageView) convertView.findViewById(R.id.twitterConnected);
            holder.instagramIcon = (ImageView) convertView.findViewById(R.id.instagramConnected);
            holder.profilePhoto = (ImageView) convertView.findViewById(R.id.profilePhoto);

            // Bind data with holder.
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

            //set text
            holder.txtTitle.setText(filteredList.get(position).getName());

            //set image colour
            //icons, am using drawable instead of resource as may have to greyscale the images
            //check which social networks are logged into, if logged in then get the feed
            if (!filteredList.get(position).getHasFacebook()) {
                //set icon to greyed out
                holder.fbIcon.setImageResource(R.drawable.ic_facebook_grey);
            } else {
                holder.fbIcon.setImageResource(R.drawable.ic_facebook);
            }

            if (!filteredList.get(position).getHasTwitter()) {
                //set icon to greyed out
                holder.twitterIcon.setImageResource(R.drawable.ic_twitter_grey);
            } else {
                holder.twitterIcon.setImageResource(R.drawable.ic_twitter);
            }

            if (!filteredList.get(position).getHasInstagram()) {
                //set icon to greyed out
                holder.instagramIcon.setImageResource(R.drawable.ic_instagram_grey);
            } else {
                holder.instagramIcon.setImageResource(R.drawable.ic_instagram);
            }

                //set profile picture
            if (filteredList.get(position).getProfileURL().equals("")) {
                holder.profilePhoto.setVisibility(View.INVISIBLE);
            } else {
                new DownloadImageTask(holder.profilePhoto).execute(filteredList.get(position).getProfileURL());
                holder.profilePhoto.setVisibility(View.VISIBLE);
            }

        return convertView;

        }


    @Override
    public Filter getFilter() {
        // return a filter that filters data based on a string constraint
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                //charsequence is the filter, convert to string
                String filterString = constraint.toString().toLowerCase();

                FilterResults results = new FilterResults();

                int count = list.size();
                final ArrayList<FriendsListItem> tempList = new ArrayList<>(count);

                String filterableString ;

                //go through each friend item and compare to the charsequence
                for (int i = 0; i < count; i++) {
                    //string to compare is the name
                    filterableString = list.get(i).getName();
                    //if it matches, then add the friend object to the list
                    if (filterableString.toLowerCase().contains(filterString)) {
                        tempList.add(list.get(i));
                    }
                }
                //alter values based on the new list
                results.values = tempList;
                results.count = tempList.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList = (ArrayList<FriendsListItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }


    ///////////////DOWNLOAD IMAGE//////////////////////////////////////////////////////
    //////////////taken from http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
    ////////////// not my own code///////////////////
    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}