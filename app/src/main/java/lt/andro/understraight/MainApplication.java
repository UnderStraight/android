package lt.andro.understraight;

import android.app.Application;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-27 16:18.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();


        Iconify.with(new FontAwesomeModule());
    }
}
