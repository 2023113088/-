package edu.hit.library.book.repo;

import edu.hit.library.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    @Query(
            "select b from Book b where "
                    + "(:q is null or :q = '' or lower(b.title) like lower(concat('%', :q, '%')) "
                    + "or lower(b.author) like lower(concat('%', :q, '%')) "
                    + "or lower(b.isbn) like lower(concat('%', :q, '%'))) "
                    + "and (:category is null or :category = '' or b.category = :category)")
    List<Book> search(@Param("q") String q, @Param("category") String category);
}
