package monitoring.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import monitoring.DailyMonitoringDataReadConverter;
import monitoring.DateFormatUtils;
import monitoring.domain.DailyMonitoringData;
import org.apache.commons.lang3.time.DateUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import util.data.DataSimulationUtils;

import java.text.ParseException;
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
    private Mongo mongo;
    private MongoTemplate mongoTemplate;

    @Autowired
    public void setMongo(Mongo mongo) {
        this.mongo = mongo;
    }

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
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
            DBObject doc = mongo.getDB("monitoring").getCollection("monitoringData").findOne(key);
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

                result.put(cal.getTime(), resultEntry);

            } // for each hour...

            cal.add(Calendar.DAY_OF_MONTH, 1);

        } // // for each day...

        return result;
    }

    /**
     * Setup example: database, data, etc.
     *
     * @throws Exception
     */
    public void setupExample() throws Exception {

        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);

        Date startDate = DateFormatUtils.timestampFormat.parse("20140101_000000");
        Date endDate = DateFormatUtils.timestampFormat.parse("20140108_000000");

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

    public void runExample() throws ParseException {

        // query aggregated view
        SortedMap<Date, Map<String, Double>> view = aggregatedValuesByHour("ATTILA",
                DateFormatUtils.timestampFormat.parse("20140101_000000"),
                DateFormatUtils.timestampFormat.parse("20140108_000000"));

        // build time series of "mem" and "cpu" metrics
        TimeSeries mem = new TimeSeries("Mem");
        TimeSeries cpu = new TimeSeries("Cpu");

        for (Date date : view.keySet()) {
            mem.add(new Hour(date), view.get(date).get("mem") * 100);
            cpu.add(new Hour(date), view.get(date).get("cpu") * 100);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(mem);
        dataset.addSeries(cpu);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Server Load",
                "Time",
                "%",
                dataset,
                true, true, false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRangeAxis().setRange(0.0, 100.0);

        ChartFrame frame = new ChartFrame("Demo", chart);
        frame.pack();
        frame.setVisible(true);
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
