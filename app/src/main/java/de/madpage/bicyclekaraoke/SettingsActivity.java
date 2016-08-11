package de.madpage.bicyclekaraoke;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class SettingsActivity extends PreferenceActivity {

    public final SettingsFragment settings = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, settings).commit();

    }

    private void foo()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        //String strUserName = sharedPrefs.getString("username", "NA");
        //boolean bAppUpdates = sharedPrefs.getBoolean("applicationUpdates",false);
        //String downloadType = sharedPrefs.getString("downloadType","1");

        //String msg = "Cur Values: ";
        //msg += "\n userName = " + strUserName;
        //msg += "\n bAppUpdates = " + bAppUpdates;
        //msg += "\n downloadType = " + downloadType;

        //Toast.toastShort(this, msg);
    }

}
