package lt.andro.understraight;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;
import com.robinhood.spark.SparkView;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lt.andro.understraight.mvp.presenter.CalibrationPresenter;
import lt.andro.understraight.mvp.presenter.CalibrationPresenterImpl;
import lt.andro.understraight.mvp.view.CalibrationView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CalibrationActivity extends AppCompatActivity implements CalibrationView {

    @BindView(R.id.calibration_stoop_value)
    TextView stoopValueView;
    @BindView(R.id.calibration_plot_sparkView)
    SparkView sparkViewPlotting;
    @BindView(R.id.calibration_progress)
    ProgressBar progressBar;

    private StoopValuesAdapter adapter;
    private CalibrationPresenter presenter;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, CalibrationActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        ButterKnife.bind(this);

        initStoopValuesPlotting();

        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        presenter = new CalibrationPresenterImpl(this, btManager);

    }

    private void initStoopValuesPlotting() {
        adapter = new StoopValuesAdapter(new ArrayList<>());
        sparkViewPlotting.setAdapter(adapter);
        sparkViewPlotting.setLineColor(getResources().getColor(R.color.colorAccent));
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onAttach();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onDetach();
    }

    @Override
    public void bindService(CalibrationPresenter calibrationPresenter) {
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                calibrationPresenter, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void unbindService(CalibrationPresenter calibrationPresenter) {
        getApplicationContext().unbindService(calibrationPresenter);
    }

    @Override
    public void showProgress(boolean visible) {
        progressBar.setVisibility(visible ? VISIBLE : GONE);
    }

    @Override
    public void showToast(String msg) {
        Log.i("CalibrationActivity", msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showValue(Short stoopValue) {
        adapter.addValue(stoopValue);
        adapter.notifyDataSetChanged();

        stoopValueView.setText(String.format(Locale.getDefault(), "Value: %d", stoopValue));
        sparkViewPlotting.setLineColor(getResources().getColor(R.color.apple_green));
    }
}
