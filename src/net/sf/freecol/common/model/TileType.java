
package net.sf.freecol.common.model;


public final class TileType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    public final  String    name;
    public final  int       basicMoveCost;
    public final  int       defenceBonus;
    public        TileType  whenForested;


    public TileType( String  name,
                     int     basicMoveCost,
                     int     defenceBonus ) {

        this.name = name;
        this.basicMoveCost = basicMoveCost;
        this.defenceBonus = defenceBonus;
    }

}
