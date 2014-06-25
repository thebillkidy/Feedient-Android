package com.feedient.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import com.feedient.android.R;
import com.feedient.android.adapters.ItemArrayAdapter;
import com.feedient.android.models.NewPostsProvider;
import com.feedient.android.models.NewPostsSchema;
import com.feedient.android.models.ViewAllFeeds;

import com.feedient.android.models.json.UserProvider;
import com.feedient.android.models.json.schema.FeedPost;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import java.util.Observable;
import java.util.Observer;

public class ViewAllFeedsActivity extends Activity implements Observer, OnRefreshListener {
    private ItemArrayAdapter itemArrayAdapter;
    private ViewAllFeeds viewAllFeeds;
    private PullToRefreshLayout mPullToRefreshLayout;
    private ListView mListView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_all_feeds); // @todo: When loading, set a loading icon

        viewAllFeeds = new ViewAllFeeds(this);
        viewAllFeeds.addObserver(this);
        viewAllFeeds.loadFeeds();

        mListView = (ListView)findViewById(R.id.list);
        itemArrayAdapter = new ItemArrayAdapter(this, viewAllFeeds.getFeedPosts());
        mListView.setAdapter(itemArrayAdapter);

        mPullToRefreshLayout = (PullToRefreshLayout)findViewById(R.id.swipe_container);
        ActionBarPullToRefresh.from(this)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
    }

    @Override
    public void update(Observable observable, Object o) {
        // Notify our list that there are updates
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemArrayAdapter.notifyDataSetChanged();
            }
        });
    }

    public ItemArrayAdapter getItemArrayAdapter() {
        return itemArrayAdapter;
    }

    public void setItemArrayAdapter(ItemArrayAdapter itemArrayAdapter) {
        this.itemArrayAdapter = itemArrayAdapter;
    }

    @Override
    public void onRefreshStarted(View view) {


        viewAllFeeds.loadNewPosts();
        mPullToRefreshLayout.setRefreshing(false);
    }
}