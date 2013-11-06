package edu.sjsu.cmpe.procurement.jobs;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.util.ajax.JSON;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;

/**
 * This job will run at every 5 second.
 */
@Every("5s")
public class ProcurementSchedulerJob extends Job {
    private final Logger log = LoggerFactory.getLogger(getClass());
    String ar[];
    JSONArray msg;
    @Override
    public void doJob() {
	String strResponse = ProcurementService.jerseyClient.resource(
		"http://ip.jsontest.com/").get(String.class);
	log.debug("Response from jsontest.com: {}", strResponse);
	//stmp consumr
	
	ProcurementSchedulerJob jobs = new ProcurementSchedulerJob();
	try {
		String lostBooks=jobs.Consumer();
		System.out.println("*************lostbooks*************");
		
		if(lostBooks != null)
		{
			
		jobs.post(lostBooks);
		}
		String[] availableBooks;
		
		availableBooks = jobs.get();
		jobs.pub(availableBooks);
		
	} catch (JMSException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
}
    public void post(String lostbooks)
    {
    	
    	try {
    		 
    		DefaultHttpClient httpClient = new DefaultHttpClient();
    		HttpPost postRequest = new HttpPost(
    			"http://54.215.210.214:9000/orders");
     
    		StringEntity input = new StringEntity("{\"id\":\"69637\",\"order_book_isbns\":"+lostbooks+"}");
    		input.setContentType("application/json");
    		postRequest.setEntity(input);
     
    		HttpResponse response = httpClient.execute(postRequest);
     
    		if (response.getStatusLine().getStatusCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : "
    				+ response.getStatusLine().getStatusCode());
    		}
     
    		BufferedReader br = new BufferedReader(
                            new InputStreamReader((response.getEntity().getContent())));
     
    		String output;
    		System.out.println("Output from Server .... \n");
    		while ((output = br.readLine()) != null) {
    			System.out.println(output);
    		}
    		
     
    		
    		httpClient.getConnectionManager().shutdown();
     
    	  } catch (MalformedURLException e) {
     
    		e.printStackTrace();
     
    	  } catch (IOException e) {
     
    		e.printStackTrace();
     
    	  }
     
    	}
     
    
    public String[] get() throws ParseException
    {
    	
    	try {
    		 
    		DefaultHttpClient httpClient = new DefaultHttpClient();
    		HttpGet getRequest = new HttpGet(
    			"http://54.215.210.214:9000/orders/69637");
    		getRequest.addHeader("accept", "application/json");
    		HttpResponse response = httpClient.execute(getRequest);
     
    		if (response.getStatusLine().getStatusCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : "
    			   + response.getStatusLine().getStatusCode());
    		}
     
    		BufferedReader br = new BufferedReader(
                             new InputStreamReader((response.getEntity().getContent())));
     
    		String output;
    		System.out.println("Output from Server .... \n");
    		while ((output = br.readLine()) != null) {
    			System.out.println(output);
    			JSONParser parser = new JSONParser();
    			Object obj = parser.parse(output);
    			 
    			JSONObject jsonObject = (JSONObject) obj;
    			msg = (JSONArray) jsonObject.get("shipped_books");
    			Iterator<String> iterator = msg.iterator();
    			ar = new String[msg.size()];
    			
    			for(int i=0;i<msg.size();i++)
    			{
    				JSONObject book = (JSONObject) msg.get(i);
    				ar[i]=book.get("isbn")+":\""+book.get("title")+"\":"+book.get("category")+"\":"+book.get("coverimage");
    				System.out.println(ar[i]);
    			}
    		}
    		
    		
     
    		httpClient.getConnectionManager().shutdown();
     
    	  } catch (ClientProtocolException e) {
     
    		e.printStackTrace();
     
    	  } catch (IOException e) {
     
    		e.printStackTrace();
    	  }
     return ar;
    	}
  //publshr
    public void pub(String[] availablebooks) throws JMSException
    {
    String user = env("APOLLO_USER", "admin");
    String password = env("APOLLO_PASSWORD", "password");
    String host = env("APOLLO_HOST", "54.215.210.214");
    int port = Integer.parseInt(env("APOLLO_PORT", "61613"));
    String destination = "/topic/69637.book";

    StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
    factory.setBrokerURI("tcp://" + host + ":" + port);

    Connection connection = factory.createConnection(user, password);
    connection.start();
    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    
 for(int i=0;i<availablebooks.length;i++)
 {
	 JSONObject ob = (JSONObject)msg.get(i);
	 	 destination = destination+"."+ob.get("category");
	 	Destination dest = new StompJmsDestination(destination);
	    MessageProducer producer = session.createProducer(dest);
	    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	    String data = availablebooks[i];
	    TextMessage msg = session.createTextMessage(data);
	    msg.setLongProperty("id", System.currentTimeMillis());
	    producer.send(msg);
	 
 }
    
    /**
     * Notify all Listeners to shut down. if you don't signal them, they
     * will be running forever.
     */
    
    connection.close();

}


	//publishr
    
    
    
    
    
    public String Consumer() throws JMSException
    {
    	String user = env("APOLLO_USER", "admin");
        String password = env("APOLLO_PASSWORD", "password");
        String host = env("APOLLO_HOST", "54.215.210.214");
        int port = Integer.parseInt(env("APOLLO_PORT", "61613"));
        String queue = "/queue/69637.book.orders";
        String destination = queue;

        StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
        factory.setBrokerURI("tcp://" + host + ":" + port);

        Connection connection = factory.createConnection(user, password);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination dest = new StompJmsDestination(destination);
        ArrayList<String> isbns = new ArrayList<String>();
        MessageConsumer consumer = session.createConsumer(dest);
        System.out.println("Waiting for messages from " + queue + "...");
        while(true) {
            Message msg = consumer.receive(500);
            if(msg==null)
            {
            break;
            }
            if( msg instanceof  TextMessage ) {
                String body = ((TextMessage) msg).getText();
                isbns.add(body.split(":")[1]);
                System.out.println("Received message = " + body);
            } else if (msg instanceof StompJmsMessage) {
                StompJmsMessage smsg = ((StompJmsMessage) msg);
                String body = smsg.getFrame().contentAsString();
                System.out.println("Received message = " + body);

            } else {
                System.out.println("Unexpected message type: "+msg.getClass());
            }
        }
        connection.close();
    String lostbooks = isbns.toString();
    System.out.println("Returnin books:"+lostbooks);
    return lostbooks;
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
    	//stmp consmr
    	
        }
   
    

    
    }
    
