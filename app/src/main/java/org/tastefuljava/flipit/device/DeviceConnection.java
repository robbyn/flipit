package org.tastefuljava.flipit.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class DeviceConnection {
    private static final String TAG = DeviceConnection.class.getSimpleName();

    private static final UUID TIMEFLIP_ID
            = UUID.fromString("F1196F50-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID ACCEL_CHARACTERISTIC
            = UUID.fromString("F1196F51-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID FACET_CHARACTERISTIC
            = UUID.fromString("F1196F52-71A4-11E6-BDF4-0800200C9A66");
    private static final UUID FACET_DESCRIPTOR
            = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID PASSWORD_CHARACTERISTIC
            = UUID.fromString("F1196F57-71A4-11E6-BDF4-0800200C9A66");

    private final Context context;
    private final Callback callback;

    public interface Callback {
        void facedChanged(int newFacet);
    }

    public static DeviceConnection open(Context context, String address, Callback callback)
            throws IOException {
        DeviceConnection cnt = new DeviceConnection(context, callback);
        cnt.open(address);
        return cnt;
    }

    private DeviceConnection(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    private void open(String address) throws IOException {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            String msg = "Could not get remote device [" + address + "]";
            Log.e(TAG, msg);
            throw new IOException(msg);
        } else {
            Log.i(TAG, "got remote device");
            BluetoothGatt gatt = device.connectGatt(context, true, gattCallback);
            Log.i(TAG, "connection requested: " + gatt);
        }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
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
                setCharacteristicNotification(gatt, charact, true);
                charact = serv.getCharacteristic(FACET_CHARACTERISTIC);
                gatt.readCharacteristic(charact);
            }
        }

        private boolean setCharacteristicNotification(
                BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic charact, boolean enable) {
            Log.d(TAG, "setCharacteristicNotification");
            bluetoothGatt.setCharacteristicNotification(charact, enable);
            BluetoothGattDescriptor descriptor = charact.getDescriptor(FACET_DESCRIPTOR);
            descriptor.setValue(enable
                    ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return bluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic charact,
                                         int status) {
            Log.i(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS
                    && charact.getUuid().equals(FACET_CHARACTERISTIC)) {
                Integer facet = charact.getIntValue(FORMAT_UINT8, 0);
                Log.i(TAG, "New value: " + facet);
                if (facet != null) {
                    facetChanged(facet);
                }
            }
        }

        private void facetChanged(int face) {
            face %= 12;
            if (faceNumber != face) {
                faceNumber = face;
                callback.facedChanged(face);
            }
        }

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic charact) {
            Log.i(TAG, "onCharacteristicChanged");
            if (charact.getUuid().equals(FACET_CHARACTERISTIC)) {
                Integer facet = charact.getIntValue(FORMAT_UINT8, 0);
                Log.i(TAG, "New value: " + facet);
                if (facet != null) {
                    facetChanged(facet);
                }
            }
        }
    };
}
