package net.sf.freecol.common.model;

/**
 * This interface marks the locations where a <code>Unit</code> can work.
 */
public interface WorkLocation extends Location {
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /**
     * Returns the production of the given type of goods.
     * 
     * @param goodsType The type of goods to get the production of.
     * @return The production of the given type of goods.
     */
    public int getProductionOf(GoodsType goodsType);

    /**
     * Returns the <code>Colony</code> this <code>WorkLocation</code> is
     * located in.
     * 
     * This method always returns a colony != null (in contrast to
     * Location.getColony(), which might return null).
     * 
     * @return The <code>Colony</code> this <code>WorkLocation</code> is
     *         located in.
     * 
     * @see Location#getColony
     */
    public Colony getColony();
}
