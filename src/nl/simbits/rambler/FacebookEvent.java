package nl.simbits.rambler;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONObject;


public class FacebookEvent 
{
    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT);
    public static final String ISO8601_FORMAT_TZ = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static SimpleDateFormat dateFormatTz = new SimpleDateFormat(ISO8601_FORMAT_TZ);

    private String mGraphId;
    private String mName;
    private Date mStartDate;
    private Date mEndDate;
    private Date mUpdatedTime;
    private boolean mAttending;
    private boolean mSecret;
    private boolean mVisited;
    private Location mLocation;

    private void init(String graphId, String name, Date startDate, Date endDate)
    {
        this.mGraphId = graphId;
        this.mName = name; 
        this.mStartDate = startDate;
        this.mEndDate = endDate;
        this.mAttending = false;
        this.mSecret = false;
        this.mVisited = false;
    }

    private void init(JSONObject event)
        throws java.text.ParseException, org.json.JSONException
    {
        String name = event.getString("name");
        String graphId = event.getString("id");
        Date startDate = dateFormat.parse(event.getString("start_time"));                
        Date endDate = dateFormat.parse(event.getString("end_time"));

        init(graphId, name, startDate, endDate);

        try {
            String rsvp = event.getString("rsvp_status");
            if (rsvp.equals("attending")) {
                this.mAttending = true;
            }
        } catch (org.json.JSONException e) {
            this.mAttending = false;
        }

        try {
            String privacy = event.getString("privacy");
            if (privacy.equals("SECRET")) {
                this.mSecret = true;
            }
        } catch (org.json.JSONException e) {
            this.mSecret = false;
        }

        try {
            JSONObject venue = event.getJSONObject("venue");
            double lat = venue.getDouble("latitude");
            double lon = venue.getDouble("longitude");
            setLocation(lat, lon);
        } catch (org.json.JSONException e) {
            this.mLocation = null;
        }

        try {
            mUpdatedTime = dateFormatTz.parse(event.getString("updated_time"));
        } catch (org.json.JSONException e) {
            this.mUpdatedTime = new Date(0);
        }

    }

    public FacebookEvent(JSONObject event) throws java.text.ParseException, org.json.JSONException
    {
        init(event);
    }

    public String getName() 
    {
        return this.mName;
    }

    public void setLocation(double lat, double lon)
    {
        Location l = new Location("FacebookEvent");
        l.setLatitude(lat);
        l.setLongitude(lon);
        this.mLocation = l;
    }

    public boolean hasLocation()
    {
        return this.mLocation != null;
    }

    public Location getLocation()
    {
        return this.mLocation;
    }

    public String getGraphId()
    {
        return mGraphId;
    }

    public void setAttending(boolean attending)
    {
        this.mAttending = attending;
    }

    public boolean isAttending()
    {
        return this.mAttending;
    }

    public void setSecret(boolean secret)
    {
        this.mSecret = secret;
    }

    public boolean isSecret()
    {
        return this.mSecret;
    }

    public boolean visited()
    {
        return this.mVisited;
    }

    public void setVisited(boolean visit)
    {
        this.mVisited = visit;
    }

    public Date getStartDate()
    {
        return this.mStartDate;
    }

    public Date getEndDate()
    {
        return this.mEndDate;
    }

    public Date getUpdatedTime()
    {
        return this.mUpdatedTime;
    }

    public boolean isOngoing()
    {
        Date now = new Date();
        return now.after(mStartDate) && now.before(mEndDate);
    }

    public boolean isBefore(FacebookEvent event)
    {
        return mUpdatedTime.compareTo(event.getUpdatedTime()) < 0;
    }

    public boolean isAfterOrEqualTo(FacebookEvent event)
    {
        return mUpdatedTime.compareTo(event.getUpdatedTime()) >= 0;
    }
    
    @Override
    public String toString()
    {
        String msg; 

        msg = this.getClass().getName() + ": " + mName + " [" + mGraphId + "]";
        msg += ", start date: " + getStartDate().toString();
        msg += ", end date: " + getEndDate().toString();
        msg += ", last updated: " + getUpdatedTime().toString();

        if (hasLocation()) {
            msg += ", location: " + getLocation();
        } else {
            msg += ", unknown venue";
        }

        msg += ", attending: " + ((isAttending()) ? "yes" : "no");
        msg += ", ongoing: " + ((isOngoing()) ? "yes" : "no");
        msg += ", secret: " + ((isSecret()) ? "yes" : "no");
        msg += ", visited: " + ((visited()) ? "yes" : "no");

        return msg;
    }

    @Override 
    public int hashCode() 
    {
        return new HashCodeBuilder(81, 28).
            append(mGraphId).
            toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;

        if (obj == this)
            return true;

        if (obj.getClass() != this.getClass())
            return false;

        FacebookEvent event = (FacebookEvent)obj;

        return new EqualsBuilder().
            append(mGraphId, event.getGraphId()).
            isEquals();
    }
}
