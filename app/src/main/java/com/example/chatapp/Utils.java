package com.example.chatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Utils {
    private String TAG = "ChatUtils";

    private Context context;
    private final Handler handler;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private AcceptThread acceptThread;
    private ConnectionThread connectionThread;
    private ConnectedThread connectedThread;

    private int state;
    private BluetoothAdapter bluetoothAdapter;
    private final String appName = "ChatApp";

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public Utils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState(){
        return state;
    }

    public synchronized void setState(int state) {
        Log.d(TAG, "In setState(): " + state);

        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, - 1).sendToTarget();
    }

    public synchronized void start() {
        Log.d(TAG, "In start()");

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {
        Log.d(TAG, "In stop()");

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "In connect()");

        if (state == STATE_CONNECTING) {
            connectionThread.cancel();
            connectionThread = null;
        }

        connectionThread = new ConnectionThread(bluetoothDevice);
        connectionThread.start();

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        setState(STATE_CONNECTING);
    }

    public void write(byte[] buffer) {
        ConnectedThread cThread;

        synchronized (this) {
            if(state != STATE_CONNECTED) {
                return;
            }
            else {
                cThread = connectedThread;
            }
        }

        connectedThread.write(buffer);
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            Log.d(TAG, "In AcceptThread()");

            BluetoothServerSocket blthServerSocket = null;
            try {
                blthServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(appName, uuid);
            } catch (IOException e) {
                Log.d(TAG, "AcceptThread()");
                e.printStackTrace();
            }
            bluetoothServerSocket = blthServerSocket;
        }

        public void run() {
            Log.d(TAG, "In AcceptThread run()");
            BluetoothSocket bluetoothSocket = null;
            try {
                bluetoothSocket = bluetoothServerSocket.accept();
            } catch (IOException e) {
                Log.d(TAG, "AcceptThread run(): " + e.getMessage());
                try {
                    bluetoothServerSocket.close();
                } catch (IOException ioException) {
                    Log.d(TAG, "AcceptThread close: " + ioException.getMessage());
                }
            }

            if (bluetoothSocket != null) {
                if (state == STATE_LISTEN || state == STATE_CONNECTING) {
                    connected(bluetoothSocket, bluetoothSocket.getRemoteDevice());
                } else if (state == STATE_NONE || state == STATE_CONNECTED) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        Log.d(TAG, "AcceptThread close socket: " + e.getMessage());
                    }
                }
            }
        }

        public void cancel() {
            Log.d(TAG, "In AcceptThread cancel()");
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "AcceptThread close serverSocket: " + e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            this.bluetoothSocket = bluetoothSocket;
            InputStream iStream = null;
            OutputStream oStream = null;

            try {
                iStream = bluetoothSocket.getInputStream();
                oStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = iStream;
            outputStream = oStream;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                bytes = inputStream.read();
                handler.obtainMessage(MainActivity.READ_MESSAGE, bytes, -1, buffer).sendToTarget();

            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
                connectionLost();
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.WRITE_MESSAGE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }

    }

    private void connectionLost() {
        Message message = handler.obtainMessage(MainActivity.TOAST_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        Utils.this.start();
    }

    private class ConnectionThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectionThread(BluetoothDevice bluetoothDevice) {
            Log.d(TAG, "In ConnectionThread()");
            this.bluetoothDevice = bluetoothDevice;

            BluetoothSocket blthSocket = null;
            try {
                blthSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.d(TAG, "ConnectionThread(): " + e.getMessage());
            }
            bluetoothSocket = blthSocket;
            Log.d(TAG, "ConnectionThread() blthSocket 1: " + blthSocket);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "ConnectionThread() blthSocket 2: " + blthSocket.getConnectionType());
            }
            Log.d(TAG, "ConnectionThread() blthSocket 3: " + blthSocket.getRemoteDevice());
            Log.d(TAG, "ConnectionThread() blthSocket 4: " + blthSocket.isConnected());
        }

        public void run() {
            Log.d(TAG, "In ConnectionThread run()");
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothSocket.connect();
            } catch (IOException e) {
                Log.d(TAG, "ConnectionThread() run 00: " + bluetoothSocket.isConnected());
                Log.d(TAG, "ConnectionThread run 0: " + Utils.this.getState());
                Log.d(TAG, "ConnectionThread run 1: " + bluetoothDevice.getName());
                Log.d(TAG, "ConnectionThread run 2: " + bluetoothDevice.getAddress());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Log.d(TAG, "ConnectionThread run 5: " + bluetoothDevice.getAlias());
                    Log.d(TAG, "ConnectionThread run 6: " + bluetoothDevice.getType());
                    Log.d(TAG, "ConnectionThread run 7: " + bluetoothDevice.createBond());
                }
                Log.d(TAG, "ConnectionThread run 8: " + bluetoothDevice.getBondState());
                if(bluetoothDevice.fetchUuidsWithSdp()){
                    Log.d(TAG, "Here: " + bluetoothDevice.getUuids());
                }

                Log.d(TAG, "ConnectionThread run100: " + e.getMessage());
                try {
                    bluetoothSocket.close();
                } catch (IOException ioException) {
                    Log.d(TAG, "ConnectionThread close: " + ioException.getMessage());
                }
                connectionFailed();
                return;
            }

            synchronized (Utils.this) {

                connectionThread = null;
            }

            connected(bluetoothSocket, bluetoothDevice);
        }

        public void cancel() {
            Log.d(TAG, "ConnectionThread cancel()");

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "ConnectionThread cancel: " + e.getMessage());
            }
        }
    }

    private synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "In ConnectionThread connected(): " + bluetoothDevice);

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.DEVICE_NAME_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, bluetoothDevice.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    private synchronized void connectionFailed() {
        Log.d(TAG, "In connectionFailed()");

        Message message = handler.obtainMessage(MainActivity.TOAST_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to the device.");
        message.setData(bundle);
        handler.sendMessage(message);

        Utils.this.start();
    }
}
