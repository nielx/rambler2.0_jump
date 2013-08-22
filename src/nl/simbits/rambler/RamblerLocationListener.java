package nl.simbits.rambler;

import android.location.Location;

public interface RamblerLocationListener
{
    abstract void onLocationChanged(Location location);
    abstract void onReachedDestination(Location location);
}
