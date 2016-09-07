package de.madpage.bicyclekaraoke;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mad on 18.06.2016.
 */
public class SoundMeter {

    public final int RAW_SAMPLE_FREQUENCY = 22050;
    public final int SIGNAL_UPDATE_FREQENCY = 2000;
    public final int LOUDNESS_WINDOW_SIZE = 10;
    public final int LOUDNESS_THRESHOLD_AMP = 300;
    public final int LOUDNESS_THRESHOLD_NR = 3;
    public final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    public final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    public final int BUFFER_MULTIPLYER = 4;
    private final MainActivity mainActivity;

    private int bufferSize;
    AudioRecord audioRecord;
    private double [] loudness_window = new double[LOUDNESS_WINDOW_SIZE];
    private int rotating_loudness_pointer = 0;
    private double loudness;
    private boolean isStarted = false;
    private List<FftFrequncy> calc_frequencies = new ArrayList<FftFrequncy>();
    private double mostPropableFrequency = 0.0;
    private DescriptiveStatistics mostPropableFrequencyStats;

    public SoundMeter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public boolean isStarted() {
        return isStarted;
    }

    Handler signalUpdateHandler = new Handler();

    public void start() {
        mostPropableFrequencyStats = new DescriptiveStatistics();
        mostPropableFrequencyStats.setWindowSize(20);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String samplingFrequency = sharedPrefs.getString("pref_key_raw_update_freq", "44100");
        // TODO
        
        try {
            // Create a new AudioRecord object to record the audio.
            bufferSize = BUFFER_MULTIPLYER * AudioRecord.getMinBufferSize(RAW_SAMPLE_FREQUENCY, channelConfiguration, audioEncoding);
            // increase bufferSize to next available power of two
            bufferSize = nextPowerOfTwo(bufferSize);

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

    /**
     * https://en.wikipedia.org/wiki/Power_of_two#Fast_algorithm_to_check_if_a_positive_number_is_a_power_of_two
     */
    private int nextPowerOfTwo(final int a)
    {
        int b = 1;
        while (b < a)
        {
            b = b << 1;
        }
        return b;
    }

    public void stop() {
        audioRecord.stop();
    }

    public double getMostPropableFrequency() { return mostPropableFrequency; }

    public List<FftFrequncy> getCalc_frequencies() { return calc_frequencies; }

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
        short[] audioData = new short[bufferSize*2];
        int bytesRead = audioRecord.read(audioData, 0, bufferSize);
        loudness = rootMeanSquared(audioData, bytesRead);

        // loudness filter
        setLoudnessStep(loudness);

        // calculate frequency
        //if (loudness > LOUDNESS_THRESHOLD_AMP) {
            //calc_frequency = getFrequencyByZeroCrossings(RAW_SAMPLE_FREQUENCY, audioData, bytesRead);
            //calc_frequency = getFrequencyByZeroCrossings(SIGNAL_UPDATE_FREQENCY, zeroCrossigBuffer);
            //calc_frequencies = fourierLowPassFilter(toDoubleArray(audioData, bytesRead), 500.0, (double) RAW_SAMPLE_FREQUENCY);
        calc_frequencies = calculateFrequencies(toDoubleArray(audioData, bytesRead));
        mostPropableFrequencyStats.addValue(calculateMostPropableFrequency());
        mostPropableFrequency = mostPropableFrequencyStats.getMean();
            //Log.d("zeroCrossings", "calc_frequency:" + calc_frequency + " loudness:" +loudness);
        //}

    }

    private double[] toDoubleArray(short[] audioData, int maxLen) {
        int size = (maxLen < audioData.length ? maxLen : audioData.length);
        double[] result = new double[size];
        for (int i = 0; i < size; i++ ) {
            result[i] = audioData[i];
        }
        return result;
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

    /**
     * http://archive.oreilly.com/oreillyschool/courses/data-structures-algorithms/soundFiles.html
     */
    private List<FftFrequncy> calculateFrequencies(double[] audioBuffer) {
        double[] outR = new double[audioBuffer.length];
        double[] outI = new double[audioBuffer.length];

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex resultC[] = fft.transform(audioBuffer, TransformType.FORWARD);

        double results[] = new double[outR.length];
        for (int i = 0; i < outR.length; i++) {
            results[i] = Math.sqrt(outR[i]*outR[i] + outI[i]*outI[i]);
        }
        for (int i = 0; i < resultC.length; i++) {
            double real = resultC[i].getReal();
            double imaginary = resultC[i].getImaginary();
            results[i] = Math.sqrt(real*real + imaginary*imaginary);
        }

        return processFrequencies(results, RAW_SAMPLE_FREQUENCY, audioBuffer.length, 4);
    }

    public class FftFrequncy {
        public float frequency;
        public float amplitude;
        @Override
        public String toString() {
            return new StringBuffer().append("f=").append(frequency).append(" amp=").append(amplitude).toString();
        }
    }

    private List<FftFrequncy> processFrequencies(double results[], float sampleRate, int numSamples, int sigma) {
        double average = 0;
        for (int i = 0; i < results.length; i++) {
            average += results[i];
        }
        average = average/results.length;

        double sums = 0;
        for (int i = 0; i < results.length; i++) {
            sums += (results[i]-average)*(results[i]-average);
        }

        double stdev = Math.sqrt(sums/(results.length-1));

        ArrayList<FftFrequncy> found = new ArrayList<FftFrequncy>();
        double max = Integer.MIN_VALUE;
        int maxF = -1;
        for (int f = 0; f < results.length/2; f++) {
            if (results[f] > average+sigma*stdev) {
                if (results[f] > max) {
                    max = results[f];
                    maxF = f;
                }
            } else {
                if (maxF != -1) {
                    FftFrequncy foundFrequency = new FftFrequncy();
                    foundFrequency.frequency = maxF*sampleRate/numSamples;
                    foundFrequency.amplitude = (float)results[f];
                    found.add(foundFrequency);
                    max = Integer.MIN_VALUE;
                    maxF = -1;
                }
            }
        }

        return (found);
    }

    private double calculateMostPropableFrequency() {
        List<FftFrequncy> frequencies = getCalc_frequencies();
        double result = 0;
        double truncationOnEachSide = 0.1;

        if (frequencies != null && frequencies.size() >0) {
            int firstValidPos = (int) (((double)frequencies.size()) * truncationOnEachSide);
            int lastValidPos = (int) Math.ceil((double)frequencies.size() * (1.0-truncationOnEachSide));

            //int median_pos = frequencies.size() /2;
            //FftFrequncy median = frequencies.get(median_pos);
            double max_amplitude = 0;
            double average_frequency_truncated = 0;
            double average_frequency_without_spikes = 0;
            int nr_of_frequency_without_spikes = 0;
            double sums_frequency = 0;

            // calculate maximal occuring amplitude
            // and calculate truncated average (minimal and maximal 10 % truncated)
            for (int i = firstValidPos; i < lastValidPos; i++) {
                FftFrequncy candidate = frequencies.get(i);
                average_frequency_truncated += candidate.frequency;
                if (max_amplitude<candidate.amplitude) {
                    max_amplitude = candidate.amplitude;
                }
            }
            average_frequency_truncated /= frequencies.size();

            // calculate stdev
            for (int i = firstValidPos; i < lastValidPos; i++) {
                FftFrequncy candidate = frequencies.get(i);
                sums_frequency += (candidate.frequency-average_frequency_truncated)*(candidate.frequency-average_frequency_truncated);
            }
            double stdev_frequencies = Math.sqrt(sums_frequency/(frequencies.size()-1));
            if (stdev_frequencies < 10.0 || stdev_frequencies == Double.NaN) {
                // fair enough - done
                return average_frequency_truncated;
            }

            // calculate truncated mean without spikes, where a spike is one of the following
            // * out of stdev range of average_frequency_truncated
            // * amplitude less than max_amplitude/2
            for (int i = firstValidPos; i < lastValidPos; i++) {
                FftFrequncy candidate = frequencies.get(i);
                if ((max_amplitude/2.0)<candidate.amplitude
                        && average_frequency_truncated + stdev_frequencies > candidate.frequency
                        && average_frequency_truncated - stdev_frequencies < candidate.frequency) {
                    nr_of_frequency_without_spikes += 1;
                    average_frequency_without_spikes += candidate.frequency;
                }

            }
            if (nr_of_frequency_without_spikes > 0) {
                average_frequency_without_spikes /= nr_of_frequency_without_spikes;
                return average_frequency_without_spikes;
            }else {
                return average_frequency_truncated;
            }
        }

        return result;
    }
}
