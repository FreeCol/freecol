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

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


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
        result.append(record.getSourceClassName()).append(' ').append(record.getSourceMethodName());
        result.append("\n\t").append(record.getLevel().getName()).append(": ").append(
                record.getMessage().replaceAll("\n", "\n\t"));
        result.append("\n\t").append(new Date(record.getMillis()).toString());
        result.append("\n\tThread ID: ").append(record.getThreadID()).append('\n');
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("\tStack trace:");
            record.getThrown().printStackTrace(pw);
            pw.println("----------------------------");
            pw.flush();
            result.append(sw.toString());
        }

        return result.toString();
    }
}
