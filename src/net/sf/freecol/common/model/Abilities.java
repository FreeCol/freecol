package net.sf.freecol.common.model;

public interface Abilities {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision: $";


    public boolean hasAbility(String id);

    public void setAbility(String id, boolean newValue);

}