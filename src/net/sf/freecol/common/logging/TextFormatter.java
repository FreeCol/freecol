
package net.sf.freecol.common.logging;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;




/**
* Formats a log record's data into human-readable text.
*/
final class TextFormatter extends Formatter {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


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
    public String format(LogRecord record) {
        String level;
        if (record.getLevel() == Level.INFO) {
            level = "INFO";
        } else if (record.getLevel() == Level.ALL) {
            level = "ALL";
        } else if (record.getLevel() == Level.SEVERE) {
            level = "SEVERE";
        } else if (record.getLevel() == Level.WARNING) {
            level = "WARNING";
        } else {
            level = "UNKNOWN";
        }

        String result = record.getSourceClassName() + ' ' + record.getSourceMethodName();
        result += "\n\t" + level + ": " + record.getMessage().replaceAll("\n", "\n\t");
        result += "\n\t" + new Date(record.getMillis()).toString();
        result += "\n\tThread ID: " + record.getThreadID() + '\n';

        return result;
    }
}
