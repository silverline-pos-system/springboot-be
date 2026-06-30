package com.nsbm.rocs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication
@EnableAsync
public class RocsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RocsApplication.class, args);
		log.info("==============================================");
		log.info("  ROCS Backend Started Successfully!");
		log.info("  Health: http://localhost:8080/api/v1/health");
		log.info("==============================================");
	}
}
