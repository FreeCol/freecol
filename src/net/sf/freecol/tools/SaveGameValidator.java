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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.freecol.common.io.FreeColSavegameFile;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Validate a saved game.
 */
public class SaveGameValidator {

    public static void main(String[] args) throws Exception {

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File("schema/data/data-savedGame.xsd");
        Schema schema = factory.newSchema(schemaLocation);
        Validator saveGameValidator = schema.newValidator();

        List<File> allFiles = new ArrayList<>();
        FileFilter ff = FreeColSavegameFile.getFileFilter();
        for (String name : args) {
            File file = new File(name);
            if (file.exists()) {
                if (file.isDirectory()) {
                    for (File fsg : file.listFiles(ff)) {
                        allFiles.add(fsg);
                    }
                } else if (ff.accept(file)) {
                    allFiles.add(file);
                }
            }
        }

        for (File file : allFiles) {
            System.out.println("Processing file " + file.getPath());
            try {
                FreeColSavegameFile mapFile = new FreeColSavegameFile(file);
                saveGameValidator.validate(new StreamSource(mapFile.getSavegameInputStream()));
                System.out.println("Successfully validated " + file.getName());
            } catch (SAXParseException e) {
                System.out.println(e.getMessage() 
                                   + " at line=" + e.getLineNumber() 
                                   + " column=" + e.getColumnNumber());
            } catch (IOException | SAXException e) {
                System.out.println("Failed to read " + file.getName());
            }
        }
    }

}

