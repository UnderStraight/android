package lt.andro.understraight.mvp.view;

import lt.andro.understraight.mvp.presenter.CalibrationPresenter;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-27 13:12.
 */
public interface CalibrationView {
    void bindService(CalibrationPresenter calibrationPresenter);

    void unbindService(CalibrationPresenter calibrationPresenter);

    void showProgress(boolean visible);

    void showToast(String msg);

    void showValue(Short stoopValue, boolean isStooping);
}
