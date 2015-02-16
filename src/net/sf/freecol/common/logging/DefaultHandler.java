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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.util.Utils;


/**
 * The default handler for FreeCol's log records. It currently only
 * logs to a file in the format offered by TextFormatter.
 */
public final class DefaultHandler extends Handler {

    private final Writer writer;

    private final boolean consoleLogging;


    /**
     * The constructor to use.
     * 
     * @param consoleLogging The flag to log to the console as well.
     * @exception FreeColException In case the log file could not be
     *     created/written to.
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
                + "\" could not be created.", e);
        }
        if (!file.canWrite()) {
            throw new FreeColException("Can not write in log file \""
                + fileName + "\".");
        }
        if ((writer = Utils.getFileUTF8Writer(file)) == null) {
            throw new FreeColException("Can not write in log file \""
                + fileName + "\".");
        }

        // We should use XMLFormatter here in the future
        // or maybe a self-made HTMLFormatter.
        setFormatter(new TextFormatter());

        try {
            StringBuilder sb = new StringBuilder(512);
            sb.append("FreeCol game version: ")
                .append(FreeCol.getRevision())
                .append("\nFreeCol protocol version: ")
                .append(DOMMessage.getFreeColProtocolVersion())
                .append("\n\nJava vendor: ")
                .append(System.getProperty("java.vendor"))
                .append("\nJava version: ")
                .append(System.getProperty("java.version"))
                .append("\nJava WM name: ")
                .append(System.getProperty("java.vm.name"))
                .append("\nJava WM vendor: ")
                .append(System.getProperty("java.vm.vendor"))
                .append("\nJava WM version: ")
                .append(System.getProperty("java.vm.version"))
                .append("\n\nOS name: ")
                .append(System.getProperty("os.name"))
                .append("\nOS architecture: ")
                .append(System.getProperty("os.arch"))
                .append("\nOS version: ")
                .append(System.getProperty("os.version"))
                .append("\n\n");
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Closes this handler so that it will stop handling log records.
     */
    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Flushes the data that this handler has logged.
     */
    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace(System.err);
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
            FreeColDebugger.handleCrash();
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
            writer.write(str, 0, str.length());
        } catch (IOException e) {
            System.err.println("Failed to write log record!");
            e.printStackTrace(System.err);
        }

        // Because FreeCol is still in a very early stage:
        flush();
    }
}
