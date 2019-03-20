package org.tastefuljava.flipit;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<DeviceRef> {
    public DeviceListAdapter(Activity activity) {
        super(activity, 0, new ArrayList<DeviceRef>());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Activity activity = (Activity)getContext();
        DeviceRef device = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = activity.getLayoutInflater().inflate(R.layout.device_item, parent, false);
        }
        // Lookup view for data population
        TextView tvName = convertView.findViewById(R.id.nameView);
        TextView tvHome = convertView.findViewById(R.id.addressView);
        // Populate the data into the template view using the data object
        tvName.setText(device.getName());
        tvHome.setText(device.getAddress());
        // Return the completed view to render on screen
        return convertView;
    }
}
