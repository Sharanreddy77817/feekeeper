package in.feekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FeeKeeperApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeeKeeperApplication.class, args);
    }

    @Bean
    Clock businessClock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}
