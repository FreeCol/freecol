/*
 *  FreeCol.java - This class is responsible for handling the command-line arguments
 *                 and starting either the stand-alone server or the client-GUI.
 *
 *  Copyright (C) 2002-2004  The FreeCol Team
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


package net.sf.freecol;

import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.logging.DefaultHandler;

import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;

import net.sf.freecol.server.FreeColServer;
import java.io.IOException;



/**
* This class is responsible for handling the command-line arguments
* and starting either the stand-alone server or the client-GUI.
*
* @see net.sf.freecol.client.FreeColClient FreeColClient
* @see net.sf.freecol.server.FreeColServer FreeColServer
*/
public final class FreeCol {
    private static final Logger logger = Logger.getLogger(FreeCol.class.getName());

    private  static final String FREECOL_VERSION = "0.2.1";

    private static final String MIN_JDK_VERSION = "1.4";
    private static final String  FILE_SEP = System.getProperty("file.separator");

    private static boolean  windowed = false,
                            sound = true;
    private static String   dataFolder = "";
    private static FreeColClient freeColClient;

    private static boolean standAloneServer = false;
    private static boolean inDebugMode = false;

    private static int serverPort;



    private FreeCol() {}


    /**
     * The entrypoint.
     *
     * @param args The command-line arguments.
     */
    public static void main(String args[]) {
        if (!checkJavaVersion()) {
            System.err.println("Java version " + MIN_JDK_VERSION +
                               " or better is required in order to run FreeCol.");
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


        handleArgs(args);

        if (standAloneServer) {
            try {
                final FreeColServer freeColServer = new FreeColServer(false, serverPort);

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

            freeColClient = new FreeColClient(windowed, lib, musicLibrary, sfxLibrary);
        }
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
    private static void handleArgs(String args[]) {
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
            } else if (args[i].equals("--windowed")) {
                windowed = true;
            } else if (args[i].equals("--no-sound")) {
                sound = false;
            } else if (args[i].equals("--usage")) {
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

                try {
                    serverPort = Integer.parseInt(args[i]);
                } catch (NumberFormatException nfe) {
                    printUsage();
                    System.exit(-1);
                }
            } else {
                printUsage();
                System.exit(0);
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
    */
    public static boolean isInDebugMode() {
        return inDebugMode;
    }

    
    /**
    * Sets the "debug mode" to be active or not.
    */
    public static void setInDebugMode(boolean debug) {
        inDebugMode = debug;
    }


    /**
    * Prints the command-line usage (the 'help' for command-line
    * arguments).
    */
    private static void printUsage() {
        System.out.println("Usage: java -jar FreeCol.jar [OPTIONS]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("--freecol-data [DIR]");
        System.out.println("  [DIR] should be the directory with FreeCol's data files, it");
        System.out.println("  has a subdirectory called 'images'");
        System.out.println("--windowed");
        System.out.println("  runs FreeCol in windowed mode instead of full screen mode");
        System.out.println("--no-sound");
        System.out.println("  runs FreeCol without sound");
        System.out.println("--usage");
        System.out.println("  displays this help screen");
        System.out.println("--version");
        System.out.println("  displays the version number");
        System.out.println("--server PORT");
        System.out.println("  starts a stand-alone server on the specifed port");
    }
}
