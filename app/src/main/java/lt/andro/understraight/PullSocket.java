package lt.andro.understraight;

import android.view.View;
import android.widget.Toast;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Gpio;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-26 12:08.
 */
public class PullSocket implements View.OnClickListener {
    private MainActivity mainActivity;
    private final Gpio.PullMode pullMode;

    PullSocket(MainActivity mainActivity, Gpio.PullMode mode) {
        this.mainActivity = mainActivity;
        this.pullMode = mode;
    }

    @Override
    public void onClick(View view) {
        try {
            Gpio gpioModule = mainActivity.mwBoard.getModule(Gpio.class);
            gpioModule.setPinPullMode(mainActivity.PIN_BEND_SENSOR, pullMode);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
            Toast.makeText(mainActivity, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
