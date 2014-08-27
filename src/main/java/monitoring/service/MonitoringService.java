package monitoring.service;

import com.mongodb.DBObject;
import com.mongodb.Mongo;
import monitoring.DailyMonitoringDataReadConverter;
import monitoring.domain.DailyMonitoringData;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MonitoringService {

    @Autowired
    private MongoProperties mongoProperties;
    private Mongo mongo;
    private MongoTemplate mongoTemplate;
    private String collectionName;

    @Autowired
    public void setMongoProperties(MongoProperties mongoProperties) {
        this.mongoProperties = mongoProperties;
    }

    @Autowired
    public void setMongo(Mongo mongo) {
        this.mongo = mongo;
    }

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Value("${monitoring.collectionName}")
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Query database and aggregate results per hour.
     *
     * @param serverName
     * @param startDate
     * @param endDate
     * @return
     */
    public SortedMap<Date, Map<String, Double>> aggregatedValuesByHour(String serverName, Date startDate, Date endDate) {

        SortedMap<Date, Map<String, Double>> result = new TreeMap<>();

        Calendar cal = DateUtils.toCalendar(startDate);

        // for each day...
        while (cal.getTime().before(endDate)) {

            // build key
            String key = DailyMonitoringData.formatId(serverName, cal.getTime());
            // find data

            // FIXME configure custom converters!
            /*
            DailyMonitoringData data = mongoTemplate.findById(
                    new Query(Criteria.where("_id").is(key)),
                    DailyMonitoringData.class);
            */
            DBObject doc = mongo.getDB(mongoProperties.getDatabase()).getCollection(collectionName).findOne(key);
            DailyMonitoringData data = new DailyMonitoringDataReadConverter().convert(doc);

            // not found?
            if (data == null) {
                continue;
            }

            // for each hour...
            for (String hh : data.getData().keySet()) {
                Map<String, Integer> samplesCount = new HashMap<>();
                Map<String, Double> samplesTotals = new HashMap<>();

                SortedMap<String, Map<String, Double>> minutes = data.getData().get(hh);

                // for each minute...
                for (String mm : minutes.keySet()) {
                    Map<String, Double> sample = minutes.get(mm);
                    // for each sample...
                    for (String metric : sample.keySet()) {
                        // update count
                        if (samplesCount.containsKey(metric)) {
                            samplesCount.put(metric, samplesCount.get(metric) + 1);
                        } else {
                            samplesCount.put(metric, 1);
                        }

                        // update total
                        if (samplesTotals.containsKey(metric)) {
                            samplesTotals.put(metric, samplesTotals.get(metric) + sample.get(metric));
                        } else {
                            samplesTotals.put(metric, sample.get(metric));
                        }
                    } // for each sample...
                } // for each minute...

                // build result entry for this hour
                Map<String, Double> resultEntry = new HashMap<>();

                for (String metric : samplesTotals.keySet()) {
                    Double avg = samplesTotals.get(metric) / samplesCount.get(metric);
                    resultEntry.put(metric, avg);
                }

                Calendar calEntry = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH);
                calEntry.add(Calendar.HOUR_OF_DAY, Integer.valueOf(hh));
                result.put(calEntry.getTime(), resultEntry);

            } // for each hour...

            cal.add(Calendar.DAY_OF_MONTH, 1);

        } // // for each day...

        return result;
    }

    /**
     * Store a sample measurement, Map of: name =&gt; value
     *
     * @param serverName
     * @param timestamp
     * @param samples     samples to store: name =&gt; value
     */
    public void addSample(String serverName, Date timestamp, Map<String, Double> samples) {
        String id = DailyMonitoringData.formatId(serverName, timestamp);
        Calendar cal = DateUtils.toCalendar(timestamp);
        Update update = new Update();

        for (Map.Entry<String, Double> sample : samples.entrySet()) {
            String hoursKey = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
            String minutesKey = String.format("%02d", cal.get(Calendar.MINUTE));
            String key = "data." + hoursKey + "." + minutesKey + "." + sample.getKey();
            update.set(key, sample.getValue());
        }

        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(id)),
                update,
                collectionName
        );
    }

}
