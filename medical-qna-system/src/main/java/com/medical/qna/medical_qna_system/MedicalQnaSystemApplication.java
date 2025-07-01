package com.medical.qna.medical_qna_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;


@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.medical.qna.medical_qna_system.repository.mysql")
@EnableNeo4jRepositories(basePackages = "com.medical.qna.medical_qna_system.repository.neo4j")
public class MedicalQnaSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicalQnaSystemApplication.class, args);
    }
}
