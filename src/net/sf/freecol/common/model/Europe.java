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
 * Represents Europe in the game.  Each <code>Player</code> has it's
 * own <code>Europe</code>.
 *
 * In Europe, you can recruit, train and purchase new units.  You can
 * also equip units, as well as sell and buy goods.
 */
public class Europe extends UnitLocation implements Ownable, Named {

    private static final Logger logger = Logger.getLogger(Europe.class.getName());

    /** The initial recruit price. */
    private static final int RECRUIT_PRICE_INITIAL = 200;

    /** The initial lower bound on recruitment price. */
    private static final int LOWER_CAP_INITIAL = 80;

    public static final String UNIT_CHANGE = "unitChange";

    /** Reasons to migrate. */
    public static enum MigrationType {
        NORMAL,     // Unit decided to migrate
        RECRUIT,    // Player is paying
        FOUNTAIN,   // As a result of a Fountain of Youth discovery
        SURVIVAL    // Emergency autorecruit in server
    }

    /** The number of recruitable units. */
    public static final int RECRUIT_COUNT = 3;

    /**
     * This array represents the types of the units that can be recruited in
     * Europe. They correspond to the slots that can be seen in the gui and that
     * can be used to communicate with the server/client.  The array holds
     * exactly 3 elements and element 0 corresponds to recruit `slot' 1.
     */
    private UnitType[] recruitables = { null, null, null };

    /** Prices for trainable or purchasable units. */
    protected java.util.Map<UnitType, Integer> unitPrices
        = new HashMap<UnitType, Integer>();

    /** Current price to recruit a unit. */
    protected int recruitPrice;

    /** The lower bound on recruitment price. */
    protected int recruitLowerCap;

    /** The owner of this instance of Europe. */
    private Player owner;

    /** A feature container for this Europe's special features. */
    private final FeatureContainer featureContainer = new FeatureContainer();


    /**
     * Deliberately empty constructor for ServerEurope.
     */
    protected Europe() {}

    /**
     * Constructor for ServerEurope.
     *
     * @param game The enclosing <code>Game</code>.
     * @param owner The owning <code>Player</code>.
     */
    protected Europe(Game game, Player owner) {
        super(game);

        this.owner = owner;
        this.recruitPrice = RECRUIT_PRICE_INITIAL;
        this.recruitLowerCap = LOWER_CAP_INITIAL;
    }

    /**
     * Creates a new <code>Europe</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Europe(Game game, String id) {
        super(game, id);
    }


    /**
     * Are any of the recruitables not of the same type?
     *
     * @return True if the recruitables are not all of the same type.
     */
    public boolean recruitablesDiffer() {
        return !(recruitables[0].equals(recruitables[1])
            && recruitables[0].equals(recruitables[2]));
    }

    /**
     * Gets the type of the recruitable in Europe at the given slot.
     *
     * @param slot The slot of the recruitable whose type needs to be
     *     returned.  Should be 0, 1 or 2.  Note: this used to be 1, 2
     *     or 3 and was called with 1-3 by some classes and 0-2 by
     *     others, the method itself expected 0-2.
     * @return The type of the recruitable in Europe at the given slot.
     * @exception IllegalArgumentException if the given <code>slot</code> does
     *                not exist.
     */
    public UnitType getRecruitable(int slot) {
        if (slot >= 0 && slot < RECRUIT_COUNT) return recruitables[slot];
        throw new IllegalArgumentException("Invalid recruitment slot: " + slot);
    }

    /**
     * Sets the type of the recruitable in Europe at the given slot to the given
     * type.
     *
     * @param slot The slot of the recruitable whose type needs to be set.
     * @param type The new type for the unit at the given slot in Europe.
     */
    public void setRecruitable(int slot, UnitType type) {
        if (slot >= 0 && slot < RECRUIT_COUNT) {
            recruitables[slot] = type;
        } else {
            throw new IllegalArgumentException("Invalid recruitment slot: "
                + slot);
        }
    }

    /**
     * Gets the price of a unit in Europe.
     *
     * @param unitType The <code>UnitType</code> to price.
     * @return The price of this unit when trained/purchased in Europe,
     *     or UNDEFINED on failure.
     */
    public int getUnitPrice(UnitType unitType) {
        Integer price = unitPrices.get(unitType);
        return (price != null) ? price.intValue() : unitType.getPrice();
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
        return Math.max((recruitPrice * difference) / required,
                        recruitLowerCap);
    }

    /**
     * Can this Europe build at least one item of the given EquipmentType?
     *
     * @param equipmentType The <code>EquipmentType</code> to check.
     * @return True if the build could succeed.
     */
    public boolean canBuildEquipment(EquipmentType equipmentType) {
        Market m = getOwner().getMarket();
        for (AbstractGoods ag : equipmentType.getRequiredGoods()) {
            GoodsType goodsType = ag.getType();
            if (!(getOwner().canTrade(goodsType)
                  && getOwner().checkGold(m.getBidPrice(goodsType,
                                                        ag.getAmount())))) {
                return false;
            }
        }
        return true;
    }


    // Override FreeColObject

    /**
     * Gets the feature container for this Europe object.
     *
     * @return The <code>FeatureContainer</code>.
     */
    @Override
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }


    // Interface Location (from UnitLocation)
    // Inheriting:
    //   FreeColObject.getId()
    //   UnitLocation.getTile
    //   UnitLocation.getLocationNameFor
    //   UnitLocation.remove
    //   UnitLocation.contains
    //   UnitLocation.getUnitCount
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer
    //   UnitLocation.getSettlement

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationName() {
        return StringTemplate.key(getNameKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public boolean canAdd(Locatable locatable) {
        if (locatable instanceof Goods) return true; // Can always land goods.
        return super.canAdd(locatable);
    }


    // Interface UnitLocation
    // Inheriting
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.getNoAddReason
    //   UnitLocation.getUnitCapacity


    // Interface Named

    /**
     * {@inheritDoc}
     */
    public String getNameKey() {
        return getOwner().getEuropeNameKey();
    }


    // Interface Ownable 

    /**
     * {@inheritDoc}
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }


    // Serialization

    private static final String OWNER_TAG = "owner";
    private static final String PRICE_TAG = "price";
    private static final String RECRUIT_TAG = "recruit";
    private static final String RECRUIT_LOWER_CAP_TAG = "recruitLowerCap";
    private static final String RECRUIT_PRICE_TAG = "recruitPrice";
    private static final String UNIT_PRICE_TAG = "unitPrice";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll,
                             boolean toSavedGame) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName(), player, showAll, toSavedGame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
        super.writeAttributes(out);

        for (int index = 0; index < recruitables.length; index++) {
            if (recruitables[index] != null) {
                writeAttribute(out, RECRUIT_TAG + index, recruitables[index]);
            }
        }

        writeAttribute(out, RECRUIT_PRICE_TAG, recruitPrice);

        writeAttribute(out, RECRUIT_LOWER_CAP_TAG, recruitLowerCap);

        writeAttribute(out, OWNER_TAG, owner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll,
                                 boolean toSavedGame) throws XMLStreamException {
        super.writeChildren(out, player, showAll, toSavedGame);

        for (UnitType unitType : getSortedCopy(unitPrices.keySet())) {
            out.writeStartElement(UNIT_PRICE_TAG);

            writeAttribute(out, UNIT_TYPE_TAG, unitType);

            writeAttribute(out, PRICE_TAG, unitPrices.get(unitType).intValue());

            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out,
                                    String[] fields) throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXMLPartialImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        // @compat 0.10.0
        if (!hasAbility("model.ability.dressMissionary")) {
            addAbility(new Ability("model.ability.dressMissionary"));
        }
        // end @compat

        for (int index = 0; index < recruitables.length; index++) {
            UnitType unitType = spec.getType(in, RECRUIT_TAG + index,
                                             UnitType.class, (UnitType)null);
            if (unitType != null) recruitables[index] = unitType;
        }

        owner = makeFreeColGameObject(in, OWNER_TAG, Player.class);

        recruitPrice = getAttribute(in, RECRUIT_PRICE_TAG,
                                    RECRUIT_PRICE_INITIAL);

        recruitLowerCap = getAttribute(in, RECRUIT_LOWER_CAP_TAG,
                                       LOWER_CAP_INITIAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers.
        unitPrices.clear();

        super.readChildren(in);

        // @compat 0.10.1
        // Sometimes units in a Europe element have a missing
        // location.  It should always be this Europe instance.
        for (Unit u : getUnitList()) {
            if (u.getLocation() == null) u.setLocationNoUpdate(this);
        }
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (UNIT_PRICE_TAG.equals(tag)) {
            UnitType unitType = spec.getType(in, UNIT_TYPE_TAG,
                                             UnitType.class, (UnitType)null);
            int price = getAttribute(in, PRICE_TAG, -1);
            if (unitType != null && price > 0) {
                unitPrices.put(unitType, new Integer(price));
            }
            closeTag(in, UNIT_PRICE_TAG);

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
