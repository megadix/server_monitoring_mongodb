package monitoring;

import monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan
public class Application implements CommandLineRunner {

    @Autowired
    private MonitoringService monitoringService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Missing command-line arguments: COMMAND");
            System.err.println("COMMAND:");
            System.err.println("  setup : create database, sample data and functions");
            System.err.println("  run : run example client");

            System.exit(1);
        }

        if ("SETUP".equalsIgnoreCase(args[0])) {
            monitoringService.setupExample();
        } else if ("RUN".equalsIgnoreCase(args[0])) {
            monitoringService.runExample();
        }
    }

}