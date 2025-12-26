/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Document the state of the translations.
 */
public class TranslationReport {
    
    private static final Logger logger = Logger.getLogger(TranslationReport.class.getName());

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
        List<LanguageStatsRecord> statistics = new ArrayList<>();

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
        if (languageFiles == null) {
            if (logger.isLoggable(Level.SEVERE)) logger.severe("No language files found in " + directory);
            System.exit(1);
        }

        Path filePath = FileSystems.getDefault().getPath(args[0],
            "FreeColMessages.properties");
        Properties master = new Properties();
        master.load(Files.newInputStream(filePath));
        //logger.info("*** Found master property file with " + master.size() + " properties.\n");

        Properties properties = new Properties();
        for (String name : languageFiles) {
            LanguageStatsRecord lstat = new LanguageStatsRecord();
            lstat.localFile = name;
            Path propPath = FileSystems.getDefault().getPath(args[0], name);
            properties.clear();
            properties.load(Files.newInputStream(propPath));
            if (logger.isLoggable(Level.INFO)) logger.info(name.length() + 8 < stars.length() ? stars.substring(0, name.length() + 8) : stars);
            if (logger.isLoggable(Level.INFO)) logger.info("*** " + name + " ***");
            if (logger.isLoggable(Level.INFO)) logger.info(name.length() + 8 < stars.length() ? stars.substring(0, name.length() + 8) : stars);

            List<String> missingKeys = new ArrayList<>();
            List<String> missingVariables = new ArrayList<>();
            List<String> copiedFromMaster = new ArrayList<>();

            for (Object o : master.keySet()) {
                String key = (String)o;
                String value = properties.getProperty(key, null);
                if (value == null) {
                    missingKeys.add(key);
                } else {
                    String masterValue = master.getProperty(key);
                    int lastIndex = 0;
                    boolean inVariable = false;

                    if (value.equalsIgnoreCase(masterValue)) {
                        // ignore some values which are most probably copies in many languages
                        if (!key.contains("newColonyName")
                                && !(key.contains("foundingFather") && key.contains(".birthAndDeath"))
                                && !(key.contains("foundingFather") && key.contains(".name"))) {
                            copiedFromMaster.add(key);
                        }
                    }

                    isInVariable(missingVariables, key, value, masterValue, lastIndex, inVariable);
                }
            }
            
            if (!missingKeys.isEmpty()) {
                if (logger.isLoggable(Level.INFO)) logger.info("** Total of " + missingKeys.size() + " properties missing:\n");
                for (String key : sort(missingKeys)) {
                    if (logger.isLoggable(Level.INFO)) logger.info(key + "=" + master.getProperty(key));
                }
                lstat.missingKeys = missingKeys.size();
                logger.info("");
            } else {
                logger.info("** No properties missing.\n");
            }
            
            if (!copiedFromMaster.isEmpty()){
                if (logger.isLoggable(Level.INFO)) logger.info("** Total of " + copiedFromMaster.size() + " properties copied from master properties:\n");
                for (String key : sort(copiedFromMaster)) {
                    if (logger.isLoggable(Level.INFO)) logger.info(key + "=" + master.getProperty(key));
                }
                lstat.copiedKeys = copiedFromMaster.size();
                logger.info("");
            } else {
                logger.info("** No properties copied.\n");
            }

            if (!missingVariables.isEmpty()) {
                if (logger.isLoggable(Level.INFO)) logger.info("** Total of " + missingVariables.size() + " properties with missing variables:\n");
                for (String key : sort(missingVariables)) {
                    if (logger.isLoggable(Level.INFO)) logger.info("* CORRECT: " + key + "=" + master.getProperty(key));
                    if (logger.isLoggable(Level.INFO)) logger.info("INCORRECT: " + key + "=" + properties.getProperty(key));
                }
                lstat.missingVariables = missingVariables.size();
                logger.info("");
            } else {
                logger.info("** No properties with missing variables.\n");
            }


            List<String> superfluousKeys = new ArrayList<>();
            List<String> superfluousVariables = new ArrayList<>();
            for (Object o : properties.keySet()) {
                String key = (String)o;
                String value = master.getProperty(key, null);
                if (value == null) {
                    superfluousKeys.add(key);
                } else {
                    String propertiesValue = properties.getProperty(key);
                    int lastIndex = 0;
                    boolean inVariable = false;

                    isInVariable(superfluousVariables, key, value, propertiesValue, lastIndex, inVariable);
                }
            }

            if (!superfluousKeys.isEmpty()) {
                if (logger.isLoggable(Level.INFO)) logger.info("** Total of " + superfluousKeys.size() + " superfluous properties:\n");
                for (String key : sort(superfluousKeys)) {
                    if (logger.isLoggable(Level.INFO)) logger.info(key + "=" + properties.getProperty(key));
                }
                lstat.superfluousKeys = superfluousKeys.size();
                logger.info("");
            } else {
                logger.info("** No superfluous properties.\n");
            }
            
            if (!superfluousVariables.isEmpty()) {
                if (logger.isLoggable(Level.INFO)) logger.info("** Total of " + superfluousVariables.size() +
                                   " properties with superfluous variables:\n");
                for (String key : sort(superfluousVariables)) {
                    if (logger.isLoggable(Level.INFO)) logger.info("* CORRECT: " + key + "=" + master.getProperty(key));
                    if (logger.isLoggable(Level.INFO)) logger.info("INCORRECT: " + key + "=" + properties.getProperty(key));
                }
                lstat.superfluousVariables = superfluousVariables.size();
                logger.info("");
            } else {
                logger.info("** No properties with superfluous variables.\n");
            }

            statistics.add(lstat);
        }
        
        if (printSummary){
            logger.info(stars);
            if (logger.isLoggable(Level.INFO)) logger.info("*** Summary of translation efforts (" + master.size() + " keys in master file) ***");
            logger.info(stars);
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
                
                if (logger.isLoggable(Level.INFO)) logger.info(output.toString());
            }
        }
    }


    /**
     * Sets inVariable as needed
     */
    private static void isInVariable(List<String> superfluousVariables, String key, String value, String propertiesValue, int lastIndex, boolean inVariable) {
        int currentIndex = lastIndex;
        boolean insideVariable = inVariable;
        for (int index = 0; index < propertiesValue.length() - 1; index++) {
            char current = propertiesValue.charAt(index);
            if (current == '%') {
                if (insideVariable) {
                    String var = propertiesValue.substring(currentIndex, index + 1);
                    if (!value.contains(var)) {
                        superfluousVariables.add(key);
                    }
                    insideVariable = false;
                } else {
                    currentIndex = index;
                    insideVariable = true;
                }
            } else if (!Character.isLetterOrDigit(current)) {
                insideVariable = false;
            }
        }
    }


    /**
     * Produces a shorter name.
     */
    private static StringBuilder shortenName(String localFile) {
        StringBuilder out = new StringBuilder(5);
        String temp = localFile.substring(16, localFile.indexOf('.'));
        if (temp.length() < 5)
            out.append("   ");
        out.append(temp);
        return out;
    }


    /**
     * Adds space after a number.
     */
    private static StringBuilder prettyPrint(int number) {
        StringBuilder output = new StringBuilder(4);
        if (number < 10) output.append(' ');
        if (number < 100) output.append(' ');
        if (number < 1000) output.append(' ');
        output.append(number);
        return output;
    }


    private static Set<String> sort(List<String> missingKeys) {
        Set<String> sorted = new TreeSet<>();
        sorted.addAll(missingKeys);
        return sorted;
    }

}
