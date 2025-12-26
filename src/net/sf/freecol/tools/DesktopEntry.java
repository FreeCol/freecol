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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Logger;

import net.sf.freecol.common.util.Utils;
import java.util.logging.Level;


/**
 * Generate a desktop entry file.
 */
public class DesktopEntry {

    private static final Logger logger = Logger.getLogger(DesktopEntry.class.getName());

    private static final File SOURCE_DIRECTORY =
        new File("data", "strings");

    private static final String GENERIC_NAME =
        "freecol.desktopEntry.GenericName";
    private static final String COMMENT =
        "freecol.desktopEntry.Comment";

    /**
     * Pass the desktop entry file to create as first argument.
     *
     * @param args The program arguments.
     * @exception FileNotFoundException if the source file is absent.
     * @exception IOException when various IO fails.
     */
    public static void main(String[] args)
        throws IOException {

        try (Writer result = Utils.getFileUTF8Writer(new File(args[0]))) {
            result.append("[Desktop Entry]\n");
            result.append("Version=1.0\n");
            result.append("Type=Application\n");
            result.append("Name=FreeCol\n");
            result.append("Exec=freecol\n");
            result.append("Icon=data/freecol.png\n");
            result.append("Categories=Game;StrategyGame;\n");
            
            String[] sourceFiles = SOURCE_DIRECTORY.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("FreeColMessages")
                            && name.endsWith(".properties");
                }
            });
            if (sourceFiles == null) {
                if (logger.isLoggable(Level.SEVERE)) logger.severe("No messages files found in " + SOURCE_DIRECTORY);
                System.exit(1);
            }

            for (String name : sourceFiles) {
                
                if (logger.isLoggable(Level.INFO)) logger.info("Processing source file: " + name);
                
                String languageCode = null;
                if (name.startsWith("FreeColMessages_")) {
                    int index = name.indexOf('.', 16);
                    languageCode = name.substring(16, index)
                            .replace('-', '@');
                }
                
                boolean foundGenericName = false;
                boolean foundComment = false;
                File sourceFile = new File(SOURCE_DIRECTORY, name);
                Reader reader = Utils.getFileUTF8Reader(sourceFile);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    int index = line.indexOf('=');
                    if (index >= 0) {
                        String key = line.substring(0, index).trim();
                        switch (key) {
                        case GENERIC_NAME:
                            result.append("GenericName");
                            foundGenericName = true;
                            break;
                        case COMMENT:
                            result.append("Comment");
                            foundComment = true;
                            break;
                        default:
                            line = bufferedReader.readLine();
                            continue;
                        }
                        if (languageCode != null) {
                            result.append('[' + languageCode + "]");
                        }
                        result.append('=');
                        result.append(line.substring(index + 1).trim());
                        result.append("\n");
                        if (foundGenericName && foundComment) {
                            break;
                        }
                    }
                    line = bufferedReader.readLine();
                }
            }
            
            result.flush();
        }

    }
}
