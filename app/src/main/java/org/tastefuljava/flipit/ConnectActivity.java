package org.tastefuljava.flipit;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = ConnectActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    private ListView deviceListView;
    private DeviceListAdapter deviceListAdapter;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        deviceListView = findViewById(R.id.deviceListView);
        deviceListAdapter = new DeviceListAdapter(this);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                DeviceRef device = deviceListAdapter.getItem(position);
                if (device != null) {
                    deviceClicked(device);
                }
            }
        });
        startScan();
    }

    private void deviceClicked(DeviceRef device) {
        Log.i(TAG, "Item clicked: " + device);
        Intent intent = new Intent(getString(R.string.ACTION_CONNECT));
        intent.putExtra("deviceAddress", device.getAddress());
        sendBroadcast(intent);
        finish();
    }

    private void startScan() {
        Log.i(TAG, "Connect");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "BLE is available, let's continue...");
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        Log.i(TAG, "got a bluetooth adapter");

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
            });
            builder.show();
            return;
        }

        final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        handler.postDelayed(new Runnable() {
            public void run() {
                Log.i(TAG, "stop scanning");
                scanner.stopScan(leScanCallback);
            }
        }, 10000);
        deviceListAdapter.clear();
        Log.i(TAG, "start scanning...");
        scanner.startScan(leScanCallback);
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            DeviceRef dev = new DeviceRef(result.getDevice().getName(),
                    result.getDevice().getAddress());
            int pos = deviceListAdapter.getPosition(dev);
            if (pos < 0) {
                deviceListAdapter.add(dev);
            }
        }
    };

}
