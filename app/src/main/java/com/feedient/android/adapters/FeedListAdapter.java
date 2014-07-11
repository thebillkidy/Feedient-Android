package com.feedient.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.feedient.android.R;
import com.feedient.android.models.json.schema.FeedPost;
import com.feedient.android.models.json.schema.entities.ExtendedLinkEntity;
import com.feedient.android.models.json.schema.entities.HashtagEntity;
import com.feedient.android.models.json.schema.entities.LinkEntity;
import com.feedient.android.models.json.schema.entities.MentionEntity;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FeedListAdapter extends BaseAdapter {
    // Statics
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");

    // Variables
    private final Activity activity;
    private final LayoutInflater inflater;
    private final List<FeedPost> feedItems;

    // ViewHolder
    static class ViewHolder {
        // Header
        ImageView imgThumbnailUser;
        TextView txtUserPostedBy;
        TextView txtDatePosted;

        // Content
        TextView txtMessage;

        // Entities Container
        LinearLayout containerEntities;
    }

    public FeedListAdapter(Activity activity, List<FeedPost> feedItems) {
        this.activity = activity;
        this.feedItems = feedItems;
        this.inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return feedItems.size();
    }

    @Override
    public Object getItem(int location) {
        return feedItems.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Setup the items to re-use
        // Disable re-use, constantly re-use since we got dynamic adding of entities
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_feed_item, null);

            // Configure ViewHolder
            ViewHolder viewHolder = new ViewHolder();

            viewHolder.imgThumbnailUser = (ImageView)convertView.findViewById(R.id.img_thumbnail_user);
            viewHolder.txtUserPostedBy  = (TextView)convertView.findViewById(R.id.txt_user_posted_by);
            viewHolder.txtDatePosted    = (TextView)convertView.findViewById(R.id.txt_date_posted);
            viewHolder.txtMessage       = (TextView)convertView.findViewById(R.id.txt_message);
            viewHolder.containerEntities = (LinearLayout)convertView.findViewById(R.id.layout_entities);


            // Set tag to rowView
            convertView.setTag(viewHolder);
        }

        // Fill Data
        ViewHolder holder = (ViewHolder)convertView.getTag();

        // Remove all the child views before adding new ones, this is because we re-use our view
        holder.containerEntities.removeAllViews();

        // Get our FeedItem and add data
        FeedPost item = feedItems.get(position);

        // Convert timestamp into x ago
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(item.getContent().getDateCreated().getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
        holder.txtDatePosted.setText(timeAgo);

        // If text show, else remove from view
        if (!TextUtils.isEmpty(item.getContent().getMessage())) {
            holder.txtMessage.setText(item.getContent().getMessage());
        } else {
            holder.txtMessage.setVisibility(View.GONE);
        }

        // User
        holder.txtUserPostedBy.setText(item.getUser().getName());

        // Load the image async
        Picasso.with(activity).load(item.getUser().getImageLink()).into(holder.imgThumbnailUser);

        // ENTITIES PARSING
        // Pictures
        if (item.getContent().getEntities().getPictures().size() > 0) {
            _handleEntityPictures(inflater, holder.containerEntities, item);
        }

        // Links
        if (item.getContent().getEntities().getLinks().size() > 0) {
            _handleEntityLinks(holder.txtMessage, item);
        }

        // Hashtags
        if (item.getContent().getEntities().getHashtags().size() > 0) {
            _handleEntityHashtags(holder.txtMessage, item);
        }

        // Mentions
        if (item.getContent().getEntities().getMentions().size() > 0) {
            _handleEntityMentions(holder.txtMessage, item);
        }

        // Extended Link
        if (item.getContent().getEntities().getExtendedLink() != null) {
            _handleEntityExtendedLink(inflater, holder.containerEntities, item);
        }

        // Return view
        return convertView;
    }

    private void _handleEntityMentions(TextView tv, FeedPost fp) {
        String text = tv.getText().toString();

        for (MentionEntity me : fp.getContent().getEntities().getMentions()) {
            text = text.replace("#" + me.getName(), "<a href=\"" + me.getProfileLink() + "\">#" + me.getName() + "</a>");
        }

        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(text));
        tv.setVisibility(View.VISIBLE);
    }

    private void _handleEntityHashtags(TextView tv, FeedPost fp) {
        String text = tv.getText().toString();

        for (HashtagEntity he : fp.getContent().getEntities().getHashtags()) {
            text = text.replace("#" + he.getName(), "<a href=\"" + he.getLink() + "\">#" + he.getName() + "</a>");
        }

        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(text));
        tv.setVisibility(View.VISIBLE);
    }

    private void _handleEntityLinks(TextView tv, FeedPost fp) {
        String text = tv.getText().toString();

        for (LinkEntity le : fp.getContent().getEntities().getLinks()) {
            text = text.replace(le.getDisplayUrl(), "<a href=\"" + le.getExpandedUrl() + "\">" + le.getDisplayUrl() + "</a>");
            text = text.replace(le.getShortenedUrl(), "<a href=\"" + le.getExpandedUrl() + "\">" + le.getDisplayUrl() + "</a>");
            text = text.replace(le.getExpandedUrl(), "<a href=\"" + le.getExpandedUrl() + "\">" + le.getDisplayUrl() + "</a>");
        }

        Log.e("Feedient", text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(text));
        tv.setVisibility(View.VISIBLE);
    }

    private void _handleEntityPictures(LayoutInflater inflater, LinearLayout container, FeedPost fp) {
        View entityPictureView = inflater.inflate(R.layout.entity_picture, null);

        // Init Elements
        ImageView imgEntityPicture = (ImageView)entityPictureView.findViewById(R.id.img_entity_picture);

        container.addView(entityPictureView);

        // Init Data
        Picasso.with(activity).load(fp.getContent().getEntities().getPictures().get(0).getLargePictureUrl()).into(imgEntityPicture);
    }

    private void _handleEntityExtendedLink(LayoutInflater inflater, LinearLayout container, FeedPost fp) {
        View entityView = inflater.inflate(R.layout.entity_extended_link, null);

        // Init Elements
        ImageView imgEntityExtendedLinkThumbnail = (ImageView)entityView.findViewById(R.id.img_entity_extended_link_thumbnail);
        TextView txtEntityExtendedLinkTitle = (TextView)entityView.findViewById(R.id.txt_entity_extended_link_title);
        TextView txtEntityExtendedLinkHost = (TextView)entityView.findViewById(R.id.txt_entity_extended_link_url_host);

        container.addView(entityView);

        // Init data
        ExtendedLinkEntity le = fp.getContent().getEntities().getExtendedLink();

        // If the name is not set, remove it from view
        if (le.getName().length() > 1) {
            txtEntityExtendedLinkTitle.setText(le.getName());
        } else {
            txtEntityExtendedLinkTitle.setVisibility(View.GONE);
        }

        try {
            URL url = new URL(le.getUrl());
            txtEntityExtendedLinkHost.setText(url.getHost());
        } catch (MalformedURLException e) {
            Log.e("Feedient", e.getMessage());
        }

        // Async load image
        Picasso.with(activity).load(le.getImageUrl()).into(imgEntityExtendedLinkThumbnail);
    }
}