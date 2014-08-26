package monitoring.service;

import monitoring.domain.DailyMonitoringData;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import util.data.DataSimulationUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MonitoringService {

    static class ExamplesServerConf {
        String serverName;
        double memMean;
        double memSd;
        double cpuMean;
        double cpuSd;

        ExamplesServerConf(String serverName, double memMean, double memSd, double cpuMean, double cpuSd) {
            this.serverName = serverName;
            this.memMean = memMean;
            this.memSd = memSd;
            this.cpuMean = cpuMean;
            this.cpuSd = cpuSd;
        }
    }

    public static class Sample {
        String name;
        Double value;

        public Sample(String name, Double value) {
            this.name = name;
            this.value = value;
        }
    }

    public static final String COLLECTION_NAME = "monitoringData";

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private DateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private MongoTemplate mongoTemplate;

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void setupExample() throws Exception {

        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);

        Date startDate = timestampFormat.parse("20140101_000000");
        Date endDate = timestampFormat.parse("20140107_000000");

        List<ExamplesServerConf> configurations = new ArrayList<>();
        configurations.add(new ExamplesServerConf("ATTILA", 0.5, 0.01, 0.2, 0.05));
        configurations.add(new ExamplesServerConf("BUBBA", 0.3, 0.2, 0.5, 0.1));
        configurations.add(new ExamplesServerConf("CALIGOLA", 0.1, 0.2, 0.8, 0.2));
        configurations.add(new ExamplesServerConf("DEMOTAPE", 0.8, 0.3, 0.7, 0.4));

        String[] metrics = {"mem", "cpu"};

        for (ExamplesServerConf conf : configurations) {

            // pre-allocate empty data for each day
            Calendar cal = DateUtils.toCalendar(DateUtils.truncate(startDate, Calendar.DAY_OF_MONTH));
            while (cal.getTime().before(endDate)) {
                DailyMonitoringData entry = new DailyMonitoringData(conf.serverName, cal.getTime(), metrics);
                entry.preallocateDay();
                mongoTemplate.insert(entry, COLLECTION_NAME);
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            SortedMap<Date, Double> memData = DataSimulationUtils.simulateServerLoad(startDate, endDate, 1,
                    conf.memMean, conf.memSd);
            SortedMap<Date, Double> cpuData = DataSimulationUtils.simulateServerLoad(startDate, endDate, 1,
                    conf.memMean, conf.memSd);

            int i = 0;
            for (Date timestamp : memData.keySet()) {
                List<Sample> samples = new ArrayList<>(2);
                samples.add(new Sample("mem", memData.get(timestamp)));
                samples.add(new Sample("cpu", cpuData.get(timestamp)));

                addSamples(conf.serverName, timestamp, samples);

                i++;
                if (i > 0 && i % 100 == 0) {
                    log.info("Server: " + conf.serverName + ", saved " + i + " samples");
                }
            }
        }
    }

    public void runExample() {
        throw new UnsupportedOperationException("TODO runExample()");
    }

    /**
     * Store a sample measurement
     *
     * @param serverName
     * @param timestamp
     * @param samples    list of samples to store
     */
    public void addSamples(String serverName, Date timestamp, List<Sample> samples) {
        String id = DailyMonitoringData.formatId(serverName, timestamp);
        Calendar cal = DateUtils.toCalendar(timestamp);
        Update update = new Update();
        for (Sample sample : samples) {
            String hoursKey = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
            String minutesKey = String.format("%02d", cal.get(Calendar.MINUTE));

            String key = "data." + hoursKey + "." + minutesKey + "." + sample.name;
            update.set(key, sample.value);
        }

        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(id)),
                update,
                COLLECTION_NAME
        );
    }

}
