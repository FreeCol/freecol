
package net.sf.freecol.common.model;

import java.util.Map;

/**
 * An object that can be built.
 */
public interface Buildable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // TODO: make this more generic
    public int getHammersRequired();
    public int getToolsRequired();
    public int getPopulationRequired();
    public Map<String, Boolean> getAbilitiesRequired();

}
