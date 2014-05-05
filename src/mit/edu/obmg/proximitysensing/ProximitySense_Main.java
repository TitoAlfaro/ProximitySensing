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
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ProximitySense_Main extends IOIOActivity {
	private final String TAG = "ProximitySensing";
	private ToggleButton button_;
	
	//Proximity
	private int sensorPin = 41;
	private AnalogInput proximityData;
	float distance = 1;
	
	//UI
	private TextView distanceValue, _vibRate;
	private NumberPicker minSensor, maxSensor, minActor, maxActor;
	
	//MultiThreading
	private Thread Vibration;
	Thread thread = new Thread(Vibration);
	
	//Vibration
	float rate;
	DigitalOutput out;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_proximity_sense__main);
		button_ = (ToggleButton) findViewById(R.id.LEDebug);
		
		distanceValue = (TextView)findViewById(R.id.distance);
		_vibRate = (TextView)findViewById(R.id.VibRate);
		
		String[] sensorNums = new String[31];
	    for(int i=0; i<sensorNums.length; i++){
			//float dec = i/10.0f;
	    	sensorNums[i] = Integer.toString(i);
	    }
	    
		minSensor = (NumberPicker)findViewById(R.id.minSensor);
	    minSensor.setMinValue(0);
	    minSensor.setMaxValue(30);
	    minSensor.setWrapSelectorWheel(false);
	    minSensor.setDisplayedValues(sensorNums);
	    minSensor.setValue(0);
	    minSensor.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		maxSensor = (NumberPicker)findViewById(R.id.maxSensor); 
		maxSensor.setMinValue(0);
		maxSensor.setMaxValue(20);
		maxSensor.setWrapSelectorWheel(false);
		maxSensor.setDisplayedValues(sensorNums);
		maxSensor.setValue(20);
	    maxSensor.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
	    
	    String[] actorNums = new String[990];
	    for(int i=0; i<actorNums.length; i++)
	    	actorNums[i] = Integer.toString(i+10);
	    
	    minActor = (NumberPicker)findViewById(R.id.minActor);
	    minActor.setMinValue(10);
	    minActor.setMaxValue(999);
	    minActor.setWrapSelectorWheel(false);
	    minActor.setDisplayedValues(actorNums);
	    minActor.setValue(10);
	    minActor.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		maxActor = (NumberPicker)findViewById(R.id.maxActor); 
		maxActor.setMinValue(10);
		maxActor.setMaxValue(999);
		maxActor.setWrapSelectorWheel(false);
		maxActor.setDisplayedValues(actorNums);
		maxActor.setValue(999);
		maxActor.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
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
				distance = proximityData.getVoltage()*10;
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			
			distanceValue.post(new Runnable() {
				public void run() {
					distanceValue.setText("Voltage: "+ distance);
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
						if (distance < minSensor.getValue()){
							rate = 1000;
						}else if (distance > maxSensor.getValue()){
							rate = 5;
						}else{
							rate = map(distance, 	(float) 0,//minSensor.getValue(), 
													(float) 20,//maxSensor.getValue(), 
													(float) 1000, 
													(float) 5);
							Log.i(TAG, "minSensor/10 = "+ minSensor.getValue());
						}
						
						_vibRate.post(new Runnable() {
							public void run() {
								_vibRate.setText("Rate: "+ rate);
							}
						});
						
						led.write(false);
						out.write(true);
						sleep((long) 50);
						led.write(true);
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