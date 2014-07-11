package com.feedient.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.feedient.android.R;
import com.feedient.android.interfaces.IProviderModel;
import com.feedient.android.models.json.UserProvider;
import com.joanzapata.android.iconify.Iconify;

import java.util.HashMap;
import java.util.List;

public class DrawerItemAdapter extends ArrayAdapter<UserProvider> {
    private final Context context;
    private final List<UserProvider> userProviders;
    private final HashMap<String, IProviderModel> providers;
    private final LayoutInflater layoutInflater;

    // ViewHolder pattern (http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder)
    public static class ViewHolderItem {
        UserProvider userProvider;
        TextView txtProviderIcon;
        TextView txtProviderUserName;
        ImageButton imgBtnRemoveProvider;
    }

    public DrawerItemAdapter(Context context, List<UserProvider> userProviders, HashMap<String, IProviderModel> providers) {
        super(context, R.layout.view_main, userProviders);

        this.context = context;
        this.providers = providers;
        this.userProviders = userProviders;
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolderItem viewHolder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.drawer_list_item, null);

            // Set up the ViewHolder
            viewHolder = new ViewHolderItem();
            viewHolder.userProvider = userProviders.get(position);
            viewHolder.txtProviderUserName = (TextView)convertView.findViewById(R.id.txt_provider_user_name);
            viewHolder.txtProviderIcon = (TextView)convertView.findViewById(R.id.txt_provider_icon);
            viewHolder.imgBtnRemoveProvider = (ImageButton)convertView.findViewById(R.id.img_btn_provider_remove);
            viewHolder.imgBtnRemoveProvider.setTag(viewHolder.userProvider);

            // Store the holder
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderItem)convertView.getTag();
        }

        if (viewHolder.userProvider != null) {
            if (viewHolder.userProvider.getProviderAccount() != null) {
                viewHolder.txtProviderIcon.setText("{" + providers.get(viewHolder.userProvider.getProviderAccount().getName().toLowerCase()).getIcon() + "}");
            }

            if (viewHolder.userProvider.getProviderAccount().getUsername() != null) {
                viewHolder.txtProviderUserName.setText(viewHolder.userProvider.getProviderAccount().getUsername());
            }

            if (viewHolder.userProvider.getProviderAccount().getFullName() != null) {
                viewHolder.txtProviderUserName.setText(viewHolder.userProvider.getProviderAccount().getFullName());
            }

            Iconify.addIcons(viewHolder.txtProviderIcon);
        }

        return convertView;
    }
}
