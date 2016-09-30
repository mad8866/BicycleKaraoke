package de.madpage.bicyclekaraoke;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.FileNotFoundException;
import java.util.Date;

/**
 * This activity shows the video content and further views
 */
public class MainActivity extends HidableActivity {

    private VideoView mVideoView;
    private FrameLayout mVideoFrame;
    private ProgressBar mProgressBar;
    private VideoFinder videoFinder;
    private int UI_UPDATE_FREQENCY;
    private int DESIRED_SPEED;
    private int INITIALIZATION_PERIOD;
    private int MAXIMUM_SPEED;
    private TextView tv_status;
    private TextView mVideoOverlay;
    private MyScaleView mRulerView;
    private SoundMeter soundmeter = new SoundMeter(this);
    private Handler uiUpdateHandler = new Handler();
    private int[] VARIABLE_DESIRED_SPEED = null;

    AlertDialog.Builder alertDialogBuilder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        MAXIMUM_SPEED = sharedPrefs.getInt("pref_key_maximum_speed", 30);

        mRulerView = (MyScaleView) findViewById(R.id.my_scale);
        mRulerView.setMaximumSpeed(MAXIMUM_SPEED);

        alertDialogBuilder = new AlertDialog.Builder(this);
        videoFinder = new VideoFinder(getContentResolver());
        mVideoOverlay = (TextView) findViewById(R.id.videoCoverOverlay);
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer vmp) {
                stopPlayback();
            }
        });
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            private long lastClickTime = new Date().getTime();
            public boolean onTouch(View v, MotionEvent event) {
                long currentClickTime = new Date().getTime();
                long diff = currentClickTime - lastClickTime;
                if (diff > 50) // anti bounce
                {
                    pauseOrContinuePlayback();
                    return true;
                }
                return false;
            }
        });
        mVideoFrame = (FrameLayout) findViewById(R.id.videoFrame);
        mVideoFrame.setAlpha(0);

        final Button start_button = (Button) findViewById(R.id.start_button);
        final Button stop_button = (Button) findViewById(R.id.stop_button);

        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPlayback();
            }
        });
        start_button.setOnTouchListener(mDelayHideTouchListener);

        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopPlayback();
            }
        });


        tv_status = (TextView) findViewById(R.id.tv_status);
        tv_status.setText("Waiting for initialization");

        uiUpdateHandler.post(uiUpdateHandlerCode);

        if (BuildConfig.DEBUG) {
            // Start imideately
            startPlayback();
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(100);
        resetVideoVisibility();

    }

    /**
     * stop and hide video, soundmeter
     */
    private void stopPlayback() {
        mVideoView.stopPlayback();
        mVideoFrame.setAlpha(0);
        soundmeter.stop();
    }

    /**
     * Either interrupts playback or continues at the interrupded location
     */
    private void pauseOrContinuePlayback() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        } else {
            if (soundmeter.isStarted()) {
                mVideoView.start();
            }
        }
    }

    /**
     * Unhide video, start video and soundmeter
     */
    private void startPlayback() {
        // load user preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        UI_UPDATE_FREQENCY = sharedPrefs.getInt("pref_key_tacho_refresh_frequency", 20);
        DESIRED_SPEED = sharedPrefs.getInt("pref_key_desired_speed", 10);
        INITIALIZATION_PERIOD = sharedPrefs.getInt("pref_key_initialization_period", 18);
        MAXIMUM_SPEED = sharedPrefs.getInt("pref_key_maximum_speed", 30);
        mRulerView.setMaximumSpeed(MAXIMUM_SPEED);
        String variableDesiredSpeed = sharedPrefs.getString("pref_key_variable_desired_speed", "");
        //boolean forceWiredHeadset = sharedPrefs.getBoolean("pref_key_force_wired_headset", true);

        // get speed array from splitted string
        VARIABLE_DESIRED_SPEED = null;
        if (variableDesiredSpeed != null && !variableDesiredSpeed.isEmpty()) {
            String[] speedStrings = variableDesiredSpeed.split(",");

            try {
                VARIABLE_DESIRED_SPEED = new int[speedStrings.length];
                for (int i = 0; i < speedStrings.length; i++) {
                    VARIABLE_DESIRED_SPEED[i] = Integer.parseInt(speedStrings[i]);
                }
            } catch (Exception ex) {
                VARIABLE_DESIRED_SPEED = null;
            }
        }

        try {
            //forceWiredHeadset(forceWiredHeadset);
            String videoFile = videoFinder.randomVideo(true);
            mVideoView.setVideoPath(videoFile);
            mVideoView.start();
            mVideoFrame.setAlpha(1);
            soundmeter.start();
            resetVideoVisibility();
         } catch (FileNotFoundException e) {
            AlertDialog ad = alertDialogBuilder.create();
            ad.setMessage(e.getMessage());
            ad.show();
        }
    }

    private void forceWiredHeadset(boolean enabled) {
        if (enabled) {
            AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //if(manager.isWiredHeadsetOn())
            //{
            manager.setWiredHeadsetOn(true);
            manager.setRouting(AudioManager.MODE_CURRENT, AudioManager.ROUTE_SPEAKER, AudioManager.ROUTE_ALL);
            manager.setMode(AudioManager.MODE_CURRENT);
            //}
        }
    }

    private Runnable uiUpdateHandlerCode = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            uiUpdateHandler.postDelayed(uiUpdateHandlerCode, 1000 / UI_UPDATE_FREQENCY);

        }
    };

    private void setVariableDesiredSpeed(int progress) {
        if (VARIABLE_DESIRED_SPEED != null) {
            if (progress == 0 || progress == 100) {
                DESIRED_SPEED = VARIABLE_DESIRED_SPEED[0];
            } else {
                int index = (VARIABLE_DESIRED_SPEED.length * progress)/100;
                DESIRED_SPEED = VARIABLE_DESIRED_SPEED[index];
            }
        }
    }

    protected void refreshUi() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        int current = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();

        try {
            int progress = (int) (current * 100 / duration);
            mProgressBar.setProgress(progress);
            setVariableDesiredSpeed(progress);
        } catch (Exception e) {
        }

        String currentStatus;

        Double peaksPerSecond = soundmeter.getPeaksPerSecondRollingAverage();

        StringBuilder sb = new StringBuilder();
        sb.append("Loudness:\n")
                .append(String.format("%1$.2f",soundmeter.getLoudness()))
                .append("\n\nDu fährst:\n")
                .append(String.format("%1$.2f",peaksPerSecond*10.0))
                .append("\n\nZiel Geschwindigkeit:\n")
                .append(DESIRED_SPEED * 10)
                .append("\n\nPeaks:\n")
                .append(soundmeter.peaksToString())
        ;
        currentStatus = sb.toString();

        boolean isInInitializingPhase = (current/1000) < INITIALIZATION_PERIOD;

        setVideoVisibility(peaksPerSecond / DESIRED_SPEED, isInInitializingPhase);
        mRulerView.setCurrentSpeedPoint((int)(peaksPerSecond*10.0));
        mRulerView.setDesiredSpeedPoint(DESIRED_SPEED*10);

        tv_status.setText(currentStatus);
    }

    /**
     * You will have best visibility if factor == 1.0
     * @param factor
     */
    private void setVideoVisibility(double factor, boolean isInInitializingPhase) {
        double alphaOfCoverOverlay = 0.0;
        if (isInInitializingPhase) {
            alphaOfCoverOverlay = 0.0;
            mVideoOverlay.setTextColor(Color.WHITE);
        } else {
            if (factor <= 1f) {
                // fade to black
                alphaOfCoverOverlay = 1.0 - Math.pow(factor, 16);
                mVideoOverlay.setBackgroundColor(Color.BLACK);
                mVideoOverlay.setTextColor(Color.WHITE);
            } else {
                // fade to white
                alphaOfCoverOverlay = 1.0 - Math.pow(factor - 2, 16);
                mVideoOverlay.setBackgroundColor(Color.WHITE);
                mVideoOverlay.setTextColor(Color.BLACK);
            }
        }

        // insert text in video frame
        StringBuilder videoOverlayText = new StringBuilder();
        if (isInInitializingPhase) {
            videoOverlayText.append("Gleich gehts los! Mach dich bereit!\n");
        }
        if (factor < 0.8f) {
            videoOverlayText.append("FAHR SCHNELLER!!!");
        } else if (factor > 1.2f) {
            videoOverlayText.append("DU BIST ZU SCHNELL!!!");
        } else {
            // speed ok
        }
        mVideoOverlay.setText(videoOverlayText.toString());
        mVideoOverlay.setAlpha((float)alphaOfCoverOverlay);
    }

    private void resetVideoVisibility() {
        setVideoVisibility(1.0, true);
    }
}
