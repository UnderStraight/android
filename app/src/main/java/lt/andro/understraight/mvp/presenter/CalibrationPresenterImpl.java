package lt.andro.understraight.mvp.presenter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation.CompletionHandler;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.data.CartesianShort;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;
import com.mbientlab.metawear.module.Mma8452qAccelerometer.Orientation;

import lt.andro.understraight.MainActivity;
import lt.andro.understraight.mvp.view.CalibrationView;
import lt.andro.understraight.utils.Constants;

import static lt.andro.understraight.utils.Constants.DELAY_READS_MILLIS;
import static lt.andro.understraight.utils.Constants.DELAY_RECONNECTION_MILLIS;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-27 12:39.
 */
public class CalibrationPresenterImpl implements CalibrationPresenter {
    public static final String ACCELEROMETER_DATA_STREAM = "AccelerometerDataStream";
    private final CalibrationView calibrationView;
    private final BluetoothManager btManager;
    MetaWearBoard mwBoard;
    private boolean isAttached;
    private Handler handler;
    private Mma8452qAccelerometer accelerometer;

    public CalibrationPresenterImpl(CalibrationView calibrationView, BluetoothManager btManager) {
        this.calibrationView = calibrationView;
        this.btManager = btManager;

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {


        MetaWearBleService.LocalBinder serviceBinder = (MetaWearBleService.LocalBinder) service;

        connectToUnderStraight(serviceBinder);
    }

    private void connectToUnderStraight(MetaWearBleService.LocalBinder serviceBinder) {

        if (!isAttached) return;
        calibrationView.showProgress(true);

        //        final String MW_MAC_ADDRESS = "E2:87:11:D8:23:9D"; // MW1
        final String MW_MAC_ADDRESS = "FB:89:1F:FE:16:D4"; // MW2
//        final String MW_MAC_ADDRESS = "D0:92:E2:8C:30:BA"; // MW3

        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);
        String msg = "Connecting to Bluetooth with MAC: " + MW_MAC_ADDRESS;
        calibrationView.showToast(msg);

        serviceBinder.executeOnUiThread();

        Log.i("MainActivity", "Service connected");

        mwBoard = serviceBinder.getMetaWearBoard(remoteDevice);
        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                String msg = "Connected to UnderStraight";
                calibrationView.showToast(msg);

                Log.i(this.getClass().getName(), "MW Connected");
                Log.i(this.getClass().getName(), "MW MetaBoot? " + mwBoard.inMetaBootMode());

                calibrationView.showProgress(false);
                startAccelerometerModule();
            }

            @Override
            public void disconnected() {
                String msg = "Disconnected";
                calibrationView.showToast(msg);
                reconnectToUnderStraight(serviceBinder);
            }

            @Override
            public void failure(int status, final Throwable error) {
                error.printStackTrace();
                calibrationView.showToast("Error connecting: " + error.getLocalizedMessage());

                // Reconnect
                reconnectToUnderStraight(serviceBinder);
            }
        });

        mwBoard.connect();
    }

    private void continuousReadValue() {
        readValue();

        if (isAttached)
            handler.postDelayed(this::continuousReadValue, DELAY_READS_MILLIS);
    }

    private void readValue() {
        try {
            accelerometer.readAnalogIn(PIN_BEND_SENSOR, Gpio.AnalogReadMode.ADC);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error reading value: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startAccelerometerModule() {
        try {
            accelerometer = mwBoard.getModule(Mma8452qAccelerometer.class);
            accelerometer.enableOrientationDetection();

            accelerometer
                    .routeData()
                    .fromOrientation()
                    .stream(ACCELEROMETER_DATA_STREAM)
                    .commit()
                    .onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            calibrationView.showToast("Accelerometer route success");
                            result.subscribe(ACCELEROMETER_DATA_STREAM, msg -> showAccelerometerMessage(msg));
                            continuousReadValue();
                        }

                        @Override
                        public void failure(Throwable error) {
                            error.printStackTrace();
                            calibrationView.showToast("Error committing route");
                        }
                    });
//            accelerometer.setOutputDataRate(Mma8452qAccelerometer.OutputDataRate.ODR_6_25_HZ);

//            // enable axis sampling
//            accelerometer.enableAxisSampling();

            accelerometer.start();
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            calibrationView.showToast("Failed to connect to accelerometer");
        }
    }


    private void reconnectToUnderStraight(MetaWearBleService.LocalBinder serviceBinder) {
        calibrationView.showProgress(false);
        handler.postDelayed(() -> {
            // Reconnect
            connectToUnderStraight(serviceBinder);
        }, Constants.DELAY_RECONNECTION_MILLIS);
    }

    private void showAccelerometerMessage(Message msg) {
        Orientation data = msg.getData(Orientation.class);
        Log.i("MainActivity", data.toString());
        calibrationView.showValue(data.isFront().x());
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {

        String msg = "Service disconnected. Reconnecting.";
        calibrationView.showToast(msg);

        if (isAttached) {
            calibrationView.showProgress(false);
            handler.postDelayed(() -> calibrationView.bindService(this), DELAY_RECONNECTION_MILLIS);
        }
    }

    @Override
    public void onAttach() {
        isAttached = true;
        calibrationView.bindService(this);
    }

    @Override
    public void onDetach() {
        isAttached = false;

        accelerometer.disableAxisSampling();
        accelerometer.stop();

        calibrationView.unbindService(this);
    }
}
