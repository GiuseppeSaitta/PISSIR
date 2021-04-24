

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.simple.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class IoTDevice {

    private final static String DEVICE_AUTH_QUEUE= "deviceAuthQueue";
    private final static String MANAGER_AUTH_QUEUE= "managerAuthQueue";
    private final static String DEVICE_COMMAND_QUEUE= "deviceCommandQueue";
    private final static String AUTH_ROUTINGKEY = "auth";
    private final static String idDevice = "1003";
    private final static String password = "psw";
    private static String command = "";
    
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.println("In attesa sulle code di autenticazione e comando");
            

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        	
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Ho ricevuto: " + message);
            if (message.equals("Authenticate")) {
            	JSONObject json = new JSONObject();
            	json.put("id", idDevice);
            	json.put("password", password);
            	message = json.toString();
                channel.basicPublish("", MANAGER_AUTH_QUEUE, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Ho mandato sulla coda ID: " + message);
                channel.queueDeclare(idDevice, false, false, false, null);
            } else if(message.equals("Start")) {
            	try {
            		channel.queueDeclare(idDevice, false, false, false, null);
					subId();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        };
        channel.basicConsume(DEVICE_AUTH_QUEUE, true, deliverCallback, consumerTag -> { });
        channel.basicConsume(DEVICE_COMMAND_QUEUE, true, deliverCallback, consumerTag -> { });
    }
    
    public static void subId() throws IOException, TimeoutException {
    	ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        command = "Start";
        System.out.println("in attesa sulla coda ID");
        

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        	
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Ho ricevuto sulla coda ID: " + message);
            
            if(message.equals("Stop")) {
            	command = "Stop";
            	String stopMessage = "Activity Stopped";
                channel.basicPublish("", "managerTaskQueue", null, stopMessage.getBytes(StandardCharsets.UTF_8));
            }
        };
        
        channel.basicConsume(idDevice, true, deliverCallback, consumerTag -> { });
    
        
        
        while(command!="Stop") {
      
        	JSONObject activityMessage  = new JSONObject();
        	activityMessage.put("id", idDevice);
        	activityMessage.put("password", password);
        	activityMessage.put("activity", "1");
        	channel.basicPublish("", "managerTaskQueue", null, activityMessage.toString().getBytes(StandardCharsets.UTF_8));
        	System.out.println("Pubblicato sulla coda dei Task: "+activityMessage.toString());
        	try {
				TimeUnit.SECONDS.sleep(7);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
    }
}