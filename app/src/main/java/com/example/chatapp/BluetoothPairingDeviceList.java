package com.example.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;

public class BluetoothPairingDeviceList extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ListView listPairedDevices, listAvailableDevices;
    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> availableDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pairing_device_list);

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
    }


}