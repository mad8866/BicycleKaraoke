package de.madpage.bicyclekaraoke;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
    public final int UI_UPDATE_FREQENCY = 10;
    private TextView tv_status;
    private SoundMeter soundmeter = new SoundMeter();
    private Handler uiUpdateHandler = new Handler();

    //TODO final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoFinder = new VideoFinder(getContentResolver());
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
        try {
            String videoFile = videoFinder.randomVideo(true);
            mVideoView.setVideoPath(videoFile);
            mVideoView.start();
            mVideoFrame.setAlpha(1);
            soundmeter.start();
         } catch (FileNotFoundException e) {
            //AlertDialog ad = alertDialogBuilder.create();
            //ad.setMessage(e.getMessage());
            //ad.show();
        }
    }

    private Runnable uiUpdateHandlerCode = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            uiUpdateHandler.postDelayed(uiUpdateHandlerCode, 1000 / UI_UPDATE_FREQENCY);

        }
    };

    protected void refreshUi() {
        int current = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();

        try {
            mProgressBar.setProgress((int) (current * 100 / duration));
        } catch (Exception e) {
        }
        /*if (TODO http://stackoverflow.com/questions/22931833/oncompletionlistener-in-videoview) {
            mVideoView.setAlpha(0);
            soundmeter.stop();

        }*/



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
