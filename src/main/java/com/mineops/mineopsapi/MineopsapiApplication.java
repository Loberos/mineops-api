package com.mineops.mineopsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de la API de MineOps.
 * <p>
 * La aplicación se organiza en bounded contexts —{@code iam}, {@code assets}, {@code workforce} y
 * {@code operations}—, cada uno con sus capas de dominio, aplicación, infraestructura e interfaces.
 * Los contextos se comunican entre sí únicamente a través de las fachadas publicadas en sus paquetes
 * {@code interfaces.acl}.
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MineopsapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MineopsapiApplication.class, args);
    }
}
