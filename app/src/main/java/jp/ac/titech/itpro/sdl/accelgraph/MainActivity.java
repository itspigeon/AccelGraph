package jp.ac.titech.itpro.sdl.accelgraph;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

    private final static String TAG = "MainActivity";
    private final int N = 5;
    private final static float alpha = 0.8F;

    private TextView rateView, accuracyView;
    private GraphView xView, yView, zView;

    private SensorManager sensorMgr;
    private Sensor accelerometer;

    private final static long GRAPH_REFRESH_WAIT_MS = 20;

    private GraphRefreshThread th = null;
    private Handler handler;

    private float[] smAccValue = new float[3];
    private float[] wgAccValue = new float[3];
    private float[][] storedAccValue = new float[3][N];
    private int idx = 0;

    private float vx, vy, vz;

    private float rate;
    private int accuracy;
    private long prevts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        rateView = (TextView) findViewById(R.id.rate_view);
        accuracyView = (TextView) findViewById(R.id.accuracy_view);
        xView = (GraphView) findViewById(R.id.x_view);
        yView = (GraphView) findViewById(R.id.y_view);
        zView = (GraphView) findViewById(R.id.z_view);

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            Toast.makeText(this, getString(R.string.toast_no_accel_error),
                Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        th = new GraphRefreshThread();
        th.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        th = null;
        sensorMgr.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        smAverage(event);
        weightedAverage(event);

        vx = event.values[0];
        vy = event.values[1];
        vz = event.values[2];

        rate = ((float) (event.timestamp - prevts)) / (1000 * 1000);
        prevts = event.timestamp;
    }

    private void smAverage(SensorEvent event) {
        for (int axis = 0; axis < 3; axis++) {
            storedAccValue[axis][idx] = event.values[axis];
            float s = 0;
            for (int i = 0; i < N; i++) {
                s = s + storedAccValue[axis][i];
            }
            smAccValue[axis] = s / N;
            idx = (idx + 1) % N;
        }
    }

    private void weightedAverage(SensorEvent event) {
        for (int axis = 0; axis < 3; axis++) {
            wgAccValue[axis] = alpha * wgAccValue[axis] + (1 - alpha) * event.values[axis];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: ");
        this.accuracy = accuracy;
    }

    private class GraphRefreshThread extends Thread {
        public void run() {
            try {
                while (th != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            rateView.setText(Float.toString(rate));
                            accuracyView.setText(Integer.toString(accuracy));
                            xView.addData(vx, wgAccValue[0], smAccValue[0], true);
                            yView.addData(vy, wgAccValue[1], smAccValue[1], true);
                            zView.addData(vz, wgAccValue[2], smAccValue[2], true);
                        }
                    });
                    Thread.sleep(GRAPH_REFRESH_WAIT_MS);
                }
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.toString());
                th = null;
            }
        }
    }
}
