/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Location;


/**
 * A class to wrap a StringBuilder for log generation purposes.
 */
public class LogBuilder {

    /** The logger. */
    private Logger logger;

    /** The logging level. */
    private Level level;

    /** The string builder to use. */
    private StringBuilder sb;

    /** The remembered buffer index. */
    private int point;


    /**
     * Create a new LogBuilder that can only be used as a buffer.
     * That is, flush is inoperative.
     *
     * @param size An initial size for the buffer.
     */
    public LogBuilder(int size) {
        this.logger = null;
        this.level = Level.INFO;
        this.sb = (size <= 0) ? null : new StringBuilder(size);
        this.point = -1;
    }

    /**
     * Create a new LogBuilder for a given logger and level.
     *
     * @param logger The <code>Logger</code> to write to.
     * @param level The logging <code>Level</code>.
     */
    public LogBuilder(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
        this.sb = (logger != null && logger.isLoggable(level))
            ? new StringBuilder(256)
            : null;
        this.point = -1;
    }


    /**
     * Convert a simple object to a string suitable for a log buffer.
     *
     * @param o The <code>Object</code> to convert.
     */
    private static String o2s(Object o) {
        return (o == null) ? "null"
            : (o instanceof Class) ? ((Class)o).getName()
            : (o instanceof String) ? (String)o
            : (o instanceof Location) ? ((Location)o).toShortString()
            : o.toString();
    }

    /**
     * Add objects to a string builder.
     *
     * @param sb The <code>StringBuilder</code> to add to.
     * @param objects The objects to add.
     */
    private static void add(StringBuilder sb, Object... objects) {
        for (Object o : objects) {
            if (o instanceof Object[]) {
                for (Object o2 : (Object[])o) {
                    sb.append(o2s(o2));
                }
            } else {
                sb.append(o2s(o));
            }
        }
    }

    /**
     * Add objects to the buffer.
     *
     * @param objects The objects to add.
     */
    public void add(Object... objects) {
        if (sb != null) add(sb, objects);
    }

    /**
     * Truncate the buffer to a given size.
     *
     * @param size The size to truncate to.
     */
    public void truncate(int size) {
        if (sb != null && sb.length() > size) sb.setLength(size);
    }

    /**
     * Remember a position in a buffer.
     */
    public void mark() {
        this.point = (sb == null) ? -1 : sb.length();
    }

    /**
     * Check if a buffer has grown since marked, and optionally insert
     * text at that point.
     *
     * @param objects Optional <code>Object</code>s to insert if the buffer has
     *     grown.
     * @return True if the buffer grew (before inserting).
     */
    public boolean grew(Object... objects) {
        if (sb == null || sb.length() <= this.point) return false;
        StringBuilder sb2 = new StringBuilder(64);
        add(sb2, objects);
        this.sb.insert(this.point, sb2.toString());
        return true;
    }

    /**
     * Shorten a buffer by a trailing delimiter.
     *
     * (Cheats, does not really check that the delimiter is there)
     *
     * @param delim The delimiter to remove.
     */
    public void shrink(String delim) {
        if (sb != null && delim != null) {
            sb.setLength(sb.length() - delim.length());
        }
    }

    /**
     * Flush the saved data to the logger.
     */
    public void flush() {
        if (sb != null) {
            this.logger.log(this.level, sb.toString());
        }
    }

    /**
     * Get the buffer contents as a string.
     *
     * @return The buffer contents.
     */
    public String toString() {
        return (sb == null) ? "" : sb.toString();
    }

    /**
     * Output to a logger.
     *
     * @param logger The <code>Logger</code> to write to.
     * @param level The logging <code>Level</code>.
     */
    public void log(Logger logger, Level level) {
        logger.log(level, toString());
    }

    /**
     * Add a group of objects to the buffer at a particular width
     *
     * @param size The width to set.
     * @param objects The <code>Object</code>s to add.
     */
    public static String wide(int size, Object... objects) {
        if (size == 0) return "";
        boolean left = size > 0;
        if (!left) size = -size;
        StringBuilder s2 = new StringBuilder(size);
        add(s2, objects);
        int delta = size - s2.length();
        if (left) {
            for (; delta > 0; delta--) s2.append(" ");
        } else {
            for (; delta > 0; delta--) s2.insert(0, " ");
        }
        if (delta < 0) s2.setLength(size);
        return s2.toString();
    }
}
