package com.ashenone.cache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.ashenone.cache.CacheApplicationTests.CacheConfig;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = CacheConfig.class)
@ExtendWith(SpringExtension.class)
class CacheApplicationTests {

	@Autowired
	BookRepository bookRepository;

	@Autowired
	CacheManager cacheManager;

	@BeforeEach
	void setUp() {
		cacheManager.getCache("book").clear();
		when(bookRepository.findByIdWithCache(any())).thenAnswer(invocation -> new Book(invocation.getArgument(0)));
		when(bookRepository.findByBookNameWithoutCache(any())).thenAnswer(invocation -> new Book(invocation.getArgument(0)));
	}

	@Test
	void test_cache() {
		Book var = bookRepository.findByIdWithCache(1L);
		Book foo = bookRepository.findByIdWithCache(1L);

		assertSame(var, foo);
		verify(bookRepository, times(1)).findByIdWithCache(any());
	}

	@Test
	void test_no_cache() {
		Book var = bookRepository.findByBookNameWithoutCache(1L);
		Book foo = bookRepository.findByBookNameWithoutCache(1L);

		assertNotSame(var, foo);
		verify(bookRepository, times(2)).findByBookNameWithoutCache(any());
	}

	@Test
	void test_new_cache() {
		Book var = bookRepository.findByIdWithCache(1L);
		Book foo = bookRepository.findByIdWithCache(2L);

		assertNotSame(var, foo);
		verify(bookRepository, times(2)).findByBookNameWithoutCache(any());
	}

	@Entity
	public class Book {

		@Id
		Long bookId;

		public Book(Long bookId) {
			this.bookId = bookId;
		}
	}

	public interface BookRepository extends JpaRepository<Book, Long> {

		@Cacheable(cacheNames = "book")
		@Query("some query")
		Book findByIdWithCache(Long bookId);

		@Query("some query")
		Book findByBookNameWithoutCache(Long bookId);
	}

	@Configuration
	@EnableCaching
	public static class CacheConfig {

		@Bean
		public BookRepository bookRepository() {
			return mock(BookRepository.class);
		}

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("book");
		}
	}
}
