package org.tastefuljava.flipit;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID TIMEFLIP_ID
            = UUID.fromString("F1196F50-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID FACET_CHARACTERISTIC
            = UUID.fromString("F1196F52-71A4-11E6-BDF4-0800200C9A66");

    private Button button;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "Hello!!!");
        button = (Button)findViewById(R.id.button);
    }

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

        final BluetoothAdapter.LeScanCallback leScanCallback
                = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.i(TAG, "Device Name: " + device.toString());
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
                                    Log.i(TAG, "Facet length: " + characteristic..getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
                                }
                            }
                        };
                BluetoothGatt gatt = device.connectGatt(MainActivity.this, true, callback);
            }
        };
        handler.postDelayed(new Runnable() {

            public void run() {
                Log.i(TAG, "stop scanning");
                bluetoothAdapter.stopLeScan(leScanCallback);
            }
        }, 10000);
        Log.i(TAG, "start scanning...");
        bluetoothAdapter.startLeScan(leScanCallback);
    }
}
