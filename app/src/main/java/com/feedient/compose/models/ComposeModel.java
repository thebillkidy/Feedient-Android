package com.feedient.compose.models;

import android.content.Context;

import com.feedient.core.adapters.FeedientRestAdapter;
import com.feedient.core.helpers.ProviderHelper;
import com.feedient.core.interfaces.FeedientService;
import com.feedient.core.interfaces.IProviderModel;
import com.feedient.core.models.json.UserProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Observable;

public class ComposeModel extends Observable {
    private Context context;
    private HashMap<String, IProviderModel> providers;
    private FeedientService feedientService;
    private final List<UserProvider> userProviders;
    private final String accessToken;

    public ComposeModel(Context context, String accessToken, List<UserProvider> userProviders) {
        this.context = context;
        this.userProviders = userProviders;
        this.accessToken = accessToken;
        this.feedientService = new FeedientRestAdapter(context).getService();
        this.providers = ProviderHelper.getProviders(context, feedientService, accessToken);
    }

    private void _triggerObservers() {
        setChanged();
        notifyObservers();
    }

    public HashMap<String, IProviderModel> getProviders() {
        return providers;
    }

    public List<UserProvider> getUserProviders() {
        return userProviders;
    }
}
