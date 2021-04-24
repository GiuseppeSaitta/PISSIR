package mygroupdi.myspringapp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class IoTManager {

    
    //Aggiunta dispositivo IOT
    @SuppressWarnings("rawtypes")
    @Produces({ MediaType.APPLICATION_JSON })
    @PostMapping("/iot")
    public Response postDevice(@RequestBody Map body) {
        JSONObject json = new JSONObject(body);  
        File tmpDir = new File("storage/"+json.get("id")+".json");
        boolean exists = tmpDir.exists();
        if(!exists){
            try (FileWriter file = new FileWriter("storage/"+json.get("id")+".json")) {
                System.out.println("Ho salvato "+json.toString());
                file.write((json).toJSONString()); 
                file.flush();
                
            } catch (IOException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("IO Exception").build();
           }
        }
        return Response.status(Response.Status.OK).entity(json.toString()).build();
    }

    //Ritorna i dispositivi IOT di un determinato utente
    @GetMapping("/iot")
    @PreAuthorize("isAuthenticated()")
    public Response  getMyDevices(@RequestParam("id") String id) {

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

    //Cancella un dispositivo IOT
    @SuppressWarnings("unchecked")
    @DeleteMapping("/iot")
    @PreAuthorize("isAuthenticated()")
    public Response deleteMyDevice(@RequestParam("id") String id, @RequestParam("userId") String userId) {

        File folder = new File("storage");
		File[] listOfFiles = folder.listFiles();
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("userId",userId);
    
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
                 if(jsonObject.get("id").equals(json.get("id")) && jsonObject.get("userId").equals(json.get("userId"))){
                    String page = "./storage/"+json.get("id")+".json";
                    File myObj = new File(page);
                    if(!myObj.delete()) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Delete Error").build();
                 }
              } catch (FileNotFoundException e1) {
                return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
             } catch (IOException e1) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("IO Exception").build();
             }
            
        }
        return Response.status(Response.Status.OK).entity("Deleted").build();
    }

        //Ritorna "true" se il dispositivo è autenticato e "false" nel caso non lo sia
        @GetMapping("/checkAuthIot")
        public String checkIfAuth(@RequestParam("id") String id, @RequestParam("userId") String userId,@RequestParam("psw") String psw) {
            File folder = new File("storage");
            File[] listOfFiles = folder.listFiles();
        
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
                     if(jsonObject.get("userId").toString().equals(userId) && jsonObject.get("id").toString().equals(id) && jsonObject.get("password").toString().equals(psw)){
                        System.out.println("Autenticated");
                        return "true";
                     } 
                  } catch (FileNotFoundException e1) {
                 
                     e1.printStackTrace();
                 } catch (IOException e1) {
                   
                     e1.printStackTrace();
                 }
                
            }
            System.out.println("Not autenticated");
            return "false";
        }

}
