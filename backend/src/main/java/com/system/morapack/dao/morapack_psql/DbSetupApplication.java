package com.system.morapack.dao.morapack_psql;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Escanea únicamente el paquete de entidades ...model (y subpaquetes).
 * Genera/actualiza el esquema JPA con Hibernate y SALE (no queda pegado).
 */
@SpringBootApplication
@EntityScan(basePackages = "com.system.morapack.dao.morapack_psql.model")
public class DbSetupApplication {

  public static void main(String[] args) {
    Map<String, Object> props = new HashMap<>();
    props.put("spring.main.web-application-type", "none");

    props.put("spring.datasource.url",
        getenvOr("DB_URL", "jdbc:postgresql://localhost:5435/postgres"));
    props.put("spring.datasource.username", getenvOr("DB_USER", "postgres"));
    props.put("spring.datasource.password", getenvOr("DB_PASSWORD", "postgres"));

    // create | create-drop | validate | update | none
    props.put("spring.jpa.hibernate.ddl-auto", getenvOr("DDL_AUTO", "update"));
    props.put("spring.jpa.show-sql", "false");
    props.put("spring.jpa.properties.hibernate.default_schema",
        getenvOr("DB_SCHEMA", "public"));

    ConfigurableApplicationContext ctx = new SpringApplicationBuilder(DbSetupApplication.class)
        .web(WebApplicationType.NONE)
        .properties(props)
        .run(args);

    ctx.getBean(EntityManagerFactory.class).createEntityManager().close();
    ctx.close();
  }

  private static String getenvOr(String k, String def) {
    String v = System.getenv(k);
    return (v == null || v.isBlank()) ? def : v;
  }
}