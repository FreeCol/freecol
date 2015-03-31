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
import java.util.HashMap;
import java.util.Map;


/**
 * Handle translations for the installer.
 */
public class InstallerTranslations {

    private static final File SOURCE_DIRECTORY =
        new File("data/strings");
    private static final File MAIN_FILE =
        new File(SOURCE_DIRECTORY, "FreeColMessages.properties");
    private static final File DESTINATION_DIRECTORY =
        new File("build/installer");
    private static final File LANGUAGE_CODES =
        new File(DESTINATION_DIRECTORY, "iso-639-2.txt");

    // it seems IzPack doesn't use ISO codes at all
    private static final String[][] IZPACK_CODES = {
        { "ca", "cat", "Catalunyan" },
        { "zh", "chn", "Chinese" },
        { "cs", "cze", "Czech" },
        { "da", "dan", "Danish" },
        { "de", "deu", "German" },
        { "en", "eng", "English" },
        { "eu", "eus", "Basque" },
        { "fi", "fin", "Finnish" },
        { "fr", "fra", "French" },
        { "gl", "glg", "Galician" },
        { "hu", "hun", "Hungarian" },
        { "it", "ita", "Italian" },
        { "ja", "jpn", "Japanese" },
        { "ms", "mys", "Malaysian" },
        { "nl", "ned", "Nederlands" },
        { "nn", "nor", "Norwegian" },
        { "pl", "pol", "Polish" },
        { "pt_BR", "por", "Portuguese (Brazilian)" },
        { "pt_PT", "prt", "Portuguese (European)" },
        { "ro", "rom", "Romanian" },
        { "ru", "rus", "Russian" },
        { "sr", "scg", "Serbian" },
        { "es", "spa", "Spanish" },
        { "sk", "svk", "Slovakian" },
        { "sv", "swe", "Swedish" },
        { "uk", "ukr", "Ukrainian" }
    };

    private static final String[] KEYS = {
        "FreeCol",
        "FreeCol.description",
        "GameManual",
        "GameManual.description",
        "SourceCode",
        "SourceCode.description",
        "Music",
        "Music.description",
        "SoundEffects",
        "SoundEffects.description",
        "MovieClips",
        "MovieClips.description",
        "MovieClips.description2",
        "Location.Web",
        "FreeColLanguage",
        "FreeColLanguage.autodetect",
        "FreeColLanguage.description",
        "UserFiles",
        "UserFiles.home",
        "UserFiles.freecol",
        "UserFiles.other"
    };


    public static void main(String[] args) throws Exception {

        /*
        if (!LANGUAGE_CODES.exists()) {
            System.out.println("Language codes not found.");
            System.exit(1);
        }
        */

        if (!MAIN_FILE.exists()) {
            System.out.println("Main input file not found.");
            System.exit(1);
        }

        if (!DESTINATION_DIRECTORY.exists()) {
            DESTINATION_DIRECTORY.mkdirs();
        }

        //Map<String, String> languageMappings = readLanguageMappings(LANGUAGE_CODES);
        Map<String, String> languageMappings = new HashMap<>();
        for (String[] mapping : IZPACK_CODES) {
            languageMappings.put(mapping[0], mapping[1]);
        }
        Map<String, String> mainProperties = readFile(MAIN_FILE);
        //Set<String> languages = new HashSet<String>();

        String[] sourceFiles = SOURCE_DIRECTORY.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("FreeColMessages_.*\\.properties");
                }
            });

        for (String name : sourceFiles) {

            String languageCode = null;
            int index = name.indexOf('.', 16);
            if (index < 0) {
                continue;
            } else {
                languageCode = languageMappings.get(name.substring(16, index));
                if (languageCode == null) {
                    index = name.indexOf('_', 16);
                    if (index < 0) {
                        continue;
                    } else {
                        languageCode = languageMappings.get(name.substring(16, index));
                    }
                }
            }

            if (languageCode == null) {
                System.out.println("Skipping source file: " + name);
                continue;
            }

            System.out.println("Processing source file: " + name);

            File sourceFile = new File(SOURCE_DIRECTORY, name);
            Map<String, String> sourceProperties = readFile(sourceFile);
            StringBuilder output = new StringBuilder();
            output.append("<?xml version = '1.0' encoding = 'UTF-8' standalone = 'yes'?>\n");
            output.append("<!-- ATTENTION: Do not modify this file directly,\n");
            output.append("     modify the source file\n         ");
            output.append(sourceFile.getPath());
            output.append("\n     instead. -->\n");
            output.append("<langpack>\n");

            for (String key : KEYS) {
                String longKey = "installer." + key;
                String value = sourceProperties.get(longKey);
                if (value == null) {
                    value = mainProperties.get(longKey);
                }
                output.append("    <str id=\"");
                output.append(key);
                output.append("\" txt=\"");
                output.append(value);
                output.append("\" />\n");
            }
            output.append("</langpack>\n");
            File destinationFile = new File(DESTINATION_DIRECTORY, "lang.xml_" + languageCode);
            try (FileWriter out = new FileWriter(destinationFile)) {
                out.write(output.toString());
            }
        }

    }

    private static Map<String, String> readFile(File file) {
        Map<String, String> result = new HashMap<>();
        try (
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
        ) {
            String line = bufferedReader.readLine();
            while (line != null) {
                int index = line.indexOf('=');
                if (index >= 0) {
                    result.put(line.substring(0, index), line.substring(index + 1));
                }
                line = bufferedReader.readLine();
            }
        } catch (Exception e) {
            // forget it
        }
        return result;
    }
    /*
    private static Map<String, String> readLanguageMappings(File file) {
        Map<String, String> result = new HashMap<>();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            String[] fields;
            while (line != null) {
                fields = line.split(":");
                if (fields[1].length() > 0) {
                    result.put(fields[1], fields[0].substring(0, 3));
                }
                line = bufferedReader.readLine();
            }
        } catch(Exception e) {
            // forget it
        }
        return result;
    }
    */
}

