package nl.simbits.rambler;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.util.List;
import java.util.Locale;

public class RamblerLocation extends Location
{
    private static final int MAX_RETURNED_ADDRESSES = 1;
    private Context mContext;
    private Address mAddress;

    public RamblerLocation(Context context, Location location)
    {
        super(location);
        this.mContext = context;
        this.mAddress = null;
    }

    public void retrieveAddresses() throws java.io.IOException
    {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(mContext);
        
        addresses = geocoder.getFromLocation(getLatitude(), 
                                             getLongitude(), 
                                             RamblerLocation.MAX_RETURNED_ADDRESSES);
        if (! addresses.isEmpty()) {
            mAddress = addresses.get(0);
        } else {
            mAddress = new Address(Locale.getDefault());
        }
    }

    public Address getAddress() throws java.io.IOException
    {
        if (mAddress == null) {
            retrieveAddresses();
        }
        return mAddress;
    }

    public String getAddressLine()
    {
        Address a = null;

        try {
            a = getAddress();
        } catch (java.io.IOException e) {
            return "";
        }

        String line = a.getAddressLine(0);

        if (line == null)
            return "";

        int i = 1;
        while (i<5) {
            String l = a.getAddressLine(i++);
            if (l == null)
                break;
            line += ", " + l;
        }

        return line;
    }
}
