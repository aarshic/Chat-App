package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private final int LOCATION_REQ = 101;
    private final int DEVICE_SELECTED = 202;

    public static final int READ_MESSAGE = 1;
    public static final int WRITE_MESSAGE = 2;
    public static final int DEVICE_NAME_MESSAGE = 3;
    public static final int TOAST_MESSAGE = 4;
    public static final int MESSAGE_STATE_CHANGED = 5;

    public static final String DEVICE_NAME = "deviceName";
    private String deviceConnected;
    public static final String TOAST = "toast";

    private Utils utils;

    private ListView listConversation;
    private EditText message;
    private Button send;
    private ArrayAdapter<String> chatAdapter;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case READ_MESSAGE:
                    byte[] readBuffer = (byte[]) msg.obj;
                    String iBuffer = new String(readBuffer, 0, msg.arg1);
                    chatAdapter.add(deviceConnected + ": " + iBuffer);
                    break;
                case WRITE_MESSAGE:
                    byte[] writebuffer = (byte[]) msg.obj;
                    String oBuffer = new String(writebuffer);
                    chatAdapter.add("Me: " + oBuffer);
                    break;
                case DEVICE_NAME_MESSAGE:
                    deviceConnected = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(MainActivity.this, deviceConnected, Toast.LENGTH_SHORT).show();
                    break;
                case TOAST_MESSAGE:
                    Toast.makeText(MainActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_STATE_CHANGED:
                    if (msg.arg1 == Utils.STATE_NONE)
                        setState("Not Connected");
                    else if (msg.arg1 == Utils.STATE_LISTEN)
                        setState("Not Connected");
                    else if (msg.arg1 == Utils.STATE_CONNECTING)
                        setState("Connecting");
                    else if (msg.arg1 == Utils.STATE_CONNECTED)
                        setState("Connected: " + deviceConnected);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listConversation = findViewById(R.id.list_conversion);
        message = findViewById(R.id.message);
        send = findViewById(R.id.send_button);

        chatAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.conversation_item);
        listConversation.setAdapter(chatAdapter);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                if(!msg.isEmpty()) {
                    message.setText("");
                    utils.write(msg.getBytes());
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "bluetooth not found", Toast.LENGTH_SHORT).show();
        }

        utils = new Utils(MainActivity.this, handler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_devices:
                Toast.makeText(MainActivity.this, "Search Devices", Toast.LENGTH_SHORT).show();
                checkPermission();
                return true;
            case R.id.bluetooth_on:
                enableBluetooth();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(MainActivity.this, permission, LOCATION_REQ);
        } else {
            Intent intent = new Intent(MainActivity.this, BluetoothPairingDeviceList.class);
            startActivityForResult(intent, DEVICE_SELECTED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, BluetoothPairingDeviceList.class);
                startActivityForResult(intent, DEVICE_SELECTED);
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\nPlease grant")
                        .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermission();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.fileList();
                            }
                        })
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DEVICE_SELECTED && resultCode == RESULT_OK) {
            String address = data.getStringExtra("address");
            Toast.makeText(MainActivity.this, "Device Selected: " + address, Toast.LENGTH_SHORT).show();
            utils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (utils != null) {
            utils.stop();
        }
    }

    private void enableBluetooth() {
        if (! bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Toast.makeText(MainActivity.this, "bluetooth enabled", Toast.LENGTH_SHORT).show();
        }

        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent scanIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            scanIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(scanIntent);
        }
    }
}