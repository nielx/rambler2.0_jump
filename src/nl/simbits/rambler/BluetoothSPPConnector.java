package nl.simbits.rambler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothSPPConnector extends Thread {
    public static final String TAG = "BluetoothSPPConnector";
    private static final UUID SPP_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    public static final String BROADCAST_INTENT_BLUETOOTH = "nl.simbits.rambler.BLUETOOTH";

    public static final int BT_NOT_CONNECTED = 0;
    public static final int BT_NOT_AVAILABLE = 1;
    public static final int BT_NOT_ENABLED = 2;
    public static final int BT_DEVICE_NOT_FOUND = 3;
    public static final int BT_COULD_NOT_CREATE_SOCKET = 4;
    public static final int BT_CONNECTION_FAILED = 5;
    public static final int BT_CONNECTED = 6;

    private BluetoothAdapter mAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private String mAddress;
    private int mState;
    
    private void init(BluetoothAdapter adapter, String address) {
        mAdapter = adapter;
        mAddress = address;
        mState = BT_NOT_CONNECTED;
    }

    public BluetoothSPPConnector(BluetoothAdapter adapter, String address) {
        init(adapter, address);
    }

    public BluetoothSPPConnector(String address) {
        this(BluetoothAdapter.getDefaultAdapter(), address);
    }

    public BluetoothDevice findDevice(String address) {
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices) {
            	Log.d(TAG, "Comparing with: " + device.getAddress() + " - " + device.getName());
                if(device.getAddress().equals(address) || device.getName().startsWith(address)) {
                    return device;
                }
            }
        }
        return null;
    }

    public synchronized int connect() {
        mState = BT_NOT_CONNECTED;

        if (mAdapter == null) { 
        	mState = BT_NOT_AVAILABLE; 
        	return mState; 
        
        }
        if (mAdapter.isEnabled() == false) {
        	mState = BT_NOT_ENABLED;
        	return mState;
        }

        if (mDevice == null) {
            BluetoothDevice device = findDevice(mAddress);
            if (device == null) {
            	mState = BT_DEVICE_NOT_FOUND;
            	return mState;
            }
            mDevice = device;
        }

        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(SPP_UUID_SECURE);
        } catch (Exception e) {
            Log.e(TAG, "Socket create failed: " + e.getMessage());
            mState = BT_COULD_NOT_CREATE_SOCKET;
            return mState;
        }

        try {
            mAdapter.cancelDiscovery();
            mSocket.connect();
            mState = BT_CONNECTED;
        } catch(IOException connectException){
            try {
                Log.e(TAG, "Socket connect failed: " + connectException.getMessage());
                mState = BT_CONNECTION_FAILED;
                mSocket.close();
            } catch(IOException closeException){
                Log.e(TAG, "Socket close failed: " + closeException.getMessage());
            }
        }
        
        return mState;
    } 

    public InputStream getInputStream() throws IOException {
        return mSocket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return mSocket.getOutputStream();
    }

    public synchronized void close() {
        if (mSocket != null) {;
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket close failed: " + e.getMessage());
            }
            mState = BT_NOT_CONNECTED;
        }
    }
    
    public synchronized int state() {
    	return mState;
    }

    public String address() {
    	if (mDevice != null)
    		return mDevice.getAddress();
    	return "";
    }
    
    public String name() {
    	if (mDevice != null)
    		return mDevice.getName();
    	return "";
    }
    
    public static String stateToString(int state) {
        String str;

        switch(state) {
           case BluetoothSPPConnector.BT_CONNECTED:
               str = "connected";
               break;
           case BluetoothSPPConnector.BT_NOT_CONNECTED:
               str = "not connected";
               break;
           case BluetoothSPPConnector.BT_NOT_AVAILABLE:
               str = "not available";
               break;
           case BluetoothSPPConnector.BT_NOT_ENABLED:
               str = "not enabled";
               break;
           case BluetoothSPPConnector.BT_DEVICE_NOT_FOUND:
               str = "device not found";
               break;
           case BluetoothSPPConnector.BT_COULD_NOT_CREATE_SOCKET:
               str = "could not create socket";
               break;
           case BluetoothSPPConnector.BT_CONNECTION_FAILED:
               str = "connection failed";
               break;
           default:
               str = "unknown message";
        }

        return str;
    }
}
