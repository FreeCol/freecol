
package net.sf.freecol.common.model;



/**
* This interface marks the locations where a <code>Unit</code> can work.
*/
public interface WorkLocation extends Location {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
    * Returns the production of the given type of goods.
    */
    public int getProductionOf(int goodsType);
}
