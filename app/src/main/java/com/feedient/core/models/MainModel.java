package com.feedient.core.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.feedient.core.R;
import com.feedient.core.adapters.FeedientRestAdapter;
import com.feedient.core.data.AssetsPropertyReader;
import com.feedient.core.helpers.ProviderHelper;
import com.feedient.core.api.FeedientService;
import com.feedient.core.interfaces.IAddProviderCallback;
import com.feedient.core.interfaces.IProviderModel;
import com.feedient.core.models.json.Account;
import com.feedient.core.models.json.UserProvider;
import com.feedient.core.models.json.feed.BulkPagination;
import com.feedient.core.models.json.feed.FeedPostList;
import com.feedient.core.models.json.request.NewFeedPost;
import com.feedient.core.models.json.request.OldFeedPost;
import com.feedient.core.models.json.response.Logout;
import com.feedient.core.models.json.response.RemoveUserProvider;
import com.feedient.core.models.json.schema.FeedPost;

import org.json.JSONArray;
import org.json.JSONException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.functions.Action1;

import java.util.*;

public class MainModel extends Observable {
    private Context context;

    private final long timerInterval;
    private int newNotifications;
    private List<FeedPost> feedPosts;
    private Map<String, BulkPagination> paginationKeys; // <userProviderId, since>
    private List<UserProvider> userProviders;
    private HashMap<String, IProviderModel> providers;
    private Account account;

    private AssetsPropertyReader assetsPropertyReader;
    private Properties properties;
    private Properties configProperties;
    private SharedPreferences sharedPreferences;
    private FeedientService feedientService;
    private String accessToken;

    private List<NavDrawerItem> navDrawerItems;

    private boolean isRefreshing;
    private boolean isLoadingOlderPosts;

    public MainModel(Context context) {
        this.context = context;

        feedPosts = new ArrayList<FeedPost>();
        paginationKeys = new HashMap<String, BulkPagination>();
        userProviders = new ArrayList<UserProvider>();
        account = new Account();
        newNotifications = 0;

        assetsPropertyReader = new AssetsPropertyReader(context);
        properties = assetsPropertyReader.getProperties("shared_preferences.properties");
        configProperties = assetsPropertyReader.getProperties("config.properties");
        sharedPreferences = context.getSharedPreferences(properties.getProperty("prefs.name"), Context.MODE_PRIVATE);
        feedientService = new FeedientRestAdapter(context).getService();

        accessToken = sharedPreferences.getString(properties.getProperty("prefs.key.token"), "");
        isRefreshing = false;

        timerInterval = Long.parseLong(configProperties.getProperty("auto_update_interval"));

        this.providers = ProviderHelper.getProviders(context, feedientService, accessToken);

        initMenuItems();
    }

    private void initMenuItems() {
        String[] navMenuTitles = context.getResources().getStringArray(R.array.nav_drawer_items);
        navDrawerItems = new ArrayList<NavDrawerItem>();

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], "{fa-plus}")); // Add provider
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], "{fa-sign-out}")); // Sign Out
    }

    public void loadUser() {
        feedientService.getAccount(accessToken, new Callback<FeedientService.LoginResponse>() {
            @Override
            public void success(FeedientService.LoginResponse loginResponse, Response response) {
                MainModel.this.account.setId(account.getId());
                MainModel.this.account.setEmail(account.getEmail());
                MainModel.this.account.setLanguage(account.getLanguage());
                MainModel.this.account.setRole(account.getRole());

                _triggerObservers();
            }

            @Override
            public void failure(RetrofitError retrofitError) {

            }
        });
    }

    /**
     * Loads the last X posts of the user
     */
    public void loadFeeds() {
        feedientService.getProviders(accessToken)
            .subscribe(new Action1<List<UserProvider>>() {
                @Override
                public void call(List<UserProvider> userProviders) {
                    JSONArray userProviderIds = new JSONArray();

                    MainModel.this.userProviders.clear();

                    for (UserProvider up : userProviders) {
                        userProviderIds.put(up.getId());
                        MainModel.this.userProviders.add(up);
                    }

                    _triggerObservers();

                    // Get all the feeds
                    feedientService.getFeeds(accessToken, userProviderIds)
                            .subscribe(new Action1<FeedPostList>() {
                                @Override
                                public void call(FeedPostList feedPostList) {
                                    MainModel.this.feedPosts.clear();
                                    MainModel.this.paginationKeys.clear();

                                    // Set the posts
                                    for (FeedPost fp : feedPostList.getFeedPosts()) {
                                        MainModel.this.feedPosts.add(fp);
                                    }

                                    // Set the paginations
                                    for (BulkPagination bp : feedPostList.getPaginations()) {
                                        paginationKeys.put(bp.getProviderId(), bp);
                                    }

                                    // We got list items added, trigger observers
                                    _triggerObservers();
                                }
                            });
                }
            });
    }

    /**
     * Loads the new posts of the user
     */
    public void loadNewPosts() {
        isRefreshing = true;
        _triggerObservers();

        JSONArray newFeedPosts = new JSONArray();

        // Get the last posts for every provider, and add the since key
        for (UserProvider up : userProviders) {
            String userProviderId = up.getId();
            String since = paginationKeys.get(up.getId()).getSince();

            try {
                newFeedPosts.put(new NewFeedPost(userProviderId, since));
            } catch (JSONException e) {
                Log.e("Feedient", e.getMessage());
            }
        }

        feedientService.getNewerPosts(accessToken, newFeedPosts)
            .subscribe(new Action1<FeedPostList>() {
                @Override
                public void call(FeedPostList feedPostList) {
                    Log.e("Feedient", "New posts: " + feedPostList.getFeedPosts().size());
                    // Add posts to the beginning (Start at the end of the array for ordering)
                    for (int i = feedPostList.getFeedPosts().size() - 1; i >= 0; i--) {
                        FeedPost fp = feedPostList.getFeedPosts().get(i);
                        MainModel.this.feedPosts.add(0, fp);
                    }

                    // Set the new paginations
                    List<BulkPagination> paginations = feedPostList.getPaginations();
                    for (int i = 0; i < paginations.size(); i++) {
                        BulkPagination bp = paginations.get(i);

                        BulkPagination old = paginationKeys.get(bp.getProviderId());
                        bp.setUntil(old.getUntil()); // Set the until key back

                        paginationKeys.put(bp.getProviderId(), bp);
                    }

                    // We got list items added, trigger observers
                    isRefreshing = false;
                    _triggerObservers();
                }
            });
    }

    public void loadOlderPosts() {
        isLoadingOlderPosts = true;
        _triggerObservers();

        JSONArray olderFeedPosts = new JSONArray();

        // Get the last posts for every provider, and add the since key
        for (UserProvider up : userProviders) {
            String userProviderId = up.getId();
            String until = paginationKeys.get(up.getId()).getUntil();

            try {
                olderFeedPosts.put(new OldFeedPost(userProviderId, until));
            } catch (JSONException e) {
                Log.e("Feedient", e.getMessage());
            }
        }

        Log.e("Feedient", "LOADING OLDER POSTS");
        feedientService.getOlderPosts(accessToken, olderFeedPosts)
                .subscribe(new Action1<FeedPostList>() {
                    @Override
                    public void call(FeedPostList feedPostList) {
                        Log.e("Feedient", "Older posts: " + feedPostList.getFeedPosts().size());
                        // Add posts to the end
                        for (FeedPost fp : feedPostList.getFeedPosts()) {
                            MainModel.this.feedPosts.add(fp);
                        }

                        // Set the new paginations
                        List<BulkPagination> paginations = feedPostList.getPaginations();
                        for (int i = 0; i < paginations.size(); i++) {
                            BulkPagination bp = paginations.get(i);

                            BulkPagination old = paginationKeys.get(bp.getProviderId());
                            bp.setSince(old.getSince()); // Set the since key back

                            paginationKeys.put(bp.getProviderId(), bp);
                        }

                        // We got list items added, trigger observers
                        isLoadingOlderPosts = false;
                        _triggerObservers();
                    }
                });

    }

    public void initAutoUpdateTimer() {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadNewPosts();
                h.postDelayed(this, timerInterval);
            }
        }, timerInterval);
    }

    public void logout() {
        feedientService.logout(this.accessToken)
            .subscribe(new Action1<Logout>() {
                @Override
                public void call(Logout logout) {
                    _removeAccessToken();
                    _triggerObservers();
                }
            });
    }

    private void _removeAccessToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(properties.getProperty("prefs.key.token"));
        editor.apply();
        accessToken = "";
    }

    private void _triggerObservers() {
        setChanged();
        notifyObservers();
    }

    public List<FeedPost> getFeedPosts() {
        return feedPosts;
    }

    public List<UserProvider> getUserProviders() {
        return userProviders;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public void setRefreshing(boolean isRefreshing) {
        this.isRefreshing = isRefreshing;
    }

    public Account getAccount() {
        return account;
    }

    public void removeUserProvider(final UserProvider up) {
        feedientService.removeUserProvider(accessToken, up.getId())
            .subscribe(new Action1<RemoveUserProvider>() {
                @Override
                public void call(RemoveUserProvider removeUserProvider) {
                    // Remove userProvider from list
                    userProviders.remove(up);
                    _triggerObservers();
                    loadFeeds();
                }
            });
    }

    public void addUserProvider(IProviderModel provider) {
        provider.popup(accessToken, new IAddProviderCallback() {
            @Override
            public void onSuccess(List<UserProvider> addedUserProviders) {
                for (UserProvider up : addedUserProviders) {
                    userProviders.add(up);
                }

                _triggerObservers();

                // On add of a userProvider, refresh the feeds
                loadFeeds();
            }
        });
    }

    public FeedientService getFeedientService() {
        return feedientService;
    }

    public HashMap<String, IProviderModel> getProviders() {
        return providers;
    }

    public String getAccessToken() {
        return sharedPreferences.getString(properties.getProperty("prefs.key.token"), "");
    }

    public List<NavDrawerItem> getNavDrawerItems() {
        return navDrawerItems;
    }

    public boolean isLoadingOlderPosts() {
        return isLoadingOlderPosts;
    }
}
