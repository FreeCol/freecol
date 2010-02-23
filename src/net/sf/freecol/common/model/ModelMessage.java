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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

/**
 * Contains a message about a change in the model.
 */
public class ModelMessage extends StringTemplate {

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
        COMBAT_RESULT,
        ACCEPTED_DEMANDS,
        REJECTED_DEMANDS
    }

    private Player owner;
    private FreeColGameObject source;
    private Location sourceLocation;
    private FreeColObject display;
    private MessageType messageType;
    private boolean beenDisplayed = false;


    public ModelMessage() {
        // empty constructor
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     * @param display The Object to display.
     */
    public ModelMessage(String id, FreeColGameObject source, FreeColObject display) {
        this(MessageType.DEFAULT, id, source, display);
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     */
    public ModelMessage(MessageType messageType, String id, FreeColGameObject source) {
        this(messageType, id, source, getDefaultDisplay(messageType, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     */
    public ModelMessage(String id, FreeColGameObject source) {
        this(MessageType.DEFAULT, id, source, getDefaultDisplay(MessageType.DEFAULT, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the message should be
     *               associated with. In addition, the owner of the source is the
     *               player getting the message.
     * @param display The Object to display.
     */
    public ModelMessage(MessageType messageType, String id, FreeColGameObject source, FreeColObject display) {
        super(id, TemplateType.TEMPLATE);
        this.messageType = messageType;
        this.source = source;
        this.display = display;

        this.sourceLocation = null;
        if (source instanceof Unit) {
            Unit u = (Unit) source;
            this.owner = u.getOwner();
            if (u.getTile() != null) {
                this.sourceLocation = u.getTile();
            } else if (u.getColony() != null) {
                this.sourceLocation = u.getColony().getTile();
            } else if (u.getIndianSettlement() != null) {
                this.sourceLocation = u.getIndianSettlement().getTile();
            } else if (u.isInEurope()) {
                this.sourceLocation = u.getOwner().getEurope();
            }
        } else if (source instanceof Settlement) {
            this.owner = ((Settlement) source).getOwner();
            this.sourceLocation = ((Settlement) source).getTile();
        } else if (source instanceof Europe) {
            this.owner = ((Europe) source).getOwner();
        } else if (source instanceof Player) {
            this.owner = (Player) source;
        } else if (source instanceof Ownable) {
            this.owner = ((Ownable) source).getOwner();
        }
    }

    /**
     * Returns the default display object for the given type.
     * @param messageType the type for which to find the default display object.
     * @param source the source object
     * @return an object to be displayed for the message. 
     */
    static private FreeColObject getDefaultDisplay(MessageType messageType, FreeColGameObject source) {
        FreeColObject newDisplay = null;
        switch(messageType) {
        case SONS_OF_LIBERTY:
        case GOVERNMENT_EFFICIENCY:
            newDisplay = FreeCol.getSpecification().getGoodsType("model.goods.bells");
            break;
        case LOST_CITY_RUMOUR:
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
     * Sets the source of the message.
     * @param newSource a new source for this message
     */
    public void setSource(FreeColGameObject newSource) {
        source = newSource;
    }
    
    /**
     * Gets the messageType of the message to display.   
     * @return The messageType. 
     */
    public MessageType getMessageType() {
        return messageType;

    }


    public String getMessageTypeName() {
        return "model.message." + messageType.toString();
    }


    /**
     * Gets the Object to display.
     * @return The Object to display.
     */
    public FreeColObject getDisplay() {
        return display;
    }

    /**
     * Sets the Object to display.
     * @param newDisplay the new object to display
     */
    public void setDisplay(FreeColGameObject newDisplay) {
        display = newDisplay;
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
     * Set the owner of this message.
     *
     * @param newOwner A <code>Player</code> to own this message.
     */
    public void setOwner(Player newOwner) {
        newOwner = owner;
    }


    /**
     * Add a new key and replacement to the ModelMessage. This is
     * only possible if the ModelMessage is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage add(String key, String value) {
        super.add(key, value);
        return this;
    }

    /**
     * Add a replacement value without a key to the ModelMessage.
     * This is only possible if the ModelMessage is of type LABEL.
     *
     * @param value a <code>String</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage add(String value) {
        super.add(value);
	return this;
    }

    /**
     * Add a new key and replacement to the ModelMessage. The
     * replacement must be a proper name. This is only possible if the
     * ModelMessage is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage addName(String key, String value) {
        super.addName(key, value);
        return this;
    }

    /**
     * Add a replacement value without a key to the ModelMessage.
     * The replacement must be a proper name.  This is only possible
     * if the ModelMessage is of type LABEL.
     *
     * @param value a <code>String</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage addName(String value) {
        super.addName(value);
	return this;
    }

    /**
     * Add a key and an integer value to replace it to this
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param amount an <code>int</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage addAmount(String key, int amount) {
        super.addAmount(key, amount);
        return this;
    }

    /**
     * Add a key and a StringTemplate to replace it to this
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param template a <code>StringTemplate</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage addStringTemplate(String key, StringTemplate template) {
        super.addStringTemplate(key, template);
	return this;
    }

    /**
     * Add a StringTemplate to this LABEL StringTemplate.
     *
     * @param template a <code>StringTemplate</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage addStringTemplate(StringTemplate template) {
        super.addStringTemplate(template);
	return this;
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
        /*
        if (!Arrays.equals(data, m.data)) {
            return false;
        }
        */
        return ( source.equals(m.source)
                && getId().equals(m.getId())
                && messageType == m.messageType );
    }
    
    @Override
    public int hashCode() {
        int value = 1;
        value = 37 * value + ((source == null) ? 0 : source.hashCode());
        value = 37 * value + getId().hashCode();
        /*
        if (data != null) {
            for (String s : data) {
                value = 37 * value + s.hashCode();
            }
        }
        */
        value = 37 * value + messageType.ordinal();
        return value;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelMessage<");
        sb.append(hashCode() + ", " + ((source == null) ? "null" : source.getId()) + ", " + getId() + ", "
                  + ((display==null) ? "null" : display.getId()) + ", ");
        /*
        if (data != null) {
            for (String s : data) {
                sb.append(s + "/");
            }
        }
        */
        sb.append(", " + messageType + " >");
        return sb.toString();
    }

    public static String getXMLElementTagName() {
        return "modelMessage";
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("owner", owner.getId());
        if (source != null) {
            if ((source instanceof Unit && ((Unit)source).isDisposed())
              ||(source instanceof Settlement && ((Settlement)source).isDisposed())) {
                if (sourceLocation==null) {
                    logger.warning("sourceLocation==null for source "+source.getId());
                    out.writeAttribute("source", owner.getId());
                } else {
                    out.writeAttribute("source", sourceLocation.getId());
                }
            } else {
                out.writeAttribute("source", source.getId());
            }
        }
        if (display != null) {
            out.writeAttribute("display", display.getId());
        }
        out.writeAttribute("messageType", messageType.toString());
        out.writeAttribute("hasBeenDisplayed", String.valueOf(beenDisplayed));
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @param game a <code>Game</code> value
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXML(XMLStreamReader in, Game game) throws XMLStreamException {
        super.readAttributes(in);
        
        String ownerPlayer = in.getAttributeValue(null, "owner");
        owner = (Player)game.getFreeColGameObject(ownerPlayer);
         
        messageType = Enum.valueOf(MessageType.class, getAttribute(in, "messageType", MessageType.DEFAULT.toString()));
        beenDisplayed = Boolean.parseBoolean(in.getAttributeValue(null, "hasBeenDisplayed"));

        String sourceString = in.getAttributeValue(null, "source");
        source = game.getFreeColGameObject(sourceString);
        if (source == null) {
            logger.warning("source null from string " + sourceString);
            source = owner;
        }
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
                    display = owner;
                    logger.warning("display null from string " + displayString);
                }
            }
        }

        super.readChildren(in);
        owner.addModelMessage(this);
    }

}
