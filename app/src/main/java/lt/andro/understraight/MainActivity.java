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

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    public static final int DELAY_READS_MILLIS = 100;
    final byte PIN_BEND_SENSOR = 0;
    @BindView(R.id.main_connection_progress)
    ProgressBar progressBar;
    @BindView(R.id.main_value)
    TextView value;
    @BindView(R.id.main_read_continuously)
    Switch readContinuously;

    MetaWearBoard mwBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        readContinuously.setOnCheckedChangeListener((compoundButton, b) -> continuousReadValue());
    }

    @Override
    protected void onPause() {
        super.onPause();
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        progressBar.setVisibility(VISIBLE);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MetaWearBleService.LocalBinder serviceBinder = (MetaWearBleService.LocalBinder) service;

        final String MW_MAC_ADDRESS = "E2:87:11:D8:23:9D";

        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        serviceBinder.executeOnUiThread();

        Log.i("test", "Service connected");

        mwBoard = serviceBinder.getMetaWearBoard(remoteDevice);
        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();

                Log.i("test", "Connected");
                Log.i("test", "MetaBoot? " + mwBoard.inMetaBootMode());

                showProgress(false);
                continuousReadValue();
            }

            @Override
            public void disconnected() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                Log.i("test", "Disconnected");
            }

            @Override
            public void failure(int status, final Throwable error) {
                Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.e("test", "Error connecting", error);
            }
        });

        mwBoard.connect();
    }

    private void continuousReadValue() {
        Handler handler = new Handler(Looper.getMainLooper());

        readValue();

        if (readContinuously.isChecked())
            handler.postDelayed(this::continuousReadValue, DELAY_READS_MILLIS);
    }

    private void readValue() {
        final Gpio gpioModule;
        try {
            gpioModule = mwBoard.getModule(Gpio.class);
            if (gpioModule == null) {
                Log.i("MainActivity", "Can't read. gpioModule is null");
                return;
            }
            gpioModule.routeData().fromAnalogIn(PIN_BEND_SENSOR, Gpio.AnalogReadMode.ADC).stream("gpio_0_adc_stream")
                    .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("gpio_0_adc_stream", new RouteManager.MessageHandler() {
                        @Override
                        public void process(Message msg) {
                            showValue(msg);
                        }
                    });

                    gpioModule.readAnalogIn(PIN_BEND_SENSOR, Gpio.AnalogReadMode.ADC);
                }
            });
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showValue(Message msg) {
        Short readValue = msg.getData(Short.class);
        String valueString = String.format(Locale.getDefault(), "gpio 0 ADC: %d, %s", readValue, createDotsString(readValue));
        Log.i("MainActivity", valueString);
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
    }
}
