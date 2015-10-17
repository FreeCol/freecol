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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import net.sf.freecol.FreeCol;


/**
 * Class for converting FreeCol Savegames (fsg-files).
 * 
 * @see #getFSGConverter()
 */
public class FSGConverter {

    /**
     * A singleton object of this class.
     * @see #getFSGConverter()
     */
    private static FSGConverter singleton;
    
    
    /**
     * Creates an instance of <code>FSGConverter</code>
     */
    private FSGConverter() {
        // Nothing to initialize;
    }
    
    
    /**
     * Gets an object for converting FreeCol Savegames.
     * @return The singleton object.
     */
    public static FSGConverter getFSGConverter() {
        // Using lazy initialization:       
        if (singleton == null) {
            singleton = new FSGConverter();
        }
        return singleton;
    }

    
    /**
     * Converts the given input file to an uncompressed and
     * indented XML-file.
     * 
     * <br><br>
     * 
     * Savegame compression is automatically detected, so using
     * this method on an uncompressed savegame creates an
     * indented version of that savegame.
     * 
     * @param in The input file.
     * @param out The output file. This file will be overwritten
     *      if it already exists.
     * @throws FileNotFoundException if the given input file could not be found.
     * @throws IOException if thrown while reading or writing the files. 
     */
    public void convertToXML(File in, File out) throws FileNotFoundException, IOException {
        try (
            FileInputStream fis = new FileInputStream(in);
            FileOutputStream fos = new FileOutputStream(out);
        ) {
            convertToXML(fis, fos);
        }
    }

    /**
     * Converts the data from the given input stream to an 
     * uncompressed and indented text to the output stream.
     * Both streams are closed by this method.
     * 
     * <br><br>
     * 
     * Savegame compression is automatically detected, so using
     * this method on an uncompressed savegame creates an
     * indented version of that savegame.
     * 
     * @param in The input stream.
     * @param out The output stream.
     * 
     * @throws IOException if thrown while reading or writing the streams. 
     */
    public void convertToXML(InputStream in, OutputStream out) throws IOException {
        try {
            in = new BufferedInputStream(in);
            out = new BufferedOutputStream(out);
            
            // Automatically detect savegame compression:
            in.mark(10);
            byte[] buf = new byte[5];
            in.read(buf, 0, 5);
            in.reset();
            if (!"<?xml".equals(new String(buf, "UTF-8"))) {
                in =  new BufferedInputStream(new GZIPInputStream(in));
            }

            // Support for XML comments has not been added:
            int indent = 0;
            int i;      
            while ((i = in.read()) != -1) {
                char c = (char) i;
                if (c == '<') {
                    i = in.read();
                    char b = (char) i;
                    if (b == '/') {
                        indent -= 4;
                    }
                    for (int h=0; h<indent; h++) {
                        out.write(' ');
                    }
                    out.write(c);
                    if (b != '\n' && b != '\r') {
                        out.write(b);
                    }
                    if (b != '/' && b != '?') {
                        indent += 4;
                    }
                } else if (c == '/') {
                    out.write(c);
                    i = in.read();
                    c = (char) i;
                    if (c == '>') {
                        indent -= 4;
                        out.write(c);
                        out.write('\n');
                    }
                } else if (c == '>') {
                    out.write(c);
                    out.write('\n');
                } else if (c != '\n' && c != '\r') {
                    out.write(c);
                }           
            }

        } finally {
            in.close();
            out.close();
        }
    }
    
    
    /**
     * Prints the usage of this program to standard out.
     */
    private static void printUsage() {
        System.out.println("A program for converting FreeCol Savegames.");
        System.out.println();
        System.out.println("Usage: java -cp FreeCol.jar net.sf.freecol.tools.FSGConverter [-][-]output:xml FSG_FILE [OUTPUT_FILE]");
        System.out.println();
        System.out.println("output:xml \tThe output will be indented XML.");
        System.out.println();
        System.out.println("The output file will get the same name as FSG_FILE if not specified (with \".fsg\" replaced with \".xml\").");
    }
    
    /**
     * An entry point for converting FreeCol Savegames.
     * 
     * @param args The command-line parameters.
     */
    public static void main(String[] args) {
        if (args.length >= 2 && args[0].endsWith("output:xml")) {
            File in = new File(args[1]);
            if (!in.exists()) {
                printUsage();
                System.exit(1);
            }
            File out;
            if (args.length >= 3) {
                out = new File(args[2]);
            } else {
                String filename = in.getName()
                    .replaceAll("." + FreeCol.FREECOL_SAVE_EXTENSION, ".xml");
                if (filename.equals(in.getName())) {
                    filename += ".xml";
                }
                out = new File(filename);
            }
            try {
                FSGConverter fsgc = FSGConverter.getFSGConverter();
                fsgc.convertToXML(in, out);
            } catch (IOException e) {
                System.out.println("An error occured while converting the file.");
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            printUsage();
            System.exit(1);
        }
    }
}
