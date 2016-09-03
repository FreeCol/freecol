/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

package net.sf.freecol.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.CharBuffer;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;


/**
 * Collection of small static helper methods.
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /** Hex constant digits for get/restoreRandomState. */
    private static final String HEX_DIGITS = "0123456789ABCDEF";


    /**
     * Check if two objects are equal but also checks for null.
     *
     * @param <T> The object type.
     * @param one First object to compare
     * @param two Second object to compare
     * @return True if the arguments are either both null or equal in the
     *     sense of their equals() method.
     */
    public static <T> boolean equals(T one, T two) {
        return (one == null) ? (two == null) : one.equals(two);
    }

    /**
     * Get a hash code for an object, even null.
     *
     * @param object The {@code Object} to use.
     * @return A hash code.
     */
    public static int hashCode(Object object) {
        return (object == null) ? 31 : object.hashCode();
    }

    /**
     * Get the internal state of a random number generator as a
     * string.  It would have been more convenient to simply return
     * the current seed, but unfortunately it is private.
     *
     * @param random A pseudo-random number source.
     * @return A {@code String} encapsulating the object state.
     */
    public static synchronized String getRandomState(Random random) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(random);
            oos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("IO exception in memory!?", e);
        }
        byte[] bytes = bos.toByteArray();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS.charAt((b >> 4) & 0x0F));
            sb.append(HEX_DIGITS.charAt(b & 0x0F));
        }
        return sb.toString();
    }

    /**
     * Restore a previously saved state.
     *
     * @param state The saved state (@see #getRandomState()).
     * @return The restored {@code Random}.
     */
    public static synchronized Random restoreRandomState(String state) {
        if (state == null || state.isEmpty()) return null;
        byte[] bytes = new byte[state.length() / 2];
        int pos = 0;
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) HEX_DIGITS.indexOf(state.charAt(pos++));
            bytes[i] <<= 4;
            bytes[i] |= (byte) HEX_DIGITS.indexOf(state.charAt(pos++));
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (Random) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Unable to restore random state.", e);
        }
        return null;
    }

    /**
     * Create a new file reader that uses UTF-8.
     *
     * @param file A {@code File} to read from.
     * @return A {@code Reader} for this file.
     */
    public static Reader getFileUTF8Reader(File file) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            logger.log(Level.WARNING, "No FileInputStream for "
                + file.getPath(), fnfe);
            return null;
        }
        InputStreamReader isr;
        try {
            isr = new InputStreamReader(fis, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            logger.log(Level.WARNING, "No InputStreamWriter for "
                + file.getPath(), uee);
            try {
                fis.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to close", ioe);
            }
            return null;
        }
        return isr;
    }

    /**
     * Get the UTF-8 encoded contents of a file.
     *
     * @param file The {@code File} to query.
     * @return The contents string, or null on error.
     */
    public static String getUTF8Contents(File file) {
        String ret = null;
        Reader reader = getFileUTF8Reader(file);
        if (reader != null) {
            CharBuffer cb = CharBuffer.allocate((int)file.length());
            try {
                reader.read(cb);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Read failed for " + file.getPath(),
                           ioe);
            }
            cb.flip();
            ret = cb.toString();
            try {
                reader.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to close", ioe);
            }
        }
        return ret;
    }
        
    /**
     * Create a new file writer that uses UTF-8.
     *
     * @param file A {@code File} to write to.
     * @return A {@code Writer} for this file.
     */
    public static Writer getFileUTF8Writer(File file) {
        return getF8W(file, false);
    }

    /**
     * Create a new appending file writer that uses UTF-8.
     *
     * @param file A {@code File} to append to.
     * @return A {@code Writer} for this file.
     */
    public static Writer getFileUTF8AppendWriter(File file) {
        return getF8W(file, true);
    }
    
    /**
     * Create a new file writer that uses UTF-8.
     *
     * @param file A {@code File} to write to.
     * @param append If true, append to the file.
     * @return A {@code Writer} for this file.
     */
    private static Writer getF8W(File file, boolean append) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, append);
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "No FileOutputStream for "
                + file.getName(), e);
            return null;
        }
        Writer wr = getUTF8Writer(fos);
        if (wr == null) {
            try {
                fos.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to close", ioe);
            }
        }
        return wr;            
    }

    /**
     * Create a new file writer that uses UTF-8.
     *
     * @param os An {@code OutputStream} to write to.
     * @return A {@code Writer} for this file.
     */
    public static Writer getUTF8Writer(OutputStream os) {
        try {
            return new OutputStreamWriter(os, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            logger.log(Level.WARNING, "No OutputStreamWriter", uee);
        }
        return null;
    }

    /**
     * Helper to make an XML Transformer.
     *
     * @param declaration If true, include the XML declaration.
     * @param indent If true, set up the transformer to indent.
     * @return A suitable {@code Transformer}.
     */
    public static Transformer makeTransformer(boolean declaration,
                                              boolean indent) {
        Transformer tf = null;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            tf = factory.newTransformer();
            if (!declaration) {
                tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }
            if (indent) {
                tf.setOutputProperty(OutputKeys.INDENT, "yes");
                tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
        } catch (TransformerException e) {
            logger.log(Level.WARNING, "Failed to install transformer!", e);
        }
        return tf;
    }
}
