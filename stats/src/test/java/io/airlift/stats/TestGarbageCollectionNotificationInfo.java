package io.airlift.stats;

import com.sun.management.GcInfo;
import org.testng.annotations.Test;
import sun.management.GarbageCollectionNotifInfoCompositeData;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestGarbageCollectionNotificationInfo
{
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "compositeData is null")
    public void testNullCompositeData()
    {
        GarbageCollectionNotificationInfo gcMonitor = new GarbageCollectionNotificationInfo(null);
    }

    @Test(timeOut = 60000)
    public void testSimpleSuccess()
            throws Exception
    {
        // First we try to force a gc
        System.gc();

        GcInfo gcInfo = null;
        // Loop through and get a GcInfo
        while (gcInfo == null) {
            for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
                gcInfo = ((com.sun.management.GarbageCollectorMXBean) mbean).getLastGcInfo();
                if (gcInfo != null) {
                    com.sun.management.GarbageCollectionNotificationInfo info = new com.sun.management.GarbageCollectionNotificationInfo(
                            mbean.getName(),
                            "end of major GC", // indicating this is indeed a major GC
                            "Allocation Failure",
                            gcInfo);

                    GarbageCollectionNotifInfoCompositeData compositeData = new GarbageCollectionNotifInfoCompositeData(info);
                    GarbageCollectionNotificationInfo gcMonitor = new GarbageCollectionNotificationInfo(compositeData);

                    assertTrue(gcMonitor.isMajorGc());
                    assertFalse(gcMonitor.isMinorGc());
                    break;
                }
            }
        }
    }
}
