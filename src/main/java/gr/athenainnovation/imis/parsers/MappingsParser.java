package gr.athenainnovation.imis.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * MappingsParser takes the Map file which contains the LGD mappings and extracts information
 * about the OSM tags and the ontology classes
 * This info is used in the vector construction.
 * 
 * @author imis-nkarag
 */

public class MappingsParser {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MappingsParser.class);
    private final HashMap<String, String> mappings;
    private final HashMap<String, Integer> mappingsWithIDs;
    
    public MappingsParser(){    
    
       mappings = new HashMap<>();
       mappingsWithIDs = new HashMap<>();
    
    }
        
    public void parseFile(File file) throws FileNotFoundException {
                 
         Scanner input = new Scanner(file); //the Map file contains lines of the mappings separated with the symbol "|"
                                            //e.g. highway motorway | Motorway
                                            //the key will be the string "highway motorway" and the value "Motorway"
         while(input.hasNext()) {
          
             String nextLine = input.nextLine();
             String[] splitContent = nextLine.split("\\|",2);   //split current line in two parts, separated by the "|" symbol
             String key = splitContent[0];                      //got the key which will be mapped at a class  
             String value = splitContent[1];                    //the value which is the mapped class
             key = key.trim();                                  
             value = value.trim();                              
             mappings.put(key, value);
         }
         //System.out.println("mappings size: " + mappings.size());  //debugging check
         constructMappingsWithIDs();
         
    LOG.info("Mappings file parsed successfully!");     
    } 
    
    private void constructMappingsWithIDs(){
        Integer i = 1;
        for (String ontologyClass : mappings.values()){  
            
            mappingsWithIDs.put(ontologyClass, i);           
            i++;
        }       
    }
    
    public HashMap getMappingsWithIDs(){
        return this.mappingsWithIDs;
    }
    
    public HashMap getMappings(){
        return this.mappings;
    }    
    
}