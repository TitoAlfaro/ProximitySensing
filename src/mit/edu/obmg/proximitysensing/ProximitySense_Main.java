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
import android.widget.TextView;
import android.widget.ToggleButton;

public class ProximitySense_Main extends IOIOActivity {
	private final String TAG = "ProximitySensing";

	private ToggleButton button_;
	
	//Proximity
	private int sensorPin = 41;
	private AnalogInput proximityData;
	private float distance;
	
	//UI
	private TextView distanceValue, _vibRate;
	
	//MultiThreading
	private Thread Vibration;
	Thread thread = new Thread(Vibration);
	
	//Vibration
	float rate = 1000;
	DigitalOutput out;
	private int sensitivityFactor = 50;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_proximity_sense__main);
		button_ = (ToggleButton) findViewById(R.id.LEDebug);
		
		distanceValue = (TextView)findViewById(R.id.distance);
		_vibRate = (TextView)findViewById(R.id.VibRate);
		
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
			//led_.write(!button_.isChecked());
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
							rate = 100;
						}else{
							rate = map(distance, (float) 0.0, (float) 2.0, (float) 200.0, (float) 20.0);
							if (rate < 0){
								rate = 0;
							}
						}
						
						_vibRate.post(new Runnable() {
							public void run() {
								_vibRate.setText("Rate: "+ rate);
								//mSensitivityValue.setText("Sensitivity: "+ sensitivityFactor);
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
	
	float map(float x, float in_min, float in_max, float out_min, float out_max)
	{
	  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
}