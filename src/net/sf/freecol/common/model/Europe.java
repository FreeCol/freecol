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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Unit.UnitState;

/**
 * Represents Europe in the game. Each <code>Player</code> has it's
 * own <code>Europe</code>.
 *
 * <br><br>
 *
 * <p>In Europe, you can recruit, train and purchase new units. You
 * can also equip units, as well as sell and buy goods.
 */
public class Europe extends UnitLocation implements Ownable, Named {

    private static final Logger logger = Logger.getLogger(Europe.class.getName());

    private static final int RECRUIT_PRICE_INITIAL = 200;

    private static final int LOWER_CAP_INITIAL = 80;

    public static final String UNIT_CHANGE = "unitChange";

    /**
     * This array represents the types of the units that can be recruited in
     * Europe. They correspond to the slots that can be seen in the gui and that
     * can be used to communicate with the server/client. The array holds
     * exactly 3 elements and element 0 corresponds to recruit slot 1.
     */
    private UnitType[] recruitables = { null, null, null };
    public static final int RECRUIT_COUNT = 3;

    public static enum MigrationType {
        NORMAL,     // Unit decided to migrate
        RECRUIT,    // Player is paying
        FOUNTAIN    // As a result of a Fountain of Youth discovery
    }


    protected java.util.Map<UnitType, Integer> unitPrices
        = new HashMap<UnitType, Integer>();

    private int recruitPrice;

    private int recruitLowerCap;

    private Player owner;

    private final FeatureContainer featureContainer = new FeatureContainer();


    /**
     * Constructor for ServerEurope.
     */
    protected Europe() {
        // empty constructor
    }

    /**
     * Constructor for ServerEurope.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> that will be using this object of
     *            <code>Europe</code>.
     */
    protected Europe(Game game, Player owner) {
        super(game);
        this.owner = owner;

        recruitPrice = RECRUIT_PRICE_INITIAL;
        recruitLowerCap = LOWER_CAP_INITIAL;
    }

    /**
     * Initializes this object from an XML-representation of this object.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public Europe(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Initiates a new <code>Europe</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Europe(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets the feature container for this Europe object.
     *
     * @return The <code>FeatureContainer</code>.
     */
    @Override
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    /**
     * Checks if there is a useable carrier unit with a specified
     * minimum amount of space available docked in this European port.
     *
     * @param space The amount of space to require.
     * @return True if there is a suitable unit present.
     * @see Unit#isCarrier
     */
    public boolean hasCarrierWithSpace(int space) {
        for (Unit u : getUnitList()) {
            if (u.isCarrier()
                && !u.isUnderRepair()
                && u.getSpaceLeft() >= space) return true;
        }
        return false;
    }

    /**
     * Return true if this Europe could build at least one item of the
     * given EquipmentType.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBuildEquipment(EquipmentType equipmentType) {
        for (AbstractGoods requiredGoods : equipmentType.getGoodsRequired()) {
            GoodsType goodsType = requiredGoods.getType();
            if (!(getOwner().canTrade(goodsType)
                  && getOwner().checkGold(getOwner().getMarket().getBidPrice(goodsType, requiredGoods.getAmount())))) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns true if not all recruitables are of the same type.
     *
     * @return a <code>boolean</code> value
     */
    public boolean recruitablesDiffer() {
        return !(recruitables[0].equals(recruitables[1]) && recruitables[0].equals(recruitables[2]));
    }

    /**
     * Gets the type of the recruitable in Europe at the given slot.
     *
     * @param slot The slot of the recruitable whose type needs to be returned.
     *            Should be 0, 1 or 2. NOTE - used to be 1, 2 or 3 and was
     *            called with 1-3 by some classes and 0-2 by others, the method
     *            itself expected 0-2.
     * @return The type of the recruitable in Europe at the given slot.
     * @exception IllegalArgumentException if the given <code>slot</code> does
     *                not exist.
     */
    public UnitType getRecruitable(int slot) {
        if ((slot >= 0) && (slot < RECRUIT_COUNT)) {
            return recruitables[slot];
        }
        throw new IllegalArgumentException("Wrong recruitement slot: " + slot);
    }

    /**
     * Sets the type of the recruitable in Europe at the given slot to the given
     * type.
     *
     * @param slot The slot of the recruitable whose type needs to be set.
     *            Should be 0, 1 or 2. NOTE - changed in order to match
     *            getRecruitable above!
     * @param type The new type for the unit at the given slot in Europe. Should
     *            be a valid unit type.
     */
    public void setRecruitable(int slot, UnitType type) {
        // Note - changed in order to match getRecruitable
        if (slot >= 0 && slot < RECRUIT_COUNT) {
            recruitables[slot] = type;
        } else {
            logger.warning("setRecruitable: invalid slot(" + slot + ") given.");
        }
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     *
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    public boolean add(Locatable locatable) {
        boolean result = super.add(locatable);
        if (result && locatable instanceof Unit) {
            Unit unit = (Unit) locatable;
            unit.setState((unit.canCarryUnits()) ? UnitState.ACTIVE
                : UnitState.SENTRY);
        }
        return result;
    }

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     *
     * @param locatable The <code>Locatable</code> to test the addabillity of.
     * @return <i>true</i>.
     */
    public boolean canAdd(Locatable locatable) {
        if (locatable instanceof Goods) {
            return true;
        } else {
            return super.canAdd(locatable);
        }
    }

    /**
     * Returns the price of a unit in Europe.
     *
     * @param unitType The type of unit of which you need the price.
     * @return The price of this unit when trained in Europe.
     *         'UNDEFINED' is returned in case the unit cannot be
     *         bought.
     */
    public int getUnitPrice(UnitType unitType) {
        Integer price = unitPrices.get(unitType);
        if (price != null) {
            return price.intValue();
        } else {
            return unitType.getPrice();
        }
    }

    /**
     * Gets the current price for a recruit.
     *
     * @return The current price of the recruit in this <code>Europe</code>.
     */
    public int getRecruitPrice() {
        int required = owner.getImmigrationRequired();
        int immigration = owner.getImmigration();
        int difference = Math.max(required - immigration, 0);
        return Math.max((recruitPrice * difference) / required, recruitLowerCap);
    }

    /**
     * Increases the base price and lower cap for recruits.
     * Only called from the server side.
     */
    public void increaseRecruitmentDifficulty() {
        Specification spec = getSpecification();
        recruitPrice += spec.getIntegerOption("model.option.recruitPriceIncrease").getValue();
        recruitLowerCap += spec.getIntegerOption("model.option.lowerCapIncrease").getValue();
    }

    /**
     * Gets the <code>Player</code> using this <code>Europe</code>.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership of this
     *            {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.key(getNameKey());
    }

    /**
     * Returns the name of the owner's home port.
     *
     * @return The name of this location.
     */
    public String getNameKey() {
        return getOwner().getEuropeNameKey();
    }


    // Serialization

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
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        super.writeAttributes(out);
        for (int index = 0; index < recruitables.length; index++) {
            if (recruitables[index] != null) {
                out.writeAttribute("recruit" + index, recruitables[index].getId());
            }
        }
        out.writeAttribute("recruitPrice", Integer.toString(recruitPrice));
        out.writeAttribute("recruitLowerCap", Integer.toString(recruitLowerCap));
        out.writeAttribute("owner", owner.getId());

        for (Entry<UnitType, Integer> entry : unitPrices.entrySet()) {
            out.writeStartElement("unitPrice");
            out.writeAttribute("unitType", entry.getKey().getId());
            out.writeAttribute("price", entry.getValue().toString());
            out.writeEndElement();
        }

        super.writeChildren(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        Specification spec = getSpecification();

        // @compat 0.10.0
        if (!hasAbility("model.ability.dressMissionary")) {
            addAbility(new Ability("model.ability.dressMissionary"));
        }
        // end compatibility code

        for (int index = 0; index < recruitables.length; index++) {
            String unitTypeId = in.getAttributeValue(null, "recruit" + index);
            if (unitTypeId != null) {
                recruitables[index] = spec.getUnitType(unitTypeId);
            }
        }

        owner = getFreeColGameObject(in, "owner", Player.class);

        recruitPrice = getAttribute(in, "recruitPrice", RECRUIT_PRICE_INITIAL);

        recruitLowerCap = getAttribute(in, "recruitLowerCap", LOWER_CAP_INITIAL);

        unitPrices.clear();
        readChildren(in);
    }

    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (in.getLocalName().equals(UNITS_TAG_NAME)) {
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                super.readChild(in);
            }
            // @compat 0.10.1
            // Sometimes units in a Europe element have a missing
            // location.  It should always be this Europe instance.
            for (Unit u : getUnitList()) {
                if (u.getLocation() == null) u.setLocationNoUpdate(this);
            }
            // end compatibility code
        } else if (in.getLocalName().equals("unitPrice")) {
            String unitTypeId = in.getAttributeValue(null, "unitType");
            Integer price = new Integer(in.getAttributeValue(null, "price"));
            unitPrices.put(getSpecification().getUnitType(unitTypeId), price);
            in.nextTag(); // close "unitPrice" tag
        } else {
            super.readChild(in);
        }
    }

    /**
     * Partial writer, so that "remove" messages can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that "remove" messages can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    protected void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Europe";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "europe".
     */
    public static String getXMLElementTagName() {
        return "europe";
    }
}
