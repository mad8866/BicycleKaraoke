package de.madpage.bicyclekaraoke;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;
import android.widget.VideoView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private VideoView mVideoView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mVideoView = (VideoView) findViewById(R.id.videoView);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        final Button start_button = (Button) findViewById(R.id.start_button);
        final Button stop_button = (Button) findViewById(R.id.stop_button);
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        start_button.setOnTouchListener(mDelayHideTouchListener);





        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mVideoView.setVideoPath("/sdcard/video/hierKommt.mp4");
                mVideoView.start();
                mVideoView.setAlpha(1);
                soundmeter.start();
            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mVideoView.stopPlayback();
                mVideoView.setAlpha(0);
                soundmeter.stop();
            }
        });


        tv_status = (TextView) findViewById(R.id.tv_status);
        tv_status.setText("Waiting for initialization");

        uiUpdateHandler.post(uiUpdateHandlerCode);

        if (BuildConfig.DEBUG) {
            // do something for a debug build
            mVideoView.setVideoPath("/sdcard/video/hierKommt.mp4");
            mVideoView.start();
            mVideoView.setAlpha(1);
            soundmeter.start();
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(100);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        if (BuildConfig.DEBUG || !BuildConfig.DEBUG) {
            //disable hide
            return;
        }

        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public final int UI_UPDATE_FREQENCY = 10;
    private TextView tv_status;
    private SoundMeter soundmeter = new SoundMeter();
    private Handler uiUpdateHandler = new Handler();
    private Runnable uiUpdateHandlerCode = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            uiUpdateHandler.postDelayed(uiUpdateHandlerCode, 1000 / UI_UPDATE_FREQENCY);


            int current = mVideoView.getCurrentPosition();
            int duration = mVideoView.getDuration();

            try {
                mProgressBar.setProgress((int) (current * 100 / duration));

            } catch (Exception e) {
            }
        }
    };

    protected void refreshUi() {
        String currentStatus = "HEY!\nMake some noise :-P";

        if (soundmeter.getLoudness() > soundmeter.LOUDNESS_THRESHOLD_AMP) {
            Log.d("zeroCrossings", "calc_frequency:\n" + soundmeter.getCalc_frequency() + "\n\n\nloudness:\n" + soundmeter.getLoudness());

            if (soundmeter.isLoudEnough()) {
                currentStatus = ("RAW_SAMPLE_FREQUENCY:\n" + soundmeter.getCalc_frequency() + "\n\n\nloudness:\n" +soundmeter.getLoudness());
            }
        }

        tv_status.setText(currentStatus);
    }
}
