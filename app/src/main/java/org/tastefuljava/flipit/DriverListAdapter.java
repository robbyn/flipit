package org.tastefuljava.flipit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class DriverListAdapter extends ArrayAdapter<DeviceRef> {
    public DriverListAdapter(@androidx.annotation.NonNull Context context, int resource) {
        super(context, resource);
    }
}
