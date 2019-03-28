/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


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
        if (sourceFiles == null) {
            System.err.println("No messages files found in "
                + sourceDirectory);
            System.exit(1);
        }
        
        for (String name : sourceFiles) {

            System.out.println("Processing source file: " + name);

            File sourceFile = new File(sourceDirectory, name);
            Map<String, String> sourceProperties = readFile(sourceFile);

            File targetFile = new File(targetDirectory, name);

            if (targetFile.exists()) {
                Map<String, String> targetProperties = readFile(targetFile);

                List<Entry<String,String>> missingProperties
                    = transform(sourceProperties.entrySet(),
                                e -> !targetProperties.containsKey(e.getKey()));
                if (!missingProperties.isEmpty()) {
                    try (
                        Writer out = Utils.getFileUTF8AppendWriter(targetFile)
                    ) {
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
                try (
                    Reader in = Utils.getFileUTF8Reader(sourceFile);
                    Writer out = Utils.getFileUTF8Writer(targetFile);
                ) {
                    int c;
                    while ((c = in.read()) != -1) {
                        out.write(c);
                    }
                }
            }
        }
    }

    private static Map<String, String> readFile(File file) {
        Map<String, String> result = new HashMap<>();
        try (
            Reader reader = Utils.getFileUTF8Reader(file);
            BufferedReader bufferedReader = new BufferedReader(reader); 
        ) {
            String line = bufferedReader.readLine();
            while (line != null) {
                int index = line.indexOf('=');
                if (index >= 0) {
                    result.put(line.substring(0, index), line.substring(index + 1));
                }
                line = bufferedReader.readLine();
            }
        } catch (IOException ioe) {
            System.err.println("Error reading file " + file.getName()
                + ": " + ioe);
        }
        return result;
    }
}
