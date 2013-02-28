/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;


public class GenerateDocumentation {

    private static final File STRING_DIRECTORY =
        new File("data/strings");
    private static final File RULE_DIRECTORY =
        new File("data/rules/classic");

    private static final File DESTINATION_DIRECTORY =
        new File("doc");

    public static void main(String[] args) throws Exception {
        generateResources();
        generateTMX();
    }

    private static void generateResources() {

        System.out.println("Processing source file: resources.properties");
        try {
            File sourceFile = new File(RULE_DIRECTORY, "resources.properties");
            File destinationFile = new File(DESTINATION_DIRECTORY, "resources.xml");
            FileWriter out = new FileWriter(destinationFile);
            out.write("<?xml version =\"1.0\" encoding=\"UTF-8\"?>\n");
            //out.write("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n");
            out.write("<properties>\n");
            FileReader fileReader = new FileReader(sourceFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            while (line != null) {
                int index = line.indexOf('=');
                if (index >= 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    out.write("  <entry key=\"" + key + "\">" + value + "</entry>\n");
                }
                line = bufferedReader.readLine();
            }
            out.write("</properties>\n");
            out.flush();
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    private static void generateTMX() {

        String[] sourceFiles = STRING_DIRECTORY.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches("FreeColMessages.*\\.properties");
                }
            });

        Map<String, Map<String, String>> translations
            = new HashMap<String, Map<String, String>>();

        for (String name : sourceFiles) {

            System.out.println("Processing source file: " + name);

            String languageCode = name.substring(15, name.length() - 11);
            if (languageCode.isEmpty()) {
                languageCode = "en";
            } else if ('_' == languageCode.charAt(0)) {
                languageCode = languageCode.substring(1);
            } else {
                // don't know what to do
                continue;
            }

            File sourceFile = new File(STRING_DIRECTORY, name);

            try {
                FileReader fileReader = new FileReader(sourceFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    int index = line.indexOf('=');
                    if (index >= 0) {
                        String key = line.substring(0, index).trim();
                        String value = line.substring(index + 1).trim()
                            .replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
                        Map<String, String> map = translations.get(key);
                        if (map == null) {
                            map = new HashMap<String, String>();
                            translations.put(key, map);
                        }
                        map.put(languageCode, value);
                    }
                    line = bufferedReader.readLine();
                }
            } catch(Exception e) {
                // forget it
            }
        }
        try {
            File destinationFile = new File(DESTINATION_DIRECTORY, "freecol.tmx");
            FileWriter out = new FileWriter(destinationFile);

            out.write("<?xml version =\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<tmx version=\"1.4b\">\n");
            out.write("<body>\n");
            for (Map.Entry<String, Map<String, String>> tu : translations.entrySet()) {
                out.write("  <tu tuid=\"" + tu.getKey() + "\">\n");
                for (Map.Entry<String, String> tuv : tu.getValue().entrySet()) {
                    out.write("    <tuv xml:lang=\"" + tuv.getKey() + "\">\n");
                    out.write("      <seg>" + tuv.getValue() + "</seg>\n");
                    out.write("    </tuv>\n");
                }
                out.write("  </tu>\n");
            }
            out.write("</body>\n");
            out.write("</tmx>\n");
            out.flush();
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

