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

package net.sf.freecol.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


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
     * @param object The <code>Object</code> to use.
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
     * @return A <code>String</code> encapsulating the object state.
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
     * @return The restored <code>Random</code>.
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
     * Create a new file writer that uses UTF-8.
     *
     * @param file A <code>File</code> to write to.
     * @return A <code>Writer</code> for this file.
     */
    public static Writer getFileUTF8Writer(File file) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "No FileOutputStream for "
                + file.getName(), e);
            return null;
        }
        OutputStreamWriter osw;
        try {
            osw = new OutputStreamWriter(fos, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, "No OutputStreamWriter for "
                + file.getName(), e);
            try {
                fos.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to close", ioe);
            }
            return null;
        }
        return osw;
    }
}
