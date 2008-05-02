package net.sf.freecol.common.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Representes a FreeCol savegame.
 */
public class FreeColSavegameFile extends FreeColDataFile {
    private static final Logger logger = Logger.getLogger(FreeColSavegameFile.class.getName());
    
    public static final String SAVEGAME_FILE = "savegame.xml";
    
    public FreeColSavegameFile(File file) throws IOException {
        super(file);
    }
    
    /**
     * Gets the input stream to the savegame data.
     * 
     * @return An <code>InputStream</code> to the file
     *      "savegame.xml" within this data file.
     * @throws IOException if thrown while opening the
     *      input stream.
     */
    public InputStream getSavegameInputStream() throws IOException {
        return getInputStream(SAVEGAME_FILE);
    }

}