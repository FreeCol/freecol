package net.sf.freecol.common.model;

import org.w3c.dom.Element;


/**
* The super class of all settlements on the map (that is colonies and indian settlements).
*/
abstract public class Settlement extends FreeColGameObject implements Location, Ownable {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The <code>Player</code> owning this <code>Settlement</code>. */
    protected Player owner;

    /** The <code>Tile</code> where this <code>Settlement</code> is located. */
    protected Tile tile;
    
    protected GoodsContainer goodsContainer;
    

    /**
    * Creates a new <code>Settlement</code>.
    *
    * @param game The <code>Game</code> in which this object belong.
    * @param owner The owner of this <code>Settlement</code>.
    * @param tile The location of the <code>Settlement</code>.    
    */
    public Settlement(Game game, Player owner, Tile tile) {
        super(game);
        this.tile = tile;
        this.owner = owner;
    }

    

    /**
    * Initiates a new <code>Settlement</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> in which this object belong.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public Settlement(Game game, Element element) {
        super(game, element);
    }
    
    

    /**
    * Gets this colony's line of sight.
    * @return The line of sight offered by this
    *       <code>Colony</code>.
    * @see Player#canSee(Tile)
    */
    public int getLineOfSight() {
        return 2;
    }
    

    /**
    * Gets the <code>Unit</code> that is currently defending this <code>Settlement</code>.
    * @param attacker The target that would be attacking this <code>Settlement</code>.
    * @return The <code>Unit</code> that has been choosen to defend this <code>Settlement</code>.
    */
    abstract public Unit getDefendingUnit(Unit attacker);

    
    /**
    * Gets the <code>Tile</code> where this <code>Settlement</code> is located.
    * @return The <code>Tile</code> where this <code>Settlement</code> is located.
    */
    public Tile getTile() {
        return tile;
    }
    
    /**
    * Gets a <code>Tile</code> from the neighbourhood of this 
    * <code>Colony</code>.
    * 
    * @param x The x-coordinate of the <code>Tile</code>.
    * @param y The y-coordinate of the <code>Tile</code>. 
    * @return The <code>Tile</code>.
    */
    public Tile getTile(int x, int y) {
        if (x==0 && y==0) {
            return getGame().getMap().getNeighbourOrNull(Map.N, tile);
        } else if (x==0 && y== 1) {
            return getGame().getMap().getNeighbourOrNull(Map.NE, tile);
        } else if (x==0 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.E, tile);
        } else if (x==1 && y== 0) {
            return getGame().getMap().getNeighbourOrNull(Map.NW, tile);
        } else if (x==1 && y== 1) {
            return tile;
        } else if (x==1 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.SE, tile);
        } else if (x==2 && y== 0) {
            return getGame().getMap().getNeighbourOrNull(Map.W, tile);
        } else if (x==2 && y== 1) {
            return getGame().getMap().getNeighbourOrNull(Map.SW, tile);
        } else if (x==2 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.S, tile);
        } else {
            return null;
        }
    }



    /**
    * Gets the owner of this <code>Settlement</code>.
    *
    * @return The owner of this <code>Settlement</code>.
    * @see #setOwner
    */
    public Player getOwner() {
        return owner;
    }

    
    /**
    * Sets the owner of this <code>Settlement</code>.
    *
    * @param owner The <code>Player</code> that shall own this <code>Settlement</code>.
    * @see #getOwner
    */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }


    /**
    * Adds a <code>Locatable</code> to this Location.
    *
    * @param locatable The <code>Locatable</code> to add to this Location.
    */
    public abstract void add(Locatable locatable);

    /**
    * Removes a <code>Locatable</code> from this Location.
    *
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public abstract void remove(Locatable locatable);

    public abstract boolean canAdd(Locatable locatable);

    /**
    * Returns the number of units in this settlement.
    *
    * @return The number of units in this settlement.
    */
    public abstract int getUnitCount();

    public abstract boolean contains(Locatable locatable);



    public void dispose() {
        getTile().setSettlement(null);
        owner.getSettlements().remove(this);
        goodsContainer.dispose();
        super.dispose();
    }

}
