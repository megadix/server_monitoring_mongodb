package monitoring.domain;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import monitoring.DailyMonitoringDataReadConverter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DailyMonitoringDataReadConverterTest {

    @Test
    public void testConvert() throws Exception {
        DBObject doc = new BasicDBObject();

        // metadata

        DBObject metadata = new BasicDBObject();
        Date dt = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        metadata.put("serverName", "server-1");
        metadata.put("date", dt);
        BasicDBList metrics = new BasicDBList();
        metrics.add(0, "mem");
        metrics.add(1, "cpu");
        metadata.put("metrics", metrics);

        doc.put("metadata", metadata);

        // data

        DBObject data = new BasicDBObject();
        for (int hh = 0; hh < 24; hh++) {
            DBObject hhDoc = new BasicDBObject();
            for (int mm = 0; mm < 60; mm++) {
                DBObject mmDoc = new BasicDBObject();
                mmDoc.put("mem", 0.5);
                mmDoc.put("cpu", 0.6);
                hhDoc.put(String.format("%02d", mm), mmDoc);
            }
            data.put(String.format("%02d", hh), hhDoc);
        }

        doc.put("data", data);

        // check

        DailyMonitoringDataReadConverter conv = new DailyMonitoringDataReadConverter();
        DailyMonitoringData result = conv.convert(doc);
        assertEquals("server-1", result.getMetadata().getServerName());
        assertEquals(dt, result.getMetadata().getDate());
        assertTrue(ArrayUtils.contains(result.getMetadata().getMetrics(), "mem"));
        assertTrue(ArrayUtils.contains(result.getMetadata().getMetrics(), "cpu"));

        assertNotNull(result.getData());
        for (int hh = 0; hh < 24; hh++) {
            String hhKey = String.format("%02d", hh);
            assertTrue(result.getData().containsKey(hhKey));

            SortedMap<String, Map<String, Double>> hhMap = result.getData().get(hhKey);
            assertNotNull(hhMap);

            for (int mm = 0; mm < 60; mm++) {
                String mmKey = String.format("%02d", mm);
                assertTrue(hhMap.containsKey(mmKey));
                Map<String, Double> mmMap = hhMap.get(mmKey);
                assertNotNull(mmMap);

                assertEquals(0.5, mmMap.get("mem").doubleValue(), 0.0);
                assertEquals(0.6, mmMap.get("cpu").doubleValue(), 0.0);
            }
        }
    }
}