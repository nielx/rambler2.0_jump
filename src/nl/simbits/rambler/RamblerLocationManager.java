package nl.simbits.rambler;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import nl.simbits.rambler.RamblerLocationListener;

public class RamblerLocationManager implements LocationListener
{
    private LocationManager locationManager;
    private RamblerLocationListener ramblerLocationListener;
    public RamblerLocationManager(Context context)
    {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void startListening(RamblerLocationListener locationListener)
    {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 5, this); 
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5, this); 
        ramblerLocationListener = locationListener;
    }
    
    public void stopListening()
    {
        locationManager.removeUpdates(this);
        ramblerLocationListener = null;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (ramblerLocationListener != null) {
            ramblerLocationListener.onLocationChanged(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
