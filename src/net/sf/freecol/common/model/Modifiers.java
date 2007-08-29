package net.sf.freecol.common.model;

public interface Modifiers {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    /**
     * Returns true if the Object has the modifier identified by
     * <code>id</code.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getModifier(String id);

    /**
     * Sets the modifier identified by <code>id</code.
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>Modifier</code> value
     */
    public void setModifier(String id, Modifier newValue);

}