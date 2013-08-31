package nl.simbits.rambler.holo;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import nl.simbits.rambler.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs);
    }
}
