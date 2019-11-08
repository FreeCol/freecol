/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Contains a message about a change in the model.
 */
public class ModelMessage extends StringTemplate {

    public static final String TAG = "modelMessage";

    /**
     * Constants categorizing the type of message.
     *
     * When this changes, be sure to update client-options.xml and the
     * messages file appropriately.
     */
    public static enum MessageType implements Named {
        DEFAULT(""),
        BUILDING_COMPLETED("model.option.guiShowBuildingCompleted"),
        COMBAT_RESULT(null), // No option, always display
        DEMANDS("model.option.guiShowDemands"),
        DISASTERS("model.option.guiShowDisasters"),
        FOREIGN_DIPLOMACY("model.option.guiShowForeignDiplomacy"),
        GIFT_GOODS("model.option.guiShowGifts"),
        GOODS_MOVEMENT("model.option.guiShowGoodsMovement"),
        GOVERNMENT_EFFICIENCY("model.option.guiShowGovernmentEfficiency"),
        LOST_CITY_RUMOUR(null), // Displayed during the turn
        MARKET_PRICES("model.option.guiShowMarketPrices"),
        MISSING_GOODS("model.option.guiShowMissingGoods"),
        SONS_OF_LIBERTY("model.option.guiShowSonsOfLiberty"),
        TUTORIAL("model.option.guiShowTutorial"),
        UNIT_ADDED("model.option.guiShowUnitAdded"),
        UNIT_ARRIVED("model.option.guiShowUnitArrived"),
        UNIT_DEMOTED("model.option.guiShowUnitDemoted"),
        UNIT_IMPROVED("model.option.guiShowUnitImproved"),
        UNIT_LOST("model.option.guiShowUnitLost"),
        UNIT_REPAIRED("model.option.guiShowUnitRepaired"),
        WAREHOUSE_CAPACITY("model.option.guiShowWarehouseCapacity"),
        WARNING("model.option.guiShowWarning");

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

    /** Compare messages by type. */
    public static final Comparator<ModelMessage> messageTypeComparator
        = (m1, m2) -> m1.getMessageType().ordinal()
                    - m2.getMessageType().ordinal();

    private String sourceId;
    private String displayId;
    private MessageType messageType;
    private boolean displayed = false;


    /**
     * Trivial constructor to allow creation with Game.newInstance.
     */
    public ModelMessage() {}
        
    /**
     * Creates a new {@code ModelMessage}.
     *
     * @param messageType The type of this model message.
     * @param id The object identifier.
     * @param defaultId The default identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     * @param display The {@code FreeColObject} to display.
     */
    public ModelMessage(MessageType messageType, String id, String defaultId,
                        FreeColGameObject source, FreeColObject display) {
        super(id, defaultId, TemplateType.TEMPLATE);

        this.messageType = messageType;
        this.sourceId = source.getId();
        this.displayId = (display != null) ? display.getId() : source.getId();
    }

    /**
     * Creates a new {@code ModelMessage}.
     *
     * @param messageType The type of this model message.
     * @param id The object identifier.
     * @param source The source of the message. This is what the
     *               message should be associated with.
     * @param display The {@code FreeColObject} to display.
     */
    public ModelMessage(MessageType messageType, String id,
                        FreeColGameObject source, FreeColObject display) {
        this(messageType, id, null, source, display);
    }

    /**
     * Creates a new {@code ModelMessage}.
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
     * Creates a new model message by reading a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ModelMessage(FreeColXMLReader xr) throws XMLStreamException {

        readFromXML(xr);
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
     * Get the option name for this message.
     *
     * @return The option name.
     */
    public String getOptionName() {
        return (this.messageType == null) ? null
            : this.messageType.getOptionName();
    }

    /**
     * Has this message been displayed?
     *
     * @return True if this message has been displayed.
     */
    public boolean getDisplayed() {
        return this.displayed;
    }

    /**
     * Sets whether this message has been displayed.
     *
     * @param displayed The new displayed state.
     */
    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    /**
     * Set a default identifier.
     *
     * @param id The new default identifier.
     * @return This {@code ModelMessage}.
     */
    public ModelMessage addDefaultId(String id) {
        return super.setDefaultId(id, ModelMessage.class);
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
            StringBuilder sb = new StringBuilder(64);
            sb.append(getSourceId());
            switch (getTemplateType()) {
            case TEMPLATE:
                StringTemplate t = getReplacement("%goods%");
                if (t != null) sb.append('-').append(t.getId());
                break;
            default:
                break;
            }
            return sb.toString();
        default:
            break;
        }
        return null;
    }

    /**
     * Split a message into a list of text and link objects.
     *
     * @param player The {@code Player} who will see the result.
     * @return A list of strings and buttons.
     */
    public List<Object> splitLinks(Player player) {
        final FreeColGameObject source = player.getGame()
            .getMessageSource(this);

        // Build a list of objects, initially containing just the plain
        // text of the message.
        List<Object> result = new ArrayList<>();
        result.add(Messages.message(this));

        for (SimpleEntry<String,StringTemplate> e : entryList()) {
            // Then for each key, check if it can be made into a link.
            // If not, ignore it.
            String key = e.getKey();
            String val = Messages.message(e.getValue());
            if (val == null) continue;
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

    /**
     * Get a comparator that sorts on the message source object.
     *
     * @param game The {@code Game} to look up source objects in.
     * @param specialized A map of specialized comparators keyed by class name.
     * @return The {@code Comparator}.
     */
    public static Comparator<ModelMessage> getSourceComparator(final Game game,
            final java.util.Map<String, Comparator<?>> specialized) {

        return new Comparator<ModelMessage>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(ModelMessage message1, ModelMessage message2) {
                String sourceId1 = message1.getSourceId();
                String sourceId2 = message2.getSourceId();
                if (Utils.equals(sourceId1, sourceId2)) {
                    return messageTypeComparator.compare(message1, message2);
                }
                FreeColGameObject source1 = game.getMessageSource(message1);
                FreeColGameObject source2 = game.getMessageSource(message2);
                int base = FreeColObject.getObjectClassIndex(source1)
                    - FreeColObject.getObjectClassIndex(source2);
                if (base == 0 && specialized != null && source1 != null) {
                    String name = source1.getClass().getName();
                    @SuppressWarnings("unchecked")
                    Comparator<FreeColObject> c
                        = (Comparator<FreeColObject>)specialized.get(name);
                    if (c != null) {
                        base = c.compare(source1, source2);
                    }
                }
                return base;
            }
        };
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        ModelMessage o = copyInCast(other, ModelMessage.class);
        if (o == null || !super.copyIn(o)) return false;
        this.sourceId = o.getSourceId();
        this.displayId = o.getDisplayId();
        this.messageType = o.getMessageType();
        this.displayed = o.getDisplayed();
        return true;
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

        xw.writeAttribute(HAS_BEEN_DISPLAYED_TAG, displayed);
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

        displayed = xr.getAttribute(HAS_BEEN_DISPLAYED_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ModelMessage) {
            ModelMessage other = (ModelMessage)o;
            return messageType == other.messageType
                && displayed == other.displayed
                && Utils.equals(sourceId, other.sourceId)
                && Utils.equals(displayId, other.displayId)
                && super.equals(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        if (sourceId != null) hash = 37 * hash + sourceId.hashCode();
        if (displayId != null) hash = 37 * hash + displayId.hashCode();
        hash = 37 * hash + messageType.ordinal();
        hash = 37 * hash + ((displayed) ? 1 : 0);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[ModelMessage ").append(hashCode())
            .append(' ').append((sourceId == null) ? "null" : sourceId)
            .append('/').append((displayId == null) ? "null" : displayId)
            .append(' ').append(messageType)
            .append(' ').append(displayed)
            .append(' ').append(super.toString())
            .append(']');
        return sb.toString();
    }
}
