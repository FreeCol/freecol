/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to deal specifically with the operating system
 */
public class OSUtils {

    private static final Logger logger = Logger.getLogger(OSUtils.class.getName());

    /**
     * Launches the web browser based on a given operating system
     *
     * @param os
     * @throws IOException
     */
    public static void LaunchBrowser(String os, String url) throws IOException {
       String[] browser = GetBrowser(os, url);
        if (browser != null) {
            try {
                final Process exec = Runtime.getRuntime().exec(browser);
            } catch (IOException e) {
                logger.log(Level.FINEST, "Web browswer failed to launch.", e);
            }
        }
    }


    /**
     * Returns the browswer for a given operating system
     *
     * @param os
     * @return
     */
    private static String[] GetBrowser(String os, String url) {
        String[] cmd = null;
        if (os == null) {
            // error, the operating system could not be determined
            return null;
        } else if (os.toLowerCase().contains("mac")) {
            // Apple Macintosh, Safari is the main browser
            return new String[] { "open" , "-a", "Safari", url };
        } else if (os.toLowerCase().contains("windows")) {
            // Microsoft Windows, use the default browser
            return new String[] { "rundll32.exe",
                    "url.dll,FileProtocolHandler", url};
        } else if (os.toLowerCase().contains("linux")) {
            // GNU Linux, use xdg-utils to launch the default
            // browser (portland.freedesktop.org)
            return new String[] { "xdg-open", url};
        } else {
            return new String[]{"firefox", url};
        }
    }
}
