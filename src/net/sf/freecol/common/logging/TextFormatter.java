/*
 *  TextFormatter.java - Formats a log record's data into human-readable text.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.common.logging;

import java.util.logging.*;
import java.util.Date;




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
    public String format(LogRecord record) {
        String level;
        if (record.getLevel() == Level.INFO) {
            level = "INFO";
        }
        else if (record.getLevel() == Level.ALL) {
            level = "ALL";
        }
        else if (record.getLevel() == Level.SEVERE) {
            level = "SEVERE";
        }
        else if (record.getLevel() == Level.WARNING) {
            level = "WARNING";
        }
        else {
            level = "UNKNOWN";
        }
        
        String result = record.getSourceClassName() + ' ' + record.getSourceMethodName();
        result += "\n\t" + level + ": " + record.getMessage();
        result += "\n\t" + new Date(record.getMillis()).toString();
        result += "\n\tThread ID: " + record.getThreadID() + '\n';
        
        return result;
    }
}
