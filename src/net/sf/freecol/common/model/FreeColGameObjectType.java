package net.sf.freecol.common.model;

import net.sf.freecol.client.gui.i18n.Messages;

public class FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision: 2916 $";

    private String id;
    private int index;

    protected final void setID(final String id) {
        this.id = id;
    }

    protected final void setIndex(final int index) {
        this.index = index;
    }

    public final String getID() {
        return id;
    }

    public final int getIndex() {
        return index;
    }

    public final String getName() {
        return Messages.message(id + ".name");
    }

    public final String getDescription() {
        return Messages.message(id + ".description");
    }

}
