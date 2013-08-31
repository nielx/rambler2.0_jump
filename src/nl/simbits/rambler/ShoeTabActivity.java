package nl.simbits.rambler;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ShoeTabActivity extends Activity {
    public static final String TAG = "ShoeTabActivity";
	
    private SharedPreferences mPreferences;
	private EditText mPosThreshold;
	private EditText mNegThreshold;
	private EditText mJumpEventWindow;
	private CheckBox mVibrateOnStep;
	private CheckBox mVibrateOnJump;
    private Button mApplyChanges;
    
    private ButtonOnClickListener mOnClickListener;
    
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shoe_tab_layout);
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPosThreshold = (EditText)findViewById(R.id.PositiveThresholdText);
        mNegThreshold = (EditText)findViewById(R.id.NegativeThresholdText);
        mJumpEventWindow = (EditText)findViewById(R.id.JumpEventWindowText);
        mVibrateOnStep = (CheckBox)findViewById(R.id.VibrateOnStepCheckBox);
        mVibrateOnJump = (CheckBox)findViewById(R.id.VibrateOnJumpCheckBox);
        mApplyChanges = (Button)findViewById(R.id.ShoeDetectApplyButton);
        
        mOnClickListener = new ButtonOnClickListener();
        mVibrateOnStep.setOnClickListener(mOnClickListener);
        mVibrateOnJump.setOnClickListener(mOnClickListener);
        mApplyChanges.setOnClickListener(mOnClickListener);
    
        fillFromPreferences();
    }
	
	private void fillFromPreferences() {
		mPosThreshold.setText(mPreferences.getString("SGPosThreshold", "30"));
		mNegThreshold.setText(mPreferences.getString("SGNegThreshold", "-30"));
		mJumpEventWindow.setText(mPreferences.getString("SGJumpEventWindow", "1000"));
		mVibrateOnStep.setChecked(mPreferences.getBoolean("SDVibrateOnStep", false));
		mVibrateOnJump.setChecked(mPreferences.getBoolean("SDVibrateOnJump", false));
	}
	
	private final class ButtonOnClickListener implements OnClickListener {
        public void onClick(View view) {
        	switch (view.getId()) {
	        	case R.id.VibrateOnStepCheckBox: {
        			Editor editor = mPreferences.edit();
        			editor.putBoolean("SDVibrateOnStep", ((CheckBox)view).isChecked());
        			editor.commit();
        			break;
	        	}
	        	
	        	case R.id.VibrateOnJumpCheckBox: {
        			Editor editor = mPreferences.edit();
        			editor.putBoolean("SDVibrateOnJump", ((CheckBox)view).isChecked());
        			editor.commit();
	        		break;
	        	}
	        	
	        	case R.id.ShoeDetectApplyButton: {
	        		try {
	        			String pos = ((TextView)mPosThreshold).getText().toString();
	        			String neg = ((TextView)mNegThreshold).getText().toString();
	        			String eventWindow = ((TextView)mJumpEventWindow).getText().toString();
	        			
	        			Editor editor = mPreferences.edit();
	        			editor.putString("SGPosThreshold", pos);
	        			editor.putString("SGNegThreshold", neg);
	        			editor.putString("SGJumpEventWindow", eventWindow);
	        			editor.commit();

	        		} catch (NumberFormatException e) {
		                Log.w(TAG, "Failed to convert input to int: " + e.getMessage());
	        		}
	        		break;
	        	}
        	}
        }
	}
}