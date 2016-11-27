package lt.andro.understraight.utils;

import android.view.View;

import butterknife.ButterKnife;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-27 11:49.
 */
public class Utils {
    public static final ButterKnife.Action<? super View> ACTION_VISIBLE = (ButterKnife.Action<View>) (view, index) -> view.setVisibility(View.VISIBLE);
    public static final ButterKnife.Action<? super View> ACTION_GONE = (ButterKnife.Action<View>) (view, index) -> view.setVisibility(View.GONE);
}
