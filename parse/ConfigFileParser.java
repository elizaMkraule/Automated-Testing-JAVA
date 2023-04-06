package main.rice.parse;
import main.rice.node.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
/**
 *This class represents the config file parser object. It reads a JSON file, parses its contents and returns a ConfigFile
 */
public class ConfigFileParser {

    /**
     * counter used to recurse over the APyNode tree and match the corresponding domains
     */
    private int counter;

    /**
     * This method Reads and returns the contents of the file located at the input filepath
     *
     * @param filepath path to the file containing the config file
     * @return a string of the contents of the config file at filepath
     * @throws IOException  if the file does not exist or cannot be read.
     */
    public String readFile(String filepath) throws IOException {
        File f = new File(filepath);
        return Files.readString(f.toPath());
    }

    /**
     * This method parses the input string i.e. the contents of a JSON file formatted according to the config file specifications
     * and builds APyNode tree for each parameter, where each node's type, exhaustive domain, and random domain  reflects the contents of the config file.
     * The list of nodes along with the parsed function name and number of random tests to generate, is then  placed in a new ConfigFile object and returned.
     *
     * @param contents a string contents of a JSON file formatted according to the config file specifications
     * @return a ConfigFile object consisting of the nodes, rand num and function name as specified in the JSON file
     */
    public ConfigFile parse(String contents) throws InvalidConfigException {
        // create the list of nodes to pass to config file
        List<APyNode<?>> nodes = new ArrayList<>();

        // create a JSON obj
        JSONObject jsonObj;
        try {
            jsonObj = new JSONObject(contents);
        } catch (Exception e) {
            throw new InvalidConfigException("invalid json");
        }

        //GET FUNCTION NAME
        String function;
        try {
            function = jsonObj.getString("fname");
        } catch (Exception e) {
            throw new InvalidConfigException("missing or invalid function name");
        }

        //GET TYPES
        JSONArray types;
        try {
            types = jsonObj.getJSONArray("types");
        } catch (Exception e) {
            throw new InvalidConfigException("missing types");
        }
        //parse types by iterating over types and parsing each
        for (Object type : types) {
            APyNode<?> node;
            try {
                node = parseType(type.toString()); // then return the corresponding node and add to a list of nodes
            } catch (Exception e) {
                throw new InvalidConfigException("invalid type");
            }
            nodes.add(node);
        }

        // GET EXHAUSTIVE AND RANDOM DOMAIN
        JSONArray exDomain;
        JSONArray ranDomain;
        try {
            exDomain = jsonObj.getJSONArray("exhaustive domain");// check if is valid
            ranDomain = jsonObj.getJSONArray("random domain"); // check if is valid
        } catch (Exception e) {
            throw new InvalidConfigException("missing domains");
        }
        // iterate over each domain , parse it and assign it to the corresponding node
        for (int i = 0; i < exDomain.length(); i++) {
            ArrayList<ArrayList<? extends Number>> exhaustiveD;
            ArrayList<ArrayList<? extends Number>> randomD;
            // first check if the domain is valid
            try {
                exhaustiveD = parseDom(exDomain.get(i).toString(), types.get(i).toString());
                randomD = parseDom(ranDomain.get(i).toString(), types.get(i).toString());
            } catch (Exception e) {
                throw new InvalidConfigException("invalid domains");
            }
            // then assign rand domain and ex domain to the corresponding node and its children w a helper function assignDomains
            this.counter = 0;
            assignDomains(nodes.get(i), exhaustiveD, randomD);
        }
        // GET NUM RANDOM
        Object randNum;
        int rand; // initialize to negative
        try {
            randNum = jsonObj.get("num random");// get the JSON obj
            rand = parseRand(randNum);
        } catch (Exception e) {
            throw new InvalidConfigException("missing  or invalid rand num");
        }

        // RETURN A CONFIG FILE
        return new ConfigFile(function, nodes,  rand);
    }

    /**
     * This method parses the values of the types in the JSON file ,determines if the type is expressed in grammar specified for the config file, and if the types are valid
     * returns the corresponding node (if its only one node) or root node to the tree, if not valid throws InvalidConfigException.
     *
     * @param type a string of the types representing nodes and its children from the JSONArray of the key "types"
     * @return  ApyNode corresponding to the type specifications in the JSON file
     * @throws InvalidConfigException if the types are invalid and not adhering to the grammar specified for the config file
     */
    private static APyNode<?> parseType(String type) throws InvalidConfigException {
        if ((type.endsWith(":")) || (type.endsWith("("))) {
            throw new InvalidConfigException("invalid type");
        }
        // Strip any excess whitespace off of the type
        type = type.strip(); // this is not necesary

        String firstType;
        String restTypes;

        // then we check if it's a compound type by checking where the parenthesis is
        int parenIdx = type.indexOf("(");
        // if there is no parenthesis its a simple type so we strip and continue
        if (parenIdx == -1) {
            firstType = type.strip();
            restTypes = "";
        } else { // else we split them and check what is what
            firstType = type.substring(0, parenIdx).strip();
            restTypes = type.substring(parenIdx + 1).strip();
        }

        // Parse the outermost value, recursing as need be (if the outermost type is List or Map)
        switch (firstType) {
            // simple types
            case "int":
                if (Objects.equals(restTypes, "")) {
                    return new PyIntNode();
                } else throw new InvalidConfigException("not a valid type ");
            case "float":
                if (Objects.equals(restTypes, "")) {
                    return new PyFloatNode();
                } else throw new InvalidConfigException("not a valid type ");
            case "bool":
                if (Objects.equals(restTypes, "")) {
                    return new PyBoolNode();
                } else throw new InvalidConfigException("not a valid type ");
                // nested types
            case "tuple":
                return new PyTupleNode<>(parseType(restTypes));
            case "list":
                return new PyListNode<>(parseType(restTypes));
            case "dict":
                return parseDict(restTypes);
            case "set":
                return new PySetNode<>(parseType(restTypes));
            case "str":
                return new PyStringNode(restTypes); // should check the char domain is valid?
            default:
                // Type did not match any of the supported types
                throw new InvalidConfigException("not a valid type ");
        }

    }
    /**
     * This helper method is used parse the node types whose parent node is a dictionary, as in if the type string contains a dictionary
     * the rest of the string is passed here to determine the structural validity, if valid a PyDictNode of the speficied children is returned
     *
     * @param type string expressing an iterable domain
     * @return a PyDictNode adhering to the type
     * @throws InvalidConfigException if the type is not actually a dictionary as in doesn't contain a colon
     */
    private static PyDictNode<?,?> parseDict(String type) throws InvalidConfigException {
        String firstType;
        String restTypes;

        // then we check if it's a compound type by checking where the parenthesis is
        int parenIdx = type.indexOf(":");
        // i there is no colon its not a dictionary
        if (parenIdx == -1) {
            throw new InvalidConfigException("not a dictionary");
        } else { // else we split them and check what is what
            firstType = type.substring(0, parenIdx).strip();
            restTypes = type.substring(parenIdx + 1);
        }
        APyNode<?> left = parseType(firstType);
        APyNode<?> right = parseType(restTypes);
        return new PyDictNode<>(left, right);
    }

    /**
     * This method parses the values of the domains in the JSON file ,determines the validity of the domains by checking if they are expressed
     * in grammar specified for the config file and if they correspond to the node types, if the domains are valid it
     * returns a list of lists containing the parsed domains, if domains are not valid throws InvalidConfigException.
     *
     * @param domains a string of the types representing nodes and its children from the JSONArray of the key "types"
     * @param types - a string of types
     * @return dom - a list of lists containing the domains
     * @throws InvalidConfigException if the domains are invalid,not adhering to the grammar specified for the config file and not compatible with the types.
     */
    public ArrayList<ArrayList<? extends Number>> parseDom(String domains, String types) throws InvalidConfigException {

        ArrayList<ArrayList<? extends Number>> dom = new ArrayList<>(); // check if its malformed
        if ((domains.strip().endsWith(":")) || (domains.strip().endsWith("("))) {
            throw new InvalidConfigException("invalid domain");
        }

        int colonIdxD = domains.indexOf(":"); // get the indexes of colons and parenthesis
        int parenIdxD = domains.indexOf("(");
        int colonIdxT = types.indexOf(":");
        int parenIdxT = types.indexOf("(");


        // if the type has a colon but domain doesn't or vice versa then it's an invalid definition
        if (((colonIdxD == -1) && (colonIdxT > -1)) || ((colonIdxD > -1) && (colonIdxT == -1))) {
            throw new InvalidConfigException("incompatible type and domain");
        }

        // if no parenthesis or colon in domain it's a simple type or string type
        if ((colonIdxD == -1) && (parenIdxD == -1)) {
            if (types.strip().contains("str")) {
                dom.add(parseDomHelper(domains.strip()));
            } else dom.add(parseSimpleDom(domains, types));
        }
        // if it has parenthesis but no colon in domain it's a iterable type of list,set or tuple
        else if ((colonIdxD == -1) && (parenIdxD > 0)) {// iterable type
            String[] doms = domains.split("[(]");
            String[] typ = types.split("[(]");
            for (int i = 0; i < doms.length; i++) {
                if (doms[i].equals("")) {
                    throw new InvalidConfigException("invalid domain");
                }
                switch (typ[i].strip()) {
                    // simple types
                    case "int" -> dom.add(parseDomInt(doms[i].strip()));
                    case "float" -> dom.add(parseDomFloat(doms[i].strip()));
                    case "bool" -> dom.add(parseDomBool(doms[i].strip()));
                    // iterable types
                    case "list", "str", "set", "dict", "tuple" -> dom.add(parseDomHelper(doms[i].strip()));
                    default ->
                        // Type did not match any of the supported types
                            throw new InvalidConfigException("not a valid type ");
                }
            }
        }
        // if domain has parenthesis and colon  it's a dictionary
        else if ((colonIdxD > 0) && (parenIdxD > 0)) {
            dom.add(parseIterableDom(domains.substring(0, parenIdxD), types.substring(0, parenIdxT))); // check compatibility of first the first domain and type
            dom.addAll((parseDom(domains.substring(parenIdxD + 1, colonIdxD), types.substring(parenIdxT + 1, colonIdxT)))); // parse the parts on the left side of colon
            dom.addAll((parseDom(domains.substring(colonIdxD + 1), types.substring(colonIdxT + 1)))); //// parse the parts on the right side of colon

        } else throw new InvalidConfigException("invalid domain");

        return dom;
    }

    /**
     * This method traverses the APyNode tree recursively and assigns the corresponding random and exhaustive domains
     *
     * @param node the to assign the domains to
     * @param random a list of random domains for the node and its children
     * @param exhaustive a list of exhaustive domains for the node and its children
     */
    public void assignDomains(APyNode<?> node, ArrayList<ArrayList<? extends Number>> exhaustive, ArrayList<ArrayList<? extends Number>> random) {

        node.setRanDomain(random.get(this.counter)); // assign domains to the root
        node.setExDomain(exhaustive.get(this.counter));

        if (node.getLeftChild() != null) { // traverse left subtree
            this.counter++;
            assignDomains(node.getLeftChild(), exhaustive, random);
        }
        if (node.getRightChild() != null) { // traverse right subtree
            counter++;
            assignDomains(node.getRightChild(), exhaustive, random);
        }
    }
    /**
     * This helper method is used parse the iterable type (list,set,tuple,dict and string) domains
     *
     * @param domain string expressing an iterable domain
     * @return domList - a list containing the domain expressed in domain string.
     * @throws InvalidConfigException if the domain is malformed or invalidly defined
     */
    private ArrayList<? extends Number> parseDomHelper(String domain) throws InvalidConfigException {
        ArrayList<Integer> domList;
        HashSet<Integer> domSet = new HashSet<>();

        // check if domain is expressed as a list
        if (domain.startsWith("[") || domain.endsWith("]")) {
            String values = domain.substring(1, domain.length() - 1);
            String[] vals = values.split(",");
            for (String val : vals
            ) {
                int n = Integer.parseInt(val.strip()); //check if its non-negative
                if (n < 0) {
                    throw new InvalidConfigException("invalid value in domain for iterable type");
                }
                else {
                    domSet.add(Integer.parseInt(val.strip()));
                }
            }
            domList = new ArrayList<>(domSet); // create a list from set to remove duplicates from domain
        } else {
            // else if domain is expressed as a range check that the range is valid
            int tildeIdx = domain.indexOf("~");
            String start = domain.substring(0, tildeIdx).strip();
            String end = domain.substring(tildeIdx + 1).strip();
            int s;
            int f;
            try {
                s = Integer.parseInt(start);
                f = Integer.parseInt(end);
            } catch (Exception e) {
                throw new InvalidConfigException(" invalid integer in domain ");
            }

            if ((s <= f)&&(s>=0)) { // check if range is valid and if its non-negative
                domList = makeList(s, f);
            } else {
                throw new InvalidConfigException("invalid definition of domain");
            }
        }
        return domList;
    }
    /**
     * This helper method is used to make sure that an iterable type is actually an iterable type  and is compatible with its domain as expressed in the config file specification and
     * checks which of the iterable types it, then passes the type and domain for to the corresponding helper method.
     *
     * @param dom a string expressing an iterable domain
     * @param type a string expressing an iterable type
     * @return domList - a list containing the domain expressed in dom string.
     * @throws InvalidConfigException if the input type doesn't match any of the iterable types
     */
    public ArrayList<? extends Number> parseIterableDom(String dom, String type) throws InvalidConfigException {
        return switch (type.strip()) {
            case "list", "set", "dict", "tuple" -> parseDomHelper(dom.strip());
            default ->
                // Type did not match any of the supported types
                    throw new InvalidConfigException(" type and domain dont match ");
        };
    }
    /**
     * This helper method is used to make sure that a simple type is actually a simple type as expressed in the config file specification and
     * checks which of the simple types it, then passes the type and domain for to the corresponding helper method.
     *
     * @param dom a string expressing a simple domain
     * @param type a string expressing a simple type
     * @return domList -  a list containing the domain expressed in intDom string.
     * @throws InvalidConfigException if the input type doesn't match any of the simple types
     */
    public ArrayList<? extends Number> parseSimpleDom(String dom, String type) throws InvalidConfigException {
        return switch (type.strip()) {
            // simple types
            case "int" -> parseDomInt(dom.strip());
            case "float" ->  parseDomFloat(dom.strip());
            case "bool" ->  parseDomBool(dom.strip());
            default -> throw new InvalidConfigException("not compatible type and domain ");
        };
    }
    /**
     * This method parses the int domain by checking if the values adhere to the int grammar rules
     *
     * @param intDom a string expressing the int domain
     * @return domList -  a list containing the domain expressed in intDom string.
     * @throws InvalidConfigException if the domain doesn't adhere to the int domain specifications or if its malformed.
     */
    private ArrayList<? extends Number> parseDomInt(String intDom) throws InvalidConfigException {

        ArrayList<Integer> domList;
        HashSet<Integer> domSet = new HashSet<>();
        if (intDom.startsWith("[") || intDom.endsWith("]")) {
            String values = intDom.substring(1, intDom.length() - 1);
            String[] vals = values.split(",");
            for (String val : vals)
                domSet.add(Integer.parseInt(val.strip()));
            domList = new ArrayList<>(domSet);
        } else {
            int tildeIdx = intDom.indexOf("~");
            String start = intDom.substring(0, tildeIdx).strip();
            String end = intDom.substring(tildeIdx + 1).strip();
            int s;
            int f;
            try {
                s = Integer.parseInt(start);
                f = Integer.parseInt(end);
            } catch (Exception e) {
                throw new InvalidConfigException("an invalid value of range domain");
            }

            if (s <= f) {
                domList = makeList(s, f);
            } else {
                throw new InvalidConfigException("invalid domain");
            }
        }
        return domList;
    }

    /**
     * This method parses the float domain by checking if the values adhere to the float grammar rules
     *
     * @param domain a string expressing the float domain
     * @return domList -  a list containing the domain expressed in domain string.
     * @throws InvalidConfigException if the domain doesn't adhere to the float domain specifications or if its malformed.
     */
    private ArrayList<? extends Number> parseDomFloat(String domain) throws InvalidConfigException {

        ArrayList<Double> domList;
        HashSet<Double> domSet = new HashSet<>();

        if (domain.startsWith("[") || domain.endsWith("]")) {
            String values = domain.substring(1, domain.length() - 1);
            String[] vals = values.split(",");
            for (String val : vals)
                domSet.add(Double.parseDouble(val.strip()));
            domList = new ArrayList<>(domSet);
        } else {
            int tildeIdx = domain.indexOf("~");
            String start = domain.substring(0, tildeIdx).strip();
            String end = domain.substring(tildeIdx + 1).strip();
            double s;
            double f;
            try {
                s = Integer.parseInt(start);
                f = Integer.parseInt(end);
            } catch (Exception e) {
                throw new InvalidConfigException("an invalid value of range domain");
            }

            if (s <= f) {
                domList = makeDoubleList(s, f);
            } else {
                throw new InvalidConfigException("invalid domain");
            }
        }
        return domList;
    }

    /**
     * This method parses the boolean domain by checking if the values adhere to the boolean grammar rules of 0 and 1.
     *
     * @param boolDom a string expressing the boolean domain
     * @return domList -  a list containing the domain expressed in boolDom string.
     * @throws InvalidConfigException if the domain doesn't adhere to the boolean domain specifications or if its malformed.
     */
    private ArrayList<? extends Number> parseDomBool(String boolDom) throws InvalidConfigException {

        ArrayList<Integer> domList;
        HashSet<Integer> domSet = new HashSet<>();
        // check if domain is expressed as a list
        if (boolDom.startsWith("[") || boolDom.endsWith("]")) {
            String values = boolDom.substring(1, boolDom.length() - 1);
            String[] vals = values.split(",");
            for (String val : vals) {
                int bool = Integer.parseInt(val.strip());
                if ((bool == 0) || (bool == 1)) { // check if it is  1 or 0
                    domSet.add(Integer.parseInt(val.strip()));
                } else {
                    throw new InvalidConfigException("invalid value for boolean");
                }
            }
            domList = new ArrayList<>(domSet);
        } else {
            // else if domain is expressed as a range check that the range is valid
            int tildeIdx = boolDom.indexOf("~");
            String start = boolDom.substring(0, tildeIdx).strip();
            String end = boolDom.substring(tildeIdx + 1).strip();
            int s = Integer.parseInt(start);
            int f = Integer.parseInt(end);

            if (((s == 0) && (f == 1)) || ((s == 0) && (f == 0)) || ((s == 1) && (f == 1))) {
                domList = makeList(s, f);
            } else {
                throw new InvalidConfigException("invalid domain for boolean");
            }
        }
        return domList;
    }
    /**
     * This method creates a list of doubles from the range from s to f.
     *
     * @param s starting number of the domain range
     * @param f finishing number of the domain range
     * @return domList -  a list containing the domain expressed in doubles of the range form s to f
     */
    private ArrayList<Double> makeDoubleList(double s, double f) {
        ArrayList<Double> domList = new ArrayList<>();
        for (; s <= f; s++) {
            domList.add(s);
        }
        return domList;
    }
    /**
     * This method creates a list of integers from the range from s to f.
     *
     * @param s starting number of the domain range
     * @param f finishing number of the domain range
     * @return domList - a list containing the domain expressed in integers of the range form s to f
     */
    private ArrayList<Integer> makeList(int s, int f) {
        ArrayList<Integer> domList = new ArrayList<>();
        for (; s <= f; s++) {
            domList.add(s);
        }
        return domList;
    }
    /**
     * This is a helper method that parses the rand num by checking if it's a non-negative integer
     *
     * @param elem the rand num object
     * @return the integer value of Object elem
     * @throws InvalidConfigException if elem is not a non-negative integer
     */
    private Integer parseRand(Object elem) throws InvalidConfigException {
        if (elem instanceof Integer) {
            if ((Integer) elem >= 0) {
                return (Integer) elem;
            }
        }
        else throw new InvalidConfigException("invalid rand num");
        return -1;
    }
}





