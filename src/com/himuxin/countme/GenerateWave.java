package com.himuxin.countme;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

public class GenerateWave {
	private final static double duration = 0.1; // Second
	private final static int sampleRate = 44100;
	private final static int numSamples = (int) (duration * sampleRate);
	private final static double sample[] = new double[numSamples];
	private static double freqOfTone = 12000; // Hz
	private final static byte generatedSnd[] = new byte[2 * numSamples];
	private final static Handler handler = new Handler();
	private static Object lock = new Object();

	public static void send(final int rate) {
		// 在新线程中发声，并且用互斥锁保证只有一个线程占用扬声器
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					freqOfTone = rate;
					genTone();
					playSound();
					try {
						Thread.sleep((long) (duration * 1000));
					} catch (InterruptedException e) {
					}
				}
			}
		};
		handler.post(runnable);
	}

	static void genTone() {
		// fill out the array
		for (int i = 0; i < numSamples; ++i) {
			sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
		}

		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.
		int idx = 0;
		for (final double dVal : sample) {
			// scale to maximum amplitude
			final short val = (short) ((dVal * 32767));
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
		}
	}

	static void playSound() {
		final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, numSamples,
				AudioTrack.MODE_STATIC);
		audioTrack.write(generatedSnd, 0, generatedSnd.length);
		audioTrack.play();
	}
}
