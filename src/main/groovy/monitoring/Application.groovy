package monitoring

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

import java.text.SimpleDateFormat;

@EnableAutoConfiguration
public class Application implements CommandLineRunner {

    @Autowired
    MonitoringRepository repository

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args)
    }

    @Override
    public void run(String... args) throws Exception {
        def dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")
        def startDate = dateFormat.parse("20140101_000000")
        def cal = startDate.toCalendar()

        repository.deleteAll()

        def data = new MonitoringData(startDate)
        (60*24).times {
            data.addValue(cal.time, Math.random())
            cal.add(Calendar.MINUTE, 1)
        }

        repository.save(data)
    }

}