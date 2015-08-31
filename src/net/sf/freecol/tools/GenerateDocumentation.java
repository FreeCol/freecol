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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.StringTemplate;


/**
 * Generate some documentation.
 */
public class GenerateDocumentation {

    private static final File STRING_DIRECTORY =
        new File("data/strings");
    private static final File RULE_DIRECTORY =
        new File("data/rules/classic");
    private static final String XSL = "specification.xsl";
    
    private static final File DESTINATION_DIRECTORY =
        new File("doc");

    private static final Map<String, String> resources = new HashMap<>();

    private static final String[] sourceFiles
        = STRING_DIRECTORY.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("FreeColMessages.*\\.properties");
            }
        });




    public static void main(String[] args) throws Exception {
        System.setProperty("jaxp.debug", "1");
        if (args.length > 0) {
            Arrays.sort(args);
        }
        readResources();
        //generateTMX();
        generateDocumentation(args);
    }

    private static void readResources() {
        System.out.println("Processing source file: resources.properties");
        File sourceFile = new File(RULE_DIRECTORY, "resources.properties");
        try (
            FileReader fileReader = new FileReader(sourceFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
        ) {
            String line = bufferedReader.readLine();
            while (line != null) {
                int index = line.indexOf('=');
                if (index >= 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    resources.put(key, value);
                }
                line = bufferedReader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void generateTMX() {

        Map<String, Map<String, String>> translations = new HashMap<>();

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

            try (
                FileReader fileReader = new FileReader(sourceFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
            ) {
                String line = bufferedReader.readLine();
                while (line != null) {
                    int index = line.indexOf('=');
                    if (index >= 0) {
                        String key = line.substring(0, index).trim();
                        String value = line.substring(index + 1).trim()
                            .replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
                        Map<String, String> map = translations.get(key);
                        if (map == null) {
                            map = new HashMap<>();
                            translations.put(key, map);
                        }
                        map.put(languageCode, value);
                    }
                    line = bufferedReader.readLine();
                }
            } catch (Exception e) {
                // forget it
            }
        }
        File destinationFile = new File(DESTINATION_DIRECTORY, "freecol.tmx");
        try (
            FileWriter out = new FileWriter(destinationFile);
        ) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateDocumentation(String[] languages) {
        for (String name : sourceFiles) {

            String languageCode = name.substring(15, name.length() - 11);
            if (languageCode.isEmpty()) {
                languageCode = "en";
            } else if ('_' == languageCode.charAt(0)) {
                languageCode = languageCode.substring(1);
                if ("qqq".equals(languageCode)) {
                    System.out.println("Skipping language code 'qqq'");
                    continue;
                }
            } else {
                // don't know what to do
                continue;
            }
            if (languages.length == 0
                || Arrays.binarySearch(languages, languageCode) >= 0) {
                System.out.println("Generating localized documentation for language code "
                                   + languageCode);

                Messages.loadMessageBundle(Messages.getLocale(languageCode));
                try {
                    TransformerFactory factory = TransformerFactory.newInstance();
                    Source xsl = new StreamSource(new File("doc", XSL));
                    Transformer stylesheet;
                    try {
                        stylesheet = factory.newTransformer(xsl);
                    } catch (TransformerException tce) {
                        System.err.println("Problem with " + XSL + " at: "
                            + tce.getLocationAsString());
                        tce.printStackTrace();
                        continue;
                    }

                    Source request  = new StreamSource(new File(RULE_DIRECTORY, "specification.xml"));
                    Result response = new StreamResult(new File(DESTINATION_DIRECTORY, "specification_"
                                                                + languageCode + ".html"));
                    stylesheet.transform(request, response);
                }
                catch (TransformerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getResource(String key) {
        return resources.get(key);
    }

    public static String localize(String template) {
        return Messages.message(template);
    }

    public static String localize(String template, String key, String number) {
        double num = Double.parseDouble(number);
        StringTemplate stringTemplate = StringTemplate.template(template)
            .addAmount(key, num);
        return Messages.message(stringTemplate);
    }
}

