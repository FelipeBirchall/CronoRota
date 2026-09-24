package com.cronorota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Ponto de entrada da aplicação. O Spring Boot varre este pacote (com.cronorota)
// e todos os sub-pacotes em busca de @Component, @Service, @Repository, @RestController, etc.
@SpringBootApplication
public class CronorotaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CronorotaApiApplication.class, args);
    }
}
