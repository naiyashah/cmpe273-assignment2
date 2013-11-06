package edu.sjsu.cmpe.library.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;

import edu.sjsu.cmpe.library.api.resources.producer;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;

public class BookRepository implements BookRepositoryInterface {
    /** In-memory map to store books. (Key, Value) -> (ISBN, Book) */
    private final ConcurrentHashMap<Long, Book> bookInMemoryMap;

    /** Never access this key directly; instead use generateISBNKey() */
    /** Never access this key directly; instead use generateISBNKey() */
    private long isbnKey;
    private String apolloUser;
    private String apolloPassword;
    private String apolloHost;
    private String apolloPort;
    private String stompQueue;
    private String stompTopic;
    private String libraryName;
    private final producer producer;

	private final LibraryServiceConfiguration configuration;

    public BookRepository(LibraryServiceConfiguration config) {
	bookInMemoryMap = seedData();
	 this.configuration = config;
     this.producer = new producer(config);
     apolloUser = configuration.getApolloUser();
     apolloPassword = configuration.getApolloPassword();
     apolloHost = configuration.getApolloHost();
     apolloPort = configuration.getApolloPort();
     stompQueue = configuration.getStompQueueName();
     stompTopic = configuration.getStompTopicName();
     libraryName = configuration.getLibraryName();
     System.out.println(apolloUser);
	isbnKey = 0;
    }
    private ConcurrentHashMap<Long, Book> seedData(){
	ConcurrentHashMap<Long, Book> bookMap = new ConcurrentHashMap<Long, Book>();
	Book book = new Book();
	book.setIsbn(1);
	book.setCategory("computer");
	book.setTitle("Java Concurrency in Practice");
	try {
	    book.setCoverimage(new URL("http://goo.gl/N96GJN"));
	} catch (MalformedURLException e) {
	    // eat the exception
	}
	bookMap.put(book.getIsbn(), book);

	book = new Book();
	book.setIsbn(2);
	book.setCategory("computer");
	book.setTitle("Restful Web Services");
	try {
	    book.setCoverimage(new URL("http://goo.gl/ZGmzoJ"));
	} catch (MalformedURLException e) {
	    // eat the exception
	}
	bookMap.put(book.getIsbn(), book);

	return bookMap;
    }

    /**
     * This should be called if and only if you are adding new books to the
     * repository.
     * 
     * @return a new incremental ISBN number
     */
    private final Long generateISBNKey() {
	// increment existing isbnKey and return the new value
	return Long.valueOf(++isbnKey);
    }

    /**
     * This will auto-generate unique ISBN for new books.
     */
    @Override
    public Book saveBook(Book newBook) {
	checkNotNull(newBook, "newBook instance must not be null");
	// Generate new ISBN
	Long isbn = generateISBNKey();
	newBook.setIsbn(isbn);
	// TODO: create and associate other fields such as author

	// Finally, save the new book into the map
	bookInMemoryMap.putIfAbsent(isbn, newBook);

	return newBook;
    }

    /**
     * @see edu.sjsu.cmpe.library.repository.BookRepositoryInterface#getBookByISBN(java.lang.Long)
     */
    @Override
    public Book getBookByISBN(Long isbn) {
	checkArgument(isbn > 0,
		"ISBN was %s but expected greater than zero value", isbn);
	return bookInMemoryMap.get(isbn);
    }

    @Override
    public List<Book> getAllBooks() {
	return new ArrayList<Book>(bookInMemoryMap.values());
    }

    /*
     * Delete a book from the map by the isbn. If the given ISBN was invalid, do
     * nothing.
     * 
     * @see
     * edu.sjsu.cmpe.library.repository.BookRepositoryInterface#delete(java.
     * lang.Long)
     */
    @Override
    public void delete(Long isbn) {
	bookInMemoryMap.remove(isbn);
	
    }
    
    public Book update(Long isbn,Status status) throws JMSException
    {
    	Book book = getBookByISBN(isbn);
    	book.setStatus(status);
    	bookInMemoryMap.put(isbn, book);
    	String msg = configuration.getLibraryName()+":"+isbn;
    	producer.producer(msg);
    	
    	        
        return book;

        

    }

    private static String env(String key, String defaultValue) {
        String rc = System.getenv(key);
        if( rc== null ) {
            return defaultValue;
        }
        return rc;
    }

    private static String arg(String []args, int index, String defaultValue) {
        if( index < args.length ) {
            return args[index];
        } else {
            return defaultValue;
        }
    
    	
    	//stomp producr
    	
    }

}
