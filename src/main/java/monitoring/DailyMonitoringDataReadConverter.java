package monitoring;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import monitoring.domain.DailyMonitoringData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Deserialize {@link monitoring.domain.DailyMonitoringData} from MongoDB objects {@link com.mongodb.BasicDBObject}
 */
@ReadingConverter
public class DailyMonitoringDataReadConverter implements Converter<DBObject, DailyMonitoringData> {

    @Override
    public DailyMonitoringData convert(DBObject doc) {
        DailyMonitoringData result = new DailyMonitoringData();

        DBObject metadata = (DBObject) doc.get("metadata");

        result.getMetadata().setServerName((String) metadata.get("serverName"));
        result.getMetadata().setDate((Date) metadata.get("date"));
        BasicDBList docMetrics = (BasicDBList) metadata.get("metrics");
        String[] metrics = new String[docMetrics.size()];
        for (int i = 0; i < docMetrics.size(); i++) {
            metrics[i] = (String) docMetrics.get(i);
        }

        result.getMetadata().setMetrics(metrics);

        DBObject data = (DBObject) doc.get("data");
        for (String hhKey : data.keySet()) {
            DBObject hhDoc = (DBObject) data.get(hhKey);
            SortedMap<String, Map<String, Double>> hhMap = new TreeMap<>();
            result.getData().put(hhKey, hhMap);

            for (String mmKey : hhDoc.keySet()) {
                DBObject mmDoc = (DBObject) hhDoc.get(mmKey);
                Map<String, Double> mmMap = mmDoc.toMap();
                hhMap.put(mmKey, mmMap);
            }
        }

        return result;
    }
}
