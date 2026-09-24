package com.cronorota.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelogioConfig {

    // Relógio da aplicação no fuso da operação (seção 24.2). Injetado em vez
    // de chamar "now()" direto, para os testes poderem fixar o instante.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
