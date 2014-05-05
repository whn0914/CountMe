package com.himuxin.countme;

import android.app.Activity;
import android.app.Fragment;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private final int waveinterval = 5;
	private Runnable sendrunnable;
	private Runnable quitrunnable;
	private final Handler handler = new Handler();

	// for update text
	private Runnable callback = new Runnable() {
		public void run() {
			updateText(audioProcess.record_thread.getCurrentNum());
		}

	};

	private Runnable callback_log = new Runnable() {
		public void run() {
			updateLog(audioProcess.record_thread.getLog());
		}
	};

	public boolean isworking = false;
	private int freqid = 12000;
	private static final String[] freq = { "15000", "15500", "16000", "16500",
			"17000", "17500", "18000", "18500", "19000", "19500", "20000",
			"20500", "21000", "21500", "22000" };

	// fields for audioprocess module
	static int frequency = 44100; // 接收频率
	static final int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
	static final int audioEncodeing = AudioFormat.ENCODING_PCM_16BIT;
	int minBufferSize;
	AudioRecord audioRecord;
	AudioProcess audioProcess = new AudioProcess();
	private TextView numView = null;
	private TextView logView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// control the spinner
		final Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		// 将可选内容与ArrayAdapter连接起来
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, freq);

		// 将adapter 添加到spinner中
		spinner.setAdapter(adapter);

		// 添加事件Spinner事件监听
		spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				freqid = Integer.parseInt(freq[arg2]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});

		// 设置默认值
		spinner.setVisibility(View.VISIBLE);

		// set the text view
		numView = (TextView) findViewById(R.id.textView1);
		numView.setText("网络中机器数：1");

		logView = (TextView) findViewById(R.id.textView3);
		logView.setText("本机加入网络\n");
		// add the button action
		final Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (isworking) {
					isworking = false;
					button.setText("加入网络");
					handler.removeCallbacks(sendrunnable);
					handler.removeCallbacks(quitrunnable);
					audioProcess.stop();
				} else {
					isworking = true;
					button.setText("离开网络");
					// 立刻发出第一次信号
					GenerateWave.send(freqid);
					// 每n秒发出信号
					sendrunnable = new Runnable() {
						int freqidnow = freqid;

						@Override
						public void run() {
							GenerateWave.send(freqidnow);
							handler.postDelayed(this, 1000 * waveinterval);
						}
					};
					handler.postDelayed(sendrunnable, 1000 * waveinterval);

					// 每n秒检查一次退出
					
					// 接收线程启动
					try {
						minBufferSize = AudioRecord.getMinBufferSize(frequency,
								channelConfiguration, audioEncodeing);
						// minBufferSize = 2 * minBufferSize;
						audioRecord = new AudioRecord(
								MediaRecorder.AudioSource.MIC, frequency,
								channelConfiguration, audioEncodeing,
								minBufferSize);
						audioProcess.frequence = frequency;
						audioProcess.start(audioRecord, minBufferSize, handler,
								callback, callback_log);
						Log.v("test", "Starting record thread");
						audioProcess.record_thread.start();
						Toast.makeText(MainActivity.this,
								"当前设备支持您所选择的采样率:" + String.valueOf(frequency),
								Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						Toast.makeText(
								MainActivity.this,
								"当前设备不支持您所选择的采样率:" + String.valueOf(frequency)
										+ ",请重新选择", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	private void updateText(int num) {
		Log.v("test", "updating num text view");
		numView.setText("网络中机器数：" + String.valueOf(num));
	}

	private void updateLog(String log) {
		String old_log = (String) logView.getText();
		String new_log = old_log + log + "\n";
		logView.setText(new_log);
	}
}
