package com.example.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
// main method to run the Eureka Server application
	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}

}
