package de.madpage.bicyclekaraoke;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import java.util.List;

/**
 * This activity shows the video content and further views
 */
public class MainActivity extends HidableActivity {

    private VideoView mVideoView;
    private FrameLayout mVideoFrame;
    private ProgressBar mProgressBar;
    private VideoFinder videoFinder;
    private int UI_UPDATE_FREQENCY = 20;
    private int DESIRED_SPEED = 10;
    private TextView tv_status;
    private TextView mVideoOverlay;
    private SoundMeter soundmeter = new SoundMeter(this);
    private Handler uiUpdateHandler = new Handler();
    private int[] VARIABLE_DESIRED_SPEED = null;

    AlertDialog.Builder alertDialogBuilder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        String variableDesiredSpeed = sharedPrefs.getString("pref_key_variable_desired_speed", "");

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
        int current = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();

        try {
            int progress = (int) (current * 100 / duration);
            mProgressBar.setProgress(progress);
            setVariableDesiredSpeed(progress);
        } catch (Exception e) {
        }

        String currentStatus;

        // = "HEY!\nMake some noise :-P";

        //if (soundmeter.getLoudness() > soundmeter.LOUDNESS_THRESHOLD_AMP) {
        //    Log.d("zeroCrossings", "calc_frequency:\n" + soundmeter.getCalc_frequency() + "\n\n\nloudness:\n" + soundmeter.getLoudness());

        //    if (soundmeter.isLoudEnough()) {

        Double peaksPerSecond = soundmeter.getPeaksPerSecondRollingAverage();

        StringBuilder sb = new StringBuilder();
        sb.append("Loudness:\n")
                .append(soundmeter.getLoudness())
                .append("\n\nDu fährst:\n")
                .append(peaksPerSecond)
                .append("\n\nZiel Geschwindigkeit:\n")
                .append(DESIRED_SPEED)
        ;
        currentStatus = sb.toString();

        setVideoVisibility(peaksPerSecond / DESIRED_SPEED);

        //    }
        //}

        tv_status.setText(currentStatus);
    }

    /**
     * You will have best visibility if factor == 1.0
     * @param factor
     */
    private void setVideoVisibility(double factor) {
        double alphaOfCoverOverlay;
        if (factor <= 1f) {
            alphaOfCoverOverlay = 1.0 - Math.pow(factor, 16);
            mVideoOverlay.setBackgroundColor(Color.BLACK);
            mVideoOverlay.setTextColor(Color.WHITE);
        } else {
            alphaOfCoverOverlay = 1.0 - Math.pow(factor - 2, 16);
            mVideoOverlay.setBackgroundColor(Color.WHITE);
            mVideoOverlay.setTextColor(Color.BLACK);
        }
        if (factor < 0.8f) {
            mVideoOverlay.setText("FAHR SCHNELLER!!!");
        } else if (factor > 1.2f) {
            mVideoOverlay.setText("DU BIST ZU SCHNELL!!!");
        } else {
            mVideoOverlay.setText(""); // speed ok
        }
        mVideoOverlay.setAlpha((float)alphaOfCoverOverlay);
    }

    private void resetVideoVisibility() {
        setVideoVisibility(1.0);
    }
}
