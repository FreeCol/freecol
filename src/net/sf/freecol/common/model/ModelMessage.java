/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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


/**
 * Contains a message about a change in the model.
 */
public class ModelMessage extends StringTemplate {

    /** Constants describing the type of message. */
    public static enum MessageType {

        DEFAULT(null),
        WARNING("model.option.guiShowWarning"),
        SONS_OF_LIBERTY("model.option.guiShowSonsOfLiberty"),
        GOVERNMENT_EFFICIENCY("model.option.guiShowGovernmentEfficiency"),
        WAREHOUSE_CAPACITY("model.option.guiShowWarehouseCapacity"),
        UNIT_IMPROVED("model.option.guiShowUnitImproved"),
        UNIT_DEMOTED("model.option.guiShowUnitDemoted"),
        UNIT_LOST("model.option.guiShowUnitLost"),
        UNIT_ADDED("model.option.guiShowUnitAdded"),
        BUILDING_COMPLETED("model.option.guiShowBuildingCompleted"),
        FOREIGN_DIPLOMACY("model.option.guiShowForeignDiplomacy"),
        MARKET_PRICES("model.option.guiShowMarketPrices"),
        LOST_CITY_RUMOUR(null), // Displayed during the turn
        MISSING_GOODS("model.option.guiShowMissingGoods"),
        TUTORIAL("model.option.guiShowTutorial"),
        COMBAT_RESULT(null), // No option, always display
        GIFT_GOODS("model.option.guiShowGifts"),
        DEMANDS("model.option.guiShowDemands"),
        GOODS_MOVEMENT("model.option.guiShowGoodsMovement");

        private String optionName;

        MessageType(String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }
    }

    private String ownerId; /* deprecated */
    private String sourceId;
    private String displayId;
    private MessageType messageType;
    private boolean beenDisplayed = false;


    public ModelMessage() {
        // empty constructor
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the
     *               message should be associated with.
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
     * @param source The source of the message. This is what the
     *               message should be associated with.
     */
    public ModelMessage(MessageType messageType, String id, FreeColGameObject source) {
        this(messageType, id, source, getDefaultDisplay(messageType, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     */
    public ModelMessage(String id, FreeColGameObject source) {
        this(MessageType.DEFAULT, id, source, getDefaultDisplay(MessageType.DEFAULT, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The ID of the message to display.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     * @param display The object to display.
     */
    public ModelMessage(MessageType messageType, String id, FreeColGameObject source, FreeColObject display) {
        super(id, TemplateType.TEMPLATE);
        this.messageType = messageType;
        this.sourceId = source.getId();
        this.displayId = (display != null) ? display.getId() : source.getId();
        this.ownerId = null;
    }

    /**
     * Set the <code>DefaultId</code> value.
     *
     * @param newDefaultId The new DefaultId value.
     * @return a <code>ModelMessage</code> value
     */
    @Override
    public final ModelMessage setDefaultId(final String newDefaultId) {
        super.setDefaultId(newDefaultId);
        return this;
    }

    /**
     * Returns the default display object for the given type.
     *
     * @param messageType The type to find the default display object for.
     * @param source The source object
     * @return An object to be displayed for the message.
     */
    static private FreeColObject getDefaultDisplay(MessageType messageType,
                                                   FreeColGameObject source) {
        FreeColObject o = null;
        switch (messageType) {
        case SONS_OF_LIBERTY:
        case GOVERNMENT_EFFICIENCY:
            o = source.getSpecification().getGoodsType("model.goods.bells");
            break;
        case UNIT_IMPROVED:
        case UNIT_DEMOTED:
        case UNIT_LOST:
        case UNIT_ADDED:
        case LOST_CITY_RUMOUR:
        case COMBAT_RESULT:
        case DEMANDS:
        case GOODS_MOVEMENT:
            o = source;
            break;
        case BUILDING_COMPLETED:
            o = source.getSpecification().getGoodsType("model.goods.hammers");
            break;
        case DEFAULT:
        case WARNING:
        case WAREHOUSE_CAPACITY:
        case FOREIGN_DIPLOMACY:
        case MARKET_PRICES:
        case MISSING_GOODS:
        case TUTORIAL:
        case GIFT_GOODS:
        default:
            if (source instanceof Player) {
                o = source;
            }
            break;
        }
        return o;
    }


    /**
     * Checks if this <code>ModelMessage</code> has been displayed.
     *
     * @return <i>true</i> if this <code>ModelMessage</code> has been
     * displayed.
     * @see #setBeenDisplayed
     */
    public boolean hasBeenDisplayed() {
        return beenDisplayed;
    }

    /**
     * Sets the <code>beenDisplayed</code> value of this
     * <code>ModelMessage</code>.  This is used to avoid showing the
     * same message twice.
     *
     * @param beenDisplayed Should be set to <code>true</code> after the
     *       message has been displayed.
     */
    public void setBeenDisplayed(boolean beenDisplayed) {
        this.beenDisplayed = beenDisplayed;
    }

    /**
     * Gets the ID of the source of the message.
     *
     * @return The source of the message.
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Sets the ID of the source object.
     *
     * @param sourceId A new source ID.
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Gets the ID of the object to display.
     *
     * @return The ID of the object to display.
     */
    public String getDisplayId() {
        return displayId;
    }

    /**
     * Sets the ID of the object to display.
     *
     * @param displayId A new display ID.
     */
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    /**
     * Gets the messageType of the message to display.
     *
     * @return The messageType.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Sets the type of the message.
     *
     * @param messageType The new messageType.
     */
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getMessageTypeName() {
        return "model.message." + messageType.toString();
    }

    /**
     * Switch the source (and display if it is the same) to a new
     * object.  Called when an object becomes invalid.
     *
     * @param newSource A new source.
     */
    public void divert(FreeColGameObject newSource) {
        if (displayId == sourceId) displayId = newSource.getId();
        sourceId = newSource.getId();
    }

    /**
     * Compatibility hack.  Do not use.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Compatibility hack.  Do not use.
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
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
     * Add a new key and replacement to the StringTemplate. The
     * replacement must be a proper name. This is only possible if the
     * StringTemplate is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param object a <code>FreeColObject</code> value
     * @return a <code>ModelMessage</code> value
     */
    public ModelMessage addName(String key, FreeColObject object) {
        super.addName(key, object);
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
     * Checks if this <code>ModelMessage</code> is equal to another
     * <code>ModelMessage</code>.
     *
     * @param o The <code>Object</code> to compare.
     * @return <i>true</i> if the sources, message IDs and data are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ModelMessage) {
            ModelMessage m = (ModelMessage) o;
            if (sourceId.equals(m.sourceId)
                && getId().equals(m.getId())
                && messageType == m.messageType) {
                return super.equals(m);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int value = 1;
        value = 37 * value + sourceId.hashCode();
        value = 37 * value + getId().hashCode();
        value = 37 * value + messageType.ordinal();
        value = 37 * value + super.hashCode();
        return value;
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (ownerId != null) {
            out.writeAttribute("owner", ownerId);
        }
        out.writeAttribute("source", sourceId);
        if (displayId != null) {
            out.writeAttribute("display", displayId);
        }
        out.writeAttribute("messageType", messageType.toString());
        out.writeAttribute("hasBeenDisplayed", String.valueOf(beenDisplayed));
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        // Remove this when the 0.9.x save hack (in Game.java) is gone.
        ownerId = in.getAttributeValue(null, "owner");

        messageType = Enum.valueOf(MessageType.class, getAttribute(in, "messageType", MessageType.DEFAULT.toString()));
        beenDisplayed = Boolean.parseBoolean(in.getAttributeValue(null, "hasBeenDisplayed"));
        sourceId = in.getAttributeValue(null, "source");
        displayId = in.getAttributeValue(null, "display");

        super.readChildren(in);
    }

    /**
     * Debug helper.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelMessage<" + hashCode()
            + ", ");
        sb.append(((sourceId == null) ? "null" : sourceId) + ", ");
        sb.append(((displayId == null) ? "null" : displayId) + ", ");
        sb.append(super.toString());
        sb.append(", " + messageType + " >");
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "modelMessage"
     */
    public static String getXMLElementTagName() {
        return "modelMessage";
    }
}
