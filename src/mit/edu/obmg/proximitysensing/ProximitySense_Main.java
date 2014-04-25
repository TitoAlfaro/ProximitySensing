package mit.edu.obmg.proximitysensing;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ProximitySense_Main extends IOIOActivity implements OnClickListener{
	private final String TAG = "ProximitySensing";
	private ToggleButton button_;
	
	//Proximity
	private int sensorPin = 41;
	private AnalogInput proximityData;
	float distance = 1;
	
	//UI
	private TextView distanceValue, _vibRate, mSensitivityValue;
	private Button ButtonPlus, ButtonMinus;
	
	//MultiThreading
	private Thread Vibration;
	Thread thread = new Thread(Vibration);
	
	//Vibration
	float rate = 1000;
	float initialRate = 500;
	DigitalOutput out;
	private int sensitivityFactor = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_proximity_sense__main);
		button_ = (ToggleButton) findViewById(R.id.LEDebug);
		
		ButtonPlus = (Button) findViewById(R.id.ButtonPlus);
		ButtonPlus.setOnClickListener(this);
		ButtonMinus = (Button) findViewById(R.id.ButtonMinus);
		ButtonMinus.setOnClickListener(this);
		
		distanceValue = (TextView)findViewById(R.id.distance);
		_vibRate = (TextView)findViewById(R.id.VibRate);
		mSensitivityValue = (TextView)findViewById(R.id.Sensitivity);	
		
	}

	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */
		private DigitalOutput led_;

		@Override
		protected void setup() throws ConnectionLostException {
			//led_ = ioio_.openDigitalOutput(0, true);
			proximityData = ioio_.openAnalogInput(sensorPin);
			
			try {
				Vibration thread_ = new Vibration(ioio_);
				thread_.start();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				distance = proximityData.getVoltage();
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			
			distanceValue.post(new Runnable() {
				public void run() {
					distanceValue.setText("Distance: "+ distance);
				}
			});
		}
		@Override
		public void disconnected() {
			Log.i(TAG, "IOIO disconnected");
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	class Vibration extends Thread{
    	private DigitalOutput led;
    	
    	private IOIO ioio_;
    	
    	public Vibration(IOIO ioio)throws InterruptedException{
    		ioio_ = ioio;
    	}
    	
    	public void run(){
    		super.run();
			while (true) {
				try {
					led = ioio_.openDigitalOutput(0, true);
					out = ioio_.openDigitalOutput(13,false);
					while (true) {
						if (distance == 0){
							rate = initialRate - sensitivityFactor;
						}else{
							rate = map(distance, (float) 0.0, (float) 2.0, (float) initialRate - sensitivityFactor, (float) 20.0);
						}
						
						_vibRate.post(new Runnable() {
							public void run() {
								_vibRate.setText("Rate: "+ rate);
								mSensitivityValue.setText("Sensitivity: "+ sensitivityFactor);
							}
						});
						
						led.write(true);
						out.write(true);
						sleep((long) rate);
						led.write(false);
						out.write(false);
						sleep((long) rate);
					}
				} catch (ConnectionLostException e) {
				} catch (Exception e) {
					Log.e(TAG, "Unexpected exception caught", e);
					ioio_.disconnect();
					break;
				} finally {
					try {
						ioio_.waitForDisconnect();
					} catch (InterruptedException e) {
					}
				}
			}
    	}
    }
	
	@Override
	public void onClick(View v) {
		switch (v.getId()){
		case R.id.ButtonPlus:
			sensitivityFactor = sensitivityFactor + 10;
			break;

		case R.id.ButtonMinus:
			sensitivityFactor = sensitivityFactor - 10;
			break;
		}
		
	}
	
	float map(float x, float in_min, float in_max, float out_min, float out_max)
	{
	  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
}