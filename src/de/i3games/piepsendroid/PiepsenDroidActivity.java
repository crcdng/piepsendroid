package de.i3games.piepsendroid;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;
import org.puredata.core.utils.PdDispatcher;

import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PiepsenDroidActivity extends Activity {

	protected static final String TAG = "PIEPSENDROID";
	private static final float DEFAULT_FREQUENCY = 440; // hello frequency
	private static final int DEFAULT_MEMORY_SIZE = 10; // hello frequency

	private TextView mTvReceivePitch;
	private TextView mTvReceiveAmplitude;
	private TextView mTvReceiveAttack;
	private TextView mTvReceivePitchCentral;
	private TextView mTvReceiveAmplitudeCentral;
	private TextView mTvReceiveAmplitudeDeviation;
	private TextView mTvReceivePitchDeviation;
	private Button mBtnPieps;
	private View mLyDiagnostic;
	private View mLyMain;

	private PdDispatcher mPdDispatcher;
	private PdService mPdService = null;

	private float mStartFrequency;
	private int mAttack = 0;
	private boolean mStopServiceOnExit = false;
	private int mAmplitudesSize = DEFAULT_MEMORY_SIZE;
	private int mFrequenciesSize = DEFAULT_MEMORY_SIZE;
	private Memory mAmplitudes = new Memory(mAmplitudesSize);
	private Memory mFrequencies = new Memory(mFrequenciesSize);;

	private float mMinFrequency;
	private float mMaxFrequency;
	private float mMinAmplitude;
	private float mMaxAmplitude;

	private final ServiceConnection mPdConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {

			// connect to the audio layer
			mPdService = ((PdService.PdBinder) service).getService();
			int sampleRate = AudioParameters.suggestSampleRate();
			Log.d(TAG, "Sample Rate: " + sampleRate);

			try {

				mPdService.initAudio(sampleRate, 1, 2, 10.0f);
				mPdService.startAudio();

				// register handlers for frequency, amplitude and attack to
				// receive messages from the patch
				mPdDispatcher = new PdUiDispatcher();

				mPdDispatcher.addListener("pitch", new PdListener.Adapter() {

					public void receiveFloat(String source, float value) {
						Log.d(TAG, "pitch: " + value);
						addFrequency(value);
						mTvReceivePitch.setText(String.valueOf((int) value));
						mTvReceivePitchCentral.setText(String
								.valueOf((int) getFrequencyCentral()));
						mTvReceivePitchDeviation.setText(String
								.valueOf((int) getFrequencyDeviation()));

						setSoundColor(getFrequencyCentral(), getAmplitudeCentral());
					}

				});

				mPdDispatcher.addListener("amplitude",
						new PdListener.Adapter() {

							public void receiveFloat(String source, float value) {
								Log.d(TAG, "amplitude: " + value);
								addAmplitude(value);

								mTvReceiveAmplitude.setText(String
										.valueOf((int) value));
								mTvReceiveAmplitudeCentral.setText(String
										.valueOf((int) getAmplitudeCentral()));
								mTvReceiveAmplitudeDeviation.setText(String
										.valueOf((int) getAmplitudeDeviation()));

							}

						});

				mPdDispatcher.addListener("attack", new PdListener.Adapter() {

					public void receiveBang(String source) {
						Log.d(TAG, "attack!: ");
						mAttack = mAttack + 1;
						mTvReceiveAttack.setText(String.valueOf(mAttack));
					}

				});

				PdBase.setReceiver(mPdDispatcher);

				// access the zipped patch stored in /res/raw
				File dir = getFilesDir();
				IoUtils.extractZipResource(
						getResources().openRawResource(R.raw.patch), dir, true);
				File patchFile = new File(dir, "patch.pd");
				PdBase.openPatch(patchFile.getAbsolutePath());

			} catch (IOException e) {

				// get us out
				Log.e(TAG, "Exception connecting to libpd: " + e.getMessage());
				Toast.makeText(getApplicationContext(),
						getString(R.string.shutdown), Toast.LENGTH_LONG).show();
				finish(); // not recommended :-/

			}

		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub

		}
	};

	// android life cycle and menu
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main);

		mLyDiagnostic = findViewById(R.id.layout_diagnostic);
		mLyMain = findViewById(R.id.layout_main);
		
		mTvReceivePitch = (TextView) findViewById(R.id.pitch);
		mTvReceivePitchCentral = (TextView) findViewById(R.id.pitchCentralValue);
		mTvReceivePitchDeviation = (TextView) findViewById(R.id.pitchDeviationValue);
		mTvReceiveAmplitude = (TextView) findViewById(R.id.amplitude);
		mTvReceiveAmplitudeCentral = (TextView) findViewById(R.id.amplitudeCentralValue);
		mTvReceiveAmplitudeDeviation = (TextView) findViewById(R.id.amplitudeDeviationValue);
		mTvReceiveAttack = (TextView) findViewById(R.id.attack);

		mBtnPieps = (Button) findViewById(R.id.pieps);
		mBtnPieps.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				triggerPieps(mFrequencies.getAverage());

			}
		});

		bindService(new Intent(this, PdService.class), mPdConnection,
				BIND_AUTO_CREATE);

	}

	@Override
	protected void onResume() {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);

		try {
			mStartFrequency = Float.parseFloat(sharedPref.getString(
					SettingsActivity.PREFS_AUDIO_FREQUENCY, ""));
			
			mMinFrequency = Float.parseFloat(sharedPref.getString(
					SettingsActivity.PREFS_AUDIO_FREQUENCY_MIN, ""));
			mMaxFrequency = Float.parseFloat(sharedPref.getString(
					SettingsActivity.PREFS_AUDIO_FREQUENCY_MAX, ""));
			mMinAmplitude = Float.parseFloat(sharedPref.getString(
					SettingsActivity.PREFS_AUDIO_AMPLITUDE_MIN, ""));
			mMaxAmplitude = Float.parseFloat(sharedPref.getString(
					SettingsActivity.PREFS_AUDIO_AMPLITUDE_MAX, ""));
			
			
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Error in Setting frequency: " + e.getMessage());
			mStartFrequency = DEFAULT_FREQUENCY;
		}

		displayDiagnosticUI(sharedPref.getBoolean(
				SettingsActivity.PREFS_UI_DIAGNOSTIC, false));

		mStopServiceOnExit = sharedPref.getBoolean(
				SettingsActivity.PREFS_GENERAL_STOPSERVICEONEXIT, false);

		super.onResume();
	}

	private void displayDiagnosticUI(boolean on) {
		if (on) {
			mLyDiagnostic.setVisibility(View.VISIBLE);
		} else {
			mLyDiagnostic.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (mStopServiceOnExit) {
			unbindService(mPdConnection);
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (!mStopServiceOnExit) {
		unbindService(mPdConnection);  // TODO unbindService is not reentrant :( Service.isRunning() ?
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// send messages to the patch
	private void triggerPieps(float n) {
		PdBase.sendFloat("frequency", n);
		PdBase.sendBang("trigger");
		Log.d(TAG, "Piep frequency: " + n);

	}

	private void addFrequency(float value) {
		mFrequencies.add(value);
	}

	private void addAmplitude(float value) {
		mAmplitudes.add(value);
	}

	private float getAmplitudeCentral() {
		return mAmplitudes.getAverage();
	}

	private float getAmplitudeDeviation() {
		return mAmplitudes.getVariance();
	}

	private float getFrequencyCentral() {
		return mFrequencies.getAverage();
	}

	private float getFrequencyDeviation() {
		return mFrequencies.getVariance();
	}

	private void setSoundColor(float frequency, float amplitude) {
		
		// hue, saturation, brightness
		float hsv[] = {frequency * 360.0f / (mMaxFrequency- mMinFrequency),
				amplitude * 1.0f / (mMaxAmplitude - mMinAmplitude), 
				amplitude * 1.0f / (mMaxAmplitude - mMinAmplitude) };
			
		mLyMain.setBackgroundColor( Color.HSVToColor(hsv)); // packed int, e.g.  0x88ff0000

	}
	
	
	
}
