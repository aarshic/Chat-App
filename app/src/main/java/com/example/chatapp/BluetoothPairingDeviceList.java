package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Set;

public class BluetoothPairingDeviceList extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ListView listPairedDevices, listAvailableDevices;
    ProgressBar scanDeviceProgressBar;
    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> availableDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pairing_device_list);

        scanDeviceProgressBar = findViewById(R.id.scan_devices_progressbar);

        listPairedDevices = findViewById(R.id.list_paired_devices);
        listAvailableDevices = findViewById(R.id.list_available_devices);
        pairedDevicesAdapter = new ArrayAdapter<String>(BluetoothPairingDeviceList.this, R.layout.activity_bluetooth_device_item);
        availableDevicesAdapter = new ArrayAdapter<String>(BluetoothPairingDeviceList.this, R.layout.activity_bluetooth_device_item);

        listPairedDevices.setAdapter(pairedDevicesAdapter);
        listAvailableDevices.setAdapter(availableDevicesAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                pairedDevicesAdapter.add(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
            }
        }

        IntentFilter intentFilterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothBroadCastReceiver, intentFilterFound);
        IntentFilter intentFilterFinish = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothBroadCastReceiver, intentFilterFinish);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_devices:
                Toast.makeText(BluetoothPairingDeviceList.this, "Scan Started", Toast.LENGTH_SHORT).show();
                scanAvailableDevices();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver bluetoothBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Aarshi", "In onReceive()");

            String bluetoothAction = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(bluetoothAction)) {
                Log.d("Aarshi", "In BluetoothDevice.ACTION_FOUND");
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d("Aarshi", "In BluetoothDevice not bonded");
                    availableDevicesAdapter.add(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(bluetoothAction)) {
                scanDeviceProgressBar.setVisibility(View.GONE);
                Log.d("Aarshi", "In BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
                if (availableDevicesAdapter.getCount() == 0) {
                    Log.d("Aarshi", "No Device Found");
                    Toast.makeText(BluetoothPairingDeviceList.this, "No Device Found", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("Aarshi", "Devices Found");
                    Toast.makeText(BluetoothPairingDeviceList.this, "Select Device", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private void scanAvailableDevices() {
        scanDeviceProgressBar.setVisibility(View.VISIBLE);
        availableDevicesAdapter.clear();

        Toast.makeText(BluetoothPairingDeviceList.this, "Scanning Available Devices", Toast.LENGTH_SHORT).show();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }
}