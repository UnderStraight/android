package lt.andro.understraight.mvp.presenter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.data.CartesianShort;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;

import hugo.weaving.DebugLog;
import lt.andro.understraight.mvp.view.CalibrationView;
import lt.andro.understraight.utils.Constants;

import static lt.andro.understraight.utils.Constants.DELAY_RECONNECTION_MILLIS;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-27 12:39.
 */
public class CalibrationPresenterImpl implements CalibrationPresenter {
    private static final String ACCELEROMETER_1_DATA_STREAM = "ACCELEROMETER_1_DATA_STREAM";
    private static final String ACCELEROMETER_2_DATA_STREAM = "ACCELEROMETER_2_DATA_STREAM";
    private static final int STOOP_THRESHOLD = 50;
    private static final String GPIO_0_ADC_STREAM = "gpio_0_adc_stream";

    private final byte GPIO_PIN = 0;

    private final CalibrationView calibrationView;
    private final BluetoothManager btManager;
    private MetaWearBoard mwBoard;
    private boolean isAttached;
    private Handler handler;
    private Mma8452qAccelerometer accelerometer;
    private Gpio gpioModule;
    private int accelerometer1Value;
    private int accelerometer2Value;

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

                startAccelerometer1Module();
                startAccelerometer2Module();
            }

            @Override
            public void disconnected() {
                String msg = "Disconnected";
                calibrationView.showToast(msg);
                reconnectToUnderStraight(serviceBinder);
            }

            @Override
            public void failure(int status, final Throwable error) {
                String msg = "Error connecting: " + error.getLocalizedMessage();
                calibrationView.showToast(msg);

                // Reconnect
                reconnectToUnderStraight(serviceBinder);
            }
        });

        mwBoard.connect();
    }

    private void startAccelerometer1Module() {
        try {
            accelerometer = mwBoard.getModule(Mma8452qAccelerometer.class);
            accelerometer.enableAxisSampling();

            accelerometer
                    .routeData()
                    .fromAxes()
                    .stream(ACCELEROMETER_1_DATA_STREAM)
                    .commit()
                    .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            calibrationView.showToast("Accelerometer route success");
                            result.subscribe(ACCELEROMETER_1_DATA_STREAM, message -> showAccelerometer1Value(message));
                        }

                        @Override
                        public void failure(Throwable error) {
                            String msg = "Error committing route";
                            Log.e("MetaWearAccelerometer", msg, error);
                            calibrationView.showToast(msg);
                        }
                    });

            // Switch the accelerometer to active mode
            accelerometer.start();
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            calibrationView.showToast("Failed to connect to accelerometer");
        }
    }

    private void startAccelerometer2Module() {
        try {
            // Route data from adc reads on pin 0
            gpioModule = mwBoard.getModule(Gpio.class);
            gpioModule.routeData().fromAnalogIn(GPIO_PIN, Gpio.AnalogReadMode.ADC).stream(GPIO_0_ADC_STREAM)
                    .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe(GPIO_0_ADC_STREAM, msg -> {
                        calibrationView.showToast("Sensor route success");
                        result.subscribe(ACCELEROMETER_2_DATA_STREAM, message -> showAccelerometer2Value(msg));
                    });
                    gpioModule.readAnalogIn(GPIO_PIN, Gpio.AnalogReadMode.ADC);
                }
            });
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            calibrationView.showToast("Failed to connect to sensor");
        }

    }


    private void reconnectToUnderStraight(MetaWearBleService.LocalBinder serviceBinder) {
        calibrationView.showProgress(false);
        handler.postDelayed(() -> {
            // Reconnect
            connectToUnderStraight(serviceBinder);
        }, Constants.DELAY_RECONNECTION_MILLIS);
    }

    @DebugLog
    private void showAccelerometer1Value(Message msg) {
        CartesianFloat axes = msg.getData(CartesianFloat.class);
        Log.i("MetaWearAccelerometer", axes.toString());

        final CartesianShort axisData = msg.getData(CartesianShort.class);


        //// AXIS

        Short z = (short) (axisData.z() * (short) -1);
        showAccelerometer1Value(z);
    }

    @DebugLog
    private void showAccelerometer1Value(Short z) {
        accelerometer1Value = z;
        updateView();
    }

    private void updateView() {
        int diff = Math.abs(accelerometer1Value - accelerometer2Value);
        Log.i(CalibrationPresenterImpl.class.getName(), String.format("A1: %d, A2: %d, diff: %d", accelerometer1Value, accelerometer2Value, diff));
        calibrationView.showValue(diff, isStooping(diff));
    }

    @DebugLog
    private void showAccelerometer2Value(Message msg) {
        Short value = msg.getData(Short.class);
        accelerometer2Value = value;
        updateView();
    }

    private boolean isStooping(int stoopValue) {
        return stoopValue > STOOP_THRESHOLD;
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
