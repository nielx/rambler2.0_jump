package nl.simbits.rambler;

import android.util.Log;
import mr.go.sgfilter.SGFilter;

public class SavitzkyGolayStepDetector implements JumpStepEventDetector {
	public final String TAG = "SavitzkyGolayStepDetector";
	
    /* coefficients for Savitzky-Golay filter for 1st derivative, A=3'd order poly */
    final double COEFFS[]   = {  3.86710240e-02,  -1.23327109e-02,  -4.23459982e-02,
                                -5.48672460e-02,  -5.33948622e-02,  -4.14272547e-02,
                                -2.24628313e-02,   5.09650855e-18,   2.24628313e-02,
                                 4.14272547e-02,   5.33948622e-02,   5.48672460e-02,
                                 4.23459982e-02,   1.23327109e-02,  -3.86710240e-02
                               };
    
    /* Sane defaults */
    private final int PEAK_POS_THRESHOLD    = 30;
	private final int PEAK_NEG_THRESHOLD    = -30;
    private final int JUMP_EVENT_WINDOW     = 100;
    private final long JUMP_EVENT_WINDOW_NS	= 1000 * (long)1e6; /* 1 seconds */
    private final int WINDOW_SIZE   		= 15; /* must be uneven */
    private final int HALF_WINDOW   		= (WINDOW_SIZE - 1) / 2;
    private final int SAMPLES       		= 32;
    /***/
    
    private int mSamples;
    private int mWindowSize;
    private int mHalfWindow;
    private int mPeakPosThreshold;
    private int mPeakNegThreshold;
    private long mJumpEventWindow;
    
    private float mPv_max;
    private float mPv_min;
    private float mPv_last;
    private int mEventCount;
    private boolean mFoundPeak;
    private long mEventTime;

	private int mJumpCount;
	private long mJumpStartEvent;
	private long mJumpIntervalNano;

    private float[] mLeftPad = new float[HALF_WINDOW];
    private float[] mRightPad = new float[HALF_WINDOW];

    private eventListener mListener;
    private SGFilter mSGFilter;
    private boolean mReset;

    private void doReset() {
        mPv_max = 0;
        mPv_min = 0;
        mPv_last = 0;
        mEventCount = 0;
        mEventTime = 0;
        mFoundPeak = true;	
        
		mJumpCount = 0;
		mJumpStartEvent = 0;
		mJumpIntervalNano = 0;
		
		mReset = false;
    }
    
    private void init(int samples, int window, eventListener listener) {
    	mSamples = samples;
    	mWindowSize = window;
    	mHalfWindow = (mWindowSize - 1) / 2;
    	
    	mPeakPosThreshold = PEAK_POS_THRESHOLD;
    	mPeakNegThreshold = PEAK_NEG_THRESHOLD;
    	mJumpEventWindow = JUMP_EVENT_WINDOW_NS;
    	
    	doReset();
		
        mListener = listener;
        mSGFilter = new SGFilter(mHalfWindow, mHalfWindow);	
    }
    
    public SavitzkyGolayStepDetector(int samples, int window, eventListener listener) {
    	init(samples, window, listener);
    }
    
    public SavitzkyGolayStepDetector(eventListener listener) {
    	init(SAMPLES, WINDOW_SIZE, listener);
    }
    
    public void setPeakThresholds(int positive, int negative) {
    	mPeakPosThreshold = positive;
    	mPeakNegThreshold = negative;
    }
    
    public int[] getPeakThresholds() {
    	return new int[] {mPeakPosThreshold, mPeakNegThreshold};
    }
    
    public void setJumpEventWindow(long milis) {
    	mJumpEventWindow = milis * (long)1e6;
    }
    
	public int getJumpInterval() {
		return (int)(mJumpIntervalNano / (long)1e6);
	}
	
	public void setJumpInterval(int millis) {
		mJumpIntervalNano = millis * (long)1e6;
	}
    
    public void detect(final float[] buffer) {
    	int mZeroCrossing = 0;
    	
    	if (mListener == null)
    		return;
    	
    	if (mReset) {
    		doReset();
    	}
    	
        for (int i=0; i<mHalfWindow; i++) {
            /* just do a simple continuous padding of first and last sample in the set
             * to prevent introduction of additional 'spikes' */
            mLeftPad[i] = buffer[0];
            mRightPad[i] = buffer[mSamples-1];
        }

        float[] vsg = mSGFilter.smooth(buffer, mLeftPad, mRightPad, COEFFS);
        
        for (float val : vsg) {
            if (val > 0) {
                if (mPv_last <= 0) {
                    mZeroCrossing = 1;
                    mPv_max = val;
                } else if (val > mPv_max) {
                    mPv_max = val;
                }
            } else if (val < 0) {
                if (mPv_last >= 0) {
                    mZeroCrossing = -1;
                    mPv_min = val;
                } else if (val < mPv_min) {
                    mPv_min = val;
                }
            }
            
            if (mZeroCrossing > 0) {
                if (mPv_min < mPeakNegThreshold) {
                    mFoundPeak = true;
                    mEventTime = System.nanoTime();
                }
            } else if (mZeroCrossing < 0) {
                if (mPv_max > mPeakPosThreshold) {
                    if (mFoundPeak) {
                    	if ((System.nanoTime() - mEventTime) <= mJumpEventWindow) {
                        //if (mEventCount < JUMP_EVENT_WINDOW) {
                            Log.i(TAG, "HAVE A JUMP");
                            mFoundPeak = false;
                            mEventCount = 0;
                            
                            if (mJumpIntervalNano > 0) {
	                            mJumpCount++;
	                            mJumpStartEvent = System.nanoTime();
                            }
                            mListener.onJump(mJumpStartEvent);
                        }
                    }
                }
            }

            if (mFoundPeak) {
                //mEventCount++;
                //if (mEventCount > JUMP_EVENT_WINDOW) {
                if ((System.nanoTime() - mEventTime) > mJumpEventWindow) {
            		Log.i(TAG, "HAVE A STEP");
                    mFoundPeak = false;
                    mEventCount = 0;
                    mListener.onStep(System.nanoTime());
                }
            } 

            if (mJumpIntervalNano > 0 && mJumpCount > 0) {
                long now = System.nanoTime();
                if (now - mJumpStartEvent > mJumpIntervalNano) {
                    Log.i(TAG, "Jump time expired. Got " + mJumpCount + " jumps");
                    mListener.onJumps(mJumpCount, now);
                    mJumpStartEvent = 0;
                    mJumpCount = 0;
                }
            }

            mPv_last = val;
        }
    }
    
    /**
     * Resets internal state on next iteration of detect()
     */
    @Override
    public void reset() {
    	mReset = true;
    }
}
