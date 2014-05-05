package com.himuxin.countme;

import java.util.ArrayList;
import java.lang.Short;

import android.media.AudioRecord;
import android.os.Handler;
import android.util.Log;

public class AudioProcess {
	private final int timeout = 120;
	public static final float pi = (float) 3.1415926;
	// 应该把处理前后处理后的普线都显示出来
	private ArrayList<short[]> inBuf = new ArrayList<short[]>();// 原始录入数据
	private ArrayList<int[]> outBuf = new ArrayList<int[]>();// 处理后的数据
	private boolean isRecording = false;
	private boolean[] result = new boolean[14]; // 某频率是否出现
	private String[] time_stamps = new String[14]; // 各个频率时间戳
	private static Object lock = new Object(); // 互斥锁
	public RecordThread record_thread;
	private String log = "";

	// Context mContext = this.getContext();
	// 存储读取到的数据
	// public FileOutputStream fos;
	// 上下文
	// Context mContext;
	public int frequence = 0;
	private int length = 256;

	// 启动程序
	public void start(AudioRecord audioRecord, int minBufferSize,
			Handler handler, Runnable callback, Runnable callback_log) {
		isRecording = true;
		result[0] = false;
		record_thread = new RecordThread(audioRecord, minBufferSize, handler,
				callback, callback_log);
		// new ProcessThread().start();
	}

	// 停止程序
	public void stop() {
		isRecording = false;
		inBuf.clear();
		// sfvSurfaceView;
		// drawBuf.clear();
		// outBuf.clear();
	}

	// 录音线程
	class RecordThread extends Thread {
		private AudioRecord audioRecord;
		private int minBufferSize;
		private Handler handler;
		private Runnable callback;
		private Runnable callback_log;

		public RecordThread(AudioRecord audioRecord, int minBufferSize,
				Handler handler, Runnable callback, Runnable callback_log) {
			this.audioRecord = audioRecord;
			this.minBufferSize = minBufferSize;
			this.handler = handler;
			this.callback = callback;
			this.callback_log = callback_log;
		}

		public void run() {
			try {
				short[] buffer = new short[minBufferSize];
				audioRecord.startRecording();
				Log.v("test", "recording");
				// fos = this.openFileOutput("data.txt", Context.MODE_PRIVATE);
				while (isRecording) {
					int res = audioRecord.read(buffer, 0, minBufferSize);
					// 将数据写入文件,以供分析使用
					// for(int i = 0; i < res; i++){
					// String str = Short.toString(buffer[i]);
					// Log.v("tag",str);
					// fos.write(str.getBytes());
					// fos.write(' ');
					// }
					// fos.write('\n');
					// 将录音结果存放到inBuf中,以备画时域图使用
					synchronized (inBuf) {
						inBuf.add(buffer);
					}
					// 保证长度为2的幂次数
					length = up2int(res);
					// length = 256;
					short[] tmpBuf = new short[length];
					System.arraycopy(buffer, 0, tmpBuf, 0, length);

					Complex[] complexs = new Complex[length];
					int[] outInt = new int[length];
					for (int i = 0; i < length; i++) {
						Short short1 = tmpBuf[i];
						complexs[i] = new Complex(short1.doubleValue());
					}
					fft(complexs, length);
					for (int i = 0; i < length; i++) {
						outInt[i] = complexs[i].getIntValue();
						// Log.v("tag",Integer.toString(outInt[i]));
					}
					synchronized (outBuf) {
						outBuf.add(outInt);
					}
					// find the peak
					int max_magnitude = Integer.MIN_VALUE;
					int max_index = -1;
					for (int i = 0; i < length / 2; i++) {
						if (outInt[i] > max_magnitude) {
							max_magnitude = outInt[i];
							max_index = i;
						}
					}
					double freq = max_index * frequence / length;
					Log.v("tag", Double.toString(freq));
					int index = getIndex(freq);
					synchronized (lock) {
						if (index >= 0 && result[index] == false) { // 第一次出现某频率
							Long tsLong = System.currentTimeMillis() / 1000;
							time_stamps[index] = tsLong.toString(); // get the
																	// time
																	// stamp
							result[index] = true;
							log = "频率：" + freq + "加入网络";
							handler.post(callback_log); // update the log
							// 转发
							GenerateWave.send((int) freq);
							log = "转发频率：" + freq;
							handler.post(callback_log); // update the log
							String vector = boolToBinary(result);
							Log.v("vector", vector);
							handler.post(callback); // update the UI thread
						} else if (index >= 0 && result[index] == true) { // 不是第一次出现
							Long tsLong = System.currentTimeMillis() / 1000;
							String ts_current = tsLong.toString(); // get the
																	// current
																	// time
																	// stamp
							if (Long.valueOf(ts_current)
									- Long.valueOf(time_stamps[index]) >= 30) { // 距离上一次接收超过30s
								time_stamps[index] = ts_current; // 更新时间戳
								// 再次转发
								GenerateWave.send((int) freq);
								log = "转发频率：" + freq;
								handler.post(callback_log); // update the log
							}
						}
					}

				}
				// try {
				// fos.close();
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				audioRecord.stop();

			} catch (Exception e) {
				Log.i("Rec E", e.toString());
			}

		}

		public int getCurrentNum() {
			return boolToInt(result);
		}

		public String getLog() {
			return log;
		}
	}

	private int getIndex(double freq) {
		if (freq > 14900.0 && freq <= 15100)
			return 0;
		else if (freq > 15400 && freq <= 15600)
			return 1;
		else if (freq > 15900 && freq <= 16100)
			return 2;
		else if (freq > 16400 && freq <= 16600)
			return 3;
		else if (freq > 16900 && freq <= 17100)
			return 4;
		else if (freq > 17400 && freq <= 17600)
			return 5;
		else if (freq > 17900 && freq <= 18100)
			return 6;
		else if (freq > 18400 && freq <= 18600)
			return 7;
		else if (freq > 18900 && freq <= 19100)
			return 8;
		else if (freq > 19400 && freq <= 19600)
			return 9;
		else if (freq > 19900 && freq <= 20100)
			return 10;
		else if (freq > 20400 && freq <= 20600)
			return 11;
		else if (freq > 20900 && freq <= 21100)
			return 12;
		else if (freq > 21400)
			return 13;
		else
			return -1;
	}

	private String boolToBinary(boolean[] arr) {
		String tmp = "";
		for (boolean n : arr) {
			if (n == true)
				tmp = tmp + "1";
			else
				tmp = tmp + "0";
		}
		return tmp;
	}

	private int boolToInt(boolean[] arr) {
		if (arr.length == 0)
			return 0;
		int count = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == true)
				count++;
		}
		return count;
	}

	/**
	 * 向上取最接近iint的2的幂次数.比如iint=320时,返回256
	 * 
	 * @param iint
	 * @return
	 */
	private int up2int(int iint) {
		int ret = 1;
		while (ret <= iint) {
			ret = ret << 1;
		}
		return ret >> 1;
	}

	// 快速傅里叶变换
	public void fft(Complex[] xin, int N) {
		int f, m, N2, nm, i, k, j, L;// L:运算级数
		float p;
		int e2, B, ip;
		Complex w = new Complex();
		Complex t = new Complex();
		N2 = N / 2;// 每一级中蝶形的个数,同时也代表m位二进制数最高位的十进制权值
		f = N;// f是为了求流程的级数而设立的
		for (m = 1; (f = f / 2) != 1; m++)
			; // 得到流程图的共几级
		nm = N - 2;
		j = N2;
		/****** 倒序运算——雷德算法 ******/
		for (i = 1; i <= nm; i++) {
			if (i < j)// 防止重复交换
			{
				t = xin[j];
				xin[j] = xin[i];
				xin[i] = t;
			}
			k = N2;
			while (j >= k) {
				j = j - k;
				k = k / 2;
			}
			j = j + k;
		}
		/****** 蝶形图计算部分 ******/
		for (L = 1; L <= m; L++) // 从第1级到第m级
		{
			e2 = (int) Math.pow(2, L);
			// e2=(int)2.pow(L);
			B = e2 / 2;
			for (j = 0; j < B; j++) // j从0到2^(L-1)-1
			{
				p = 2 * pi / e2;
				w.real = Math.cos(p * j);
				// w.real=Math.cos((double)p*j); //系数W
				w.image = Math.sin(p * j) * -1;
				// w.imag = -sin(p*j);
				for (i = j; i < N; i = i + e2) // 计算具有相同系数的数据
				{
					ip = i + B; // 对应蝶形的数据间隔为2^(L-1)
					t = xin[ip].cc(w);
					xin[ip] = xin[i].cut(t);
					xin[i] = xin[i].sum(t);
				}
			}
		}
	}

	// 删除退出的手机
	public void clear() {
		synchronized (lock) {
			// 获取时间戳
			Long tsLong = System.currentTimeMillis() / 1000;
			String ts_current = tsLong.toString(); // get the current time stamp
			// 逐个比较并且删除超过n秒的
			for (int i = 0; i < 14; i++) {
				if (Long.valueOf(ts_current) - Long.valueOf(time_stamps[i]) >= timeout)
					result[i] = false;
			}
		}
	}
}
