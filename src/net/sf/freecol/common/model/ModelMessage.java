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
    public static enum MessageType { 
            DEFAULT,
            WARNING,
            SONS_OF_LIBERTY,
            GOVERNMENT_EFFICIENCY,
            WAREHOUSE_CAPACITY,
            UNIT_IMPROVED,
            UNIT_DEMOTED,
            UNIT_LOST,
            UNIT_ADDED,
            BUILDING_COMPLETED,
            FOREIGN_DIPLOMACY,
            MARKET_PRICES,
            LOST_CITY_RUMOUR,
            GIFT_GOODS,
            MISSING_GOODS,
            TUTORIAL,
            COMBAT_RESULT }

    private Player owner;
    private FreeColGameObject source;
    private Tile sourceTile;
    private FreeColObject display;
    private MessageType type;
    private String[] data;
    private boolean beenDisplayed = false;


    public ModelMessage() {
    }


    private static String[] convertData(String[][] data) {
        if (data == null) {
            return null;
        }
        String[] strings = new String[data.length * 2];
        for (int index = 0; index < data.length; index++) {
            strings[index * 2] = data[index][0];
            strings[index * 2 + 1] = data[index][1];
        }
        return strings;
    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param id The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @param type The type of this model message.
    * @param display The Object to display.
    */
    @Deprecated
    public ModelMessage(FreeColGameObject source, String id, String[][] data, MessageType type, FreeColObject display) {
        this(source, type, display, id, convertData(data));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     * @param type The type of this model message.
     * @param display The Object to display.
     * @param id The ID of the message to display.
     * @param data Contains data to be displayed in the message or <i>null</i>.
     */
    public ModelMessage(FreeColGameObject source, MessageType type, FreeColObject display,
                        String id, String... data) {
        this.source = source;
        this.sourceTile = null;
        if (source instanceof Unit) {
            Unit u = (Unit) source;
            this.owner = u.getOwner();
            if (u.getTile() != null) {
                this.sourceTile = u.getTile();
            } else if (u.getColony() != null) {
                this.sourceTile = u.getColony().getTile();
            } else if (u.getIndianSettlement() != null) {
                this.sourceTile = u.getIndianSettlement().getTile();
            }
        } else if (source instanceof Settlement) {
            this.owner = ((Settlement) source).getOwner();
            this.sourceTile = ((Settlement) source).getTile();
        } else if (source instanceof Europe) {
            this.owner = ((Europe) source).getOwner();
        } else if (source instanceof Player) {
            this.owner = (Player) source;
        } else if (source instanceof Ownable) {
            this.owner = ((Ownable) source).getOwner();
        }

        setId(id);
        this.data = data;
        this.type = type;
        if (display == null) {
            this.display = getDefaultDisplay(type, source);
        } else {
            this.display = display;
        }
        verifyFields();
    }

    /**
     * Checks that all the fields as they are set in the constructor are valid.
     */
    private void verifyFields() {
        if (getId() == null) {
            throw new IllegalArgumentException("ModelMessage should not have a null id.");
        }
        if (source == null) {
            throw new IllegalArgumentException("ModelMessage with ID " + this.getId() + " should not have a null source.");
        }
        if (owner == null) {
            throw new IllegalArgumentException("ModelMessage with ID " + this.getId() + " should not have a null owner.");
        }
        if (!(display == null ||
              display instanceof FreeColGameObject ||
              display instanceof FreeColGameObjectType)) {
            throw new IllegalArgumentException("The display must be a FreeColGameObject or FreeColGameObjectType!");
        }

        if (data != null && data.length % 2 != 0) {
            throw new IllegalArgumentException("Data length must be multiple of 2.");
        }
    }
    
    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     * @param id The ID of the message to display.
     * @param data Contains data to be displayed in the message or <i>null</i>.
     * @param type The type of this model message.
     */
     public ModelMessage(FreeColGameObject source, String id, String[][] data, MessageType type) {
         this(source, type, getDefaultDisplay(type, source), id, convertData(data));

     }

     /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     * @param id The ID of the message to display.
     * @param data Contains data to be displayed in the message or <i>null</i>.
     */
     public ModelMessage(FreeColGameObject source, String id, String[][] data) {
         this(source, MessageType.DEFAULT, getDefaultDisplay(MessageType.DEFAULT, source), id, convertData(data));
     }
     
    /**
     * Returns the default display object for the given type.
     * @param type the type for which to find the default display object.
     * @param source the source object
     * @return an object to be displayed for the message. 
     */
    static private FreeColObject getDefaultDisplay(MessageType type, FreeColGameObject source) {
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
        case COMBAT_RESULT:
            newDisplay = source;
            break;
        case BUILDING_COMPLETED:
            newDisplay = FreeCol.getSpecification().getGoodsType("model.goods.hammers");
            break;
        case TUTORIAL:
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
     * Returns the data to be displayed in the message.
     * @return The data as a <code>String[][]</code> or <i>null</i>
     *         if no data applies.
     */
    public String[] getData() {
        return data;
    }

    /**
     * Gets the type of the message to display.   
     * @return The type. 
     */
    public MessageType getType() {
        return type;

    }


    public String getTypeName() {
        return Messages.message("model.message." + type.toString());
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
        return owner;
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
        if (!Arrays.equals(data, m.data)) {
            return false;
        }
        return ( source.equals(m.source)
                && getId().equals(m.getId())
                && type == m.type );
    }
    
    @Override
    public int hashCode() {
        int value = 1;
        value = 37 * value + source.hashCode();
        value = 37 * value + getId().hashCode();
        if (data != null) {
            for (String s : data) {
                value = 37 * value + s.hashCode();
            }
        }
        value = 37 * value + type.ordinal();
        return value;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelMessage<");
        sb.append(hashCode() + ", " + source.getId() + ", " + getId() + ", ");
        if (data != null) {
            for (String s : data) {
                sb.append(s + "/");
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
        out.writeAttribute("owner", owner.getId());
        if (source!=null) {
            if (source instanceof Unit && ((Unit)source).isDisposed())
                out.writeAttribute("source", sourceTile.getId());
            else if (source instanceof Settlement && ((Settlement)source).isDisposed())
                out.writeAttribute("source", sourceTile.getId());
            else
                out.writeAttribute("source", source.getId());
        }
        if (display != null) {
            out.writeAttribute("display", display.getId());
        }
        out.writeAttribute("type", type.toString());
        out.writeAttribute("ID", getId());
        out.writeAttribute("hasBeenDisplayed", String.valueOf(beenDisplayed));
        if (data != null) {
            toArrayElement("data", data, out);
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        type = Enum.valueOf(MessageType.class, getAttribute(in, "type", MessageType.DEFAULT.toString()));
        beenDisplayed = Boolean.parseBoolean(in.getAttributeValue(null, "hasBeenDisplayed"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("data")) {
                data =  readFromArrayElement("data", in, new String[0]);
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
        setId(in.getAttributeValue(null, "ID"));
        
        String ownerPlayer = in.getAttributeValue(null, "owner");
        owner = (Player)game.getFreeColGameObject(ownerPlayer);
         
        type = Enum.valueOf(MessageType.class, getAttribute(in, "type", MessageType.DEFAULT.toString()));
        beenDisplayed = Boolean.parseBoolean(in.getAttributeValue(null, "hasBeenDisplayed"));

        String sourceString = in.getAttributeValue(null, "source");
        source = game.getFreeColGameObject(sourceString);
        
        String displayString = in.getAttributeValue(null, "display");
        if (displayString != null) {
            // usually referring to a unit, colony or player
            display = game.getFreeColGameObject(displayString);
            if (display==null) {
                // either the unit, colony has been killed/destroyed
                // or the message refers to goods or building type
                try {
                    display = FreeCol.getSpecification().getType(displayString);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("data")) {
                data =  readFromArrayElement("data", in, new String[0]);
            }
        }

        verifyFields();
        owner.addModelMessage(this);
    }

}
