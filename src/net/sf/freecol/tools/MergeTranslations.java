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
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * Merge some translation updates.
 */
public class MergeTranslations {
    
    public static void main(String[] args) throws Exception {

        File sourceDirectory = new File(args[0]);
        if (!sourceDirectory.isDirectory()) {
            System.exit(1);
        }

        File targetDirectory = new File(args[1]);
        if (!targetDirectory.isDirectory()) {
            System.exit(1);
        }

        final String localeKey = args.length > 2 ? args[2] : "";
        String[] sourceFiles = sourceDirectory.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("FreeColMessages_" + localeKey + ".*\\.properties");
                }
            });
        
        for (String name : sourceFiles) {

            System.out.println("Processing source file: " + name);

            File sourceFile = new File(sourceDirectory, name);
            Map<String, String> sourceProperties = readFile(sourceFile);

            File targetFile = new File(targetDirectory, name);

            if (targetFile.exists()) {
                Map<String, String> targetProperties = readFile(targetFile);

                List<Entry<?,?>> missingProperties
                    = sourceProperties.entrySet().stream()
                    .filter(e -> !targetProperties.containsKey(e.getKey()))
                    .collect(Collectors.toList());
                if (!missingProperties.isEmpty()) {
                    try (FileWriter out = new FileWriter(targetFile, true)) {
                        out.write("### Merged from trunk on "
                                + DateFormat.getDateTimeInstance().format(new Date())
                                + " ###\n");
                        for (Entry<?,?> entry : missingProperties) {
                            out.write((String) entry.getKey());
                            out.write("=");
                            out.write((String) entry.getValue());
                            out.write("\n");
                        }
                    }
                }
            } else {
                System.out.println("Copying " + name + " from trunk.");
                FileWriter out;
                try (FileReader in = new FileReader(sourceFile)) {
                    out = new FileWriter(targetFile);
                    int c;
                    while ((c = in.read()) != -1) {
                        out.write(c);
                    }
                }
                out.close();

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

}

