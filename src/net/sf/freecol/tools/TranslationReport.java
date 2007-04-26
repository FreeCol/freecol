
package net.sf.freecol.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;


public class TranslationReport {

    private static final String stars =
        "************************************************************";

    public static void main(String[] args) throws Exception {

        //String dirName = "src/net/sf/freecol/client/gui/i18n/";
        String dirName = args[0];
        File directory = new File(dirName);
        if (!directory.isDirectory()) {
            System.exit(1);
        }
        final String localeKey = args.length > 1 ? args[1] : "";
        String[] fileNames = directory.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches("FreeColMessages_" + localeKey + ".*\\.properties");
                }
            });

        File masterFile = new File(directory, "FreeColMessages.properties");
        Properties master = new Properties();
        master.load(new FileInputStream(masterFile));
        //System.out.println("*** Found master property file with " + master.size() + " properties.\n");

        for (String name : fileNames) {
            File propertyFile = new File(directory, name);
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertyFile));
            System.out.println(stars.substring(0, name.length() + 8));
            System.out.println("*** " + name + " ***");
            System.out.println(stars.substring(0, name.length() + 8));

            ArrayList<String> missingKeys = new ArrayList<String>();
            ArrayList<String> missingVariables = new ArrayList<String>();
            ArrayList<String> copiedFromMaster = new ArrayList<String>();
            
            for (Enumeration keys = master.keys() ; keys.hasMoreElements()  ;)  {
                String key = (String) keys.nextElement();
                String value = properties.getProperty(key, null);
                if (value == null) {
                    missingKeys.add(key);
                } else {
                    String masterValue = master.getProperty(key);
                    int lastIndex = 0;
                    boolean inVariable = false;
                    
                    if (value.equalsIgnoreCase(masterValue)){
                        // ignore some values which are most probably copies in many languages
                        if (!key.contains("newColonyName")
                                && !(key.contains("foundingFather") && key.contains(".birthAndDeath"))
                                && !(key.contains("foundingFather") && key.contains(".name")) ){
                            copiedFromMaster.add(key);
                        }
                    }

                    for (int index = 0; index < masterValue.length() - 1; index++) {
                        char current = masterValue.charAt(index);
                        if (current == '%') {
                            if (inVariable) {
                                String var = masterValue.substring(lastIndex, index + 1);
                                if (value.indexOf(var) == -1) {
                                    missingVariables.add(key);
                                }
                                inVariable = false;
                            } else {
                                lastIndex = index;
                                inVariable = true;
                            }
                        } else if (!Character.isLetterOrDigit(current)) {
                            inVariable = false;
                        }
                    }
                }
            }
            
            if (missingKeys.size() > 0) {
                System.out.println("** Total of " + missingKeys.size() + " properties missing:\n");
                for (String key : sort(missingKeys)) {
                    System.out.println(key + "=" + master.getProperty(key));
                }
                System.out.println("");
            } else {
                System.out.println("** No properties missing.\n");
            }
            
            if (copiedFromMaster.size() > 0){
                System.out.println("** Total of " + copiedFromMaster.size() + " properties copied from master properties:\n");
                for (String key : sort(copiedFromMaster)) {
                    System.out.println(key + "=" + master.getProperty(key));
                }
                System.out.println("");
            } else {
                System.out.println("** No properties copied.\n");
            }

            if (missingVariables.size() > 0) {
                System.out.println("** Total of " + missingVariables.size() + " properties with missing variables:\n");
                for (String key : sort(missingVariables)) {
                    System.out.println("* CORRECT: " + key + "=" + master.getProperty(key));
                    System.out.println("INCORRECT: " + key + "=" + properties.getProperty(key));
                }
                System.out.println("");
            } else {
                System.out.println("** No properties with missing variables.\n");
            }


            ArrayList<String> superfluousKeys = new ArrayList<String>();
            ArrayList<String> superfluousVariables = new ArrayList<String>();
            for (Enumeration keys = properties.keys() ; keys.hasMoreElements()  ;)  {
                String key = (String) keys.nextElement();
                String value = master.getProperty(key, null);
                if (value == null) {
                    superfluousKeys.add(key);
                } else {
                    String propertiesValue = properties.getProperty(key);
                    int lastIndex = 0;
                    boolean inVariable = false;

                    for (int index = 0; index < propertiesValue.length() - 1; index++) {
                        char current = propertiesValue.charAt(index);
                        if (current == '%') {
                            if (inVariable) {
                                String var = propertiesValue.substring(lastIndex, index + 1);
                                if (value.indexOf(var) == -1) {
                                    superfluousVariables.add(key);
                                }
                                inVariable = false;
                            } else {
                                lastIndex = index;
                                inVariable = true;
                            }
                        } else if (!Character.isLetterOrDigit(current)) {
                            inVariable = false;
                        }
                    }
                }
            }

            if (superfluousKeys.size() > 0) {
                System.out.println("** Total of " + superfluousKeys.size() + " superfluous properties:\n");
                for (String key : sort(superfluousKeys)) {
                    System.out.println(key + "=" + properties.getProperty(key));
                }
                System.out.println("");
            } else {
                System.out.println("** No superfluous properties.\n");
            }
            if (superfluousVariables.size() > 0) {
                System.out.println("** Total of " + superfluousVariables.size() +
                                   " properties with superfluous variables:\n");
                for (String key : sort(superfluousVariables)) {
                    System.out.println("* CORRECT: " + key + "=" + master.getProperty(key));
                    System.out.println("INCORRECT: " + key + "=" + properties.getProperty(key));
                }
                System.out.println("");
            } else {
                System.out.println("** No properties with superfluous variables.\n");
            }


        }

    }

    private static TreeSet<String> sort(ArrayList<String> missingKeys) {
        TreeSet<String> sorted = new TreeSet<String>();
        sorted.addAll(missingKeys);
        return sorted;
    }

}

