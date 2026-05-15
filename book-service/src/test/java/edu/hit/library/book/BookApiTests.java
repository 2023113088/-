package edu.hit.library.book;

import edu.hit.library.book.repo.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
            "eureka.client.enabled=false",
            "spring.cloud.config.enabled=false",
            "server.port=9002"
        })
@AutoConfigureMockMvc
class BookApiTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookRepository books;

    @Test
    void exposesSampleBooksAndInstanceInfo() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) books.count())));

        mockMvc.perform(get("/api/books/instance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service", is("BOOK-SERVICE")))
                .andExpect(jsonPath("$.port", is("9002")));
    }
}
