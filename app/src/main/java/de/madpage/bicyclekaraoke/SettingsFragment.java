package de.madpage.bicyclekaraoke;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Mad on 09.08.2016.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs;
    Activity context;
    Preference myFrequencyList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs);

        context = getActivity();
        if (context == null){
            Log.e("error", "context is null");
        }

        /* Set the default values by reading the "android:defaultValue"
           attributes from each preference at the 'activity_prefs.xml' file. */
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        myFrequencyList = (Preference) findPreference("pref_key_raw_update_freq");
        prefs.registerOnSharedPreferenceChangeListener(this);

        Preference button = (Preference) findPreference("pref_key_general_exitlink");
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    context.finish();
                    return true;
                }
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("pref_key_raw_update_freq")) {
            String freq = sharedPreferences.getString("pref_key_raw_update_freq", "8k Hz");
            if (freq.equals("8000")) {
                freq = "8k Hz";
            } else if (freq.equals("16000")) {
                freq = "16k Hz";
            } else if (freq.equals("22050")) {
                freq = "22.05k Hz";
            } else if (freq.equals("44100")) {
                freq = "44.1k Hz";
            } else if (freq.equals("48000")) {
                freq = "48k Hz";
            } else {
                freq = "8k Hz";
            }
            myFrequencyList.setSummary(freq);
        } //else if (key.equals("pref_key_reset_all")) {
            //boolean a = sharedPreferences.getBoolean("pref_key_reset_all", true);
            //PreferenceManager.setDefaultValues(context, R.xml.prefs, true);
            //boolean b = sharedPreferences.getBoolean("pref_key_reset_all", true);
        //}
    }
}