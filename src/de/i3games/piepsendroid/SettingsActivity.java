package de.i3games.piepsendroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	public static final String PREF_KEY_FREQUENCY = "prefs_frequency";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);
	}

}
