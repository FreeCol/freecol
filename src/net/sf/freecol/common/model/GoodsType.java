
package net.sf.freecol.common.model;


import net.sf.freecol.client.gui.i18n.Messages;


public final class GoodsType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    private static  int  nextIndex;

    public final  int        index;
    public final  String     name;
    public final  boolean    isFarmed;
    public        GoodsType  madeFrom;
    public        GoodsType  makes;


    public GoodsType( String name, boolean isFarmed ) {

        index = nextIndex ++;
        this.name = Messages.message( name );
        this.isFarmed = isFarmed;
    }


    public boolean isRawMaterial() {

        return makes != null;
    }


    public boolean isRefined() {

        return madeFrom != null;
    }

}
