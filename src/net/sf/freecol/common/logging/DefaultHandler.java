/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import net.sf.freecol.common.util.OSUtils;
import net.sf.freecol.common.util.Utils;


/**
 * The default handler for FreeCol's log records. It currently only
 * logs to a file in the format offered by TextFormatter.
 */
public final class DefaultHandler extends Handler {

    /** Lock for the writer. */
    private final Object writerLock = new Object();

    /** A writer to write log records with. */
    private Writer writer = null;

    /** Flag to enable console logging. */
    private final boolean consoleLogging;


    /**
     * The constructor to use.
     * 
     * @param consoleLogging The flag to log to the console as well.
     * @param writer The {@code Writer} to use for logging.
     */
    public DefaultHandler(boolean consoleLogging, Writer writer) {
        this.consoleLogging = consoleLogging;
        this.writer = writer;
        
        // We should use XMLFormatter here in the future
        // or maybe a self-made HTMLFormatter.
        setFormatter(new TextFormatter());

        try {
            StringBuilder sb = new StringBuilder(512);
            sb.append("FreeCol game version: ")
                .append(FreeCol.getRevision())
                .append("\nFreeCol protocol version: ")
                .append(FreeCol.getFreeColProtocolVersion())
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
                .append(OSUtils.getOperatingSystem())
                .append("\nOS architecture: ")
                .append(System.getProperty("os.arch"))
                .append("\nOS version: ")
                .append(System.getProperty("os.version"))
                .append("\n\n");
            synchronized (this.writerLock) {
                this.writer.write(sb.toString());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }

    /**
     * Closes this handler so that it will stop handling log records.
     */
    @Override
    public void close() {
        synchronized (this.writerLock) {
            if (this.writer != null) {
                try {
                    this.writer.close();
                    this.writer = null;
                } catch (IOException ioe) {
                    ioe.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Flushes the data that this handler has logged.
     */
    @Override
    public void flush() {
        synchronized (this.writerLock) {
            if (this.writer != null) {
                try {
                    this.writer.flush();
                } catch (IOException ioe) {
                    ioe.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Publishes the given LogRecord by writing its data to a file using a
     * TextFormatter.
     * 
     * @param record The {@code LogRecord} to publish.
     */
    @Override
    public void publish(LogRecord record) {
        if (record.getLevel().intValue() < getLevel().intValue()
            && record.getThrown() == null) {
            return;
        }

        String str = getFormatter().format(record);
        if (consoleLogging
            && record.getLevel().intValue() >= Level.WARNING.intValue()) {
            System.err.println(str);
        }

        synchronized (this.writerLock) {
            if (this.writer != null) {
                try {
                    this.writer.write(str, 0, str.length());
                    // Because FreeCol is still in a very early stage.
                    flush();
                } catch (IOException ioe) {
                    System.err.println("Failed to write log record: " + str);
                    ioe.printStackTrace(System.err);
                }
            }
        }

        // Do this last, as it might shut down a debug run
        Throwable t = record.getThrown();
        if (t != null && !(t instanceof FreeColException
                && ((FreeColException)t).debugAllowed())) {
            FreeColDebugger.handleCrash();
        }
    }
}
