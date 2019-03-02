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

package net.sf.freecol.common.util;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to deal specifically with the operating system
 */
public class OSUtils {

    private static final Logger logger = Logger.getLogger(OSUtils.class.getName());

    private static final String SEPARATOR
        = System.getProperty("file.separator");


    /**
     * What OS are we using?
     *
     * @return The operating system name.
     */
    public static String getOperatingSystem() {
        return System.getProperty("os.name");
    }

    /**
     * Does the OS look like Mac OS X?
     *
     * @return True if Mac OS X appears to be present.
     */
    public static boolean onMacOSX() {
        return "Mac OS X".equals(getOperatingSystem());
    }

    /**
     * Does the OS look like some sort of unix?
     *
     * @return True we hope.
     */
    public static boolean onUnix() {
        return "/".equals(SEPARATOR);
    }

    /**
     * Does the OS look like some sort of Windows?
     *
     * @return True if Windows appears to be present.
     */
    public static boolean onWindows() {
        return getOperatingSystem().startsWith("Windows");
    }

    /**
     * Launches the web browser based on a given operating system
     *
     * @param url The URL to launch
     */
    final public static void launchBrowser(String url) {
        // Use Desktop Class first
        try {
            URI uri = java.net.URI.create(url);
            java.awt.Desktop.getDesktop().browse(uri);
        } catch (IOException e) {

            // If Desktop Class fails to launch browser, log the error
            logger.log(Level.FINEST, "Web browser failed to launch via Desktop Class.", e);

            // Use Runtime to launch browser
            String[] browser = getBrowserExecString(url);
            try {
                Runtime.getRuntime().exec(browser);
            } catch (IOException re) {
                logger.log(Level.FINEST, "Web browser failed to launch via Runtime Class.", re);
            }
        }
    }

    /**
     * What should we run to launch a browser?
     *
     * @param url The URL to launch
     * @return An OS-dependent array of commands to exec a browser.
     *
     * @see #launchBrowser(String)
     */
    final private static String[] getBrowserExecString(String url) {
        if (onMacOSX()) {
            // Apple Macintosh, Safari is the main browser
            return new String[] { "open" , "-a", "Safari", url };
        } else if (onUnix()) {
            // GNU Linux, use xdg-utils to launch the default
            // browser (portland.freedesktop.org)
            return new String[] { "xdg-open", url };
        } else if (onWindows()) {
            // Microsoft Windows, use the default browser
            return new String[] { "rundll32.exe",
                    "url.dll,FileProtocolHandler", url };
        } else {
            return new String[] { "firefox", url };
        }
    }
}
