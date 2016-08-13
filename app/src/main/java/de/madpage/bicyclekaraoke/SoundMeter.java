package de.madpage.bicyclekaraoke;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by Mad on 18.06.2016.
 */
public class SoundMeter {

    public final int RAW_SAMPLE_FREQUENCY = 44100;
    public final int SIGNAL_UPDATE_FREQENCY = 1000;
    public final int LOUDNESS_WINDOW_SIZE = 10;
    public final int LOUDNESS_THRESHOLD_AMP = 300;
    public final int LOUDNESS_THRESHOLD_NR = 3;
    public final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    public final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    public final int BUFFER_MULTIPLYER = 4;

    private int bufferSize;
    AudioRecord audioRecord;
    private double [] loudness_window = new double[LOUDNESS_WINDOW_SIZE];
    private int rotating_loudness_pointer = 0;
    private double loudness;
    private double calc_frequency;

    Handler signalUpdateHandler = new Handler();

    public void start() {
        try {
            // Create a new AudioRecord object to record the audio.
            bufferSize = BUFFER_MULTIPLYER * AudioRecord.getMinBufferSize(RAW_SAMPLE_FREQUENCY, channelConfiguration, audioEncoding);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RAW_SAMPLE_FREQUENCY, channelConfiguration, audioEncoding, bufferSize);
            audioRecord.startRecording();
        } catch (Throwable t) {
            Log.e("AudioRecord", "Recording Failed");
        }

        if (signalUpdateHandler != null) {
            signalUpdateHandler.post(signalUpdateHandlerCode);
        }
    }

    public void stop() {

    }

    public double getCalc_frequency() {
        return calc_frequency;
    }

    public double getLoudness() {
        return loudness;
    }

    private Runnable signalUpdateHandlerCode = new Runnable() {
        @Override
        public void run() {
            if (audioRecord.getState()==AudioRecord.STATE_INITIALIZED && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                refreshSignal();
            } else {
                Log.d("Handler", "Waiting for Audio Initialization");
            }
            signalUpdateHandler.postDelayed(signalUpdateHandlerCode, 1000 / SIGNAL_UPDATE_FREQENCY);
        }
    };



    protected void refreshSignal() {
        short[] audioData = new short[bufferSize*2];
        int bytesRead = audioRecord.read(audioData, 0, bufferSize);
        loudness = rootMeanSquared(audioData, bytesRead);

        setLoudnessStep(loudness);

        if (loudness > LOUDNESS_THRESHOLD_AMP) {
            calc_frequency = getFrequencyByZeroCrossings(RAW_SAMPLE_FREQUENCY, audioData, bytesRead);
            Log.d("zeroCrossings", "calc_frequency:" + calc_frequency + " loudness:" +loudness);
        }

    }

    private void setLoudnessStep(double loudness) {
        loudness_window[rotating_loudness_pointer]=loudness;
        rotating_loudness_pointer = (rotating_loudness_pointer+1)% LOUDNESS_WINDOW_SIZE;
    }

    public boolean isLoudEnough() {
        int noOfMinorLoudness = 0;
        for ( int i = 0; i < loudness_window.length; i++) {
            if (loudness_window[i]<LOUDNESS_THRESHOLD_AMP) {
                noOfMinorLoudness++;
            }
        }

        return noOfMinorLoudness < LOUDNESS_THRESHOLD_NR;
    }

    /**
     * from https://github.com/gast-lib/gast-lib/blob/master/library/src/root/gast/audio/processing/ZeroCrossing.java
     */
    public static int getFrequencyByZeroCrossings(int sampleRate, short[] audioData, Integer useCustomLength)
    {
        int numSamples;
        if (useCustomLength == null) {
            numSamples = audioData.length;
        } else {
            numSamples = useCustomLength;
        }
        int numCrossing = 0;
        for (int p = 0; p < numSamples-1; p++)
        {
            if ((audioData[p] > 0 && audioData[p + 1] <= 0) ||
                    (audioData[p] < 0 && audioData[p + 1] >= 0))
            {
                numCrossing++;
            }
        }

        float numSecondsRecorded = (float)numSamples/(float)sampleRate;
        float numCycles = numCrossing/2;
        float frequency = numCycles/numSecondsRecorded;

        return (int)frequency;
    }

    private static double rootMeanSquared(short[] nums, Integer useCustomLength)
    {
        int numSamples;
        if (useCustomLength == null) {
            numSamples = nums.length;
        } else {
            numSamples = useCustomLength;
        }
        double ms = 0;
        for (int i = 0; i < numSamples; i++)
        {
            ms += nums[i] * nums[i];
        }
        ms /= nums.length;
        return Math.sqrt(ms);
    }

}
