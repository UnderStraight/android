package lt.andro.understraight;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Gpio;
import com.robinhood.spark.SparkView;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    public static final int DELAY_READS_MILLIS = 300;
    public static final int DELAY_RECONNECTION_MILLIS = 2000;
    public static final String GPIO_0_ADC_STREAM = "gpio_0_adc_stream";
    final byte PIN_BEND_SENSOR = 0;
    @BindView(R.id.main_connection_progress)
    ProgressBar progressBar;
    @BindView(R.id.main_value)
    TextView value;
    @BindView(R.id.main_read_continuously)
    Switch readContinuously;
    @BindView(R.id.main_plot_sparkView)
    SparkView sparkViewPlotting;

    MetaWearBoard mwBoard;
    private BendValuesAdapter adapter;

    private boolean isRunning = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startPreCalibration();

        ButterKnife.bind(this);
        initSparkPlotting();
        readContinuously.setOnCheckedChangeListener((compoundButton, b) -> continuousReadValue());
    }

    private void startPreCalibration() {
        PreCalibrationActivity.startActivity(this);
    }

    private void initSparkPlotting() {
        adapter = new BendValuesAdapter(new ArrayList<>());
        sparkViewPlotting.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        getApplicationContext().unbindService(this);
        handler = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        handler = new Handler(Looper.getMainLooper());

        progressBar.setVisibility(VISIBLE);

        bindService();
    }

    private void bindService() {
        if (!isRunning) return;

        showProgress(true);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MetaWearBleService.LocalBinder serviceBinder = (MetaWearBleService.LocalBinder) service;

        connectToUnderStraight(serviceBinder);
    }

    private void connectToUnderStraight(MetaWearBleService.LocalBinder serviceBinder) {

        if (!isRunning) return;
        showProgress(true);

        //        final String MW_MAC_ADDRESS = "E2:87:11:D8:23:9D"; // MW1
        final String MW_MAC_ADDRESS = "FB:89:1F:FE:16:D4"; // MW2
//        final String MW_MAC_ADDRESS = "D0:92:E2:8C:30:BA"; // MW3

        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);
        Toast.makeText(this, "Connecting to Bluetooth with MAC: " + MW_MAC_ADDRESS, Toast.LENGTH_SHORT).show();

        serviceBinder.executeOnUiThread();

        Log.i("MainActivity", "Service connected");

        mwBoard = serviceBinder.getMetaWearBoard(remoteDevice);
        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Toast.makeText(MainActivity.this, "Connected to UnderStraight", Toast.LENGTH_LONG).show();

                Log.i("MainActivity", "MW Connected");
                Log.i("MainActivity", "MW MetaBoot? " + mwBoard.inMetaBootMode());

                showProgress(false);
                initGpioRouteStream();
            }

            @Override
            public void disconnected() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                Log.i("MainActivity", "MW Disconnected");
                reconnectToUnderStraight(serviceBinder);
            }

            @Override
            public void failure(int status, final Throwable error) {
                Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Error connecting", error);

                // Reconnect
                reconnectToUnderStraight(serviceBinder);
            }
        });

        mwBoard.connect();
    }

    private void reconnectToUnderStraight(MetaWearBleService.LocalBinder serviceBinder) {
        showProgress(false);
        handler.postDelayed(() -> {
            // Reconnect
            connectToUnderStraight(serviceBinder);
        }, DELAY_RECONNECTION_MILLIS);
    }

    private void continuousReadValue() {
        readValue();

        if (readContinuously.isChecked() && isRunning)
            handler.postDelayed(this::continuousReadValue, DELAY_READS_MILLIS);
    }

    private void readValue() {
        try {
            getGpioModule().readAnalogIn(PIN_BEND_SENSOR, Gpio.AnalogReadMode.ADC);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error reading value: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Gpio getGpioModule() throws UnsupportedModuleException {
        return mwBoard.getModule(Gpio.class);
    }

    private void initGpioRouteStream() {
        try {
            if (getGpioModule() == null) {
                String msg = "Can't read. gpioModule is null";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                Log.i("MainActivity", msg);
                return;
            }
            getGpioModule().routeData().fromAnalogIn(PIN_BEND_SENSOR, Gpio.AnalogReadMode.ADC).stream(GPIO_0_ADC_STREAM)
                    .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    Toast.makeText(MainActivity.this, "Successfully created ADC input route.", Toast.LENGTH_SHORT).show();
                    result.subscribe(GPIO_0_ADC_STREAM, msg -> addValue(msg));

                    continuousReadValue();
                }
            });
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error initializing board: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addValue(Message msg) {
        Short readValue = msg.getData(Short.class);

        adapter.addValue(readValue);
        adapter.notifyDataSetChanged();

        String valueString = String.format(Locale.getDefault(), "gpio 0 ADC: %d", readValue);
        Log.i("MainActivity", valueString + ": " + createDotsString(readValue));
        value.setText(valueString);
    }

    private String createDotsString(Short readValue) {
        String dots = ".";
        for (int i = 1; i < readValue; i++) {
            dots += ".";
        }
        return dots;
    }

    private void showProgress(boolean visible) {
        progressBar.setVisibility(visible ? VISIBLE : View.GONE);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        String msg = "Service disconnected. Reconnecting.";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        if (isRunning) {
            showProgress(false);
            handler.postDelayed(this::bindService, DELAY_RECONNECTION_MILLIS);
        }
    }
}
