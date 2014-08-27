package monitoring.domain;

import monitoring.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class DailyMonitoringDataTest {

    @Test
    public void testFormatId() throws Exception {
        assertEquals("server-1/20140101", DailyMonitoringData.formatId("server-1",
                DateFormatUtils.timestampFormat.parse("20140101_103421")));
        assertEquals("server-1/20140101", DailyMonitoringData.formatId("server-1",
                DateFormatUtils.timestampFormat.parse("20140101_000000")));
    }

    @Test
    public void testPreallocateDay() throws Exception {
        String[] metrics = {"cpu", "mem"};
        DailyMonitoringData data = new DailyMonitoringData("server-1",
                DateFormatUtils.timestampFormat.parse("20140101_103421"), metrics);
        data.preallocateDay();

        assertEquals(24, data.getData().size());
        for (int hours = 0; hours < 24; hours++) {
            String hh = String.format("%02d", hours);
            assertEquals(60, data.getData().get(hh).size());
            for (int minutes = 0; minutes < 60; minutes++) {
                String mm = String.format("%02d", minutes);
                assertEquals(2, data.getData().get(hh).get(mm).size());
                assertTrue(data.getData().get(hh).get(mm).containsKey("mem"));
                assertNull(data.getData().get(hh).get(mm).get("mem"));
                assertTrue(data.getData().get(hh).get(mm).containsKey("cpu"));
                assertNull(data.getData().get(hh).get(mm).get("cpu"));
            }
        }
    }

    @Test
    public void testSetValue() throws Exception {
        String[] metrics = {"cpu", "mem"};
        DailyMonitoringData data = new DailyMonitoringData("server-1",
                DateFormatUtils.timestampFormat.parse("20140101_103421"), metrics);

        assertEquals(0, data.getData().size());

        Date dt;

        dt = DateFormatUtils.timestampFormat.parse("20140101_100121");
        data.setValue(DateUtils.toCalendar(dt), "cpu", 0.1234);
        assertEquals(1, data.getData().size());
        assertTrue(data.getData().containsKey("10"));
        assertTrue(data.getData().get("10").containsKey("01"));
        assertTrue(data.getData().get("10").get("01").containsKey("cpu"));
        assertEquals(0.1234, data.getData().get("10").get("01").get("cpu").doubleValue(), 0.0);

        dt = DateFormatUtils.timestampFormat.parse("20140101_113401");
        data.setValue(DateUtils.toCalendar(dt), "mem", 0.1234);
        assertEquals(2, data.getData().size());
        assertTrue(data.getData().containsKey("11"));
        assertTrue(data.getData().get("11").containsKey("34"));
        assertTrue(data.getData().get("11").get("34").containsKey("mem"));
        assertEquals(0.1234, data.getData().get("11").get("34").get("mem").doubleValue(), 0.0);
    }

    @Test
    public void testId() throws Exception {
        DailyMonitoringData data = new DailyMonitoringData();
        assertNull(data.getId());

        data.getMetadata().setServerName("server-1");
        assertNull(data.getId());

        data.getMetadata().setDate(DateFormatUtils.timestampFormat.parse("20140101_103421"));
        assertEquals("server-1/20140101", data.getId());

        data.getMetadata().setServerName("server-2");
        assertEquals("server-2/20140101", data.getId());

        data.getMetadata().setDate(DateFormatUtils.timestampFormat.parse("20140102_103421"));
        assertEquals("server-2/20140102", data.getId());

    }
}