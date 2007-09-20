
package net.sf.freecol.common.model;


import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Element;

/**
* Represents a locatable goods of a specified type and amount.
*/
public class Goods extends FreeColObject implements Locatable, Ownable, Named {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(Goods.class.getName());

//    public static final GoodsType FOOD, BELLS, CROSSES, HAMMERS, HORSES, TOOLS, MUSKETS, TRADE_GOODS, FURS, FISH;
    // Need to change Units to the new specification to remove reliance on these static quick links.
    // Only the essential should have a quick link.
    public static GoodsType FOOD, SUGAR, TOBACCO, COTTON, FURS, LUMBER, ORE, SILVER, HORSES, 
                                    RUM, CIGARS, CLOTH, COATS, TRADEGOODS, TOOLS, MUSKETS, 
                                    FISH, BELLS, CROSSES, HAMMERS;
    public static int NUMBER_OF_TYPES;
/*
    public static final int FOOD = 0,
                            SUGAR = 1,
                            TOBACCO = 2,
                            COTTON = 3,
                            FURS = 4,
                            LUMBER = 5,
                            ORE = 6,
                            SILVER = 7,
                            HORSES = 8,
                            RUM = 9,
                            CIGARS = 10,
                            CLOTH = 11,
                            COATS = 12,
                            TRADE_GOODS = 13,
                            TOOLS = 14,
                            MUSKETS = 15;

    public static final int NUMBER_OF_TYPES = 16;
    // Unstorable goods:
    public static final int FISH = 16, // Stored as food.
                            BELLS = 17,
                            CROSSES = 18,
                            HAMMERS = 19;

    public static final int NUMBER_OF_ALL_TYPES = 20;
*/

    private Game game;
    private Location location;
    private GoodsType type;
    private int amount;

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
        this.type = type;
        this.amount = amount;
    }

    public Goods(Game game, Location location, int goodsIndex, int amount) {
        this(game, location, FreeCol.getSpecification().getGoodsType(goodsIndex), amount);
    }

    public Goods(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;
        readFromXML(in);
    }
    
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
                String fieldName = g.getID().substring(g.getID().lastIndexOf('.') + 1).toUpperCase();
                Goods.class.getDeclaredField(fieldName).set(null, g);
            } catch (Exception e) {
                logger.warning("Error assigning a GoodsType to Goods." +
                        g.getID().toUpperCase() + "\n" + e.toString());
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
        return Integer.toString(amount) + " " + getName();
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
    * Gets the type of goods that is needed to produce the given type
    * of goods.
    *
    * @param goodsType The type of manufactured goods.
    * @return The type of raw material or <code>-1</code> if the given type
    *         of goods does not have a raw material.
    */
    /* Depreciated
    public static int getRawMaterial(int goodsType) {
        GoodsType  good = FreeCol.getSpecification().goodsType( goodsType );
        return good.isRefined() ? good.madeFrom.index : -1;        
    }
    */

    /**
    * Gets the type of goods which can be produced by the given
    * raw material.
    *
    * @param rawMaterialGoodsType The type of raw material.
    * @return The type of manufactured goods or <code>-1</code> if the given type
    *         of goods does not have a manufactured goods.
    */
    /* Depreciated
    public static int getManufactoredGoods(int rawMaterialGoodsType) {
        GoodsType  good = FreeCol.getSpecification().goodsType( rawMaterialGoodsType );
        return good.isRawMaterial() ? good.makes.index : -1;
    }
    */

    /**
    * Checks if the given type of goods can be produced on a {@link ColonyTile}.
    * @param goodsType The type of goods to test.
    * @return The result.
    */
    /*  Depreciated
    public static boolean isFarmedGoods(int goodsType) {

        return FreeCol.getSpecification().goodsType(goodsType).isFarmed;
    }
    */

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
                    + getType().getName() + " (" + type + ") with amount: " + getAmount() + " from "
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
    * Gets the value <code>amount</code>.
    * @return The current value of amount.
    */
    public int getAmount() {
        return amount;
    }

    /**
    * Sets the value <code>amount</code>.
    * 
    * This method does not check this new amount in any way (thus the amount might be negative)
    * 
    * @param newAmount The new value for amount.
    */
    public void setAmount(int newAmount) {
        amount = newAmount;
    }

    /**
    * Gets the value <code>type</code>. Note that type of goods should NEVER change.
    * @return The current value of type.
    */
    public GoodsType getType() {
        return type;
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
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("type", type.getName());
        out.writeAttribute("amount", Integer.toString(amount));

        if (location != null) {
            out.writeAttribute("location", location.getID());
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
        type = FreeCol.getSpecification().getGoodsType(in.getAttributeValue(null, "type"));
        amount = Integer.parseInt(in.getAttributeValue(null, "amount"));

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
