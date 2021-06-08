package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBluetooth();
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
                return true;
            case R.id.bluetooth_on:
                enableBluetooth();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "bluetooth not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(MainActivity.this, "bluetooth already enabled", Toast.LENGTH_SHORT).show();
        } else {
            bluetoothAdapter.enable();
            Toast.makeText(MainActivity.this, "bluetooth enabled", Toast.LENGTH_SHORT).show();
        }
    }
}