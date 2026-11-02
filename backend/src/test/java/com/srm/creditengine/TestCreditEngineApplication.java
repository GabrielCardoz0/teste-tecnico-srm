package com.srm.creditengine;

import org.springframework.boot.SpringApplication;

public class TestCreditEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(CreditEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
