package de.i3games.piepsendroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

	public static final String PREFS_AUDIO_FREQUENCY = "prefs_audio_frequency";
	public static final String PREFS_UI_DIAGNOSTIC = "prefs_ui_diagnostic";
	public static final String PREFS_GENERAL_STOPSERVICEONEXIT = "prefs_general_stopserviceonexit";
	public static final String PREFS_AUDIO_FREQUENCY_MIN = "prefs_audio_frequency_min";
	public static final String PREFS_AUDIO_FREQUENCY_MAX = "prefs_audio_frequency_max";
	public static final String PREFS_AUDIO_AMPLITUDE_MIN = "prefs_audio_amplitude_min";
	public static final String PREFS_AUDIO_AMPLITUDE_MAX = "prefs_audio_amplitude_max";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);
	}

}
