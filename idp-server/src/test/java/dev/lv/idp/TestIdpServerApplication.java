package dev.lv.idp;

import org.springframework.boot.SpringApplication;

public class TestIdpServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(IdpServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
