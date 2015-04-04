/**
 *  Copyright (C) 2002-2015   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;


/**
 * Document the state of the translations.
 */
public class TranslationReport {
    
    private static class LanguageStatsRecord{
        String localFile = "";
        int missingKeys = 0;
        int missingVariables = 0;
        int copiedKeys = 0;
        public int superfluousVariables;
        public int superfluousKeys;
    }

    private static final String stars =
        "*****************************************************************";

    private static final boolean printSummary = true;

    public static void main(String[] args) throws Exception {
        ArrayList<LanguageStatsRecord> statistics = new ArrayList<>();

        //String dirName = "src/net/sf/freecol.common.i18n/";
        String dirName = args[0];
        File directory = new File(dirName);
        if (!directory.isDirectory()) {
            System.exit(1);
        }
        final String localeKey = args.length > 1 ? args[1] : "";
        String[] languageFiles = directory.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("FreeColMessages_" + localeKey + ".*\\.properties");
                }
            });

        File masterFile = new File(directory, "FreeColMessages.properties");
        Properties master = new Properties();
        master.load(new FileInputStream(masterFile));
        //System.out.println("*** Found master property file with " + master.size() + " properties.\n");

        for (String name : languageFiles) {
            LanguageStatsRecord lstat = new LanguageStatsRecord();
            lstat.localFile = name;
            File propertyFile = new File(directory, name);
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertyFile));
            System.out.println(name.length()+8 < stars.length() ? stars.substring(0, name.length() + 8) : stars);
            System.out.println("*** " + name + " ***");
            System.out.println(name.length()+8 < stars.length() ? stars.substring(0, name.length() + 8) : stars);

            ArrayList<String> missingKeys      = new ArrayList<>();
            ArrayList<String> missingVariables = new ArrayList<>();
            ArrayList<String> copiedFromMaster = new ArrayList<>();
            
            for (Enumeration<Object> keys = master.keys() ; keys.hasMoreElements()  ;)  {
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
                                if (!value.contains(var)) {
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
            
            if (!missingKeys.isEmpty()) {
                System.out.println("** Total of " + missingKeys.size() + " properties missing:\n");
                for (String key : sort(missingKeys)) {
                    System.out.println(key + "=" + master.getProperty(key));
                }
                lstat.missingKeys = missingKeys.size();
                System.out.println("");
            } else {
                System.out.println("** No properties missing.\n");
            }
            
            if (!copiedFromMaster.isEmpty()){
                System.out.println("** Total of " + copiedFromMaster.size() + " properties copied from master properties:\n");
                for (String key : sort(copiedFromMaster)) {
                    System.out.println(key + "=" + master.getProperty(key));
                }
                lstat.copiedKeys = copiedFromMaster.size();
                System.out.println("");
            } else {
                System.out.println("** No properties copied.\n");
            }

            if (!missingVariables.isEmpty()) {
                System.out.println("** Total of " + missingVariables.size() + " properties with missing variables:\n");
                for (String key : sort(missingVariables)) {
                    System.out.println("* CORRECT: " + key + "=" + master.getProperty(key));
                    System.out.println("INCORRECT: " + key + "=" + properties.getProperty(key));
                }
                lstat.missingVariables = missingVariables.size();
                System.out.println("");
            } else {
                System.out.println("** No properties with missing variables.\n");
            }


            ArrayList<String> superfluousKeys = new ArrayList<>();
            ArrayList<String> superfluousVariables = new ArrayList<>();
            for (Enumeration<Object> keys = properties.keys() ; keys.hasMoreElements()  ;)  {
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
                                if (!value.contains(var)) {
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

            if (!superfluousKeys.isEmpty()) {
                System.out.println("** Total of " + superfluousKeys.size() + " superfluous properties:\n");
                for (String key : sort(superfluousKeys)) {
                    System.out.println(key + "=" + properties.getProperty(key));
                }
                lstat.superfluousKeys = superfluousKeys.size();
                System.out.println("");
            } else {
                System.out.println("** No superfluous properties.\n");
            }
            
            if (!superfluousVariables.isEmpty()) {
                System.out.println("** Total of " + superfluousVariables.size() +
                                   " properties with superfluous variables:\n");
                for (String key : sort(superfluousVariables)) {
                    System.out.println("* CORRECT: " + key + "=" + master.getProperty(key));
                    System.out.println("INCORRECT: " + key + "=" + properties.getProperty(key));
                }
                lstat.superfluousVariables = superfluousVariables.size();
                System.out.println("");
            } else {
                System.out.println("** No properties with superfluous variables.\n");
            }

            statistics.add(lstat);
        }
        
        if (printSummary){
            System.out.println(stars);
            System.out.println("*** Summary of translation efforts (" + master.size() + " keys in master file) ***");
            System.out.println(stars);
            for (LanguageStatsRecord stats : statistics){
                StringBuilder output = new StringBuilder();
                output.append(shortenName(stats.localFile));
                output.append(": ");
                output.append(prettyPrint(stats.missingKeys));
                output.append(" keys missing, ");
                output.append(prettyPrint(stats.missingVariables));
                output.append(" vars missing, ");
                output.append(prettyPrint(stats.copiedKeys));
                output.append(" entries copied, ");
                output.append(prettyPrint(stats.superfluousKeys));
                output.append(" redundant keys, ");
                output.append(prettyPrint(stats.superfluousVariables));
                output.append(" redundant vars. ");
                
                float percentageDone =  (100 * (master.size() - (stats.missingKeys + stats.copiedKeys))) / (float) master.size();
                percentageDone = Math.round(percentageDone*100)/100f;
                output.append(percentageDone).append("% finished.");
                
                System.out.println(output.toString());
            }
        }
    }

    private static StringBuilder shortenName(String localFile) {
        StringBuilder out = new StringBuilder(5);
        String temp = localFile.substring(16, localFile.indexOf('.'));
        if (temp.length() < 5)
            out.append("   ");
        out.append(temp);
        return out;
    }

    private static StringBuilder prettyPrint(int number) {
        StringBuilder output = new StringBuilder(4);
        if (number < 10)
            output.append(" ");
        if (number < 100)
            output.append(" ");
        if (number < 1000)
            output.append(" ");
        output.append(number);
        return output;
    }

    private static TreeSet<String> sort(ArrayList<String> missingKeys) {
        TreeSet<String> sorted = new TreeSet<>();
        sorted.addAll(missingKeys);
        return sorted;
    }

}

