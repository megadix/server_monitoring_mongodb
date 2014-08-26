package monitoring.domain;

import monitoring.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.util.*;

@Document
public class DailyMonitoringData {

    public static class Metadata {
        private String serverName;
        private Date date;
        private String[] metrics;

        public Metadata() {
        }

        public Metadata(String serverName, Date date, String[] metrics) {
            this.serverName = serverName;
            this.date = date;
            this.metrics = metrics;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String[] getMetrics() {
            return metrics;
        }

        public void setMetrics(String[] metrics) {
            this.metrics = metrics;
        }
    }


    @Id
    private String id;
    private Metadata metadata = new Metadata();
    /** 3-levels map: 2-digit hour/2-digit minute/metric =&gt; value */
    private SortedMap<String, SortedMap<String, Map<String, Double>>> data = new TreeMap<>();

    public DailyMonitoringData() {
    }

    public DailyMonitoringData(String serverName, Date day, String[] metrics) {
        this.id = formatId(serverName, day);
        this.metadata.setServerName(serverName);
        this.metadata.setDate(DateUtils.truncate(day, Calendar.DAY_OF_MONTH));
        this.metadata.setMetrics(metrics);
    }

    public static String formatId(String serverName, Date day) {
        String id = DateFormatUtils.dayFormat.format(day) + "/" + serverName;
        return id;
    }

    public void preallocateDay() {
        Calendar cal = DateUtils.toCalendar(metadata.date);

        for (int hour = 0; hour < 24; hour++) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
            for (int minute = 0; minute < 60; minute++) {
                cal.set(Calendar.MINUTE, minute);
                setValue(cal, "mem", null);
                setValue(cal, "cpu", null);
            }
        }
    }

    /**
     * Set a measurement value for the specified timestamp
     * @param cal timestamp - must be of the same day as metadata.date!
     * @param metric metric to insert/update
     * @param value value
     */
    public void setValue(Calendar cal, String metric, Double value) {
        Assert.isTrue(DateUtils.isSameDay(metadata.date, cal.getTime()));

        String hoursKey = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
        String minutesKey = String.format("%02d", cal.get(Calendar.MINUTE));

        SortedMap<String, Map<String, Double>> minutesMap = data.get(hoursKey);
        if (minutesMap == null) {
            minutesMap = new TreeMap<>();
        }

        Map<String, Double> samplesMap = minutesMap.get(minutesKey);
        if (samplesMap == null) {
            samplesMap = new HashMap<>();
        }

        samplesMap.put(metric, value);
        minutesMap.put(minutesKey, samplesMap);
        data.put(hoursKey, minutesMap);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SortedMap<String, SortedMap<String, Map<String, Double>>> getData() {
        return data;
    }

    public void setData(SortedMap<String, SortedMap<String, Map<String, Double>>> data) {
        this.data = data;
    }


}
