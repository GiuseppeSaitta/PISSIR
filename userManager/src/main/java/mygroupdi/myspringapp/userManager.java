package mygroupdi.myspringapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("rawtypes")
@RestController
public class userManager {

    //Aggiunta di un utente al DB
    @PostMapping("/user")
    @Produces({ MediaType.APPLICATION_JSON })
    @PreAuthorize("isAuthenticated()")
    public Response postUser(@RequestBody Map body) {
        JSONObject json = new JSONObject(body);  
        File tmpDir = new File("storage/"+json.get("id")+".json");
        boolean exists = tmpDir.exists();
        if(!exists){
            try (FileWriter file = new FileWriter("storage/"+json.get("id")+".json")) {
                file.write((json).toJSONString()); 
                file.flush();
            
            } catch (IOException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("IO Exception").build();
           }
        }  else  return Response.status(Response.Status.OK).entity("Already created").build();
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    //Aggiunta degli Ecopoints agli utenti
    @PostMapping("/ecopoints")
    public Response postEcopoints(@RequestBody Map body) throws IOException, ParseException {
        JSONObject json = new JSONObject(body);  
        File tmpDir = new File("storage/"+json.get("userId")+".json");

        JSONParser jsonParser = new JSONParser();
       
            FileReader reader = new FileReader(tmpDir);
            
            JSONObject oldData = (JSONObject) jsonParser.parse(reader);
            int oldPoints = Integer.parseInt(oldData.get("ecopoints").toString());
            int newPoints = Integer.parseInt(json.get("ecopoints").toString());
            int tot = oldPoints + newPoints;

            JSONObject newData = new JSONObject();
                
            newData.put("name", oldData.get("name").toString());
            newData.put("id", oldData.get("id").toString());
            newData.put("email", oldData.get("email").toString());
            newData.put("ecopoints", tot);

            tmpDir.delete();
                
            FileWriter file = new FileWriter("storage/"+newData.get("id")+".json");
            file.write((newData).toJSONString()); 
            file.flush();
            System.out.println("Ho aggiunto "+newPoints+" punti a "+ newData.get("name").toString());
            file.close();
            return Response.status(Response.Status.OK).entity("Already created").build();
    }

    //Ritorna i dati di un utente
    @GetMapping("/user")
    @Produces({ MediaType.APPLICATION_JSON })
    @PreAuthorize("isAuthenticated()")
    public Response getMyInformation(@RequestParam("id") String id) {
        JSONObject ret = new JSONObject();
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
                 if(jsonObject.get("id").equals(id)) ret = jsonObject;
              } catch (FileNotFoundException e1) {
                return Response.status(Response.Status.NOT_FOUND).entity("File not Found").build();
             } catch (IOException e1) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("IO Exception").build();
             }
        }
        return Response.ok(ret, MediaType.APPLICATION_JSON).build();
    }

}
