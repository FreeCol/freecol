/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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


/**
 * A wrapper for the pseudo-random number generator seed.
 */
public class FreeColSeed {

    public static final long DEFAULT_SEED = 0L;

    /** The seed to use for pseudo-random number generators. */
    private static long freeColSeed = DEFAULT_SEED;

    /** Has a seed been set? */
    private static boolean seeded = false;


    /**
     * Gets the seed for the PRNG.
     *
     * @return The seed.
     */
    public static long getFreeColSeed() {
        return FreeColSeed.freeColSeed;
    }

    /**
     * Has the game been seeded?
     *
     * @return True if a seed has been set.
     */
    public static boolean hasFreeColSeed() {
        return FreeColSeed.seeded;
    }
    
    /**
     * Generate a new seed.
     */
    public static void generateFreeColSeed() {
        FreeColSeed.freeColSeed = new SecureRandom().nextLong();
    }

    /**
     * Sets the seed for the PRNG.
     *
     * @param arg A string defining the seed.
     * @return True if the seed was set successfully.
     */
    public static boolean setFreeColSeed(String arg) {
        try {
            FreeColSeed.freeColSeed = Long.parseLong(arg);
            FreeColSeed.seeded = true;
            return true;
        } catch (NumberFormatException nfe) {}
        return false;
    }
}
