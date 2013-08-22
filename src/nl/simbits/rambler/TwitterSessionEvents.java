package nl.simbits.rambler;

import java.util.LinkedList;

public class TwitterSessionEvents {

    private static LinkedList<TwitterAuthListener> mAuthListeners = 
        new LinkedList<TwitterAuthListener>();
    private static LinkedList<TwitterLogoutListener> mLogoutListeners = 
        new LinkedList<TwitterLogoutListener>();

    public static void addAuthListener(TwitterAuthListener listener) {
        mAuthListeners.add(listener);
    }

    public static void removeAuthListener(TwitterAuthListener listener) {
        mAuthListeners.remove(listener);
    }

    public static void addLogoutListener(TwitterLogoutListener listener) {
        mLogoutListeners.add(listener);
    }

    public static void removeLogoutListener(TwitterLogoutListener listener) {
        mLogoutListeners.remove(listener);
    }
    
    public static void onLoginSuccess() {
        for (TwitterAuthListener listener : mAuthListeners) {
            listener.onAuthSucceed();
        }
    }
    
    public static void onLoginError(String error) {
        for (TwitterAuthListener listener : mAuthListeners) {
            listener.onAuthFail(error);
        }
    }
    
    public static void onLogoutBegin() {
        for (TwitterLogoutListener l : mLogoutListeners) {
            l.onLogoutBegin();
        }
    }
    
    public static void onLogoutFinish() {
        for (TwitterLogoutListener l : mLogoutListeners) {
            l.onLogoutFinish();
        }   
    }
    
    public static interface TwitterAuthListener {
        public void onAuthSucceed();
        public void onAuthFail(String error);
    }
    
    public static interface TwitterLogoutListener {
        public void onLogoutBegin();
        public void onLogoutFinish();
    }
    
}
