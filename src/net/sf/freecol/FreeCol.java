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

package net.sf.freecol;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.filechooser.FileSystemView;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.logging.DefaultHandler;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.util.XMLStream;
import net.sf.freecol.server.FreeColServer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * This class is responsible for handling the command-line arguments
 * and starting either the stand-alone server or the client-GUI.
 *
 * @see net.sf.freecol.client.FreeColClient FreeColClient
 * @see net.sf.freecol.server.FreeColServer FreeColServer
 */
public final class FreeCol {

    private static final Logger logger = Logger.getLogger(FreeCol.class.getName());

    public static final String  META_SERVER_ADDRESS = "meta.freecol.org";
    public static final int     META_SERVER_PORT = 3540;
    public static final int     DEFAULT_PORT = 3541;
    public static final int     DEFAULT_TIMEOUT = 60; // 1 minute
    public static final int     TIMEOUT_MIN = 10; // 10s

    public static final String CLIENT_THREAD = "FreeColClient:";
    public static final String SERVER_THREAD = "FreeColServer:";
    public static final String METASERVER_THREAD = "FreeColMetaServer:";
    public static final String DEFAULT_TC = "freecol";

    private static final String FREECOL_VERSION = "0.10.0-trunk";
    private static String FREECOL_REVISION;

    private static final String MIN_JDK_VERSION = "1.5";

    private static final String DEFAULT_SPLASH_FILE = "splash.jpg";

    private static boolean  sound = true,
                            javaCheck = true,
                            memoryCheck = true,
                            consoleLogging = false,
                            introVideo = true;
    private static String dataFolder = "data";
    private static String logFile = null;

    private static FreeColClient freeColClient;

    private static boolean standAloneServer = false;
    private static boolean publicServer = true;

    public static final int DEBUG_OFF = 0;
    public static final int DEBUG_LIMITED = 1;
    public static final int DEBUG_FULL = 2;
    public static final int DEBUG_FULL_COMMS = 3;
    private static int debugLevel = DEBUG_OFF;
    private static int debugRunTurns = -1;
    private static String debugRunSave = null;

    private static String fontName = null;

    private static int serverPort = DEFAULT_PORT;
    private static String serverName = null;

    private static File mainUserDirectory = null;

    private static File saveDirectory;
    /** Directory containing automatically created save games.
     *  At program start, the path of this directory is based on the path
     *  where to store regular save games. If the value of saveGame is
     *  changed by the user during the game, then the value of
     *  autoSaveDirectory will not be effected.
     */
    private static File autoSaveDirectory;

    private static File tcUserDirectory;

    private static File userModsDirectory;

    private static String tc = DEFAULT_TC;

    private static File savegameFile = null;

    private static File clientOptionsFile = null;

    private static Level logLevel = Level.INFO;

    private static boolean checkIntegrity = false;

    private static final Options options = new Options();

    private static String splashFilename = DEFAULT_SPLASH_FILE;
    private static Dimension windowSize;

    private static long freeColSeed = 0L;

    private static int freeColTimeout = -1;


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

        // parse command line arguments
        handleArgs(args);

        createAndSetDirectories();
        initLogging();

        if (freeColSeed == 0L) freeColSeed = new SecureRandom().nextLong();
        logger.info("Using seed: " + freeColSeed);

        Locale locale = getLocale();
        Locale.setDefault(locale);
        Messages.setMessageBundle(locale);

        if (javaCheck && !checkJavaVersion()) {
            System.err.println("Java version " + MIN_JDK_VERSION +
                               " or better is recommended in order to run FreeCol." +
                               " Use --no-java-check to skip this check.");
            System.exit(1);
        }

        int  minMemory = 128;  // million bytes
        if (memoryCheck && Runtime.getRuntime().maxMemory() < minMemory * 1000000) {
            System.out.println("You need to assign more memory to the JVM. Restart FreeCol with:");
            System.out.println("java -Xmx" + minMemory + "M -jar FreeCol.jar");
            System.exit(1);
        }

        if (standAloneServer) {
            startServer();
        } else {
            freeColClient = new FreeColClient(savegameFile, windowSize, sound, splashFilename, introVideo, fontName);
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
        if (logFile == null) {
            logFile = mainUserDirectory.getPath() + File.separator
                + "FreeCol.log";
        }
        try {
            baseLogger.addHandler(new DefaultHandler(consoleLogging, logFile));
            Logger freecolLogger = Logger.getLogger("net.sf.freecol");
            freecolLogger.setLevel(logLevel);
        } catch (FreeColException e) {
            e.printStackTrace();
        }
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable e) {
                    baseLogger.log(Level.WARNING, "Uncaught exception from thread: " + thread, e);
                }
            });
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
        File options = getClientOptionsFile();
        if (options.canRead()) {
            try {
                in = xif.createXMLStreamReader(new FileInputStream(options), "UTF-8");
                in.nextTag();
                /**
                 * The following code was contributed by armcode to fix
                 * bug #[ 2045521 ] "Exception in Freecol.log on starting
                 * game". I was never able to reproduce the bug, but the
                 * patch did no harm either.
                 */
                for(int eventid = in.getEventType();eventid != XMLEvent.END_DOCUMENT; eventid = in.getEventType()) {

                    //TODO: Is checking for XMLEvent.ATTRIBUTE needed?
                    if (eventid == XMLEvent.START_ELEMENT) {
                        if (ClientOptions.LANGUAGE.equals(in.getAttributeValue(null, "id"))) {
                            return LanguageOption.getLocale(in.getAttributeValue(null, "value"));
                        }
                    }
                    in.nextTag();
                }
                //We don't have a language option in our file, it is either not there or the file is corrupt
                logger.log(Level.WARNING, "Language setting not found in client options file.  Using default.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception while loading options.", e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while closing stream.", e);
                }
            }
        }
        return Locale.getDefault();
    }

    /**
     * Returns the default server network port.
     * @return The port number.
     */
    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }

    /**
     * Returns the file containing the client options.
     * @return The file.
     */
    public static File getClientOptionsFile() {
        return clientOptionsFile;
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
     * Try to make a directory.
     *
     * @param file A <code>File</code> specifying where to make the directory.
     * @return True if the directory is there after the call.
     */
    private static boolean insistDirectory(File file) {
        if (file.exists()) {
            if (file.isDirectory()) return true;
            System.out.println("Could not create directory " + file.getName()
                + " under " + file.getParentFile().getName()
                + " because a non-directory with that name is already there.");
            return false;
        }
        return file.mkdir();
    }

    /**
     * Creates a freecol dir for the current user.
     *
     * The directory is created within the current user's
     * home directory. This directory will be called "freecol"
     * and underneath that directory a "save" directory will
     * be created.
     *
     * For MacOS X the Library/FreeCol is used
     * (which is the standard path for application related files).
     *
     * For os.name beginning with "Windows" JFileChooser() is used to
     * find the path to "My Documents" (or localized equivalent)
     */
    private static void createAndSetDirectories() {
        // TODO: The location of the save directory should be determined by the installer.;

        String freeColDirectoryName = "/".equals(System.getProperty("file.separator")) ?
                ".freecol" : "freecol";

        File userHome = FileSystemView.getFileSystemView().getDefaultDirectory();

        // Checks for OS specific paths, however if the old {home}/.freecol exists
        // that overrides OS-specifics for backwards compatibility.
        // TODO: remove compatibility code
        if (System.getProperty("os.name").equals("Mac OS X")) {
            // We are running on a Mac and should use {home}/Library/FreeCol
            if (!new File(userHome, freeColDirectoryName).isDirectory()) {
                userHome = new File(userHome, "Library");
                freeColDirectoryName = "FreeCol";
            }
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            // We are running on Windows and should use "My Documents" (or localized equivalent)
            if (!new File(userHome, freeColDirectoryName).isDirectory()) {
                freeColDirectoryName = "FreeCol";
            }
        }

        if (mainUserDirectory == null) {
            mainUserDirectory = new File(userHome, freeColDirectoryName);
        }
        if (!insistDirectory(mainUserDirectory)) return;
        if (saveDirectory == null) {
            saveDirectory = new File(mainUserDirectory, "save");
        }
        if (!insistDirectory(saveDirectory)) saveDirectory = null;

        autoSaveDirectory = new File(saveDirectory, "autosave");
        if (!insistDirectory(autoSaveDirectory)) autoSaveDirectory = null;

        tcUserDirectory = new File(mainUserDirectory, tc);
        if (!insistDirectory(tcUserDirectory)) tcUserDirectory = null;

        userModsDirectory = new File(mainUserDirectory, "mods");
        if (!insistDirectory(userModsDirectory)) userModsDirectory = null;

        clientOptionsFile = (tcUserDirectory == null) ? null
            : new File(tcUserDirectory, "options.xml");
    }

    /**
     * Set up the save file and directory
     * @param name the name of the save file to use
     */
    private static void setSavegame(String name) {
        if(name == null){
            System.out.println("No savegame given with --load-savegame parameter");
            System.exit(1);
        }

        savegameFile = new File(name);
        if (!savegameFile.exists() || !savegameFile.isFile()) {
            savegameFile = new File(getSaveDirectory(), name);
            if (!savegameFile.exists() || !savegameFile.isFile()) {
                System.out.println("Could not find savegame file: " + name);
                System.exit(1);
            }
        } else {
            setSaveDirectory(savegameFile.getParentFile());
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
     * Set the directory where the savegames should be put.
     * @param saveDirectory a <code>File</code> value for the savegame directory
     */
    public static void setSaveDirectory(File saveDirectory) {
        FreeCol.saveDirectory = saveDirectory;
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
     * Gets the mods directory.
     *
     * @return The directory where the standard mods are located.
     */
    public static File getStandardModsDirectory() {
        return new File(getDataDirectory(), "mods");
    }

    /**
     * Gets the user mods directory.
     *
     * @return The directory where user mods are located, or null if none.
     */
    public static File getUserModsDirectory() {
        return userModsDirectory;
    }

    /**
     * Returns the directory where the autogenerated savegames
     * should be put.
     *
     * @return The directory.
     */
    public static File getAutosaveDirectory() {
        return autoSaveDirectory;
    }

    /**
     * Returns the directory for saving options.
     *
     * @return The directory.
     */
    public static File getOptionsDirectory() {
        return tcUserDirectory;
    }

    public static InputStream getSpecificationInputStream() throws IOException {
        final FreeColTcFile tcData = new FreeColTcFile(tc);
        return tcData.getSpecificationInputStream();
    }

    /**
     * Ensure that the Java version is good enough. JDK 1.4 or better is
     * required.
     *
     * @return true if Java version is at least 1.5.0.
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
    @SuppressWarnings("static-access")
    private static void handleArgs(String[] args) {
        // create the command line parser
        CommandLineParser parser = new PosixParser();

        /**
         * Ugly hack: try to determine language first, so that usage,
         * etc. will be localized.
         */
        String localeArg = null;
        String locationArg = null;
        for (int index = 0; index < args.length - 1; index++) {
            if ("--default-locale".equals(args[index])) {
                localeArg = args[++index];
            } else if ("--freecol-data".equals(args[index])) {
                locationArg = args[++index];
            }
        }
        if (locationArg != null) {
            dataFolder = locationArg;
        }
        if (localeArg == null) {
            Messages.setMessageBundle(Locale.getDefault());
        } else {
            Locale locale = LanguageOption.getLocale(localeArg);
            Locale.setDefault(locale);
            Messages.setMessageBundle(locale);
        }

        // create the Options
        options.addOption(OptionBuilder.withLongOpt("freecol-data")
                          .withDescription(Messages.message("cli.freecol-data"))
                          .withArgName(Messages.message("cli.arg.directory"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("tc")
                          .withDescription(Messages.message("cli.tc"))
                          .withArgName(Messages.message("cli.arg.name"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("home-directory")
                          .withDescription(Messages.message("cli.home-directory"))
                          .withArgName(Messages.message("cli.arg.directory"))
                          .withType(new File("dummy"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("log-console")
                          .withDescription(Messages.message("cli.log-console"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("log-file")
                          .withDescription(Messages.message("cli.log-file"))
                          .withArgName(Messages.message("cli.arg.name"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("log-level")
                          .withDescription(Messages.message("cli.log-level"))
                          .withArgName(Messages.message("cli.arg.loglevel"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("no-java-check")
                          .withDescription(Messages.message("cli.no-java-check"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("windowed")
                          .withDescription(Messages.message("cli.windowed"))
                          .withArgName(Messages.message("cli.arg.dimensions"))
                          .hasOptionalArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("default-locale")
                          .withDescription(Messages.message("cli.default-locale"))
                          .withArgName(Messages.message("cli.arg.locale"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("no-memory-check")
                          .withDescription(Messages.message("cli.no-memory-check"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("no-intro")
                          .withDescription(Messages.message("cli.no-intro"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("no-sound")
                          .withDescription(Messages.message("cli.no-sound"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("usage")
                          .withDescription(Messages.message("cli.help"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("help")
                          .withDescription(Messages.message("cli.help"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("version")
                          .withDescription(Messages.message("cli.version"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("debug")
                          .withDescription(Messages.message("cli.debug"))
                          .withArgName(Messages.message("cli.arg.debuglevel"))
                          .hasOptionalArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("debug-run")
                          .withDescription(Messages.message("cli.debug-run"))
                          .withArgName(Messages.message("cli.arg.debugRun"))
                          .hasOptionalArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("private")
                          .withDescription(Messages.message("cli.private"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("server")
                          .withDescription(Messages.message("cli.server"))
                          .withArgName(Messages.message("cli.arg.port"))
                          .hasOptionalArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("load-savegame")
                          .withDescription(Messages.message("cli.load-savegame"))
                          .withArgName(Messages.message("cli.arg.file"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("server-name")
                          .withDescription(Messages.message("cli.server-name"))
                          .withArgName(Messages.message("cli.arg.name"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("splash")
                          .withDescription(Messages.message("cli.splash"))
                          .withArgName(Messages.message("cli.arg.file"))
                          .hasOptionalArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("check-savegame")
                          .withDescription(Messages.message("cli.check-savegame"))
                          .create());
        options.addOption(OptionBuilder.withLongOpt("font")
                          .withDescription(Messages.message("cli.font"))
                          .withArgName(Messages.message("cli.arg.font"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("seed")
                          .withDescription(Messages.message("cli.seed"))
                          .withArgName(Messages.message("cli.arg.seed"))
                          .hasArg()
                          .create());
        options.addOption(OptionBuilder.withLongOpt("timeout")
                          .withDescription(Messages.message("cli.timeout"))
                          .withArgName(Messages.message("cli.arg.timeout"))
                          .hasArg()
                          .create());

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("default-locale")) {
                // slightly ugly: strip encoding from LC_MESSAGES
                String languageID = line.getOptionValue("default-locale");
                int index = languageID.indexOf('.');
                if (index > 0) {
                    languageID = languageID.substring(0, index);
                }
                Locale newLocale = LanguageOption.getLocale(languageID);
                Locale.setDefault(newLocale);
                Messages.setMessageBundle(newLocale);
            }
            if (line.hasOption("splash")) {
                final String str = line.getOptionValue("splash");
                if (str != null) {
                    splashFilename = str;
                }
            }
            if (line.hasOption("freecol-data")) {
                dataFolder = line.getOptionValue("freecol-data");
            }
            if (line.hasOption("tc")) {
                tc = line.getOptionValue("tc");
            }
            if (line.hasOption("home-directory")) {
                String arg = line.getOptionValue("home-directory");
                mainUserDirectory = new File(arg);
                String errMsg = null;
                if(!mainUserDirectory.exists()){
                    errMsg = "cli.error.home.notExists";
                }
                if(!mainUserDirectory.canRead()){
                    errMsg = "cli.error.home.noRead";
                }
                if(!mainUserDirectory.canWrite()){
                    errMsg = "cli.error.home.noWrite";
                }
                if(errMsg != null){
                    System.out.println(Messages.message(StringTemplate.template(errMsg)
                                                        .addName("%string%", arg)));
                    System.exit(1);
                }
            }
            if (line.hasOption("log-console")) {
                consoleLogging = true;
            }
            if (line.hasOption("log-file")) {
                logFile = line.getOptionValue("log-file");
            }
            if (line.hasOption("log-level")) {
                String logLevelString = line.getOptionValue("log-level").toUpperCase();
                try {
                    logLevel = Level.parse(logLevelString);
                } catch (IllegalArgumentException e) {
                    printUsage();
                    System.exit(1);
                }
            }
            if (line.hasOption("no-java-check")) {
                javaCheck = false;
            }
            if (line.hasOption("windowed")) {
                String dimensions = line.getOptionValue("windowed");
                if (dimensions == null) {
                    windowSize = new Dimension(-1, -1);
                } else {
                    String[] xy = dimensions.split("[^0-9]");
                    if (xy.length == 2) {
                        windowSize = new Dimension(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    } else {
                        printUsage();
                        System.exit(1);
                    }
                }
            }
            if (line.hasOption("no-sound")) {
                sound = false;
            }
            if (line.hasOption("no-intro")) {
                introVideo = false;
            }
            if (line.hasOption("no-memory-check")) {
                memoryCheck = false;
            }
            if (line.hasOption("help") || line.hasOption("usage")) {
                printUsage();
                System.exit(0);
            }
            if (line.hasOption("version")) {
                System.out.println("FreeCol " + getVersion());
                System.exit(0);
            }
            if (line.hasOption("debug")) {
                // If the optional argument is supplied use limited mode.
                try {
                    debugLevel = Integer.parseInt(line.getOptionValue("debug"));
                    debugLevel = Math.min(Math.max(debugLevel, DEBUG_OFF),
                                          DEBUG_FULL_COMMS);
                } catch (NumberFormatException e) {
                    debugLevel = DEBUG_FULL;
                }
                // user set log level has precedence
                if (!line.hasOption("log-level")) {
                    logLevel = Level.FINEST;
                }
                if (line.hasOption("debug-run")) {
                    String opt = line.getOptionValue("debug-run");
                    int comma = opt.indexOf(",");
                    String turns = opt.substring(0, (comma < 0) ? opt.length()
                                                 : comma);
                    try {
                        debugRunTurns = Integer.parseInt(turns);
                    } catch (NumberFormatException e) {
                        debugRunTurns = -1;
                    }
                    if (comma > 0) debugRunSave = opt.substring(comma + 1);
                }
            }
            if (line.hasOption("server")) {
                standAloneServer = true;
                String arg = line.getOptionValue("server");
                if (arg != null) {
                    try {
                        serverPort = Integer.parseInt(arg);
                    } catch (NumberFormatException nfe) {
                        System.out.println(Messages.message(StringTemplate.template("cli.error.port")
                                                            .addName("%string%", arg)));
                        System.exit(1);
                    }
                }
            }
            if (line.hasOption("private")) {
                publicServer = false;
            }
            if (line.hasOption("check-savegame")) {
                setSavegame(line.getOptionValue("load-savegame"));
                checkIntegrity = true;
                standAloneServer = true;
                serverPort = DEFAULT_PORT;
            }
            if (line.hasOption("load-savegame")) {
                setSavegame(line.getOptionValue("load-savegame"));
            }
            if (line.hasOption("server-name")) {
                serverName = line.getOptionValue("server-name");
            }
            if (line.hasOption("font")) {
                fontName = line.getOptionValue("font");
            }
            if (line.hasOption("seed")) {
                String seedStr = line.getOptionValue("seed");
                try {
                    freeColSeed = Long.parseLong(seedStr);
                } catch (NumberFormatException e) {
                    System.err.println("Ignoring bad seed: " + seedStr);
                }
            }
            if (line.hasOption("timeout")) {
                String timeoutStr = line.getOptionValue("timeout");
                int result;
                try {
                    result = Integer.parseInt(timeoutStr);
                } catch (NumberFormatException nfe) {
                    result = -1;
                }
                if (result < TIMEOUT_MIN) {
                    System.err.println("Ignoring bad timeout: " + timeoutStr);
                } else {
                    freeColTimeout = result;
                }
            }
        } catch (ParseException e) {
            System.err.println("\n" + e.getMessage() + "\n");
            printUsage();
            System.exit(1);
        }

    }


    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -Xmx 128M -jar freecol.jar [OPTIONS]", options);
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
     * Returns the name of the log file.
     *
     * @return a <code>String</code> value
     */
    public static String getLogFile() {
        return logFile;
    }

    /**
     * Checks if the program is in "Debug mode".
     * @return <code>true</code> if the program is in debug
     *       mode and <code>false</code> otherwise.
     */
    public static boolean isInDebugMode() {
        return debugLevel > DEBUG_OFF;
    }

    /**
     * Gets the debug level.
     *
     * @return The debug level.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * Sets the "debug mode" to be active or not.
     * @param debug Should be <code>true</code> in order
     *       to active debug mode and <code>false</code>
     *       otherwise.
     */
    public static void setInDebugMode(boolean debug) {
        debugLevel = (debug) ? DEBUG_FULL : DEBUG_OFF;
    }

    /**
     * Gets the turns to run in debug mode.
     *
     * @return The turns to run in debug mode.
     */
    public static int getDebugRunTurns() {
        return debugRunTurns;
    }

    /**
     * Complete debug run.
     */
    public static void completeDebugRun() {
        debugRunTurns = 0;
    }

    /**
     * Is a debug run complete?
     *
     * @return True if a debug run is complete.
     */
    public static boolean isDebugRunComplete() {
        return debugRunTurns == 0;
    }

    /**
     * Gets the name of a file to save to at the end of a debug run.
     *
     * @return The name of a file to save to at the end of a debug run.
     */
    public static String getDebugRunSaveName() {
        return debugRunSave;
    }

    /**
     * Gets the seed for the PRNG.
     *
     * @return The seed.
     */
    public static long getFreeColSeed() {
        return freeColSeed;
    }

    /**
     * Increments the seed for the PRNG.
     */
    public static void incrementFreeColSeed() {
        freeColSeed++;
    }

    /**
     * Gets the timeout.
     * Use the command line specified one if any, otherwise default
     * to `infinite' in single player and the DEFAULT_TIMEOUT for
     * multiplayer.
     *
     * @param singleplayer True if this is a single player game.
     * @return A suitable timeout value.
     */
    public static int getFreeColTimeout(boolean single) {
        return (freeColTimeout >= TIMEOUT_MIN) ? freeColTimeout
            : (single) ? Integer.MAX_VALUE
            : DEFAULT_TIMEOUT;
    }

    private static void startServer() {
        logger.info("Starting stand-alone server.");
        try {
            final FreeColServer freeColServer;
            if (savegameFile != null) {
                XMLStream xs = null;
                try {
                    // Get suggestions for "singleplayer" and "public game" settings from the file:
                    final FreeColSavegameFile fis = new FreeColSavegameFile(savegameFile);
                    xs = FreeColServer.createXMLStreamReader(fis);
                    final XMLStreamReader in = xs.getXMLStreamReader();
                    in.nextTag();
                    xs.close();

                    freeColServer = new FreeColServer(fis, serverPort, serverName);
                    if (checkIntegrity) {
                        String integrityCheckMsg = "";
                        boolean integrityOK = freeColServer.getIntegrity();
                        if(integrityOK){
                            integrityCheckMsg = Messages.message("cli.check-savegame.success");
                        }
                        else{
                            integrityCheckMsg = Messages.message("cli.check-savegame.failure");
                        }
                        System.out.println(integrityCheckMsg);
                        System.exit(integrityOK ? 0 : 1);
                    }
                } catch (Exception e) {
                    if (checkIntegrity) {
                        System.out.println(Messages.message("cli.check-savegame.failure"));
                    }
                    System.out.println("Could not load savegame.");
                    System.exit(1);
                    return;
                } finally {
                    xs.close();
                }
            } else {
                try {
                    FreeColTcFile tcData = new FreeColTcFile(tc);
                    Specification specification = tcData.getSpecification();
                    freeColServer = new FreeColServer(specification, publicServer, false, serverPort, serverName);
                } catch (NoRouteToServerException e) {
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
            System.err.println("Error while loading server: " + e);
            System.exit(1);
        }
    }

}
