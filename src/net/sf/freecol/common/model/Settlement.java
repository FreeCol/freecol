package net.sf.freecol.common.model;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;


/**
* The super class of all settlements on the map (that is colonies and indian settlements).
*/
abstract public class Settlement extends FreeColGameObject implements Location {

    /** The <code>Player</code> owning this <code>Settlement</code>. */
    protected Player owner;

    /** The <code>Tile</code> where this <code>Settlement</code> is located. */
    protected Tile tile;
    
    

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

}
