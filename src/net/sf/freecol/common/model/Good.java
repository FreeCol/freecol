
package net.sf.freecol.common.model;


import net.sf.freecol.client.gui.i18n.Messages;


public final class Good
{
    public final  int      index;
    public final  String   name;
    public final  boolean  isFarmed;
    public        Good     madeFrom;
    public        Good     makes;

    private static  int  nextIndex;


    public Good( String name, boolean isFarmed ) {

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
