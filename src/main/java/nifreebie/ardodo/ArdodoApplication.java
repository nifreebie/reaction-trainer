package nifreebie.ardodo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class ArdodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArdodoApplication.class, args);
    }
}
