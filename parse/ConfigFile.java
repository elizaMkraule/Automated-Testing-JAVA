package main.rice.parse;
import main.rice.node.APyNode;
import java.util.*;
/**
 * This class represents the contents of a single config file more specifically
 * it bundles together three pieces of data that will be extracted from the config file during the parsing process.
 */
public class ConfigFile {


    /**
     *The name of the function under test.
     */
    String funcName;
    /**
     *A List of PyNodes that will be used to generate TestCases for the function under test.
     */
    List<APyNode<?>> nodes;
    /**
     *The number of random test cases to be generated.
     */
    int numRand;

    /**
     * Constructor for a ConfigFile object, which takes in three pieces of data:
     * @param nodes - A List of PyNodes that will be used to generate TestCases for the function under test.
     * @param funcName - The name of the function under test.
     * @param numRand - The number of random test cases to be generated.
     */
     public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand) {
         this.funcName = funcName;
         this.nodes = nodes;
         this.numRand = numRand;
     }

    /**
     * Returns the name of the function under test.
     * @return funcName
     */
    public String getFuncName(){return this.funcName;}
    /**
     *Returns the List of PyNodes that will be used to generate TestCases for the function under test.
     * @return nodes a list of nodes from this config file
     */
    public List<APyNode<?>> getNodes(){
         return this.nodes;
    }

    /**
     * Returns the number of random test cases to be generated.
     * @return numRand the int numRand from this config file
     */
    public int getNumRand(){return this.numRand;}

 }