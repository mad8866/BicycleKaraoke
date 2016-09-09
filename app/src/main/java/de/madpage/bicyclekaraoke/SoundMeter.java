package de.madpage.bicyclekaraoke;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.util.CircularArray;
import android.util.Log;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.complex.Complex;

import java.nio.DoubleBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Mad on 18.06.2016.
 */
public class SoundMeter {

    private int RAW_SAMPLE_FREQUENCY = 44100;
    private int SIGNAL_UPDATE_FREQENCY = 1000;
    //private final int LOUDNESS_WINDOW_SIZE = 10;
    private int PEAK_WINDOW = 1000;
    private int LOUDNESS_THRESHOLD_AMP = 300;
    private final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_MULTIPLYER = 4;
    private final MainActivity mainActivity;

    private int bufferSize;
    AudioRecord audioRecord;
    //private double [] loudness_window = new double[LOUDNESS_WINDOW_SIZE];
    private int rotating_loudness_pointer = 0;
    private double loudness;
    private boolean isStarted = false;
    private double currentPeaksPerSecond = 0.0;
    private double peaksPerSecondRollingAverage = 0.0;
    private ArrayDeque<Long> peaks = new ArrayDeque<Long>();
    private ArrayDeque<Double> peaksRollingAverage = new ArrayDeque<Double>();

    private ArrayDeque<Double> loudnessHistory = null;

    private int getOneSecondBufferSize() {
        return RAW_SAMPLE_FREQUENCY * 16; // mono, 16bit
    }

    private int getBufferSizePerUpdate() {
        return RAW_SAMPLE_FREQUENCY * 16 / SIGNAL_UPDATE_FREQENCY; // mono, 16bit
    }

    public double getPeaksPerSecondRollingAverage() {
        return peaksPerSecondRollingAverage;
    }

    public SoundMeter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public boolean isStarted() {
        return isStarted;
    }

    Handler signalUpdateHandler = new Handler();

    public void start() {
        loudnessHistory = new ArrayDeque<Double>();


        // load user preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String samplingFrequency = sharedPrefs.getString("pref_key_raw_update_freq", "44100");
        SIGNAL_UPDATE_FREQENCY = sharedPrefs.getInt("pref_key_signal_calc_frequency", 1000);
        PEAK_WINDOW = sharedPrefs.getInt("pref_key_peak_window_size", 1000);
        LOUDNESS_THRESHOLD_AMP = sharedPrefs.getInt("pref_key_loudnes_threshold", 300);
        try {
            RAW_SAMPLE_FREQUENCY = Integer.parseInt(samplingFrequency);
        }catch (NumberFormatException ex) {

        }
        
        try {
            // Create a new AudioRecord object to record the audio.
            bufferSize = BUFFER_MULTIPLYER * AudioRecord.getMinBufferSize(RAW_SAMPLE_FREQUENCY, channelConfiguration, audioEncoding);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RAW_SAMPLE_FREQUENCY, channelConfiguration, audioEncoding, bufferSize);
            audioRecord.startRecording();
            isStarted = true;

        } catch (Throwable t) {
            Log.e("AudioRecord", "Recording Failed");
        }

        if (signalUpdateHandler != null) {
            signalUpdateHandler.post(signalUpdateHandlerCode);
        }
    }

    public void stop() {
        audioRecord.stop();
    }

    public double getLoudness() {
        return loudness;
    }

    private Runnable signalUpdateHandlerCode = new Runnable() {
        @Override
        public void run() {
            if (isStarted()) {
                if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED
                        && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    refreshSignal();
                }
            }
            signalUpdateHandler.postDelayed(signalUpdateHandlerCode, 1000 / SIGNAL_UPDATE_FREQENCY);
        }
    };


    protected void refreshSignal() {
        // reading as much signal from audioRecord as available (since last read)
        short[] audioData = new short[getBufferSizePerUpdate()*2];
        int bytesRead = audioRecord.read(audioData, 0, getBufferSizePerUpdate());
        loudness = rootMeanSquared(audioData, bytesRead);

        loudnessHistory.addFirst(loudness);
        while (loudnessHistory.size() > SIGNAL_UPDATE_FREQENCY) {
            // store exactly one second
            loudnessHistory.removeLast();
        }

        double[] arrayLoudness = getDoubleArray(loudnessHistory);
        double average = StatUtils.mean(arrayLoudness);
        double variance = StatUtils.variance(arrayLoudness, average);
        double stdev = Math.sqrt(variance);
        long currentTime = new Date().getTime();
        boolean peakWasRemovedOrAdded = false;

        if (loudness> average + stdev && loudness > LOUDNESS_THRESHOLD_AMP) {
            // add peak to list
            peaks.addFirst(currentTime);
            peakWasRemovedOrAdded = true;
        }
        while (peaks.size() > 0 && peaks.getLast().longValue() + PEAK_WINDOW < currentTime) {
            peaks.removeLast();
            peakWasRemovedOrAdded = true;
        }

        if (peaks.size() == 0) {
            peaksPerSecondRollingAverage = 0.0;
        }else if(peakWasRemovedOrAdded) {
            // determine current peaks per second
            if (peaks.size() > 2) {
                double diff = peaks.getFirst().longValue() - peaks.getLast().longValue();
                double averageDistance = diff / (double) peaks.size();
                currentPeaksPerSecond = 1000.0 / averageDistance;
            }

            // update rolling average
            peaksRollingAverage.addFirst(currentPeaksPerSecond);
            while (peaksRollingAverage.size() > 10) {
                peaksRollingAverage.removeLast();
            }
            double sum = 0.0;
            Double[] arrayPeaks = peaksRollingAverage.toArray(new Double[0]);
            for (int i = 0; i < arrayPeaks.length; i++) {
                sum += arrayPeaks[i];
            }
            peaksPerSecondRollingAverage = sum / arrayPeaks.length;
        }


    }

    private double[] getDoubleArray(ArrayDeque<Double> deque) {
        int size = deque.size();
        double[] result = new double[size];
        Double[] input = deque.toArray(new Double[0]);
        for (int i = 0; i < size; i++ ) {
            result[i] = (double) input[i];
        }
        return result;
    }


    //private void setLoudnessStep(double loudness) {
    //    loudness_window[rotating_loudness_pointer]=loudness;
    //    rotating_loudness_pointer = (rotating_loudness_pointer+1)% LOUDNESS_WINDOW_SIZE;
    //}

    public boolean isLoudEnough() {
        //int noOfMinorLoudness = 0;
        //for ( int i = 0; i < loudness_window.length; i++) {
        //    if (loudness_window[i]<LOUDNESS_THRESHOLD_AMP) {
        //        noOfMinorLoudness++;
        //    }
        //}

        //return noOfMinorLoudness < LOUDNESS_THRESHOLD_NR;
        return true;
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
