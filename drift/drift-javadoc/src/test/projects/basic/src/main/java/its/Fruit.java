package its;

import com.facebook.drift.annotations.ThriftEnum;
import com.facebook.drift.annotations.ThriftEnumValue;

/**
 * Type of fruit
 */
@ThriftEnum
public enum Fruit
{
    /**
     * Large and sweet
     */
    APPLE(2),

    /**
     * Yellow
     */
    BANANA(3),

    /**
     * Small and tart
     */
    CHERRY(5);

    private final int id;

    Fruit(int id)
    {
        this.id = id;
    }

    @ThriftEnumValue
    public int getId()
    {
        return id;
    }
}
