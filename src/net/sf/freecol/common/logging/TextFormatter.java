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

package net.sf.freecol.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * Formats a log record's data into human-readable text.
 */
final class TextFormatter extends Formatter {

    /**
     * The constructor to use.
     */
    public TextFormatter() {
    }

    /**
     * Formats the given log record's data into human-readable text.
     * 
     * @param record The log record whose data needs to be formatted.
     * @return The log record's data as a string.
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder result = new StringBuilder();
        result.append(record.getSourceClassName())
            .append(' ').append(record.getSourceMethodName())
            .append("\n\t").append(record.getLevel().getName())
            .append(": ").append(record.getMessage().replaceAll("\n", "\n\t"))
            .append("\n\t").append(new Date(record.getMillis()))
            .append("\n\tThread: ").append(record.getThreadID())
            .append('\n');
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("\tStack trace:");
            record.getThrown().printStackTrace(pw);
            pw.println("----------------------------");
            pw.flush();
            result.append(sw);
        }

        return result.toString();
    }
}
