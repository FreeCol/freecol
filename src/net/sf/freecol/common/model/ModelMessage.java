/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;
import static net.sf.freecol.common.util.StringUtils.*;

import org.w3c.dom.Element;


/**
 * Contains a message about a change in the model.
 */
public class ModelMessage extends StringTemplate {

    /** Constants describing the type of message. */
    public static enum MessageType implements Named {

        DEFAULT(""),
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

        private final String optionName;

        MessageType(String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        /**
         * Get a message key for this message type.
         *
         * @return A message key.
         */
        private String getKey() {
            return "messageType." + getEnumKey(this);
        }

        // Implement Named

        /**
         * {@inheritDoc}
         */
        public String getNameKey() {
            return Messages.nameKey("model." + getKey());
        }
    }

    private String sourceId;
    private String displayId;
    private MessageType messageType;
    private boolean beenDisplayed = false;


    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param id The object identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     */
    public ModelMessage(String id, FreeColGameObject source) {
        this(MessageType.DEFAULT, id, source,
             getDefaultDisplay(MessageType.DEFAULT, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param id The object identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     * @param display The <code>FreeColObject</code> to display.
     */
    public ModelMessage(String id, FreeColGameObject source,
                        FreeColObject display) {
        this(MessageType.DEFAULT, id, source, display);
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The object identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     */
    public ModelMessage(MessageType messageType, String id,
                        FreeColGameObject source) {
        this(messageType, id, source, getDefaultDisplay(messageType, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The object identifier.
     * @param defaultId The default identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     */
    public ModelMessage(MessageType messageType, String id, String defaultId,
                        FreeColGameObject source) {
        this(messageType, id, defaultId, source,
             getDefaultDisplay(messageType, source));
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The object identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     * @param display The <code>FreeColObject</code> to display.
     */
    public ModelMessage(MessageType messageType, String id,
                        FreeColGameObject source, FreeColObject display) {
        this(messageType, id, null, source, display);
    }

    /**
     * Creates a new <code>ModelMessage</code>.
     *
     * @param messageType The type of this model message.
     * @param id The object identifier.
     * @param defaultId The default identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     * @param display The <code>FreeColObject</code> to display.
     */
    public ModelMessage(MessageType messageType, String id, String defaultId,
                        FreeColGameObject source, FreeColObject display) {
        super(id, defaultId, TemplateType.TEMPLATE);
        this.messageType = messageType;
        this.sourceId = source.getId();
        this.displayId = (display != null) ? display.getId() : source.getId();
    }

    /**
     * Creates a new model message by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ModelMessage(FreeColXMLReader xr) throws XMLStreamException {

        readFromXML(xr);
    }

    /**
     * Create a new model message by reading a element.
     *
     * @param element The <code>Element</code> to read from.
     */
    public ModelMessage(Element element) {
        readFromXMLElement(element);
    }


    /**
     * Gets the source of the message.
     *
     * @return The identifier for the source of the message.
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Sets the source object.
     *
     * @param sourceId A new source object identifier.
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Gets the object to display.
     *
     * @return The identifier of the object to display.
     */
    public String getDisplayId() {
        return displayId;
    }

    /**
     * Sets the object to display.
     *
     * @param displayId A new display object identifier.
     */
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    /**
     * Switch the source (and display if it is the same) to a new
     * object.  Called when the source object becomes invalid.
     *
     * @param newSource A new source.
     */
    public void divert(FreeColGameObject newSource) {
        if (Utils.equals(displayId, sourceId)) displayId = newSource.getId();
        sourceId = newSource.getId();
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

    /**
     * Has this message been displayed?
     *
     * @return True if this message has been displayed.
     */
    public boolean hasBeenDisplayed() {
        return beenDisplayed;
    }

    /**
     * Sets whether this message has been displayed.
     *
     * @param beenDisplayed The new displayed state.
     */
    public void setBeenDisplayed(boolean beenDisplayed) {
        this.beenDisplayed = beenDisplayed;
    }


    /**
     * Gets the default display object for the given type.
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
            if (source instanceof Player) o = source;
            break;
        }
        return o;
    }

    /**
     * Get a key for a message that might be ignored.
     *
     * @return A key, or null if the message should not be ignored.
     */
    public String getIgnoredMessageKey() {
        switch (getMessageType()) {
        case WAREHOUSE_CAPACITY:
            String key = getSourceId();
            switch (getTemplateType()) {
            case TEMPLATE:
                for (String k : getKeys()) {
                    if ("%goods%".equals(k)) {
                        key += "-" + getReplacement(k).getId();
                    }
                }
                break;
            }
            return key;
        default:
            break;
        }
        return null;
    }

    /**
     * Split a message into a list of text and link objects.
     *
     * @param player The <code>Player</code> who will see the result.
     * @return A list of strings and buttons.
     */
    public List<Object> splitLinks(Player player) {
        final FreeColGameObject source = player.getGame()
            .getMessageSource(this);

        // Build a list of objects, initially containing just the plain
        // text of the message.
        List<Object> result = new ArrayList<>();
        result.add(Messages.message(this));

        for (String key : getKeys()) {
            // Then for each key, check if it can be made into a link.
            // If not, ignore it.
            String val = Messages.message(getReplacement(key));
            Object b = Utility.getMessageButton(key, val, player, source);
            if (b == null) continue;

            // ...if so, find all instances of the replacement of the key
            // in the object list texts, and replace them with buttons.
            List<Object> next = new ArrayList<>();
            for (Object o : result) {
                if (o instanceof String) {
                    String str = (String)o;
                    int index, start = 0;
                    while ((index = str.indexOf(val, start)) >= 0) {
                        if (index > start) {
                            next.add(str.substring(start, index));
                        }
                        next.add(b);
                        start = index + val.length();
                    }
                    next.add(str.substring(start, str.length()));
                } else {
                    next.add(o);
                }
            }
            result = next;
        }
        return result;
    }

        
    // Quasi-override StringTemplate routines to return ModelMessages

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage add(String key, String value) {
        return (ModelMessage)super.add(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage add(String value) {
        return (ModelMessage)super.add(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addName(String key, String value) {
        return (ModelMessage)super.addName(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addName(String value) {
        return (ModelMessage)super.addName(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addName(String key, FreeColObject object) {
        return (ModelMessage)super.addName(key, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addNamed(String key, Named named) {
        return (ModelMessage)super.addNamed(key, named);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addAmount(String key, Number amount) {
        return (ModelMessage)super.addAmount(key, amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addStringTemplate(String key, StringTemplate template) {
        return (ModelMessage)super.addStringTemplate(key, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelMessage addStringTemplate(StringTemplate template) {
        return (ModelMessage)super.addStringTemplate(template);
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ModelMessage) {
            ModelMessage m = (ModelMessage)o;
            return sourceId.equals(m.sourceId)
                && messageType == m.messageType
                && super.equals(m);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + sourceId.hashCode();
        return 37 * hash + messageType.ordinal();
    }


    // Serialization

    private static final String DISPLAY_TAG = "display";
    private static final String HAS_BEEN_DISPLAYED_TAG = "hasBeenDisplayed";
    private static final String MESSAGE_TYPE_TAG = "messageType";
    private static final String SOURCE_TAG = "source";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(SOURCE_TAG, sourceId);

        if (displayId != null) xw.writeAttribute(DISPLAY_TAG, displayId);

        xw.writeAttribute(MESSAGE_TYPE_TAG, messageType);

        xw.writeAttribute(HAS_BEEN_DISPLAYED_TAG, beenDisplayed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        sourceId = xr.getAttribute(SOURCE_TAG, (String)null);

        displayId = xr.getAttribute(DISPLAY_TAG, (String)null);

        messageType = xr.getAttribute(MESSAGE_TYPE_TAG, 
                                      MessageType.class, MessageType.DEFAULT);

        beenDisplayed = xr.getAttribute(HAS_BEEN_DISPLAYED_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ModelMessage<").append(hashCode())
            .append(", ").append((sourceId == null) ? "null" : sourceId)
            .append(", ").append((displayId == null) ? "null" : displayId)
            .append(", ").append(super.toString())
            .append(", ").append(messageType)
            .append(" >");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "modelMessage"
     */
    public static String getXMLElementTagName() {
        return "modelMessage";
    }
}
