package monitoring;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.util.*;

@Document
public class MonitoringData {

    @Id
    private String id;
    /**
     * Two levels map: 2-digit hour/2-digit minute
     */
    private Map<String, Map<String, Double>> data = new TreeMap<String, Map<String, Double>>();

    public MonitoringData() {
        this(new Date());
    }

    public MonitoringData(Date date) {
        this.id = DateFormatUtils.getDayFormat().format(date);
    }

    public void addValue(Date dt, Double value) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(dt);

        int hours = cal.get(Calendar.HOUR_OF_DAY);
        String hoursKey = hours < 10 ? "0" + hours : "" + hours;

        int minutes = cal.get(Calendar.MINUTE);
        String minutesKey = minutes < 10 ? "0" + minutes : "" + minutes;

        if (data.containsKey(hoursKey)) {
            data.get(hoursKey).put(minutesKey, value);
        } else {
            HashMap<String, Double> entry = new HashMap<>();
            entry.put(minutesKey, value);
            data.put(hoursKey, entry);
        }

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Map<String, Double>> getData() {
        return data;
    }

    public void setData(Map<String, Map<String, Double>> data) {
        this.data = data;
    }


}
