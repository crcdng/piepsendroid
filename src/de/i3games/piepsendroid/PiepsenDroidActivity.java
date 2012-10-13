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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PiepsenDroidActivity extends Activity {

	protected static final String TAG = "PIEPSENDROID";
	private static final float DEFAULT_FREQUENCY = 440; // hello frequency
	
	private TextView mTvReceivePitch;
	private Button mBtnPieps;

	private PdDispatcher mPdDispatcher;
	private PdService mPdService = null;
	
	private float mFrequency;
	
	private final ServiceConnection mPdConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {

			// connect to the audio layer
			mPdService = ((PdService.PdBinder) service).getService();
			int sampleRate = AudioParameters.suggestSampleRate();
			Log.d(TAG, "Sample Rate: " + sampleRate);
			 
			try {
				
				mPdService.initAudio(sampleRate, 1, 2, 10.0f); 
				mPdService.startAudio();
				
				// register a handler to receive messages from the patch
				mPdDispatcher = new PdUiDispatcher();
				mPdDispatcher.addListener("pitch", new PdListener.Adapter() {
								
					public void receiveFloat(String source, float value) {
						Log.d(TAG, "pitch: " + value);
						mFrequency = value;
						mTvReceivePitch.setText(String.valueOf((int)value));
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
				Toast.makeText(getApplicationContext(), getString(R.string.shutdown), Toast.LENGTH_LONG).show();				
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
		setContentView(R.layout.main);
		
		mTvReceivePitch = (TextView) findViewById(R.id.pitch);
		mBtnPieps = (Button) findViewById(R.id.pieps);
		mBtnPieps.setOnClickListener(new OnClickListener() {			

			public void onClick(View v) {
				triggerPieps(mFrequency);
				
			}
		});
		
		bindService(new Intent(this, PdService.class), mPdConnection,
				BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		try {
			mFrequency = Float.parseFloat(sharedPref.getString(SettingsActivity.PREF_KEY_FREQUENCY, ""));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "Error in Setting frequency: " + e.getMessage());
			mFrequency = DEFAULT_FREQUENCY;
		}
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unbindService(mPdConnection);
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
	
}
