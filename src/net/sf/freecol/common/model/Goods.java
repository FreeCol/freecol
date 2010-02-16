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
import net.sf.freecol.client.gui.i18n.Messages;

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
            throw new IllegalArgumentException("Parameter 'game' must not be 'null'.");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Parameter 'type' must not be 'null'.");
        }

        if (location != null && location.getGoodsContainer() == null){
            throw new IllegalArgumentException("This location cannot store goods: " + location.toString());
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
        return toString(this);
    }

    public static String toString(Goods goods) {
        return toString(goods.getType(), goods.getAmount());
    }

    public static String toString(GoodsType goodsType, int amount) {
        return Messages.message("model.goods.goodsAmount",
                                "%goods%", goodsType.getName(),
                                "%amount%", String.valueOf(amount));
    }

    public String getNameKey() {
        return getType().getNameKey();
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
            return type.getName() + " (" + Messages.message("model.goods.boycotted") + ")";
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
     * Gets the location of this goods.
     *
     * @return The goods location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of the goods.
     *
     * @param location The new <code>Location</code> of the goods.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /** DO NOT USE, this is going away (into the server) soon. */
    public void changeLocation(Location location) {
       if (location != null && location.getGoodsContainer() == null) {
           throw new IllegalArgumentException("Goods have to be located in a GoodsContainers.");
       }

       if (this.location != null) {
           this.location.remove(this);
       }
       this.location = null;

       if (location != null) {
           location.add(this);
       }
       this.location = location;
    }


    /**
    * Gets the amount of space this <code>Goods</code> take.
    * @return The amount.
    */
    public int getSpaceTaken() {
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
