package net.sf.freecol.common.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Support for reading a FreeCol data file. The data file
 * is either a ZIP-file or a directory containing certain files.
 */
public class FreeColDataFile {
    private static final Logger logger = Logger.getLogger(FreeColDataFile.class.getName());
    
    private static final String RESOURCES_PROPERTIES_FILE = "resources.properties";

    /** The file this object represents. */
    private final File file;
    
    /**
     * An open JarFile created using {@link #file} (only if {@link #file} is
     * a ZIP-file).
     */ 
    private final JarFile jarFile;
    
    /**
     * Used for keeping track of open files (only if {@link #file} is
     * a directory). 
     */    
    private final List<InputStream> openStreams;
    
    /**
     * A prefix string for the jar-entries (only if {@link #file} is
     * a ZIP-file).
     */
    private final String jarDirectory;

    /**
     * A inputstream directly to a savegame (only if {@link file} is
     * an outdated savegame).
     */
    private final InputStream supportOldSavegames;

    
    /**
     * Opens the given file for reading.
     * 
     * @param file The file to be read.
     * @throws IOException if thrown while opening the file.
     */
    public FreeColDataFile(File file) throws IOException {
        this.file = file;
        if (file.isDirectory()) {
            this.jarFile = null;
            this.openStreams = new LinkedList<InputStream>();
            this.jarDirectory = null;
            this.supportOldSavegames = null;
        } else {
            // START SUPPORT OLD SAVEGAMES
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            in.mark(10);
            byte[] buf = new byte[5];
            in.read(buf, 0, 5);
            in.reset();
            if ((new String(buf)).startsWith("PK")) {
                // START KEEP
                this.jarFile = new JarFile(file);
                this.openStreams = null;
                this.jarDirectory = file.getName().split("\\.")[0];
                // END KEEP
                this.supportOldSavegames = null;
            } else { 
                if (!(new String(buf)).equals("<?xml")) {
                    in = new BufferedInputStream(new GZIPInputStream(in));
                }
                this.supportOldSavegames = in;
                this.jarFile = null;
                this.openStreams = null;
                this.jarDirectory = null;
            }
            // END SUPPORT OLD SAVEGAMES            
        }
    }


    /**
     * Returns an input stream for the specified resource.
     * @param filename The filename of a resource within this collection of
     *      data. If this object represents a directory then the provided filename
     *      should be relative towards the path of the directory. In case
     *      of a compressed archieve it should be the path within the
     *      archive. 
     * @return
     */
    public InputStream getInputStream(String filename) throws IOException {
        if (supportOldSavegames != null) {
            return supportOldSavegames;
        } else if (file.isDirectory()) {
            final InputStream fis = new BufferedInputStream(new FileInputStream(new File(file, filename)));
            openStreams.add(fis);
            return fis;
        } else {
            return new BufferedInputStream(jarFile.getInputStream(jarFile.getJarEntry(jarDirectory + "/" + filename)));
        }
    }

    /**
     * Closes this data file by closing all input streams.
     */
    public void close() {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException e) {}
        }
        if (openStreams != null) {
            for (InputStream is : openStreams) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        if (supportOldSavegames != null) {
            try {
                supportOldSavegames.close();
            } catch (IOException e) {}
        }
    }
}
