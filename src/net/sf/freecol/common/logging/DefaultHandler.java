/*
 *  DefaultHandler.java - The default handler for FreeCol's log records. It currently
 *                        only logs to a file in the format offered by TextFormatter.
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

import net.sf.freecol.common.FreeColException;
import java.util.logging.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;



/**
* The default handler for FreeCol's log records. It currently
* only logs to a file in the format offered by TextFormatter.
*/
public final class DefaultHandler extends Handler {

    private static final String fileName = new String("FreeCol.log");

    private FileWriter fileWriter;

    
    
    

    /**
    * The constructor to use.
    * @throws FreeColException In case the log file could not be created/written to.
    */
    public DefaultHandler() throws FreeColException {
        File file = new File(fileName);

        if (file.exists()) {
            if (file.isDirectory()) {
                throw new FreeColException("Log file \"" + fileName + "\" could not be created.");
            } else if (file.isFile()) {
                file.delete();
            }
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new FreeColException("Log file \"" + fileName + "\" could not be created.");
        }

        if (!file.canWrite()) {
            throw new FreeColException("Can not write in log file \"" + fileName + "\".");
        }

        try {
            fileWriter = new FileWriter(file);
        } catch (IOException e) {
            throw new FreeColException("Can not write in log file \"" + fileName + "\".");
        }

        // We should use XMLFormatter here in the future
        // or maybe a self-made HTMLFormatter.
        setFormatter(new TextFormatter());
    }






    /**
    * Closes this handler so that it will stop handling log records.
    */
    public void close() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
    * Flushes the data that this handler has logged.
    */
    public void flush() {
        try {
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
    * Publishes the given LogRecord by writing its data to a file using
    * a TextFormatter.
    *
    * @param record The log record to publish.
    */
    public void publish(LogRecord record) {
        if (record.getLevel().intValue() < getLevel().intValue()) {
            return;
        }

        String str = getFormatter().format(record);
        try {
            fileWriter.write(str, 0, str.length());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Because FreeCol is still in a very early stage:
        flush();
    }
}
