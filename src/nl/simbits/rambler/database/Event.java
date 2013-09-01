package nl.simbits.rambler.database;


import android.graphics.Bitmap;

import java.util.Date;

import nl.simbits.rambler.RamblerLocation;

public class Event {
    public enum EventType {
        FACEBOOK,
        TWITTER,
        BLUETOOTH
    }

    private String mEventMessage;
    private EventType mEventType;
    private RamblerLocation mLocation;
    private String mPictureUrl;
    private Bitmap mPicture;
    private Date mDate;

    public Event(EventType type, String message, RamblerLocation location, String pictureUrl) {
        mEventMessage = message;
        mEventType = type;
        mLocation = location;
        mPictureUrl = pictureUrl;
        mDate = new Date();
    }

    public EventType getType() {
        return mEventType;
    }

    public String getMessage() {
        return mEventMessage;
    }

    public Date getDate() {
        return mDate;
    }

    public RamblerLocation getLocation() {
        return mLocation;
    }

    public String getPictureUrl() {
        return mPictureUrl;
    }

    public void setPicture(Bitmap bitmap) {
        mPicture = bitmap;
    }

    public Bitmap getPicture() {
        return mPicture;
    }
}
