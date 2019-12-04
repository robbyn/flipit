package org.tastefuljava.flipit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.tastefuljava.flipit.server.ServerConnection;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final UUID TIMEFLIP_ID
            = UUID.fromString("F1196F50-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID ACCEL_CHARACTERISTIC
            = UUID.fromString("F1196F51-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID FACET_CHARACTERISTIC
            = UUID.fromString("F1196F52-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID PASSWORD_CHARACTERISTIC
            = UUID.fromString("F1196F57-71A4-11E6-BDF4-0800200C9A66");

    private static final String[] FACE_NAMES = {"???", "email",
            "pause",
            "administration",
            "congé",
            "hors-horaire",
            "appel",
            "documentation",
            "3202-soca",
            "sysadmin",
            "réunion",
            "2983-conteneurisation",
            "en-relation-externe"};

    private BluetoothAdapter bluetoothAdapter;
    private PentaView pentaView;
    private Handler handler = new Handler();
    private ServerConnection cnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect(view);
            }
        });
        pentaView = findViewById(R.id.pentaView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        cnt = ServerConnection.open("maurice@perry.ch", "test1234");
        registerReceiver(receiver, new IntentFilter(getString(R.string.ACTION_CONNECT)));
    }

    @Override
    protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
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

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra("deviceAddress");
            connect(address);
        }
    };

    private BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                int faceNumber = -1;

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
                            BluetoothGattCharacteristic characteristic
                                    = serv.getCharacteristic(PASSWORD_CHARACTERISTIC);
                            characteristic.setValue("000000".getBytes(StandardCharsets.US_ASCII));
                            gatt.writeCharacteristic(characteristic);
                            Log.i(TAG, "Password write requested");
                        }
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS
                            && characteristic.getUuid().equals(PASSWORD_CHARACTERISTIC)) {
                        Log.i(TAG, "Password written");
                        BluetoothGattService serv = gatt.getService(TIMEFLIP_ID);
                        BluetoothGattCharacteristic charact
                                = serv.getCharacteristic(FACET_CHARACTERISTIC);
                        gatt.readCharacteristic(charact);
//                        gatt.setCharacteristicNotification(charact, true);
                    }
                }

                @Override
                public void onCharacteristicRead(final BluetoothGatt gatt,
                                                 final BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.i(TAG, "onCharacteristicRead");
                    if (status == BluetoothGatt.GATT_SUCCESS
                            && characteristic.getUuid().equals(FACET_CHARACTERISTIC)) {
                        Log.i(TAG, "Facet props: " + String.format("%02X", characteristic.getProperties()));
                        byte[] data = characteristic.getValue();
                        Log.i(TAG, "Facet length: " + data.length);
                        if (data.length == 1) {
                            Log.i(TAG, "Facet value: " + data[0]);
                            facetChanged(data[0]);
                        }
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                gatt.readCharacteristic(characteristic);
                            }
                        }, 2000);
                    }
                }

                private void facetChanged(int face) {
                    if (face >= 0 && face < FACE_NAMES.length && faceNumber != face) {
                        faceNumber = face;
                        pentaView.setText("\uf1b9");
                        sendFacet(FACE_NAMES[face]);
                    }
                }

                private void sendFacet(String face) {
                    try {
                        Log.i(TAG, "Sending request");
                        URL url = new URL("http://perry.ch/flipit-server/api/activity/log");
                        HttpURLConnection cnt = (HttpURLConnection) url.openConnection();
                        cnt.setDoOutput(true);
                        cnt.setRequestProperty("Content-Type", "application/json");
                        cnt.setRequestMethod("POST");
                        try (Writer writer = new OutputStreamWriter(cnt.getOutputStream(),
                                StandardCharsets.UTF_8)) {
                            String json = "{\"username\":\"briner\",\"facename\":\"" + face + "\"}";
                            Log.i(TAG, json);
                            writer.write(json);
                        }
                        int st = cnt.getResponseCode();
                        if (st >= 200 && st <= 299) {
                            Log.i(TAG, "Request sent");
                        } else {
                            Log.e(TAG, "Request error: " + st);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "sendFace", e);
                    }
                }

                @Override
                public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                                 BluetoothGattCharacteristic characteristic) {
                    Log.i(TAG, "onCharacteristicChanged");
                    Log.i(TAG, "New value: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
                }
            };

    private void connect(String address) {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "Could not get remote device");
        } else {
            Log.i(TAG, "got remote device");
            BluetoothGatt gatt = device.connectGatt(this, true, gattCallback);
            Log.i(TAG, "connection requested: " + gatt);
        }
    }

    public void connect(View view) {
        startActivity(new Intent(MainActivity.this, ConnectActivity.class));
    }
}
