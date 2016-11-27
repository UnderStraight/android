package lt.andro.understraight;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-26 18:10.
 */
class StoopValuesAdapter extends SparkAdapter {
    private static final int MAX_LENGTH = 500;
    private static final int AVERAGING_LENGTH = 10;
    private final ArrayList<Short> values;
    private final ArrayList<Short> averagingArray = new ArrayList<>(AVERAGING_LENGTH);
    @SuppressWarnings("FieldCanBeLocal")
    private long averageSumTemp;

    StoopValuesAdapter(ArrayList<Short> values) {
        this.values = values;
    }

    private short average(List<Short> numbers) {
        averageSumTemp = 0;
        for (Short number : numbers) {
            averageSumTemp += number;
        }
        return (short) (averageSumTemp / numbers.size());
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public float getBaseLine() {
        return 0f;
    }

    @Override
    public Short getItem(int index) {
        return values.get(index);
    }

    @Override
    public float getY(int index) {
        return values.get(index);
    }

    void addValue(Short readValue) {
        values.add(getAveraged(readValue));
        if (values.size() > MAX_LENGTH) {
            values.remove(0);
        }
    }

    private Short getAveraged(Short readValue) {
        averagingArray.add(readValue);
        if (averagingArray.size() > AVERAGING_LENGTH) {
            averagingArray.remove(0);
        }
        return average(averagingArray);
    }

}
