package lt.andro.understraight;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lt.andro.understraight.mvp.view.PreCalibrationView;
import lt.andro.understraight.utils.Utils;

public class PreCalibrationActivity extends AppCompatActivity implements PreCalibrationView {

    @BindViews({R.id.pre_calibration_bt_warning, R.id.pre_calibration_start_calibration,
            R.id.pre_calibration_stand_straight, R.id.pre_calibration_ampersand, R.id.pre_calibration_put_your_bra})
    List<View> contentViews;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, PreCalibrationActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_calibration);

        ButterKnife.bind(this);

        showContentViews(false);
        new Handler().postDelayed(() -> showContentViews(true), 2000);
    }

    @OnClick(R.id.pre_calibration_start_calibration)
    void onStartCalibrationButtonClicked(View view) {
        CalibrationActivity.startActivity(this);
    }

    @Override
    public void showContentViews(boolean visible) {
        if (contentViews != null)
            ButterKnife.apply(contentViews, visible ? Utils.ACTION_VISIBLE : Utils.ACTION_GONE);
    }
}
