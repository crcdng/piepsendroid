package de.i3games.piepsendroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	public static final String PREFS_AUDIO_FREQUENCY = "prefs_audio_frequency";
	public static final String PREFS_UI_DIAGNOSTIC = "prefs_ui_diagnostic";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);
	}

}
