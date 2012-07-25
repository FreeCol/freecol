/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.networking.DOMMessage;


/**
 * The default handler for FreeCol's log records. It currently only
 * logs to a file in the format offered by TextFormatter.
 */
public final class DefaultHandler extends Handler {

    private FileWriter fileWriter;

    private final boolean consoleLogging;


    /**
     * The constructor to use.
     * 
     * @param consoleLogging The flag to log to the console as well.
     * @throws FreeColException In case the log file could not be
     *             created/written to.
     */
    public DefaultHandler(boolean consoleLogging, String fileName)
        throws FreeColException {
        this.consoleLogging = consoleLogging;
        File file = new File(fileName);

        if (file.exists()) {
            if (file.isDirectory()) {
                throw new FreeColException("Log file \"" + fileName
                    + "\" could not be created.");
            } else if (file.isFile()) {
                file.delete();
            }
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new FreeColException("Log file \"" + fileName
                + "\" could not be created: " + e.getMessage());
        }

        if (!file.canWrite()) {
            throw new FreeColException("Can not write in log file \""
                + fileName + "\".");
        }

        try {
            fileWriter = new FileWriter(file);
        } catch (IOException e) {
            throw new FreeColException("Can not write in log file \""
                + fileName + "\".");
        }

        // We should use XMLFormatter here in the future
        // or maybe a self-made HTMLFormatter.
        setFormatter(new TextFormatter());

        try {
            String str = "FreeCol game version: " + FreeCol.getRevision()
                + "\nFreeCol protocol version: "
                + DOMMessage.getFreeColProtocolVersion()
                + "\n\nJava vendor: " + System.getProperty("java.vendor")
                + "\nJava version: " + System.getProperty("java.version")
                + "\nJava WM name: " + System.getProperty("java.vm.name")
                + "\nJava WM vendor: " + System.getProperty("java.vm.vendor")
                + "\nJava WM version: " + System.getProperty("java.vm.version")
                + "\n\nOS name: " + System.getProperty("os.name")
                + "\nOS architecture: " + System.getProperty("os.arch")
                + "\nOS version: " + System.getProperty("os.version")
                + "\n\n";
            fileWriter.write(str, 0, str.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes this handler so that it will stop handling log records.
     */
    @Override
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
    @Override
    public void flush() {
        try {
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Publishes the given LogRecord by writing its data to a file using a
     * TextFormatter.
     * 
     * @param record The <code>LogRecord</code> to publish.
     */
    @Override
    public void publish(LogRecord record) {
        if (record.getThrown() != null) {
            FreeColDebugger.handleCrash(record);
        }
        if (record.getLevel().intValue() < getLevel().intValue()) {
            return;
        }

        String str = getFormatter().format(record);
        if (consoleLogging
            && record.getLevel().intValue() >= Level.WARNING.intValue()) {
            System.err.println(str);
        }

        try {
            fileWriter.write(str, 0, str.length());
        } catch (IOException e) {
            System.err.println("Failed to write log record!");
            e.printStackTrace(System.err);
        }

        // Because FreeCol is still in a very early stage:
        flush();
    }
}
