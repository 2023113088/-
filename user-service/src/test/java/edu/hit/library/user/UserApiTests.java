package edu.hit.library.user;

import edu.hit.library.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
            "eureka.client.enabled=false",
            "spring.cloud.config.enabled=false",
            "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes-long"
        })
@AutoConfigureMockMvc
class UserApiTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void registersReaderAndLogsIn() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reader1","password":"123456","email":"reader1@test.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("READER")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reader1","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role", is("READER")));
    }

    @Test
    void registersAdminWhenRoleProvided() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin1","password":"123456","email":"admin1@test.com","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }
}
