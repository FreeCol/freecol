package net.sf.freecol.common;

import java.security.SecureRandom;
import java.util.logging.Logger;

public class FreeColSeed {


    private static final Logger logger = Logger.getLogger(FreeColSeed.class.getName());

    
    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static long freeColSeed;
    

    /**
     * Gets the seed for the PRNG.
     *
     * @return The seed.
     */
    public static long getFreeColSeed() {
        if (freeColSeed == 0L) {
            freeColSeed = new SecureRandom().nextLong();
            logger.info("Using seed: " + freeColSeed);
        }
        return freeColSeed;
    }

    /**
     * Increments the seed for the PRNG.
     */
    public static void incrementFreeColSeed() {
        freeColSeed = getFreeColSeed() + 1;
        logger.info("Reseeded with: " + freeColSeed);
    }

    public static void initialize(long initianValue) {
        FreeColSeed.freeColSeed = initianValue;
        
    }


}
