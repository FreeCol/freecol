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


/**
 * Generate a desktop entry file.
 */
public class DesktopEntry {

    private static final File SOURCE_DIRECTORY =
        new File("data", "strings");

    private static final String GENERIC_NAME =
        "freecol.desktopEntry.GenericName";
    private static final String COMMENT =
        "freecol.desktopEntry.Comment";

    /**
     * Pass the desktop entry file to create as first argument.
     */
    public static void main(String[] args) throws Exception {

        try (FileWriter result = new FileWriter(new File(args[0]))) {
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
            
            for (String name : sourceFiles) {
                
                System.out.println("Processing source file: " + name);
                
                String languageCode = null;
                if (name.startsWith("FreeColMessages_")) {
                    int index = name.indexOf('.', 16);
                    languageCode = name.substring(16, index)
                            .replace('-', '@');
                }
                
                boolean foundGenericName = false;
                boolean foundComment = false;
                File sourceFile = new File(SOURCE_DIRECTORY, name);
                FileReader fileReader = new FileReader(sourceFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    int index = line.indexOf('=');
                    if (index >= 0) {
                        String key = line.substring(0, index).trim();
                        if (null != key) switch (key) {
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
                            result.append("[" + languageCode + "]");
                        }
                        result.append("=");
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

