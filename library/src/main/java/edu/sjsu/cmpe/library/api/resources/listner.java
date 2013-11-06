package edu.sjsu.cmpe.library.api.resources;

import java.net.MalformedURLException;
import java.net.URL;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class listner {

	
	//listnr
	
		private String apolloUser;
	     private String apolloPassword;
	     private String apolloHost;
	     private String apolloPort;
	     private String stompTopic;
	     private final LibraryServiceConfiguration configuration;
	     private BookRepositoryInterface bookRepository;
	     private long isbn;
	     private String title;
	     private String category;
	     private String coverimage;
	     
	     Book addbk = new Book();
	     public listner(LibraryServiceConfiguration config, BookRepositoryInterface bookRepository) 
	     {
	         this.configuration = config;
	         this.bookRepository = bookRepository;
	         apolloUser = configuration.getApolloUser();
	         apolloPassword = configuration.getApolloPassword();
	         apolloHost = configuration.getApolloHost();
	         apolloPort = configuration.getApolloPort();
	         stompTopic = configuration.getStompTopicName();
	     }
	     
	     public Runnable listener() throws JMSException, MalformedURLException
	     {
	    	 StompJmsConnectionFactory topicfactory = new StompJmsConnectionFactory();
	     	topicfactory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);

	     	Connection topicconnection = topicfactory.createConnection(apolloUser, apolloPassword);
	     	topicconnection.start();
	     	Session topicsession = topicconnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	     	Destination topicdest = new StompJmsDestination(stompTopic);

	     	MessageConsumer topicconsumer = topicsession.createConsumer(topicdest);
	     	System.currentTimeMillis();
	     	System.out.println("Waiting for messages...");
	     	while(true) {
	     	    Message topicmsg = topicconsumer.receive();
	     	    if(topicmsg== null)
	     	    {
	     	    	break;
	     	    }
	     	    if( topicmsg instanceof  TextMessage ) {
	     		String body = ((TextMessage) topicmsg).getText();
	     		isbn = Long.parseLong(body.split(":")[0]);
	     		title = body.split(":")[1];
	     		category = body.split(":")[2];
	     		coverimage= body.split(":")[3];
	     		addbk = bookRepository.getBookByISBN(isbn);
	     		if(addbk != null){
	     		addbk.setIsbn(isbn);
	     		addbk.setTitle(title);
	     		addbk.setStatus(addbk.getStatus());
	     		addbk.setCategory(category);
	     		addbk.setCoverimage(new URL (coverimage));
	     		bookRepository.saveBook(addbk);
	     		
	     		
	     		}
	     		
	     		System.out.println("Received message = " + body);

	     	    } else if (topicmsg instanceof StompJmsMessage) {
	     		StompJmsMessage smsg = ((StompJmsMessage) topicmsg);
	     		String body = smsg.getFrame().contentAsString();
	     		System.out.println("Received message = " + body);

	     	    } else {
	     		System.out.println("Unexpected message type: "+topicmsg.getClass());
	     	    }
	     	}
	     	topicconnection.close();
			return topicsession;
	     }
	}
	
	//listner

