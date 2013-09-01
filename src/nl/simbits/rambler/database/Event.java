package nl.simbits.rambler.database;


import java.util.Date;

import nl.simbits.rambler.RamblerLocation;

public class Event {
    public enum EventType {
        FACEBOOK,
        TWITTER,
        MAP,
        STEP
    }

    private String mEventMessage;
    private EventType mEventType;
    private RamblerLocation mLocation;
    private Date mDate;

    public Event(EventType type, String message, RamblerLocation location) {
        mEventMessage = message;
        mEventType = type;
        mLocation = location;
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
}
