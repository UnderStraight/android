package lt.andro.understraight;

import com.robinhood.spark.SparkAdapter;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

/**
 * @author Vilius Kraujutis
 * @since 2016-11-26 18:10.
 */
class StoopValuesAdapter extends SparkAdapter {
    private static final int MAX_LENGTH = 150;
    private static final int AVERAGING_LENGTH = 2;
    private final ArrayList<Integer> values;
    private final ArrayList<Integer> averagingArray = new ArrayList<>(AVERAGING_LENGTH);
    @SuppressWarnings("FieldCanBeLocal")
    private long averageSumTemp;

    StoopValuesAdapter(ArrayList<Integer> values) {
        this.values = values;
    }

    @DebugLog
    private Integer average(List<Integer> numbers) {
        averageSumTemp = 0;
        for (Integer number : numbers) {
            averageSumTemp += number;
        }
        return (int) (averageSumTemp / numbers.size());
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
    public Integer getItem(int index) {
        return values.get(index);
    }

    @Override
    public float getY(int index) {
        return values.get(index);
    }

    void addValue(int readValue) {
        values.add(getAveraged(readValue));
        if (values.size() > MAX_LENGTH) {
            values.remove(0);
        }
    }

    private int getAveraged(int readValue) {
        averagingArray.add(readValue);
        if (averagingArray.size() > AVERAGING_LENGTH) {
            averagingArray.remove(0);
        }
        return average(averagingArray);
    }

}
