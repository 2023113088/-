package edu.hit.library.book.bootstrap;

import edu.hit.library.book.entity.Book;
import edu.hit.library.book.repo.BookRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 空库时写入示例书目（H2 与 MySQL 均生效）；按 ISBN 幂等，多实例并行启动不会重复插入。
 */
@Component
public class BookSampleDataRunner implements ApplicationRunner {

    private final BookRepository books;

    public BookSampleDataRunner(BookRepository books) {
        this.books = books;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (books.findByIsbn("978-DEMO-0001").isEmpty()) {
            Book b1 = new Book();
            b1.setTitle("云原生技术实践");
            b1.setAuthor("实验指导组");
            b1.setIsbn("978-DEMO-0001");
            b1.setPublisher("校内");
            b1.setCategory("计算机");
            b1.setStock(5);
            b1.setPrice(0);
            books.save(b1);
        }
        if (books.findByIsbn("978-DEMO-0002").isEmpty()) {
            Book b2 = new Book();
            b2.setTitle("微服务架构设计");
            b2.setAuthor("Martin Fowler");
            b2.setIsbn("978-DEMO-0002");
            b2.setPublisher("图灵");
            b2.setCategory("计算机");
            b2.setStock(3);
            b2.setPrice(89);
            books.save(b2);
        }
    }
}
