/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.model;


import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Element;

/**
 * Represents locatable goods of a specified type and amount. Use
 * AbstractGoods to represent abstract or potential goods that need
 * not be present in any particular location.
 *
 * @see AbstractGoods
 */
public class Goods extends AbstractGoods implements Locatable, Ownable, Named {


    private static Logger logger = Logger.getLogger(Goods.class.getName());

//    public static final GoodsType FOOD, BELLS, CROSSES, HAMMERS, HORSES, TOOLS, MUSKETS, TRADE_GOODS, FURS, FISH;
    // Need to change Units to the new specification to remove reliance on these static quick links.
    // Only the essential should have a quick link.
    public static GoodsType FOOD, SUGAR, TOBACCO, COTTON, FURS, LUMBER, ORE, SILVER, HORSES, 
                                    RUM, CIGARS, CLOTH, COATS, TRADEGOODS, TOOLS, MUSKETS, 
                                    FISH, BELLS, CROSSES, HAMMERS;
    public static int NUMBER_OF_TYPES;

    private Game game;
    private Location location;

    // ------------------------------------------------------------ constructor

    /**
     * Creates a standard <code>Goods</code>-instance given the place where
     * the goods is.
     * 
     * This constructor only asserts that the game and 
     * that the location (if given) can store goods. The goods will not
     * be added to the location (use Location.add for this).
     * 
     * @param game The <code>Game</code> in which this object belongs
     * @param location The location of the goods (may be null)
     * @param type The type of the goods.
     * @param amount The amount of the goods.
     * 
     * @throws IllegalArgumentException if the location cannot store any goods.
     */
    public Goods(Game game, Location location, GoodsType type, int amount) {
        if (game == null) {
            throw new NullPointerException();
        }
        
        if (location != null && location.getGoodsContainer() == null){
            throw new IllegalArgumentException("This location cannot store goods");
        }

        if (type == null) {
            throw new NullPointerException();
        }

        this.game = game;
        this.location = location;
        setType(type);
        setAmount(amount);
    }

    /**
     * Creates a new <code>Goods</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param location a <code>Location</code> value
     * @param goodsIndex an <code>int</code> value
     * @param amount an <code>int</code> value
     */
    public Goods(Game game, Location location, int goodsIndex, int amount) {
        this(game, location, FreeCol.getSpecification().getGoodsType(goodsIndex), amount);
    }

    /**
     * Creates a new <code>Goods</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Goods(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;
        readFromXML(in);

    }
    
    /**
     * Creates a new <code>Goods</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public Goods(Game game, Element e) {
        this.game = game;
        readFromXMLElement(e);
    }


    // ------------------------------------------------------------ static methods

    /**
     * Initializes the important Types for quick reference - performed by Specification.java
     * Should be softcoded as much as possible, and this should be amended later
     * @param numberOfTypes Initializer for NUMBER_OF_TYPES
     */
    public static void initialize(List<GoodsType> goodsList, int numberOfTypes) {
        for (GoodsType g : goodsList) {
            try {
                String fieldName = g.getId().substring(g.getId().lastIndexOf('.') + 1).toUpperCase();
                Goods.class.getDeclaredField(fieldName).set(null, g);
            } catch (Exception e) {
                logger.warning("Error assigning a GoodsType to Goods." +
                        g.getId().toUpperCase() + "\n" + e.toString());
            }
        }
        NUMBER_OF_TYPES = numberOfTypes;
    }

    // ------------------------------------------------------------ retrieval methods

    /**
    * Gets the owner of this <code>Ownable</code>.
    *
    * @return The <code>Player</code> controlling this
    *         {@link Ownable}.
    */
    public Player getOwner() {
        return (location instanceof Ownable) ? ((Ownable) location).getOwner() : null;
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership
     *      of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *      this method. 
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }

    /**
    * Returns a textual representation of this object.
    * @return A <code>String</code> with the format:
    *         <br>AMOUNT GOODSTYPE
    *         <br><br>Example:
    *         <br>15 Cotton
    */
    public String toString() {
        return Integer.toString(getAmount()) + " " + getName();
    }

    /**
     * Returns the name of this type of goods.
     *
     * @return The name of this type of goods.
     */
    public String getName() {
        return getType().getName();
    }

    /**
     * Returns the name of this type of goods.
     *
     * @param sellable Whether this type of goods is sellable;
     * @return The name of this type of goods.
     */
    public String getName(boolean sellable) {
        return getType().getName(sellable);
    }

    /**
     * Returns a textual representation of the Good of type <code>type</code>.
     * @param type  The type of good to return
     * @return
     *
     *//*   COMEBACKHERE
    public static String getName(GoodsType type) {
        return type.getName();
    }

    public static String getName(GoodsType type, boolean sellable) {
        if (sellable) {
            return type.getName();
        } else {
            return type.getName() + " (" + Messages.message("model.goods.Boycotted") + ")";
        }
    }
*/
    /**
    * Returns the <code>Tile</code> where this <code>Goods</code> is located,
    * or <code>null</code> if it's location is <code>Europe</code>.
    *
    * @return The Tile where this Unit is located. Or null if
    * its location is Europe.
    */
    public Tile getTile() {
        return (location != null) ? location.getTile() : null;
    }


    /**
    * Sets the location of the goods.
    * @param location The new location of the goods,
    */
    public void setLocation(Location location) {
        
        if (location != null && location.getGoodsContainer() == null) {
            throw new IllegalArgumentException("Goods have to be located in a GoodsContainers.");
        }
        
        try {
            if ((this.location != null)) {
                this.location.remove(this);
            }

            if (location != null) {
                location.add(this);
            }

            this.location = location;
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Could not move the goods of type: "
                    + getType().getName() + " with amount: " + getAmount() + " from "
                    + this.location + " to " + location, e);
        }
    }


    /**
    * Gets the location of this goods.
    * @return The location.
    */
    public Location getLocation() {
        return location;
    }


    /**
    * Gets the amount of space this <code>Goods</code> take.
    * @return The amount.
    */
    public int getTakeSpace() {
        return 1;
    }

    /**
    * If the amount of goods is greater than the container can hold,
    * then this method adjusts the amount to the maximum amount possible.
    */
    public void adjustAmount() {
        int maxAmount = location.getGoodsContainer().getGoodsCount(getType());
        
        if (getAmount() > maxAmount)
            setAmount(maxAmount);
    }

    /**
     * Loads the cargo onto a carrier that is on the same tile.
     * 
     * This method has the same effect as setLocation but ensures that: 1.) The
     * given unit is at the same tile as the goods (including a check if
     * transfering the goods in Europe makes sense) and 2.) that the source
     * location of the goods is not null. checks that
     * 
     * @param carrier The carrier onto which to the load the goods.
     * @exception IllegalStateException If the carrier is on another tile than
     *                this unit, the location of the goods is null or both
     *                carriers are not in port in Europe.
     */
    public void loadOnto(Unit carrier) {
        if (getLocation() == null) {
            throw new IllegalStateException("The goods need to be taken from a place, but 'location == null'.");
        }
        if ((getLocation().getTile() != carrier.getTile())) {
            throw new IllegalStateException("It is not allowed to load cargo onto a ship on another tile.");
        }
        if (getLocation().getTile() == null){
            // In Europe
            Unit source = (Unit)getLocation();

            // Make sure that both carriers are in a port in Europe.
            if (!carrier.isInEurope() || !source.isInEurope()){
                throw new IllegalStateException("Loading cargo onto a ship that is not in port in Europe.");
            }
        }
        setLocation(carrier);
    }


    /**
     * Unload this Goods from a carrier into a colony.
     * 
     * This method has the same effect as setLocation but performs checks whether the 
     * goods are on a carrier and whether the carrier is in a colony.
     * 
     * @exception IllegalStateException If the goods are not on a unit or the unit not in a colony.
     */
    public void unload() {
        if (!(getLocation() instanceof Unit)){
            throw new IllegalStateException("Goods not on a unit");
        }

        Unit carrier = (Unit) getLocation();
        Location location = carrier.getLocation();

        if (location instanceof Europe || location.getTile().getSettlement() == null || !(location.getTile().getSettlement() instanceof Colony)) {
            throw new IllegalStateException("Goods may only be unloaded while the carrier is in a colony");
        }
        setLocation(location.getTile().getSettlement());
    }


    /**
    * Gets the game object this <code>Goods</code> belongs to.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return game;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("type", getType().getId());
        out.writeAttribute("amount", Integer.toString(getAmount()));

        if (location != null) {
            out.writeAttribute("location", location.getId());
        } else {
            logger.warning("Creating an XML-element for a 'Goods' without a 'Location'.");
        }

        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {        
        setType(FreeCol.getSpecification().getGoodsType(in.getAttributeValue(null, "type")));
        setAmount(Integer.parseInt(in.getAttributeValue(null, "amount")));

        final String locationStr = in.getAttributeValue(null, "location");
        if (locationStr != null) {
            location = (Location) getGame().getFreeColGameObject(locationStr);
        }
        
        in.nextTag();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "goods".
    */
    public static String getXMLElementTagName() {
        return "goods";
    }
}
