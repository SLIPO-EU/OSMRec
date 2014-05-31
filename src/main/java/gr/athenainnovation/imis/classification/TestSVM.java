
package gr.athenainnovation.imis.classification;

import gr.athenainnovation.imis.OSMRec.OSMRec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

/**
 * Provides functionality for executing the SVM test.
 * 
 * @author imis-nkarag
 */

public class TestSVM {
    
    private float score;
    private final String model;
    private final String path;
    private final String output;
    
    public TestSVM(String path, String model, String output){
        score = 100;
        this.path = path;
        this.model = model;
        this.output = output;
    }
    
    public void executeTest(){      
        
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArray);
        PrintStream original = System.out;       

        String classificationLine = path + "/src/main/resources/svm_multiclass_classify "
        + path + "/target/classes/output/vectors "
        + path + "/target/classes/output/" + model + " " 
        + path + "/target/classes/output/"+ output;     
        
        System.setOut(printStream);           
        
        CommandLine commandLineClassification = CommandLine.parse(classificationLine);
        DefaultExecutor classificationExecutor = new DefaultExecutor();
        classificationExecutor.setExitValue(0);
        String svmTestOutput = null;
        
        try {
            classificationExecutor.execute(commandLineClassification);
            
            printStream.close();
            System.setOut(original);
            svmTestOutput = byteArray.toString();
            System.out.println(svmTestOutput);

            String[] scoreContainer = svmTestOutput.split("\\n");
            
            int index = scoreContainer[5].indexOf("%");           
            String scoreString = scoreContainer[5].substring(index-5, index);   //(28, 34);
            score = Float.parseFloat(scoreString);
            setScore(score);
            
        }
        catch(NumberFormatException e){
            
            String[] scoreContainer = svmTestOutput.split("\\n");
            int index = scoreContainer[5].indexOf("%");
            String scoreString = scoreContainer[5].substring(index-4, index);
            score = Float.parseFloat(scoreString);
            setScore(score);
            
        }
        catch (IOException ex) {
            
            Logger.getLogger(OSMRec.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    private void setScore(float score){
        this.score = score;
    }
    
    public float getScore(){
        return score;
    }
    
}