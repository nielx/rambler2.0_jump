package nl.simbits.rambler.social;

import android.app.IntentService;
import android.content.Intent;

public class SocialService extends IntentService {
    private final String TAG = "SocialService";

    public SocialService() {
        super("RamblerSocialService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
