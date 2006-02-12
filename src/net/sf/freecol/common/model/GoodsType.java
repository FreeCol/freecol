
package net.sf.freecol.common.model;


import net.sf.freecol.client.gui.i18n.Messages;


public final class GoodsType
{
    public final  int        index;
    public final  String     name;
    public final  boolean    isFarmed;
    public        GoodsType  madeFrom;
    public        GoodsType  makes;

    private static  int  nextIndex;


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
