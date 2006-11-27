
package net.sf.freecol.common.model;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
* Represents a locatable goods of a specified type and amount.
*/
public class Goods implements Locatable, Ownable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(Goods.class.getName());

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

    private Game game;
    private Location location;
    private int type;
    private int amount;



    /**
    * Creates a new <code>Goods</code>.
    *
    * @param game The <code>Game</code> in which this object belongs
    * @param location The location of the goods,
    * @param type The type of the goods.
    * @param amount The amount of the goods.
    */
    public Goods(Game game, Location location, int type, int amount) {
        if (game == null) {
            throw new NullPointerException();
        }

        if (location == null) {
            throw new NullPointerException();
        }

        this.game = game;
        this.location = location;
        this.type = type;
        this.amount = amount;
    }


    public Goods(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;
        readFromXML(in);
    }
    
    public Goods(Game game, Element e) {
        this.game = game;
        readFromXMLElement(e);
    }

    public Goods(int type) {
        this.game = null;
        this.location = null;
        this.type = type;
        this.amount = 0;
    }


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
        return getName(type);
    }

    /**
     * Returns the name of this type of goods.
     *
     * @param sellable Whether this type of goods is sellable;
     * @return The name of this type of goods.
     */
    public String getName(boolean sellable) {
        return getName(type, sellable);
    }

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
    public static int getRawMaterial(int goodsType) {
        GoodsType  good = FreeCol.specification.goodsType( goodsType );
        return good.isRefined() ? good.madeFrom.index : -1;        
    }


    /**
    * Gets the type of goods which can be produced by the given
    * raw material.
    *
    * @param rawMaterialGoodsType The type of raw material.
    * @return The type of manufactured goods or <code>-1</code> if the given type
    *         of goods does not have a manufactured goods.
    */
    public static int getManufactoredGoods(int rawMaterialGoodsType) {
        GoodsType  good = FreeCol.specification.goodsType( rawMaterialGoodsType );
        return good.isRawMaterial() ? good.makes.index : -1;
    }


    /**
    * Checks if the given type of goods can be produced on a {@link ColonyTile}.
    * @param goodsType The type of goods to test.
    * @return The result.
    */
    public static boolean isFarmedGoods(int goodsType) {

        return FreeCol.specification.goodsType(goodsType).isFarmed;
    }


    /**
     * Returns a textual representation of the Good of type <code>type</code>.
     * @param type  The type of good to return
     * @return
     *
     * TODO - needs to be completed
     */
    public static String getName(int type) {

        if ( 0 <= type  &&  type < FreeCol.specification.numberOfGoodsTypes() ) {
            return FreeCol.specification.goodsType(type).name;
        }
        return Messages.message("model.goods.Unknown");
    }


    public static String getName(int type, boolean sellable) {

        if (sellable) {
            return getName(type);
        }

        return getName(type) + " (" + Messages.message("model.goods.Boycotted") + ")";
    }


    /**
    * Sets the location of the goods.
    * @param location The new location of the goods,
    */
    public void setLocation(Location location) {
        try {
            if ((this.location != null)) {
                this.location.remove(this);
            }

            if (location != null) {
                location.add(this);
            }

            this.location = location;
        } catch (IllegalStateException e) {
            throw (IllegalStateException) new IllegalStateException("Could not move the goods of type: "
                    + getName(getType()) + " (" + type + ") with amount: " + getAmount() + " from "
                    + this.location + " to " + location).initCause(e);
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
    * @param a The new value for amount.
    */
    public void setAmount(int a) {
        amount = a;
    }

    /**
    * Gets the value <code>type</code>. Note that type of goods should NEVER change.
    * @return The current value of type.
    */
    public int getType() {
        return type;
    }


    /**
    * If the amount of goods is greater than the source can supply,
    * then this method adjusts the amount to the maximum amount possible.
    */
    public void adjustAmount() {
        int maxAmount = location.getGoodsContainer().getGoodsCount(getType());
        setAmount((getAmount() > maxAmount) ? maxAmount : getAmount());
    }


    /**
    * Prepares the <code>Goods</code> for a new turn.
    */
    public void newTurn() {

    }

    /**
    * Loads the cargo onto a carrier that is on the same tile.
    *
    * @param carrier The carrier this unit shall embark.
    * @exception IllegalStateException If the carrier is on another tile than this unit.
    */
    public void loadOnto(Unit carrier) {
        if (getLocation() == null) {
            throw new IllegalStateException("The goods need to be taken from a place, but 'location == null'.");
        } else if ((getLocation().getTile() == carrier.getTile())) {
            if (carrier.getLocation() instanceof Europe && (carrier.getState() == Unit.TO_EUROPE || carrier.getState() == Unit.TO_AMERICA)) {
                throw new IllegalStateException("Unloading cargo from a ship that is not in port in Europe.");
            }

            setLocation(carrier);
        } else {
            throw new IllegalStateException("It is not allowed to load cargo onto a ship on another tile.");
        }
    }


    /**
    * Unload the goods from the ship. This method should only be invoked if the ship is in a harbour.
    *
    * @exception IllegalStateException If not in harbour.
    * @exception ClassCastException If not located on a ship.
    */
    public void unload() {
        Location l = ((Unit) getLocation()).getLocation();

        logger.info("Unloading cargo from a ship.");
        if (l instanceof Europe) {
            if ((((Unit) getLocation()).getState() == Unit.TO_EUROPE || ((Unit) getLocation()).getState() == Unit.TO_AMERICA)) {
                throw new IllegalStateException("Unloading cargo from a ship that is not in port in Europe.");
            }

            setLocation(l);
        } else if (l.getTile().getSettlement() != null && l.getTile().getSettlement() instanceof Colony) {
            setLocation(l.getTile().getSettlement());
        } else {
            throw new IllegalStateException("Goods may only leave a ship while in a harbour.");
        }
    }


    /**
    * Gets the game object this <code>Goods</code> belongs to.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return game;
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
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
    public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("type", Integer.toString(type));
        out.writeAttribute("amount", Integer.toString(amount));

        if (location != null) {
            out.writeAttribute("location", location.getID());
        } else {
            logger.warning("Creating an XML-element for a 'Goods' without a 'Location'.");
        }

        out.writeEndElement();
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
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code> the <code>Element</code>
     *      should be created within.
     * @return An XML-representation of this object.
     */    
    public Element toXMLElement(Player player, Document document) {
        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory xif = XMLOutputFactory.newInstance();
            XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
            toXML(xsw, player);
            xsw.close();
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document tempDocument = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));
                return (Element) document.importNode(tempDocument.getDocumentElement(), true);
            } catch (ParserConfigurationException pce) {
                // Parser with specified options can't be built
                StringWriter swe = new StringWriter();
                pce.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                throw new IllegalStateException("ParserConfigurationException");
            } catch (SAXException se) {
                StringWriter swe = new StringWriter();
                se.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                throw new IllegalStateException("SAXException");
            } catch (IOException ie) {
                StringWriter swe = new StringWriter();
                ie.printStackTrace(new PrintWriter(swe));
                logger.warning(swe.toString());
                throw new IllegalStateException("IOException");
            }                                    
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            throw new IllegalStateException("XMLStreamException");
        }
    }    

    /**
     * Initialize this object from an XML-representation of this object.
     * @param element An XML-element that will be used to initialize
     *      this object.
     */
    public void readFromXMLElement(Element element) {
        XMLInputFactory xif = XMLInputFactory.newInstance();        
        try {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xmlTransformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                String xml = stringWriter.toString();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
                xsr.nextTag();
                readFromXML(xsr);
            } catch (TransformerException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
                throw new IllegalStateException("TransformerException");
            }
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            throw new IllegalStateException("XMLStreamException");
        }
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {        
        type = Integer.parseInt(in.getAttributeValue(null, "type"));
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
