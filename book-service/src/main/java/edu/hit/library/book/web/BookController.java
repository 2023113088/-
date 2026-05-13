package edu.hit.library.book.web;

import edu.hit.library.book.entity.Book;
import edu.hit.library.book.repo.BookRepository;
import edu.hit.library.book.web.dto.BookRequest;
import edu.hit.library.book.web.dto.BookResponse;
import edu.hit.library.book.web.dto.StockDelta;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookRepository books;

    public BookController(BookRepository books) {
        this.books = books;
    }

    private static BookResponse toDto(Book b) {
        return new BookResponse(
                b.getId(),
                b.getTitle(),
                b.getAuthor(),
                b.getIsbn(),
                b.getPublisher(),
                b.getCategory(),
                b.getStock(),
                b.getPrice());
    }

    @GetMapping
    public List<BookResponse> list() {
        return books.findAll().stream().map(BookController::toDto).toList();
    }

    @GetMapping("/search")
    public List<BookResponse> search(
            @RequestParam(required = false) String q, @RequestParam(required = false) String category) {
        String cq = q != null && !q.isBlank() ? q.trim() : null;
        String cc = category != null && !category.isBlank() ? category.trim() : null;
        return books.search(cq, cc).stream().map(BookController::toDto).toList();
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return books.findById(id).map(BookController::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest req) {
        Book b = new Book();
        apply(b, req);
        return toDto(books.save(b));
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest req) {
        Book b = books.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在"));
        apply(b, req);
        return toDto(books.save(b));
    }

    @PutMapping("/{id}/stock")
    public BookResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockDelta delta) {
        Book b = books.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在"));
        int next = b.getStock() + delta.delta();
        if (next < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存不足");
        }
        b.setStock(next);
        return toDto(books.save(b));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!books.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在");
        }
        books.deleteById(id);
    }

    private void apply(Book b, BookRequest req) {
        b.setTitle(req.title());
        b.setAuthor(req.author());
        b.setIsbn(req.isbn());
        b.setPublisher(req.publisher());
        b.setCategory(req.category());
        b.setStock(req.stock());
        b.setPrice(req.price());
    }
}
