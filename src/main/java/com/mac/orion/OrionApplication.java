package com.mac.orion;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication/*(scanBasePackages = "com.mac.orion.ui")*/
public class OrionApplication {

    public static void main(String[] args) {
//		SpringApplication.run(OrionApplication.class, args);
        DesktopApplicationLoader.boot(args);
    }


}
