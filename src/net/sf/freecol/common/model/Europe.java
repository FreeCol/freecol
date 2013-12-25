/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
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

    public static final Ability ABILITY_DRESS_MISSIONARY
        = new Ability(Ability.DRESS_MISSIONARY, true);

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
     * This list represents the types of the units that can be
     * recruited in Europe.
     */
    protected List<UnitType> recruitables = new ArrayList<UnitType>();

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
     * {@link #readFromXML(FreeColXMLReader)} or
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
        UnitType template = null;
        for (UnitType unitType : recruitables) {
            if (template == null) {
                template = unitType;
            } else if (template != unitType) {
                return true;
            }
        }
        return false;
    }

    public List<UnitType> getRecruitables() {
        return recruitables;
    }

    /**
     * Gets the type of the recruitable in Europe at the given slot.
     *
     * @param slot The slot of the recruitable whose type needs to be
     *     returned.
     * @return The type of the recruitable in Europe at the given slot.
     * @exception IllegalArgumentException if the given <code>slot</code> does
     *                not exist.
     */
    public UnitType getRecruitable(int slot) {
        if (slot >= 0 && slot < RECRUIT_COUNT && slot < recruitables.size()) {
            return recruitables.get(slot);
        } else {
            throw new IllegalArgumentException("Invalid recruitment slot: " + slot);
        }
    }

    /**
     * Only used by ServerEurope.
     */
    protected void addRecruitable(UnitType unitType) {
        recruitables.add(unitType);
    }

    /**
     * Removes the UnitType at the given index from the list of
     * recruitable unit types and adds another.
     *
     * @param slot The index of the recruitable to be replaced
     * @param type The new type for the unit at the given slot in Europe.
     */
    public void replaceRecruitable(int slot, UnitType type) {
        if (slot >= 0 && slot < RECRUIT_COUNT && slot < recruitables.size()) {
            recruitables.remove(slot);
            if (type != null) {
                recruitables.add(type);
            }
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
        if (!owner.isColonial()) return -1;
        int required = owner.getImmigrationRequired();
        int immigration = owner.getImmigration();
        int difference = Math.max(required - immigration, 0);
        return Math.max((recruitPrice * difference) / required,
                        recruitLowerCap);
    }

    /**
     * Get any immigration produced in Europe.
     *
     * Col1 penalizes immigration by -4 per unit in Europe per turn,
     * but there is a +2 player bonus, which we might as well add
     * here.  Total immigration per turn can not be negative, but that
     * is handled in ServerPlayer.
     *
     * @param production The current total colony production.
     * @return Immigration produced this turn in Europe.
     */
    public int getImmigration(int production) {
        final Specification spec = getSpecification();
        int n = 0;
        for (Unit u : getUnitList()) {
            if (u.isPerson()) n++;
        }
        n *= spec.getInteger(GameOptions.EUROPEAN_UNIT_IMMIGRATION_PENALTY);
        n += spec.getInteger(GameOptions.PLAYER_IMMIGRATION_BONUS);
        // Do not allow total production to be negative.
        if (n + production < 0) n = -production;
        return n;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBuildEquipment(EquipmentType eq, int amount) {
        Player player = getOwner();
        Market market = player.getMarket();
        int price = 0;
        for (AbstractGoods goods : eq.getRequiredGoods()) {
            GoodsType goodsType = goods.getType();
            // Refuse to trade in boycotted goods
            if (!player.canTrade(goodsType)) return false;
            if (amount > 0) {
                price += market.getBidPrice(goodsType,
                    amount * goods.getAmount());
            }
        }
        return player.checkGold(price);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int canBuildRoleEquipment(Role role) {
        Player player = getOwner();
        Market market = player.getMarket();
        int price = 0;
        for (AbstractGoods ag : role.getRequiredGoods()) {
            GoodsType goodsType = ag.getType();
            // Refuse to trade in boycotted goods
            if (!player.canTrade(goodsType)) return -1;
            price += market.getBidPrice(goodsType, ag.getAmount());
        }
        return price;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equipForRole(Unit unit, Role role) {
        throw new RuntimeException("Only valid in the server.");
    }



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


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    public Set<Ability> getAbilitySet(String id, FreeColGameObjectType fcgot,
                                      Turn turn) {
        Set<Ability> result = super.getAbilitySet(id, fcgot, turn);
        // Always able to dress a missionary.
        if (id == null || id == Ability.DRESS_MISSIONARY) {
            result.add(ABILITY_DRESS_MISSIONARY);
        }
        return result;
    }


    // Serialization

    private static final String OWNER_TAG = "owner";
    private static final String PRICE_TAG = "price";
    private static final String RECRUIT_TAG = "recruit";
    private static final String RECRUIT_ID_TAG = "id";
    private static final String RECRUIT_LOWER_CAP_TAG = "recruitLowerCap";
    private static final String RECRUIT_PRICE_TAG = "recruitPrice";
    private static final String UNIT_PRICE_TAG = "unitPrice";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (xw.validFor(getOwner())) {
            xw.writeAttribute(RECRUIT_PRICE_TAG, recruitPrice);

            xw.writeAttribute(RECRUIT_LOWER_CAP_TAG, recruitLowerCap);

            xw.writeAttribute(OWNER_TAG, owner);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(getOwner())) {

            for (Ability ability : getSortedAbilities()) {
                ability.toXML(xw);
            }

            for (Modifier modifier : getSortedModifiers()) {
                modifier.toXML(xw);
            }

            for (UnitType unitType : getSortedCopy(unitPrices.keySet())) {
                xw.writeStartElement(UNIT_PRICE_TAG);

                xw.writeAttribute(UNIT_TYPE_TAG, unitType);

                xw.writeAttribute(PRICE_TAG, unitPrices.get(unitType).intValue());

                xw.writeEndElement();
            }

            for (UnitType unitType : recruitables) {
                xw.writeStartElement(RECRUIT_TAG);
                xw.writeAttribute(RECRUIT_ID_TAG, unitType.getId());
                xw.writeEndElement();
            }
        }
    }

    // @compat 0.10.7
    private boolean clearRecruitables = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        // @compat 0.10.7
        for (int index = 0; index < 3; index++) {
            UnitType unitType = xr.getType(spec, RECRUIT_TAG + index,
                                           UnitType.class, (UnitType)null);
            if (unitType != null) {
                recruitables.add(unitType);
                clearRecruitables = false;
            }
        }
        // end @compat

        owner = xr.findFreeColGameObject(getGame(), OWNER_TAG,
                                         Player.class, (Player)null, true);

        recruitPrice = xr.getAttribute(RECRUIT_PRICE_TAG,
                                       RECRUIT_PRICE_INITIAL);

        recruitLowerCap = xr.getAttribute(RECRUIT_LOWER_CAP_TAG,
                                          LOWER_CAP_INITIAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        unitPrices.clear();
        featureContainer.clear();

        // @compat 0.10.7
        if (clearRecruitables) {
            // in future, always clear
            recruitables.clear();
        }
        // end @compat

        super.readChildren(xr);

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
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (UNIT_PRICE_TAG.equals(tag)) {
            UnitType unitType = xr.getType(spec, UNIT_TYPE_TAG,
                                           UnitType.class, (UnitType)null);

            int price = xr.getAttribute(PRICE_TAG, -1);

            unitPrices.put(unitType, new Integer(price));
            xr.closeTag(UNIT_PRICE_TAG);

        } else if (Ability.getXMLElementTagName().equals(tag)) {
            addAbility(new Ability(xr, spec));

        } else if (Modifier.getXMLElementTagName().equals(tag)) {
            addModifier(new Modifier(xr, spec));

        } else if (RECRUIT_TAG.equals(tag)) {
            UnitType unitType = xr.getType(spec, RECRUIT_ID_TAG, UnitType.class, (UnitType)null);
            recruitables.add(unitType);
            xr.closeTag(RECRUIT_TAG);
        } else {
            super.readChild(xr);
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
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "europe".
     */
    public static String getXMLElementTagName() {
        return "europe";
    }
}
