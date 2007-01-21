
package net.sf.freecol;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.logging.DefaultHandler;
import net.sf.freecol.server.FreeColServer;



/**
* This class is responsible for handling the command-line arguments
* and starting either the stand-alone server or the client-GUI.
*
* @see net.sf.freecol.client.FreeColClient FreeColClient
* @see net.sf.freecol.server.FreeColServer FreeColServer
*/
public final class FreeCol {
    private static final Logger logger = Logger.getLogger(FreeCol.class.getName());
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final  Specification  specification = new Specification();

    private  static final String FREECOL_VERSION = "0.5.4-cvs";

    public static final String  META_SERVER_ADDRESS = "meta.freecol.org";
    public static final int     META_SERVER_PORT = 3540;

    private static final String MIN_JDK_VERSION = "1.5";
    private static final String  FILE_SEP = System.getProperty("file.separator");

    private static boolean  windowed = false,
                            sound = true,
                            javaCheck = true,
                            memoryCheck = true;
    private static Rectangle windowSize = new Rectangle(-1, -1);
    private static String   dataFolder = "";
    private static FreeColClient freeColClient;

    private static boolean standAloneServer = false;
    private static boolean inDebugMode = false;

    private static int serverPort;
    private static String serverName = null;
    
    private static File saveDirectory;
    
    private static File savegameFile = null;



    private FreeCol() {}


    /**
     * The entrypoint.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {

        // TODO: The location of the save directory should be determined by the installer.
        saveDirectory = new File(System.getProperty("user.home"));
        if (!saveDirectory.exists()) {
            saveDirectory = new File("save");
        } else {        
            saveDirectory = new File(saveDirectory, "freecol" + FILE_SEP + "save");
        }
        
        handleArgs(args);

        if (javaCheck && !checkJavaVersion()) {
            System.err.println("Java version " + MIN_JDK_VERSION +
                            " or better is recommended in order to run FreeCol." +
                            " Use --no-java-check to skip this check.");
            return;
        }

        int  minMemory = 128;  // million bytes
        if (memoryCheck && Runtime.getRuntime().maxMemory() < minMemory * 1000000) {
            System.out.println("You need to assign more memory to the JVM. Restart FreeCol with:");
            System.out.println("java -Xmx" + minMemory + "M -jar FreeCol.jar");
            return;
        }

        final Logger baseLogger = Logger.getLogger("");
        final Handler[] handlers = baseLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            baseLogger.removeHandler(handlers[i]);
        }

        try {
            baseLogger.addHandler(new DefaultHandler());
        } catch (FreeColException e) {
            e.printStackTrace();
        }
        
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }

        if (standAloneServer) {
            logger.info("Starting stand-alone server.");
            try {
                final FreeColServer freeColServer;
                if (savegameFile != null) {
                    try {
                        // Get suggestions for "singleplayer" and "public game" settings from the file:
                        XMLStreamReader in = FreeColServer.createXMLStreamReader(savegameFile);
                        in.nextTag();                    
                        final boolean defaultSingleplayer = Boolean.valueOf(in.getAttributeValue(null, "singleplayer")).booleanValue();
                        final boolean defaultPublicServer;
                        final String publicServerStr =  in.getAttributeValue(null, "publicServer");
                        if (publicServerStr != null) {
                            defaultPublicServer = Boolean.valueOf(publicServerStr).booleanValue();
                        } else {
                            defaultPublicServer = false;
                        }
                        in.close();
                        
                        freeColServer = new FreeColServer(savegameFile, defaultPublicServer, defaultSingleplayer, serverPort, serverName);
                    } catch (Exception e) {
                        System.out.println("Could not load savegame.");
                        return;
                    }
                } else {
                    freeColServer = new FreeColServer(true, false, serverPort, serverName);
                }

                Runtime runtime = Runtime.getRuntime();
                runtime.addShutdownHook(new Thread() {
                    public void run() {
                        freeColServer.getController().shutdown();
                    }
                });
            } catch (IOException e) {
                System.err.println("Error while loading server: " + e);
                System.exit(-1);
            }
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel(new FreeColLookAndFeel(dataFolder));
                    } catch (UnsupportedLookAndFeelException e) {
                        logger.warning("Could not load the \"FreeCol Look and Feel\"");
                    } catch (FreeColException e) {
                        e.printStackTrace();
                        System.out.println("\nThe data files could not be found by FreeCol. Please make sure");
                        System.out.println("they are present. If FreeCol is looking in the wrong directory");
                        System.out.println("then run the game with a command-line parameter:");
                        System.out.println("");
                        printUsage();
                        return;
                    }
                }
            });

            // TODO: don't use same datafolder for both images and music because the images are best kept inside the .JAR file.

            logger.info("Now starting to load images.");

            ImageLibrary lib;
            try {
                lib = new ImageLibrary(dataFolder);
            } catch (FreeColException e) {
                e.printStackTrace();
                System.out.println("\nThe data files could not be found by FreeCol. Please make sure");
                System.out.println("they are present. If FreeCol is looking in the wrong directory");
                System.out.println("then run the game with a command-line parameter:");
                System.out.println("");
                printUsage();
                return;
            }

            MusicLibrary    musicLibrary = null;
            SfxLibrary      sfxLibrary = null;
            if (sound) {
                try {
                    musicLibrary = new MusicLibrary(dataFolder);
                } catch (FreeColException e) {
                    System.out.println("The music files could not be loaded by FreeCol. Disabling music.");
                }

                try {
                    sfxLibrary = new SfxLibrary(dataFolder);
                } catch (FreeColException e) {
                    System.out.println("The sfx files could not be loaded by FreeCol. Disabling sfx.");
                }
            }

            freeColClient = new FreeColClient(windowed, windowSize, lib, musicLibrary, sfxLibrary);

            if (savegameFile != null) {
                final FreeColClient theFreeColClient = freeColClient;
                final File theSavegameFile = savegameFile;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        theFreeColClient.getConnectController().loadGame(theSavegameFile);
                    }
                });
            }
        }
    }


    /**
    * Returns the directory where the savegames should be put.
    * @return The directory where the savegames should be put.
    */
    public static File getSaveDirectory() {
        return saveDirectory;
    }
    
    /**
     * Returns the directory where the autogenerated savegames 
     * should be put.
     * 
     * @return The directory.
     */
    public static File getAutosaveDirectory() {
    	return saveDirectory;
    }

    /**
    * Ensure that the Java version is good enough. JDK 1.4 or better is
    * required.
    *
    * @return true if Java version is at least 1.4.0.
    */
    private static boolean checkJavaVersion() {
        // Must use string comparison because some JVM's provide
        // versions like "1.4.1"
        String version = System.getProperty("java.version");
        boolean success = (version.compareTo(MIN_JDK_VERSION) >= 0);
        return success;
    }



    /**
     * Checks the command-line arguments and takes appropriate actions
     * for each of them.
     *
     * @param args The command-line arguments.
     */
    private static void handleArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--freecol-data")) {
                i++;
                if (i < args.length) {
                    dataFolder = args[i];

                    // append a file separator to the data folder if necessary
                    if ( ! dataFolder.endsWith(FILE_SEP)) {
                        dataFolder += FILE_SEP;
                    }
                } else {
                    printUsage();
                    System.exit(0);
                }
            } else if (args[i].equals("--no-java-check")) {
                javaCheck = false;
            } else if (args[i].length() >= 10 && args[i].substring(0, 10).equals("--windowed")) {
                if (args[i].length() > 10 && args[i].charAt(10) != ' ') {
                    // TODO: Check if the input values are legal.
                    try {
                        int x = 0;
                        int j = 10;
                        
                        if (args[i].charAt(10) == '=') {
                            j++;
                        }

                        for (; args[i].charAt(j) != 'x'; j++) {
                            x *= 10;
                            x += Character.digit(args[i].charAt(j), 10);
                        }

                        int y = 0;
                        for (j++; j < args[i].length() && args[i].charAt(j) != ' '; j++) {
                            y *= 10;
                            y += Character.digit(args[i].charAt(j), 10);
                        }
                        windowSize = new Rectangle(x, y);
                    } catch (Exception e) {
                        printUsage();
                        System.exit(0);
                    }
                } else if (args[i].length() != 10) {
                    printUsage();
                    System.exit(0);
                }
                
                windowed = true;
            } else if (args[i].equals("--no-sound")) {
                sound = false;
            } else if (args[i].equals("--no-memory-check")) {
                memoryCheck = false;
            } else if (args[i].equals("--usage") || args[i].equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (args[i].equals("--version")) {
                System.out.println("FreeCol " + getVersion());
                System.exit(0);
            } else if (args[i].equals("--debug")) {
                inDebugMode = true;
            } else if (args[i].equals("--server")) {
                standAloneServer = true;
                i++;
                if (i >= args.length) {
                    printUsage();
                    System.out.println("You will need to specify a port number when using the \"--server\" option.");
                    System.exit(1);
                }

                try {
                    serverPort = Integer.parseInt(args[i]);
                } catch (NumberFormatException nfe) {
                    printUsage();
                    System.out.println("The text after the \"--server\" option should be a valid port number.");
                    System.exit(1);
                }
            } else if (args[i].equals("--load-savegame")) {
                i++;
                if (i < args.length) {
                    savegameFile = new File(args[i]);
                    if (!savegameFile.exists() || !savegameFile.isFile()) {                        
                        savegameFile = new File(getSaveDirectory(), args[i]);
                        if (!savegameFile.exists() || !savegameFile.isFile()) {
                        	System.out.println("The given savegame file could not be found: " + args[i]);
                        	System.exit(1);
                        }
                    }
                } else {
                    printUsage();
                    System.exit(0);
                }
            } else if (args[i].equals("--server-help")) {
                printServerUsage();
                System.exit(0);
            } else if (args[i].equals("--server-name")) {
                if (!standAloneServer) {
                    printServerUsage();
                    System.exit(1);
                }
                i++;
                if (i >= args.length) {
                    printUsage();
                    System.out.println("You will need to specify a name when using the \"--server-name\" option.");
                    System.exit(1);
                }
                serverName = args[i];
            } else {
                printUsage();
                System.exit(1);
            }
        }
    }


    /**
    * Gets the current version of game.
    *
    * @return The current version of the game using the format "x.y.z",
    *         where "x" is major, "y" is minor and "z" is revision.
    */
    public static String getVersion() {
        return FREECOL_VERSION;
    }


    /**
    * Checks if the program is in "Debug mode".
    * @return <code>true</code> if the program is in debug
    *       mode and <code>false</code> otherwise.
    */
    public static boolean isInDebugMode() {
        return inDebugMode;
    }

    
    /**
    * Sets the "debug mode" to be active or not.
    * @param debug Should be <code>true</code> in order
    *       to active debug mode and <code>false</code>
    *       otherwise.
    */
    public static void setInDebugMode(boolean debug) {
        inDebugMode = debug;
    }


    /**
    * Prints the command-line usage for the server options.
    */
    private static void printServerUsage() {
        System.out.println("Usage: java -Xmx512M -jar FreeCol.jar --server PORT [OPTIONS]");
        System.out.println("");
        System.out.println("Starts a stand-alone server on the specifed port");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("--server-name NAME");
        System.out.println("  specifies a custom name for the server");
        System.out.println("--load-savegame SAVEGAME_FILE");
        System.out.println("  loads the given savegame.");      
        System.out.println("--no-java-check");
        System.out.println("  skips the java version check");        
        System.out.println();
    }


    /**
    * Prints the command-line usage (the 'help' for command-line
    * arguments).
    */
    private static void printUsage() {
        System.out.println("Usage: java -Xmx512M -jar FreeCol.jar [OPTIONS]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("--freecol-data DIR");
        System.out.println("  DIR should be the directory with FreeCol's data files, it");
        System.out.println("  has a subdirectory called 'images'");
        System.out.println("--windowed[[=]WIDTHxHEIGHT]");
        System.out.println("  runs FreeCol in windowed mode instead of full screen mode");
        System.out.println("--load-savegame SAVEGAME_FILE");
        System.out.println("  loads the given savegame.");
        System.out.println("--no-sound");
        System.out.println("  runs FreeCol without sound");
        System.out.println("--no-java-check");
        System.out.println("  skips the java version check");
        System.out.println("--no-memory-check");
        System.out.println("  skips the memory check");        
        System.out.println("--usage");
        System.out.println("  displays this help screen");
        System.out.println("--version");
        System.out.println("  displays the version number");
        System.out.println("--server PORT");
        System.out.println("  starts a stand-alone server on the specifed port");
        System.out.println("--server-help");
        System.out.println("  displays a help screen for the more advanced server options");
        System.out.println();
    }
}
