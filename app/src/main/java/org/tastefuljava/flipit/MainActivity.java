package org.tastefuljava.flipit;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID TIMEFLIP_ID
            = UUID.fromString("F1196F50-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID FACET_CHARACTERISTIC
            = UUID.fromString("F1196F52-71A4-11E6-BDF4-0800200C9A66");

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler = new Handler();
    private ListView deviceListView;
    private ArrayAdapter<DeviceRef> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {connect(view);    }
        });
        deviceListView = findViewById(R.id.deviceListView);
//        deviceListAdapter = new ArrayAdapter<DeviceRef>(this, R.layout.device_item);
//        deviceListView.setAdapter(deviceListAdapter);
//        deviceListAdapter.add(new DeviceRef("TimeFlip", "00:00:00:00:00:00"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG,"Device Name: " + result.getDevice().getName() + " address: " + result.getDevice().getAddress() + " rssi: " + result.getRssi());
        }
    };

    public void connect(View view) {
        Log.i(TAG, "Connect");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "BLE is available, let's continue...");
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
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

        BluetoothGattCallback callback =
                new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        if (status == BluetoothGatt.GATT_SUCCESS
                                && newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.i(TAG, "Connected to gatt " + gatt.getDevice());
                            Log.i(TAG, "Attempting to start service discovery:" +
                                    gatt.discoverServices());
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            BluetoothGattService serv = gatt.getService(TIMEFLIP_ID);
                            if (serv == null) {
                                Log.i(TAG, "Not TimeFlip");
                            } else {
                                Log.i(TAG, "TimeFlip found");
                                BluetoothGattCharacteristic charact
                                        = serv.getCharacteristic(FACET_CHARACTERISTIC);
                                gatt.readCharacteristic(charact);
                            }
                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt,
                                                     BluetoothGattCharacteristic characteristic, int status) {
                        Log.i(TAG, "onCharacteristicRead");
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.i(TAG, "Facet props: " + String.format("%02X", characteristic.getProperties()));
                            byte[] data = characteristic.getValue();
                            Log.i(TAG, "Facet length: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
                        }
                    }
                };
//        BluetoothGatt gatt = device.connectGatt(MainActivity.this, true, callback);
        final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        handler.postDelayed(new Runnable() {

            public void run() {
                Log.i(TAG, "stop scanning");
                scanner.startScan(leScanCallback);
            }
        }, 10000);
        Log.i(TAG, "start scanning...");
        scanner.startScan(leScanCallback);
    }
}
