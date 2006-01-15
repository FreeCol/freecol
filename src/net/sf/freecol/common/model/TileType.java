
package net.sf.freecol.common.model;


public final class TileType
{

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
