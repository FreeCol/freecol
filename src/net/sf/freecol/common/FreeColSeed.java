/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.common;

import java.security.SecureRandom;
import java.util.logging.Logger;


/**
 * A wrapper for the pseudo-random number generator seed.
 */
public class FreeColSeed {

    private static final Logger logger = Logger.getLogger(FreeColSeed.class.getName());

    public static final long DEFAULT_SEED = 0L;

    private static long freeColSeed = DEFAULT_SEED;
    

    /**
     * Gets the seed for the PRNG.
     *
     * @param generate Force generation of a new seed.
     * @return The seed.
     */
    public static long getFreeColSeed(boolean generate) {
        if (generate) {
            freeColSeed = new SecureRandom().nextLong();
            logger.info("Using seed: " + freeColSeed);
        }
        return freeColSeed;
    }

    /**
     * Sets the seed for the PRNG.
     *
     * @param arg A string defining the seed.
     */
    public static void setFreeColSeed(String arg) {
        try {
            FreeColSeed.freeColSeed = Long.parseLong(arg);
        } catch (NumberFormatException nfe) {}
    }

    /**
     * Increments the seed for the PRNG.
     */
    public static void incrementFreeColSeed() {
        freeColSeed = getFreeColSeed(false) + 1;
        logger.info("Reseeded with: " + freeColSeed);
    }
}
