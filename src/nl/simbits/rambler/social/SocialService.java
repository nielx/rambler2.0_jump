package nl.simbits.rambler.social;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;

public class SocialService extends IntentService {
    private final String TAG = "SocialService";

    // Intent actions
    public final static String QUERY_FACEBOOK_STATUS = "rambler.intent.QUERY_FACEBOOK_STATUS";

    // Intent messages
    public final static String FACEBOOK_STATUS = "rambler.message.FACEBOOK_STATUS";

    // Other constants
    public static final String[] FACEBOOK_READ_PERMISSIONS = {"offline_access",
            "read_stream", "user_photos", "user_likes", "user_events", "photo_upload"};
    public static final String[] FACEBOOK_PUBLISH_PERMISSIONS = {
            "publish_stream"
    };



    private Boolean mFacebookConnection = false;
    private String mFacebookName = "";

    public SocialService() {
        super("RamblerSocialService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        InitializeFacebook();

        if (intent.getAction().equals(QUERY_FACEBOOK_STATUS)) {
            // Process the Facebook Status
            Intent facebook_status = new Intent(FACEBOOK_STATUS);
            facebook_status.putExtra("authenticated", mFacebookConnection);
            if (mFacebookConnection) {
                // Get the user info
                Request.newMeRequest(Session.getActiveSession(), new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        mFacebookName = user.getName();
                    }
                }).executeAndWait();
                facebook_status.putExtra("name", mFacebookName);
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(facebook_status);
        }
    }


    private void InitializeFacebook() {
        // Extra logging for Facebook
        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        // Get the Facebook session
        Session session = Session.getActiveSession();
        if (session == null) {
            // Create a Facebook Session
            session = new Session(this);
            Session.setActiveSession(session);
        }

        // Check the status of the Session
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            Log.d(TAG, "Facebook Session has a cached token, we can load");
        } else if (session.getState().equals(SessionState.CREATED)) {
            Log.d(TAG, "There is no previous Facebook Session loaded");
        } else if (session.getState().equals(SessionState.OPENED)) {
            mFacebookConnection = true;
        } else {
            Log.d(TAG, "There is another facebook status: " + session.getState().toString());
        }
    }
}
