
package net.sf.freecol.common.logging;

import net.sf.freecol.common.FreeColException;
import java.util.logging.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.networking.Message;


/**
* The default handler for FreeCol's log records. It currently
* only logs to a file in the format offered by TextFormatter.
*/
public final class DefaultHandler extends Handler {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

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
        
        try {
            String str = "FreeCol game version: " + FreeCol.getVersion() + "\n" 
                        + "FreeCol protocol version: " + Message.getFreeColProtocolVersion() + "\n\n"
                        + "Java vendor: " + System.getProperty("java.vendor") + "\n"
                        + "Java version: " + System.getProperty("java.version") + "\n"
                        + "Java WM name: " + System.getProperty("java.vm.name") + "\n"
                        + "Java WM vendor: " + System.getProperty("java.vm.vendor") + "\n"
                        + "Java WM version: " + System.getProperty("java.vm.version") + "\n\n"
                        + "OS name: " + System.getProperty("os.name") + "\n"
                        + "OS architecture: " + System.getProperty("os.arch") + "\n"
                        + "OS version: " + System.getProperty("os.version") + "\n\n";
            fileWriter.write(str, 0, str.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
