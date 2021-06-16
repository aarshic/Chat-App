package com.example.chatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Utils {
    private final Handler handler;
    private int state;
    private BluetoothAdapter bluetoothAdapter;
    private final String appName = "ChatApp";
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private AcceptThread acceptThread;
    private ConnectionThread connectionThread;
    private ConnectedThread connectedThread;

    public Utils(Handler handler) {
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, - 1).sendToTarget();
    }

    public synchronized void start() {
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
            if(state != STATE_CONNECTED)
                return;
            else
                cThread = connectedThread;
        }
        cThread.write(buffer);
    }

    private class ConnectionThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectionThread(BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;

            BluetoothSocket blthSocket = null;
            try {
                blthSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = blthSocket;
        }

        public void run() {
            try {
                if (bluetoothAdapter.isDiscovering())
                    bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
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
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket blthServerSocket = null;
            try {
                blthServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(appName, uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = blthServerSocket;
        }

        public void run() {
            BluetoothSocket bluetoothSocket = null;
            try {
                bluetoothSocket = bluetoothServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    bluetoothServerSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            if (bluetoothSocket != null) {
                if (state == STATE_LISTEN || state == STATE_CONNECTING)
                    connected(bluetoothSocket, bluetoothSocket.getRemoteDevice());
                else if (state == STATE_NONE || state == STATE_CONNECTED) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
                connectionLost();
                e.printStackTrace();
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.WRITE_MESSAGE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
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
        Message message = handler.obtainMessage(MainActivity.TOAST_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to the device.");
        message.setData(bundle);
        handler.sendMessage(message);
        Utils.this.start();
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(MainActivity.TOAST_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);
        Utils.this.start();
    }
}
