package com.mygroupid.rabbitmq.rabbit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TaskManager {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");  
    
    String activityStartTime;
    String activityEndTime;
    String command;
    String authDevice;
    String authUser;
    int tot;

    //Ritorna i dispositivi IOT di un determinato utente
    @GetMapping("/mytasks")
    public Response getMyTasks(@RequestParam("id") String id) {
    
        File folder = new File("storage");
        File[] listOfFiles = folder.listFiles();
        ArrayList<JSONObject> retArr = new ArrayList<JSONObject>();
    
        for(int i=0; i<listOfFiles.length; i++) {
                
            JSONParser jsonParser = new JSONParser();
            try(FileReader reader = new FileReader(listOfFiles[i])) {
                    Object obj = new Object();
                    try {
                        obj = jsonParser.parse(reader);
                    } catch (org.json.simple.parser.ParseException e) {
                         e.printStackTrace();
                    }
                    JSONObject jsonObject = (JSONObject) obj;
                    if(jsonObject.get("userId").equals(id)) retArr.add(jsonObject);
                } catch (FileNotFoundException e1) {
                    return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
                } catch (IOException e1) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("IO Exception").build();
            }
        }
        return Response.ok(retArr, MediaType.APPLICATION_JSON).build();
    }

    //Metodo che contatta i dispositivi IOT per l'inizio o la fine di un'attività
    @SuppressWarnings("rawtypes")
    @PostMapping("/task")
    public Response startActivity(@RequestBody Map body) throws Exception {
        JSONObject json = new JSONObject(body); 
        authUser = json.get("userId").toString();
        String command = (String) json.get("control");
        json.remove("control");
        String message = "";
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (com.rabbitmq.client.Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {    
                if(command.equals("start")) {
                    message = "Start";
                    LocalDateTime now = LocalDateTime.now();  
                    activityStartTime = dtf.format(now).toString();  
                    channel.basicPublish("", "deviceCommandQueue", null, message.getBytes(StandardCharsets.UTF_8));
                    SubscriberTask(json.get("userId").toString());
                   
                } else {
                    message = "Stop";
                    LocalDateTime now = LocalDateTime.now();  
                    activityEndTime = dtf.format(now).toString();
                    writeActivity(json.get("userId").toString(), json.get("id").toString());
                    channel.basicPublish("", json.get("id").toString(), null, message.getBytes(StandardCharsets.UTF_8));
                }
            System.out.println("Ho mandato sulla coda dei comandi: " + message);
        }

        return Response.status(Response.Status.OK).entity(message).build();
    }

    //Metodo che contatta i dispositivi IOT per permettergli di autenticarsi
    @SuppressWarnings("rawtypes")
    @PostMapping("/auth")
    public Response authDevice(@RequestBody Map body) {
        JSONObject json = new JSONObject(body); 
        try {
            subscriberAuth(json);
            authDevice = json.get("id").toString();
            authUser = json.get("userId").toString();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String message = "Authenticate";
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (com.rabbitmq.client.Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) { 
                channel.basicPublish("", "deviceAuthQueue", null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Ho mandato sulla coda dell'autenticazione: " + message);
        } catch (TimeoutException e1) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Timeout exception").build();
         } catch (IOException e1) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("IO Exception").build();
         }
    
        return Response.status(Response.Status.OK).entity("Already created").build();
    }

    //Metodo che riceve i dati dai dispositivi IOT tramite RabbitMQ
    public void subscriberAuth(JSONObject iotDevice) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.println("In attesa sulla coda di autenticazione.");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Ho ricevuto: " + message);
            JSONParser parser = new JSONParser();  
            try {
                JSONObject json = (JSONObject) parser.parse(message);
                if(json.get("id").equals(authDevice)){
                    json.put("userId",authUser);
                
                    URL url = new URL("http://localhost:8082/iot");
                    URLConnection con = url.openConnection();
                    HttpURLConnection http = (HttpURLConnection)con;
                    http.setRequestMethod("POST"); // PUT is another valid option
                    http.setDoOutput(true);
                    byte[] out = json.toString().getBytes(StandardCharsets.UTF_8);
                    int length = out.length;

                    http.setFixedLengthStreamingMode(length);
                    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    http.connect();
                    try(OutputStream os = http.getOutputStream()) {
                    os.write(out);
                    System.out.println("Dispositivo iscritto!");
                    }    
                }
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }  
        };
        channel.basicConsume("managerAuthQueue", true, deliverCallback, consumerTag -> { });
    }

    //Metodo che contatta l'User Manager per aggiungere i punti
    public void addPoints(JSONObject json) throws IOException{
        URL url = new URL("http://localhost:8081/ecopoints");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        json.replace("userId", json.get("userId").toString(), authUser);
        byte[] out = json.toString().getBytes(StandardCharsets.UTF_8);
        int length = out.length;
        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try(OutputStream os = http.getOutputStream()) {
            os.write(out);
            System.out.println("Punti mandati!");
        }
    }

    //Metodo che controlla tramite l'IOT manager se un dispositivo IOT è autenticato
    public String checkAuth(JSONObject json) throws IOException{
        URL url = new URL("http://localhost:8082/checkAuthIot?id="+json.get("id").toString()+"&userId="+authUser+"&psw="+json.get("password").toString());
        HttpURLConnection connection2 = (HttpURLConnection) url.openConnection();
        connection2.setRequestProperty("accept", "application/json");
        InputStream inputStream = connection2.getInputStream();
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    //Metodo che riceve i dati di una certa attività dai dispositivi IOT
    public void SubscriberTask(String userId) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        command = "start";
        System.out.println("In attesa sulla coda delle attività.");
        tot = 0;
        JSONParser parser = new JSONParser();  

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Ho ricevuto: " + message);
            
            if(message.equals("Activity Stopped")){ 
                
                JSONObject pointsToAdd = new JSONObject();
                pointsToAdd.put("userId", userId);
                pointsToAdd.put("ecopoints", tot);
                addPoints(pointsToAdd);
             
                command = "Stop";
                return;
            }
    
            try {
                JSONObject json = (JSONObject) parser.parse(message);
                json.put("userId",userId);
                if(checkAuth(json).equals("true")){
                    tot += (Integer.parseInt(json.get("activity").toString()))*2;
                }
              
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }  

        };

        if(command == "Stop"){
            return;
        } 
        channel.basicConsume("managerTaskQueue", true, deliverCallback, consumerTag -> { });
    }

    //Metodo che salva nello storage le attività svolte
    public void writeActivity(String userId, String deviceId){
        JSONObject json = new JSONObject();
        json.put("userId", userId);  
        json.put("deviceId", deviceId);  
        json.put("activityStartDate", activityStartTime);  
        json.put("activityEndDate", activityEndTime);  
        json.put("earnedEcoPoints", Integer.toString(tot));
        int min = 00001;
        int max = 99999;

        String random_int = Integer.toString((int)Math.floor(Math.random()*(max-min+1)+min));
       
        try (FileWriter file = new FileWriter("storage/"+random_int+".json")) {
            file.write((json).toJSONString()); 
            file.flush();
            System.out.println("scritto: "+random_int);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

