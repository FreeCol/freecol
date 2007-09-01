package net.sf.freecol.common.model;


/**
 * Represents a lost city rumour.
 */
public class LostCityRumour extends FreeColGameObjectType {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** Constants describing types of Lost City Rumours. */
    public static final int NO_SUCH_RUMOUR = -1,
        BURIAL_GROUND = 0,
        EXPEDITION_VANISHES = 1, 
        NOTHING = 2,
        LEARN = 3,
        TRIBAL_CHIEF = 4,
        COLONIST = 5,
        TREASURE = 6,
        FOUNTAIN_OF_YOUTH = 7;

    public static final int NUMBER_OF_RUMOURS = 8;
}


