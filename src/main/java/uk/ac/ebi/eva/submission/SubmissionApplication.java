package uk.ac.ebi.eva.submission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubmissionApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SubmissionApplication.class, args);
    }

}
