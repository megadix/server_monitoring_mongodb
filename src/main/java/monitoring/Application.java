package monitoring;

import monitoring.domain.DailyMonitoringData;
import monitoring.service.MonitoringService;
import org.apache.commons.lang3.time.DateUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import util.data.DataSimulationUtils;

import java.text.ParseException;
import java.util.*;

@EnableAutoConfiguration
@ComponentScan
public class Application implements CommandLineRunner {

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

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private MonitoringService monitoringService;
    private MongoTemplate mongoTemplate;

    @Autowired
    public void setMonitoringService(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Setup example: database, data, etc.
     *
     * @throws Exception
     */
    public void setupExample() throws Exception {

        mongoTemplate.dropCollection(monitoringService.getCollectionName());
        mongoTemplate.createCollection(monitoringService.getCollectionName());

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
                mongoTemplate.insert(entry, monitoringService.getCollectionName());
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            SortedMap<Date, Double> memData = DataSimulationUtils.simulateServerLoad(startDate, endDate, 1,
                    conf.memMean, conf.memSd);
            SortedMap<Date, Double> cpuData = DataSimulationUtils.simulateServerLoad(startDate, endDate, 1,
                    conf.memMean, conf.memSd);

            int i = 0;
            for (Date timestamp : memData.keySet()) {
                Map<String, Double> samples;

                samples = new HashMap<>();
                samples.put("mem", memData.get(timestamp));
                samples.put("cpu", cpuData.get(timestamp));

                monitoringService.addSample(conf.serverName, timestamp, samples);

                i++;
                if (i > 0 && i % 100 == 0) {
                    log.info("Server: " + conf.serverName + ", saved " + i + " samples");
                }
            }
        }
    }

    public void runExample() throws ParseException {

        // query aggregated view
        SortedMap<Date, Map<String, Double>> view = monitoringService.aggregatedValuesByHour("ATTILA",
                DateFormatUtils.timestampFormat.parse("20140101_000000"),
                DateFormatUtils.timestampFormat.parse("20140102_000000"));

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
     * Application entry point
     *
     * @param args
     */
    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Missing command-line arguments: COMMAND");
            System.err.println("COMMAND:");
            System.err.println("  SETUP : create database, sample data and functions");
            System.err.println("  RUN : run example client");

            System.exit(1);
        }

        if ("SETUP".equalsIgnoreCase(args[0])) {
            setupExample();

        } else if ("RUN".equalsIgnoreCase(args[0])) {
            runExample();
        }
    }
}