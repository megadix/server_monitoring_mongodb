package util.data;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.codehaus.groovy.runtime.DateGroovyMethods;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class that generates dummy data for testing purposes.
 * By Dimitri De Franciscis
 * <a href="http://www.megadix.it/">www.megadix.it</a>
 * CCDF3FB0-165D-11E4-8C21-0800200C9A66
 */
public class DataSimulationUtils {
    /**
     * Emulate typical server load with a 24h cycle and peak hours.
     *
     * @param startDate       beginning of measurements
     * @param endDate         end of measurements
     * @param intervalMinutes interval between measurements
     * @param mean            mean value of generated data
     * @param stdDev          standard deviation of generated data
     * @return
     */
    public static SortedMap<Date, Double> simulateServerLoad(Date startDate, Date endDate, int intervalMinutes, double mean, double stdDev) {

        double variability = 0.005;

        TreeMap<Date, Double> result = new TreeMap<Date, Double>();

        // normal distribution
        NormalDistribution valDistrib = new NormalDistribution(mean, stdDev);
        NormalDistribution perturbationDist = new NormalDistribution(0.0, variability);

        // def phase = - Math.PI * 3 / 4
        Double phase = -Math.PI / 2;
        // calculate phase increment for a 24 hours cycle
        double numIncrements = 1440 / intervalMinutes;
        double phaseIncr = Math.PI * 2.0 / numIncrements;
        double perturbation = 0.0;

        Calendar cal = DateGroovyMethods.toCalendar(startDate);

        while (cal.getTime().before(endDate)) {
            Double val = (Math.sin(phase) + (Math.sin(phase) + Math.sin((double) phase * 3.0) / 3.0) + Math.sin((double) phase * 5.0) / 5.0 + Math.sin((double) phase * 7.0) / 7.0 + Math.sin((double) phase * 9.0) / 9.0 + Math.sin((double) phase * 11.0) / 11.0) / 6.0 / 2.0;

            val += Math.sin(perturbation) / 2;
            val += valDistrib.sample();

            val = Math.min(1.0, val);
            val = Math.max(0.0, val);

            result.put(cal.getTime(), val);
            cal.add(Calendar.MINUTE, intervalMinutes);
            phase = phase + phaseIncr;
            perturbation += perturbationDist.sample();
        }


        return ((SortedMap<Date, Double>) (result));
    }

}
