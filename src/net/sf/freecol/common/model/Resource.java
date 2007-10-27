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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
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
* Represents a locatable goods of a specified type and quantity.
*/
public class Resource extends TileItem {


    private static Logger logger = Logger.getLogger(Resource.class.getName());

    private ResourceType type;
    private int quantity;

    /**
     * Creates a standard <code>Resource</code>-instance.
     * 
     * This constructor asserts that the game, tile and type are valid.
     * 
     * @param game The <code>Game</code> in which this object belongs.
     * @param tile The <code>Tile</code> on which this object sits.
     * @param type The <code>ResourceType</code> of this Resource.
     */
    public Resource(Game game, Tile tile, ResourceType type) {
        super(game, tile);
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.quantity = type.getRandomValue();
    }

    public Resource(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    public Resource(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>TileImprovement</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    /*
    public TileImprovement(Game game, String id) {
        super(game, id);
    }
    */

    /**
    * Returns a textual representation of this object.
    * @return A <code>String</code> of either:
    * <ol>
    * <li>QUANTITY RESOURCETYPE (eg. 250 Minerals) if there is a limited quantity
    * <li>RESOURCETYPE (eg. Game) if it is an unlimited resource
    * </ol>
    */
    public String toString() {
        if (quantity > -1) {
            return Integer.toString(quantity) + " " + getName();
        } else {
            return getName();
        }
    }

    /**
     * Returns the name of this <code>Resource</code>.
     * @return The name of this Resource.
     */
    public String getName() {
        return getType().getName();
    }

    /**
     * Returns the name of a given <code>ResourceType</code>.
     * @return The name of this ResourceType.
     *//*
    public static String getName(ResourceType resType) {
        return Messages.message(resType.getName());
    }
*/
    /**
     * Returns the <code>ResourceType</code> of this Resource.
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * Returns a <code>String</code> with the output of this Resource.
     */
    public String getOutputString() {
        return type.getOutputString();
    }

    /**
     * Returns the current quantity.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the current quantity.
     */
    public void setQuantity(int newQuantity) {
        quantity = newQuantity;
    }

    /**
     * Returns the best GoodsType
     */
    public GoodsType getBestGoodsType() {
        return type.getBestGoodsType();
    }

    /**
     * Returns the bonus (checking available stock) for next turn.
     * @param goodsType The GoodsType to check
     * @param potential Potential of Tile + Improvements
     */
    public int getBonus(GoodsType goodsType, int potential) {
        int bonusAmount = (int) ((potential + type.getBonus(goodsType)) * type.getFactor(goodsType)) - potential;
        if (quantity > -1 && bonusAmount > quantity) {
            bonusAmount = quantity;
        }
        return potential + bonusAmount;
    }

    /**
     * Reduces the available quantity by the bonus output of <code>GoodsType</code>.
     * @param goodsType The GoodsType to check
     * @param potential Potential of Tile + Improvements
     */
    public int useQuantity(GoodsType goodsType, int potential) {
        // Return -1 here if not limited resource?
        return useQuantity(getBonus(goodsType, potential) - potential);
    }    

    /**
    * Reduces the value <code>quantity</code>.
    * @param usedQuantity The quantity that was used up.
    * @return The final value of quantity.
    */
    public int useQuantity(int usedQuantity) {
        if (quantity >= usedQuantity) {
            quantity -= usedQuantity;
        } else if (quantity == -1) {
            logger.warning("useQuantity called for unlimited resource");
        } else {
            // Shouldn't generally happen.  Do something more drastic here?
            logger.severe("Insufficient quantity in " + this);
            quantity = 0;
        }
        return quantity;
    }

    /**
     * Disposes this resource.
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    // ------------------------------------------------------------ API methods

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", getTile().getId());
        out.writeAttribute("type", getType().getId());
        out.writeAttribute("quantity", Integer.toString(quantity));

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }
        type = FreeCol.getSpecification().getResourceType(in.getAttributeValue(null, "type"));
        quantity = Integer.parseInt(in.getAttributeValue(null, "quantity"));
        
        in.nextTag();
    }

    /**
    * Gets the tag name of the root element representing this object.
    * @return "resource".
    */
    public static String getXMLElementTagName() {
        return "resource";
    }

    public void setName(String newName) {
    }

}
