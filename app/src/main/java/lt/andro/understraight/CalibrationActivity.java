package lt.andro.understraight;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CalibrationActivity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, CalibrationActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
    }
}
