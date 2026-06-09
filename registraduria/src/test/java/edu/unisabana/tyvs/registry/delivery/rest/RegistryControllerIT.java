// src/test/java/edu/unisabana/tyvs/registry/delivery/rest/RegistryControllerIT.java
package edu.unisabana.tyvs.registry.delivery.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;

// src/test/java/.../RegistryControllerIT.java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RegistryControllerIT {

    @TestConfiguration
    static class TestBeans {
        @Bean
        public RegistryRepositoryPort registryRepositoryPort() throws Exception {
            String jdbc = "jdbc:h2:mem:regdb;DB_CLOSE_DELAY=-1";
            var repo = new edu.unisabana.tyvs.registry.infrastructure.persistence.RegistryRepository(jdbc);
            repo.initSchema();
            return repo;
        }

        @Bean
        public edu.unisabana.tyvs.registry.application.usecase.Registry registry(RegistryRepositoryPort port) {
            return new edu.unisabana.tyvs.registry.application.usecase.Registry(port);
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    public void shouldRegisterValidPerson() {
        String json = "{\"name\":\"Ana\",\"id\":100,\"age\":30,\"gender\":\"FEMALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assert resp.getStatusCode() == HttpStatus.OK;
        assert "VALID".equals(resp.getBody());
    }

    @Test
    public void shouldReturnBadRequestForInvalidGender() {
        String json = "{\"name\":\"Laura\",\"id\":103,\"age\":25,\"gender\":\"INVALID_GENDER\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assert resp.getStatusCode() == HttpStatus.BAD_REQUEST;
        assert "INVALID_INPUT".equals(resp.getBody());
    }

    @Test
    public void shouldReturnUnderageForMinor() {
        String json = "{\"name\":\"Luis\",\"id\":104,\"age\":16,\"gender\":\"MALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assert resp.getStatusCode() == HttpStatus.OK;
        assert "UNDERAGE".equals(resp.getBody());
    }

    @Test
    public void shouldReturnDeadForDeceased() {
        String json = "{\"name\":\"Marta\",\"id\":105,\"age\":60,\"gender\":\"FEMALE\",\"alive\":false}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assert resp.getStatusCode() == HttpStatus.OK;
        assert "DEAD".equals(resp.getBody());
    }
}
