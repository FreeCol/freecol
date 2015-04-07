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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Location;


/**
 * A class to wrap a StringBuilder for log generation purposes.
 */
public class LogBuilder {

    /** The string builder to use. */
    private final StringBuilder sb;

    /** The remembered buffer index. */
    private final List<Integer> points = new ArrayList<>();


    /**
     * Create a new LogBuilder that can only be used as a buffer.
     *
     * @param size An initial size for the buffer.
     */
    public LogBuilder(int size) {
        this.sb = (size <= 0) ? null : new StringBuilder(size);
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
     * Add a delimited collection to the buffer.
     *
     * @param delim An internal delimiter.
     * @param c The <code>Collection</code> of objects to add.
     */
    public <T> void addCollection(String delim, Collection<T> c) {
        if (sb != null) {
            for (T t : c) add(sb, t, delim);
            if (!c.isEmpty()) shrink(delim);
        }
    }

    /**
     * Add a stack trace to the buffer.
     */
    public void addStackTrace() {
        if (sb != null) FreeColDebugger.addStackTrace(this);
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
        if (sb != null) {
            this.points.add(0, sb.length());
        }
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
        if (sb == null) return false;
        int p = this.points.remove(0);
        if (sb.length() <= p) return false;
        StringBuilder sb2 = new StringBuilder(64);
        add(sb2, objects);
        this.sb.insert(p, sb2.toString());
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
     * Output to a logger.
     *
     * @param logger The <code>Logger</code> to write to.
     * @param level The logging <code>Level</code>.
     */
    public void log(Logger logger, Level level) {
        if (sb != null && logger != null && level != null
            && logger.isLoggable(level)) {
            logger.log(level, toString());
        }
    }

    /**
     * Get the amount of accumulated data.
     *
     * @return The amount of data accumulated so far.
     */
    public int size() {
        return (sb == null) ? 0 : sb.length();
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

    /**
     * Get the buffer contents as a string.
     *
     * @return The buffer contents.
     */
    @Override
    public String toString() {
        return (sb == null) ? "" : sb.toString();
    }
}
