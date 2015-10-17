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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Unit.UnitState;


/**
 * Represents Europe in the game.  Each <code>Player</code> has it's
 * own <code>Europe</code>.
 *
 * In Europe, you can recruit, train and purchase new units.  You can
 * also equip units, as well as sell and buy goods.
 */
public class Europe extends UnitLocation
    implements Ownable, Named, TradeLocation {

    private static final Logger logger = Logger.getLogger(Europe.class.getName());

    /** The initial recruit price. */
    private static final int RECRUIT_PRICE_INITIAL = 200;

    /** The initial lower bound on recruitment price. */
    private static final int LOWER_CAP_INITIAL = 80;

    public static final String UNIT_CHANGE = "unitChange";

    public static final Ability ABILITY_DRESS_MISSIONARY
        = new Ability(Ability.DRESS_MISSIONARY, true);

    /**
     * Migration handling.
     *
     * Migration routines operate on:
     * - "indexes" which refer to a valid member of the recruitables
     *   list, and must be [0, RECRUIT_COUNT)
     * - "slots", where slot zero means "pick a random migrant" and the
     *   other slots in [1, RECRUIT_COUNT] refer to the index == slot-1
     * The following constant should be used when the random choice
     * behaviour is desired.
     */
    public static enum MigrationType {
        NORMAL,     // Unit decided to migrate
        RECRUIT,    // Player is paying
        FOUNTAIN,   // As a result of a Fountain of Youth discovery
        SURVIVAL;   // Emergency autorecruit in server

        /** The number of recruitable unit types. */
        private static final int MIGRANT_COUNT = 3;

        /**
         * The unspecific migrant slot to use to denote a random
         * choice between specific slots.
         */
        private static final int CHOOSE_MIGRANT_SLOT = 0;

        /**
         * The migrant slot to use when there is no reason to choose
         * between them.
         */
        private static final int DEFAULT_MIGRANT_SLOT = 1;

        public static int getMigrantCount() {
            return MIGRANT_COUNT;
        }

        public static int getUnspecificSlot() {
            return CHOOSE_MIGRANT_SLOT;
        }

        public static int getDefaultSlot() {
            return DEFAULT_MIGRANT_SLOT;
        }

        public static boolean validMigrantIndex(int x) {
            return 0 <= x && x < MIGRANT_COUNT;
        }

        public static int migrantIndexToSlot(int x) {
            return x + 1;
        }

        public static int migrantSlotToIndex(int x) {
            return x - 1;
        }

        public static int convertToMigrantSlot(Integer i) {
            return (i == null || !validMigrantSlot(i)) ? CHOOSE_MIGRANT_SLOT
                : i;
        }

        public static boolean validMigrantSlot(int x) {
            return 0 <= x && x <= MIGRANT_COUNT;
        }

        public static boolean specificMigrantSlot(int x) {
            return 1 <= x && x <= MIGRANT_COUNT;
        }

        public static boolean unspecificMigrantSlot(int x) {
            return CHOOSE_MIGRANT_SLOT == x;
        }
    }

    /**
     * This list represents the types of the units that can be
     * recruited in Europe.
     */
    protected final List<UnitType> recruitables = new ArrayList<>();

    /** Prices for trainable or purchasable units. */
    protected final java.util.Map<UnitType, Integer> unitPrices = new HashMap<>();

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
     * Get a list of the current recruitables.
     *
     * @return A list of recruitable <code>UnitType</code>s.
     */
    public List<UnitType> getRecruitables() {
        return new ArrayList<>(recruitables);
    }

    /**
     * Add a recruitable unit type.
     *
     * @param unitType The recruitable <code>UnitType</code> to add.
     * @return True if the recruitable was added.
     */
    protected boolean addRecruitable(UnitType unitType) {
        if (recruitables.size() < MigrationType.MIGRANT_COUNT) {
            recruitables.add(unitType);
            return true;
        }
        return false;
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
        return (price != null) ? price : unitType.getPrice();
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
     * {@inheritDoc}
     */
    @Override
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColGameObject getLinkTarget(Player player) {
        return (getOwner() == player) ? this : null;
    }

    // Interface Location (from UnitLocation)
    // Inheriting:
    //   FreeColObject.getId()
    //   UnitLocation.getTile
    //   UnitLocation.getLocationLabelFor
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
    public StringTemplate getLocationLabel() {
        return StringTemplate.key(this);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRank() {
        return Location.LOCATION_RANK_EUROPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        return "Europe";
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
    public int priceGoods(List<AbstractGoods> goods) {
        Player player = getOwner();
        Market market = player.getMarket();
        int price = 0;
        for (AbstractGoods ag : goods) {
            if (ag.getAmount() <= 0) continue;
            GoodsType goodsType = ag.getType();
            // Refuse to trade in boycotted goods
            if (!player.canTrade(goodsType)) {
                price = -1;
                break;
            }
            price += market.getBidPrice(goodsType, ag.getAmount());
        }
        return price;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equipForRole(Unit unit, Role role, int roleCount) {
        throw new RuntimeException("Only valid in the server.");
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return getOwner().getEuropeNameKey();
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }


    // Interface TradeLocation

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGoodsCount(GoodsType goodsType) {
        return GoodsContainer.HUGE_CARGO_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExportAmount(GoodsType goodsType, int turns) {
        return (getOwner().canTrade(goodsType)) ? GoodsContainer.HUGE_CARGO_SIZE
            : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImportAmount(GoodsType goodsType, int turns) {
        return (getOwner().canTrade(goodsType)) ? GoodsContainer.HUGE_CARGO_SIZE
            : 0;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeResources() {
        Player owner = getOwner();
        if (owner != null) {
            owner.setEurope(null);
            HighSeas highSeas = owner.getHighSeas();
            if (highSeas != null) highSeas.removeDestination(this);
        }
        super.disposeResources();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Ability> getAbilities(String id, FreeColGameObjectType fcgot,
                                     Turn turn) {
        Set<Ability> result = super.getAbilities(id, fcgot, turn);
        // Always able to dress a missionary.
        if (id == null || Ability.DRESS_MISSIONARY.equals(id)) {
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
    // @compat 0.10.0
    private static final String UNITS_TAG = "units";
    // end @compat 0.10.0


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
                addRecruitable(unitType);
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

        if (Ability.getXMLElementTagName().equals(tag)) {
            addAbility(new Ability(xr, spec));

        } else if (Modifier.getXMLElementTagName().equals(tag)) {
            addModifier(new Modifier(xr, spec));

        } else if (RECRUIT_TAG.equals(tag)) {
            UnitType unitType = xr.getType(spec, RECRUIT_ID_TAG,
                                           UnitType.class, (UnitType)null);
            if (unitType != null) addRecruitable(unitType);
            xr.closeTag(RECRUIT_TAG);

        } else if (UNIT_PRICE_TAG.equals(tag)) {
            UnitType unitType = xr.getType(spec, UNIT_TYPE_TAG,
                                           UnitType.class, (UnitType)null);

            int price = xr.getAttribute(PRICE_TAG, -1);

            unitPrices.put(unitType, price);
            xr.closeTag(UNIT_PRICE_TAG);

        // @compat 0.10.0
        } else if (UNITS_TAG.equals(tag)) {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                super.readChild(xr);
            }
        // end @compat 0.10.0

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
    @Override
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
