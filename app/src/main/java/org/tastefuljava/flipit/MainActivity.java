package org.tastefuljava.flipit;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.tastefuljava.flipit.device.DeviceConnection;
import org.tastefuljava.flipit.server.Facet;
import org.tastefuljava.flipit.server.ServerConnection;
import org.tastefuljava.flipit.server.User;

import java.io.IOException;
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

    private BluetoothAdapter bluetoothAdapter;
    private PentaView pentaView;
    private ServerConnection cnt;
    private User user;
    private DeviceConnection device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this::connect);
        pentaView = findViewById(R.id.pentaView);
        cnt = ServerConnection.open("maurice@perry.ch", "test1234");
        user = cnt.currentUser();
        registerReceiver(receiver, new IntentFilter(getString(R.string.ACTION_CONNECT)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        pentaView.setText("");
    }

    @Override
    protected void onStop() {
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
            try {
                connect(address);
            } catch (IOException e) {
                showError("Error", e.getMessage());
            }
        }
    };

    private void showError(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void connect(String address) throws IOException {
        device = DeviceConnection.open(this, address, (face) -> {
            Facet facet = null;
            if (user != null) {
                facet = user.getFacet(face);
            }
            if (facet == null || facet.getSymbol() == null) {
                pentaView.setText("");
            } else {
                pentaView.setText(facet.getSymbol());
            }
            cnt.sendFacet(face);
        });
    }

    public void connect(View view) {
        startActivity(new Intent(MainActivity.this, ConnectActivity.class));
    }
}
