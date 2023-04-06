package main.rice;
import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.parse.*;
import main.rice.test.*;
import java.io.IOException;
import java.util.*;

/**
 * The Main class is the entry point to this test case generator.
 * It ties together all the different components and generates a concise set of test cases.
 *
 */
public class Main {
    /**
     * a variable to store the name of the function we are generating the set of test cases
     */
    private static String fname;

    /**
     * This method takes as its input a String[] that contains three string arguments: the path to the config file,
     * path to the directory containing the buggy implementations and the path to the reference solution and passes this String[] to generate test,
     * then it prints the resulting concise test set.
     * @param args - a String[] containing the 3 string paths to files necessary to create the concise test set
     *
     */
     public static void main(String[] args) throws IOException, InvalidConfigException, InterruptedException{
         Set<TestCase> tests = generateTests(args);
         System.out.println("The concise test set for "+fname+": "+tests);
     }

    /**
     * This method ties together all the components of FEAT and generates the concise test set for the specific input String[] args.
     * @param args String[] of paths to files necessary for FEAT
     * @return the concise test set
     * @throws IOException if a file cant be read or a file path is invalid
     * @throws InterruptedException if computeExpectedResults() or runTests() is interrupted
     * @throws InvalidConfigException if the input file cant be parsed i.e. the input file is invalid
     */
    public static Set<TestCase> generateTests(String[] args) throws IOException,InvalidConfigException, InterruptedException{
         String configPath = args[0]; // get the strings
         String pathToBuggy = args[1];
         String refSoln = args[2];
         ConfigFileParser parser = new ConfigFileParser(); // create a config file parser object
         ConfigFile configFile = parser.parse(parser.readFile(configPath)); // create a config file by parsing the file in the config path
         BaseSetGenerator bSetGen = new BaseSetGenerator(configFile.getNodes(),configFile.getNumRand()); // pass the nodes and num rand,
         List<TestCase> baseTests = bSetGen.genBaseSet(); // then call gen base set
         fname= configFile.getFuncName();
         Tester tester = new Tester(configFile.getFuncName(),refSoln,pathToBuggy,baseTests); // create a tester obj and pass to constructor
         tester.computeExpectedResults(); // compute expected results
         TestResults res = tester.runTests();
        return ConciseSetGenerator.setCover(res);} // get the concise set
 }