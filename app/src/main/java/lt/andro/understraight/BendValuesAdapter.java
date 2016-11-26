package lt.andro.understraight;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-26 18:10.
 */
class BendValuesAdapter extends SparkAdapter {
    public static final int MAX_LENGTH = 50;
    private final ArrayList<Short> values;

    public BendValuesAdapter(ArrayList<Short> values) {
        this.values = values;
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Short getItem(int index) {
        return values.get(index);
    }

    @Override
    public float getY(int index) {
        return values.get(index);
    }

    public void addValue(Short readValue) {
        values.add(readValue);
        if (values.size() > MAX_LENGTH) {
            values.remove(0);
        }
    }
}
