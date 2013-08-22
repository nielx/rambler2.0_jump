package nl.simbits.rambler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class RamblerShoeConnector extends Thread{
	public static final String TAG = "RamblerShoeConnector";
	
    final int BUFFER = 32;

	private boolean mCancel;
	private boolean mRunning;
	
	private Context mContext;
	private JumpStepEventDetector mDetector;
	private String mAddressOrName;
	BluetoothSPPConnector mConnector;

	public RamblerShoeConnector(Context context, JumpStepEventDetector detector) {
	    super("RamblerShoeConnector");

	    mContext = context;
	    mDetector = detector;
		mRunning = false;
		mCancel = false;
		mConnector = null;
	}
	
	public synchronized boolean connect(String addressOrName) {
		if (mRunning) {
			Log.w(TAG, "Thread already started");
			return false;
		}
		
		mAddressOrName = addressOrName;
		start();
		
		return true;
	}
	
	public synchronized void cancel() {
		mCancel = true;
	}
	
	public synchronized boolean running() {
		return mRunning;
	}
	
	public synchronized void broadcastState() {
		if (mConnector != null) {
			broadcastConnectionState(mConnector);
		}
	}
	
	private synchronized void setRunning(boolean running) {
		mRunning = running;
	}
	
	private synchronized void broadcastConnectionState(BluetoothSPPConnector connector) {
	    Intent intent = new Intent();
		Bundle params = new Bundle();
		
		if (mContext == null)
			return;
		
		int state = connector.state();
		
		params.putInt("state", state);
		params.putString("state_text", BluetoothSPPConnector.stateToString(state));
		params.putString("name", connector.name());
		params.putString("address", connector.address());
		
		intent.putExtras(params);
	    intent.setAction(BluetoothSPPConnector.BROADCAST_INTENT_BLUETOOTH);
	    
	    mContext.sendBroadcast(intent);
	}
	
	@Override
	public void run() {
		InputStream inStream;

		Looper.prepare();
		
		Log.d(TAG, "BEGIN " + TAG + " for device: " + mAddressOrName);
	    	    
	    if (mAddressOrName.equals("")) 
	    	return;
	    
		mConnector = new BluetoothSPPConnector(mAddressOrName);
		
		int state = mConnector.connect();
	    
		broadcastConnectionState(mConnector);

	    if (state != BluetoothSPPConnector.BT_CONNECTED) {
	        Log.e(TAG, "Could not initiate bluetooth connection to " + mAddressOrName + 
	        		   ": " + BluetoothSPPConnector.stateToString(state));
	        cancel();
	        return;
	    }
	    
	    try {
	        inStream = mConnector.getInputStream();
	        InputStreamReader inStreamReader = new InputStreamReader(inStream);
	        BufferedReader inBuffer = new BufferedReader(inStreamReader);
	        float[] sampleBuffer = new float[BUFFER];
	        int sample = 0;
	        
	        setRunning(true);
	        
	        while (!mCancel) {
	            try {
	            	String line;
	                if ((line = inBuffer.readLine()) != null) {
	                    float v = Float.parseFloat(line);
	                    sampleBuffer[sample++] = v;

	                    if (sample == BUFFER) {
	                    	mDetector.detect(sampleBuffer);
	                    	sample = 0;
	                    }
	                }
	            } catch (IOException e) {
	                Log.e(TAG, "Failed to get input and output streams: " + e.getMessage());
	                cancel();
	            } catch (NumberFormatException e) {
	                Log.w(TAG, "Failed to convert input to float: " + e.getMessage());
	            }
	        }
	            
	    } catch (IOException e) {
	        Log.e(TAG, "Failed to get input and output streams: " + e.getMessage());
	    }
	    
	    mConnector.close();
	    broadcastConnectionState(mConnector);
	    
	   	setRunning(false);
	}
}
