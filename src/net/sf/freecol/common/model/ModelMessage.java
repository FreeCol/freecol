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

import java.util.Arrays;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Contains a message about a change in the model.
 */
public class ModelMessage extends FreeColObject {


    /** Constants describing the type of message. */
    public static final int DEFAULT = 0;
    public static final int WARNING = 1;
    public static final int SONS_OF_LIBERTY = 2;
    public static final int GOVERNMENT_EFFICIENCY = 3;
    public static final int WAREHOUSE_CAPACITY = 4;
    public static final int UNIT_IMPROVED = 5;
    public static final int UNIT_DEMOTED = 6;
    public static final int UNIT_LOST = 7;
    public static final int UNIT_ADDED = 8;
    public static final int BUILDING_COMPLETED = 9;
    public static final int FOREIGN_DIPLOMACY = 10;
    public static final int MARKET_PRICES = 11;
    public static final int LOST_CITY_RUMOUR = 12;
    public static final int GIFT_GOODS = 13;
    public static final int MISSING_GOODS = 14;
    public static final int TUTORIAL = 15;

    private FreeColGameObject source;
    private FreeColObject display;
    private int type;
    private String messageID;
    private String[][] data;
    private boolean beenDisplayed = false;


    public ModelMessage() {
    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @param type The type of this model message.
    * @param display The Object to display.
    * @see FreeColGameObject#addModelMessage(FreeColGameObject, String, String[][], int)
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data, int type, FreeColObject display) {
        this.source = source;
        this.messageID = messageID;
        this.data = data;
        this.type = type;
        this.display = display;
        verifyFields();
    }
    
    /**
     * Checks that all the fields as they are set in the constructor are valid.
     */
    private void verifyFields() {
        if (source == null) {
            throw new IllegalArgumentException("The source cannot be null.");
        }
        if (messageID == null) {
            throw new IllegalArgumentException("The messageID cannot be null.");
        }
        if (!(display == null ||
              display instanceof FreeColGameObject ||
              display instanceof FreeColGameObjectType)) {
            throw new IllegalArgumentException("The display must be a FreeColGameObject or FreeColGameObjectType!");
        }

        if (data != null) {
            for (String[] s : data) {
                if (s == null || s.length != 2) {
                    throw new IllegalArgumentException("The data can only contain arrays of size 2.");
                }
            }
        }
    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @param type The type of this model message.
    * @see FreeColGameObject#addModelMessage(FreeColGameObject, String, String[][], int)
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data, int type) {
        this(source, messageID, data, type, getDefaultDisplay(type, source));

    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @see FreeColGameObject#addModelMessage(FreeColGameObject, String, String[][], int)
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data) {
        this(source, messageID, data, DEFAULT);
    }
    
    /**
     * Returns the default display object for the given type.
     * @param type the type for which to find the default display object.
     * @param source the source object
     * @return an object to be displayed for the message. 
     */
    static private FreeColObject getDefaultDisplay(int type, FreeColGameObject source) {
        FreeColObject newDisplay = null;
        switch(type) {
        case SONS_OF_LIBERTY:
        case GOVERNMENT_EFFICIENCY:
            newDisplay = FreeCol.getSpecification().getGoodsType("model.goods.bells");
            break;
        case LOST_CITY_RUMOUR:
            newDisplay = new LostCityRumour();
            break;
        case UNIT_IMPROVED:
        case UNIT_DEMOTED:
        case UNIT_LOST:
        case UNIT_ADDED:
            newDisplay = source;
            break;
        case BUILDING_COMPLETED:
            newDisplay = FreeCol.getSpecification().getGoodsType("model.goods.hammers");
            break;
        case DEFAULT:
        case WARNING:
        case WAREHOUSE_CAPACITY:
        case FOREIGN_DIPLOMACY:
        case MARKET_PRICES:
        case GIFT_GOODS:
        case MISSING_GOODS:
        default:
            if (source instanceof Player) {
                newDisplay = source;
            }
        }
        return newDisplay;
    }


    /**
    * Checks if this <code>ModelMessage</code> has been displayed.
    * @return <i>true</i> if this <code>ModelMessage</code> has been displayed.
    * @see #setBeenDisplayed
    */
    public boolean hasBeenDisplayed() {
        return beenDisplayed;
    }


    /**
    * Sets the <code>beenDisplayed</code> value of this <code>ModelMessage</code>.
    * This is used to avoid showing the same message twice.
    * 
    * @param beenDisplayed Should be set to <code>true</code> after the
    *       message has been displayed.
    */
    public void setBeenDisplayed(boolean beenDisplayed) {
        this.beenDisplayed = beenDisplayed;
    }


    /**
    * Gets the source of the message. This is what the message
    * should be associated with. In addition, the owner of the source is the
    * player getting the message.
    *
    * @return The source of the message.
    * @see #getOwner
    */
    public FreeColGameObject getSource() {
        return source;
    }
    
    /**
     * Gets the ID of the message to display.   
     * @return The ID. 
     */
    public String getID() {
        return messageID;
    }
    
    /**
     * Returns the data to be displayed in the message.
     * @return The data as a <code>String[][]</code> or <i>null</i>
     *         if no data applies.
     */
    public String[][] getData() {
        return data;
    }

    /**
     * Gets the type of the message to display.   
     * @return The type. 
     */
    public int getType() {
        return type;

    }


    public String getTypeName() {
        switch (type) {
        case WARNING:
            return Messages.message("model.message.warning");
        case SONS_OF_LIBERTY:
            return Messages.message("model.message.sonsOfLiberty");
        case GOVERNMENT_EFFICIENCY:
            return Messages.message("model.message.governmentEfficiency");
        case WAREHOUSE_CAPACITY:
            return Messages.message("model.message.warehouseCapacity");
        case UNIT_IMPROVED:
            return Messages.message("model.message.unitImproved");
        case UNIT_DEMOTED:
            return Messages.message("model.message.unitDemoted");
        case UNIT_LOST:
            return Messages.message("model.message.unitLost");
        case UNIT_ADDED:
            return Messages.message("model.message.unitAdded");
        case BUILDING_COMPLETED:
            return Messages.message("model.message.buildingCompleted");
        case FOREIGN_DIPLOMACY:
            return Messages.message("model.message.foreignDiplomacy");
        case MARKET_PRICES:
            return Messages.message("model.message.marketPrices");
        case LOST_CITY_RUMOUR:
            return Messages.message("model.message.lostCityRumour");
        case GIFT_GOODS:
            return Messages.message("model.message.giftGoods");
        case MISSING_GOODS:
            return Messages.message("model.message.missingGoods");
        case DEFAULT:
        default:
            return Messages.message("model.message.default");
        }
    }


    /**
     * Gets the Object to display.
     * @return The Object to display.
     */
    public FreeColObject getDisplay() {
        return display;
    }

    /**
    * Returns the owner of this message. The owner of this method
    * is the owner of the {@link #getSource source}.
    * 
    * @return The owner of the message. This is the <code>Player</code>
    *       who should receive the message.
    */
    public Player getOwner() {
        if (source instanceof Unit) {
            return ((Unit) source).getOwner();
        } else if (source instanceof Settlement) {
            return ((Settlement) source).getOwner();
        } else if (source instanceof Europe) {
            return ((Europe) source).getOwner();
        } else if (source instanceof Player) {
            return (Player) source;
        } else if (source instanceof Ownable) {
            return ((Ownable) source).getOwner();
        }

        return null;
    }


    /**
    * Checks if this <code>ModelMessage</code> is equal to another <code>ModelMessage</code>.
    * @return <i>true</i> if the sources, message IDs and data are equal.
    */
    @Override
    public boolean equals(Object o) {
        if ( ! (o instanceof ModelMessage) ) { return false; }
        ModelMessage m = (ModelMessage) o;
        // Check that the content of the data array is equal
        if (!(data == m.data)) {
            if (data != null && m.data != null && data.length == m.data.length) {
                for (int i = 0 ; i < data.length ; i++) {
                    if (!Arrays.equals(data[i], m.data[i]))
                        return false;
                }
            } else {
                return false;
            }
        }
        return ( source.equals(m.source)
                && messageID.equals(m.messageID)
                && type == m.type );
    }
    
    @Override
    public int hashCode() {
        int value = 1;
        value = 37 * value + source.hashCode();
        value = 37 * value + messageID.hashCode();
        if (data != null) {
            for (String[] s : data) {
                value = 37 * value + s[0].hashCode();
                value = 37 * value + s[1].hashCode();
            }
        }
        value = 37 * value + type;
        return value;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelMessage<");
        sb.append(hashCode() + ", " + source.getID() + ", " + messageID + ", ");
        if (data != null) {
            for (String[] s : data) {
                sb.append(Arrays.toString(s) + "/");
            }
        }
        sb.append(", " + type + " >");
        return sb.toString();
    }

    
    public static String getXMLElementTagName() {
        return "modelMessage";
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("source", source.getID());
        out.writeAttribute("display", display.getID());
        out.writeAttribute("type", String.valueOf(type));
        out.writeAttribute("messageID", messageID);
        out.writeAttribute("hasBeenDisplayed", String.valueOf(beenDisplayed));
        toArrayElement("data", new String[][] {}, out);
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        messageID = in.getAttributeValue(null, "messageID");
        type = getAttribute(in, "type", DEFAULT);
        beenDisplayed = Boolean.parseBoolean(in.getAttributeValue(null, "hasBeenDisplayed"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("data")) {
                data =  readFromArrayElement("data", in, new String[0][0]);
            }
        }

        in.nextTag();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @param game a <code>Game</code> value
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXML(XMLStreamReader in, Game game) throws XMLStreamException {
        messageID = in.getAttributeValue(null, "messageID");
        type = getAttribute(in, "type", DEFAULT);
        beenDisplayed = Boolean.parseBoolean(in.getAttributeValue(null, "hasBeenDisplayed"));

        source = game.getFreeColGameObject(in.getAttributeValue(null, "type"));
        String displayString = in.getAttributeValue(null, "source");
        if (displayString != null) {
            if (game.getFreeColGameObject(displayString) != null) {
                display = game.getFreeColGameObject(displayString);
            } else if (FreeCol.getSpecification().getType(displayString) != null) {
                display = FreeCol.getSpecification().getType(displayString);
            }
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("data")) {
                data =  readFromArrayElement("data", in, new String[0][0]);
            }
        }

        in.nextTag();

    }

}
