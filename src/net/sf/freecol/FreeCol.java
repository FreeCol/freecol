/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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


package net.sf.freecol;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.logging.DefaultHandler;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.server.FreeColServer;


/**
* This class is responsible for handling the command-line arguments
* and starting either the stand-alone server or the client-GUI.
*
* @see net.sf.freecol.client.FreeColClient FreeColClient
* @see net.sf.freecol.server.FreeColServer FreeColServer
*/
public final class FreeCol {
    

    public static final String  META_SERVER_ADDRESS = "meta.freecol.org";
    public static final int     META_SERVER_PORT = 3540;

    /**
     * The space not being used in windowed mode.
     */
    private static final int     DEFAULT_WINDOW_SPACE = 100;

    private static final Logger logger = Logger.getLogger(FreeCol.class.getName());
    
    /**
     * Initialized on first access. @See #getSpecification
     */
    private static Specification specification;

    private static final String FREECOL_VERSION = "0.8.0-svn";
    private static String FREECOL_REVISION;
    
    private static final String MIN_JDK_VERSION = "1.5";
    private static final String FILE_SEP = System.getProperty("file.separator");

    private static final String DEFAULT_SPLASH_FILE = "splash.jpg";

    private static boolean  windowed = false,
                            sound = true,
                            javaCheck = true,
                            memoryCheck = true,
                            consoleLogging = false;
    private static Dimension windowSize = new Dimension(-1, -1);
    private static String   dataFolder = "";
    
    private static FreeColClient freeColClient;

    private static boolean standAloneServer = false;
    private static boolean inDebugMode = false;

    private static int serverPort;
    private static String serverName = null;
    
    private static File saveDirectory;
    
    private static File savegameFile = null;
    
    private static File clientOptionsFile = null;

    private static Level logLevel = Level.INFO;

    private FreeCol() {
        // Hide constructor
    }

    /**
     * The entrypoint.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {

        try {
            Manifest manifest = new Manifest(FreeCol.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
            Attributes attribs = manifest.getMainAttributes();
            String revision = attribs.getValue("Revision");
            FREECOL_REVISION = FREECOL_VERSION + " (Revision: " + revision + ")";
        } catch (Exception e) {
            System.out.println("Unable to load Manifest.");
            FREECOL_REVISION = FREECOL_VERSION;
        }

        initLogging();
        
        // Display splash screen:
        JWindow splash = null;
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("--splash")) {
                String splashFile = DEFAULT_SPLASH_FILE;
                if (args[i].indexOf('=') > 0) {
                    splashFile = args[i].substring(args[i].indexOf('=') + 1);
                }
                splash = displaySplash(splashFile);
                break;
            }
        }

        createAndSetDirectories();
        Locale.setDefault(getLocale());
        handleArgs(args);
        
        if (javaCheck && !checkJavaVersion()) {
            removeSplash(splash);
            System.err.println("Java version " + MIN_JDK_VERSION +
                            " or better is recommended in order to run FreeCol." +
                            " Use --no-java-check to skip this check.");
            return;
        }

        int  minMemory = 128;  // million bytes
        if (memoryCheck && Runtime.getRuntime().maxMemory() < minMemory * 1000000) {
            removeSplash(splash);
            System.out.println("You need to assign more memory to the JVM. Restart FreeCol with:");
            System.out.println("java -Xmx" + minMemory + "M -jar FreeCol.jar");
            return;
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
                        removeSplash(splash);
                        System.out.println("Could not load savegame.");
                        return;
                    }
                } else {
                    try {
                        freeColServer = new FreeColServer(true, false, serverPort, serverName);
                    } catch (NoRouteToServerException e) {
                        removeSplash(splash);
                        System.out.println(Messages.message("server.noRouteToServer"));
                        System.exit(1);
                        return;
                    }
                }

                Runtime runtime = Runtime.getRuntime();
                runtime.addShutdownHook(new Thread() {
                    public void run() {
                        freeColServer.getController().shutdown();
                    }
                });
            } catch (IOException e) {
                removeSplash(splash);
                System.err.println("Error while loading server: " + e);
                System.exit(-1);
            }
        } else {
            final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            if (windowSize.width == -1 || windowSize.height == -1) {
                // Allow room for frame handles, taskbar etc if using windowed mode:
                windowSize.width = bounds.width - DEFAULT_WINDOW_SPACE;
                windowSize.height = bounds.height - DEFAULT_WINDOW_SPACE;
            }
            final Dimension preloadSize;
            if (windowed) {
                preloadSize = windowSize;
            } else {
                preloadSize = new Dimension(bounds.width, bounds.height);
            }

            try {
                UIManager.setLookAndFeel(new FreeColLookAndFeel(dataFolder, preloadSize));
            } catch (UnsupportedLookAndFeelException e) {
                logger.warning("Could not load the \"FreeCol Look and Feel\"");
            } catch (FreeColException e) {
                removeSplash(splash);
                e.printStackTrace();
                System.out.println("\nThe data files could not be found by FreeCol. Please make sure");
                System.out.println("they are present. If FreeCol is looking in the wrong directory");
                System.out.println("then run the game with a command-line parameter:");
                System.out.println("");
                printUsage();
                return;
            }

            // This needs to be initialized before ImageLibrary
            getSpecification();

            // TODO: don't use same datafolder for both images and music because the images are best kept inside the .JAR file.

            logger.info("Now starting to load images.");

            ImageLibrary lib;
            try {
                lib = new ImageLibrary(dataFolder);
            } catch (FreeColException e) {
                removeSplash(splash);
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
        
        removeSplash(splash);
    }

    /**
     * Displays a splash screen.
     * @return The splash screen. It should be removed by the caller
     *      when no longer needed by a call to removeSplash().
     */
    private static JWindow displaySplash(String filename) {
        try {
            Image im = Toolkit.getDefaultToolkit().getImage(filename);
            JWindow f = new JWindow();
            f.getContentPane().add(new JLabel(new ImageIcon(im)));
            f.pack();
            Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            f.setLocation(center.x - f.getWidth() / 2, center.y - f.getHeight() / 2);
            f.setVisible(true);
            return f;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while displaying splash screen", e);
            return null;
        }
    }

    /**
     * Removes splash screen.
     */
    private static void removeSplash(JWindow splash) {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
        }
    }
    
    /**
     * Initialize loggers.
     */
    private static void initLogging() {
        final Logger baseLogger = Logger.getLogger("");
        final Handler[] handlers = baseLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            baseLogger.removeHandler(handlers[i]);
        }
        try {
            baseLogger.addHandler(new DefaultHandler(consoleLogging));
            if (inDebugMode) {
                logLevel = Level.FINEST;
            } 
            Logger freecolLogger = Logger.getLogger("net.sf.freecol");
            freecolLogger.setLevel(logLevel);
        } catch (FreeColException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Determines the <code>Locale</code> to be used.
     * @return Currently this method returns the locale set by
     *      the ClientOptions (read directly from "options.xml").
     *      This behavior will probably be changed.
     */
    public static Locale getLocale() { 
        XMLInputFactory xif = XMLInputFactory.newInstance();

        XMLStreamReader in = null;
        try {
            in = xif.createXMLStreamReader(new BufferedReader(new FileReader(getClientOptionsFile())));
            in.nextTag();
            while (!ClientOptions.LANGUAGE.equals(in.getLocalName())) {
                in.nextTag();
            }
            Locale l = LanguageOption.getLocale(in.getAttributeValue(null, "value"));
            return l;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while loading options.", e);
            return Locale.getDefault();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception while closing stream.", e);
                return Locale.getDefault();
            }
        }
    }

    /**
     * Returns the file containing the client options.
     * @return The file.
     */
    public static File getClientOptionsFile() {
        return clientOptionsFile;
    }
    
    /**
     * Returns the specification object for Freecol. 
     * 
     * @return the specification to be used by all other classes.
     */
    public static Specification getSpecification() {
        return Specification.getSpecification();
    }

    /**
     * Gets the <code>FreeColClient</code>.
     * @return The <code>FreeColClient</code>, or <code>null</code>
     *      if the game is run as a standalone server. 
     */
    public static FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Creates a freecol dir for the current user.
     * 
     * The directory is created within the current user's
     * home directory. This directory will be called "freecol"
     * and underneath that directory a "save" directory will
     * be created.
     */
    private static void createAndSetDirectories() {
        // TODO: The location of the save directory should be determined by the installer.;
        
        File mainUserDirectory = new File(System.getProperty("user.home"), "freecol");
        if (mainUserDirectory.exists() && mainUserDirectory.isFile()) {
            logger.warning("Could not create .freecol under "
                    + System.getProperty("user.home") + " because there "
                    + "already exists a regular file with the same name.");
            return;
        } else if (!mainUserDirectory.exists()) {
            mainUserDirectory.mkdir();
        }
        clientOptionsFile = new File(mainUserDirectory, "options.xml");
        saveDirectory = new File(mainUserDirectory, "save");
        if (saveDirectory.exists() && saveDirectory.isFile()) {
            logger.warning("Could not create freecol/save under "
                    + System.getProperty("user.home") + " because there "
                    + "already exists a regular file with the same name.");
            return;
        } else if (!saveDirectory.exists()) {
            saveDirectory.mkdir();
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
     * Returns the data directory.
     * @return The directory where the data files are located.
     */
    public static File getDataDirectory() {
        if (dataFolder.equals("")) {
            return new File("data");
        } else {        
            return new File(dataFolder);
        }
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
            } else if (args[i].equals("--log-console")) {
                consoleLogging = true;
                initLogging();
            } else if (args[i].equals("--log-level")) {
                i++;
                if (i < args.length) {
                    String logLevelString = args[i].toUpperCase();
                    try {
                        logLevel = Level.parse(logLevelString);
                        initLogging();
                    } catch (IllegalArgumentException e) {
                        printUsage();
                        System.exit(1);
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
                        windowSize = new Dimension(x, y);
                    } catch (Exception e) {
                        printUsage();
                        System.exit(0);
                    }
                } else if (args[i].length() != 10) {
                    printUsage();
                    System.exit(0);
                }
                
                windowed = true;
            } else if (args[i].length() > 16 && args[i].substring(0, 16).equals("--default-locale")) {
                if (args[i].charAt(16) == '=') {
                    // slightly ugly: strip encoding from LC_MESSAGES
                    String languageID = args[i].substring(17);
                    int index = languageID.indexOf('.');
                    if (index > 0) {
                        languageID = languageID.substring(0, index);
                    }
                    Locale.setDefault(LanguageOption.getLocale(languageID));
                } else {
                    printUsage();
                    System.exit(0);
                }
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
            } else if (args[i].equals("--specification")) {
                i++;
                if (i < args.length) {
                    File specFile = new File(args[i]);
                    if (!specFile.exists() || !specFile.isFile()) {                        
                        specFile = new File(getSaveDirectory(), args[i]);
                        if (!specFile.exists() || !specFile.isFile()) {
                            System.out.println("The given specification file could not be found: " + args[i]);
                            System.exit(1);
                        }
                    }
                    Specification.setSpecificationFile(specFile);
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
            } else if (args[i].startsWith("--splash")) {
                // Ignore - already handled;
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
     * Gets the current revision of game.
     *
     * @return The current version and SVN Revision of the game.
     */
    public static String getRevision() {
        return FREECOL_REVISION;
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
        System.out.println("--specification SPECIFICATION_FILE");
        System.out.println("  loads the given specification file.");
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
        System.out.println("--specification SPECIFICATION_FILE");
        System.out.println("  loads the given specification file.");
        System.out.println("--default-locale=LANGUAGE[_COUNTRY[_VARIANT]]");
        System.out.println("  sets the default locale.");
        System.out.println("--splash[=SPLASH_IMAGE_FILE]");
        System.out.println("  displays a splash screen while loading the game");
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
