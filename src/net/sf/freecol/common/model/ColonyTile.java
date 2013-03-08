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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Player.NoClaimReason;


/**
 * Represents a work location on a tile. Each ColonyTile except the
 * colony center tile provides a work place for a single unit and
 * produces a single type of goods. The colony center tile generally
 * produces two different of goods, one food type and one new world
 * raw material.
 */
public class ColonyTile extends WorkLocation implements Ownable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /**
     * The maximum number of units a ColonyTile can hold.
     */
    public static final int UNIT_CAPACITY = 1;

    /**
     * The tile to work.  This is accessed through getWorkTile().
     * Beware!  Do not confuse this with getTile(), which returns
     * the colony center tile (because every work location belongs to
     * the enclosing colony).
     */
    protected Tile workTile;


    /**
     * Constructor for ServerColonyTile, deliberately empty.
     */
    protected ColonyTile() {}

    /**
     * Constructor for ServerColonyTile.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The <code>Colony</code> this object belongs to.
     * @param workTile The tile in which this <code>ColonyTile</code>
     *                 represents a <code>WorkLocation</code> for.
     */
    protected ColonyTile(Game game, Colony colony, Tile workTile) {
        super(game);

        setColony(colony);
        this.workTile = workTile;
    }

    /**
     * Initiates a new <code>ColonyTile</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occured during parsing.
     */
    public ColonyTile(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Initiates a new <code>ColonyTile</code> with the given ID.  The
     * object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public ColonyTile(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets a description of the tile, with the name of the tile
     * and any improvements made to it (road/plow).
     *
     * @return The description label for this tile
     */
    public StringTemplate getLabel() {
        return workTile.getLabel();
    }

    /**
     * Is this the tile where the <code>Colony</code> is located?
     *
     * @return True if this is the colony center tile.
     */
    public boolean isColonyCenterTile() {
        return getWorkTile() == getTile();
    }

    /**
     * Gets the work tile, that is the actual tile being worked.
     *
     * @return The <code>Tile</code> in which this
     *     <code>ColonyTile</code> represents a <code>WorkLocation</code> for.
     */
    public Tile getWorkTile() {
        return workTile;
    }

    /**
     * Relocates any worker on this <code>ColonyTile</code>.
     * The workers are added to another {@link WorkLocation}
     * within the {@link Colony}.
     */
    public void relocateWorkers() {
        for (Unit unit : getUnitList()) {
            for (WorkLocation wl : getColony().getCurrentWorkLocations()) {
                if (wl != this && wl.canAdd(unit)) {
                    unit.setLocation(wl);
                    break;
                }
            }
        }
    }

    /**
     * Gets a unit who is occupying the tile.
     *
     * @return A <code>Unit</code> who is occupying the work tile, if any.
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        return workTile.getOccupyingUnit();
    }

    /**
     * Is there a fortified enemy unit on the work tile?
     * Production can not occur on occupied tiles.
     *
     * @return True if an fortified enemy unit is in the tile.
     */
    public boolean isOccupied() {
        return workTile.isOccupied();
    }

    /**
     * Gets the primary production of a colony center tile.  In the
     * standard rule sets, this is always some kind of food and all
     * tile improvements contribute to the production.
     *
     * @return The primary production, as an <code>AbstractGoods</code>.
     */
    private AbstractGoods getPrimaryProduction() {
        if (workTile.getType().getPrimaryGoods() == null) return null;
        AbstractGoods primaryProduction
            = new AbstractGoods(workTile.getType().getPrimaryGoods());
        int potential = primaryProduction.getAmount();
        if (workTile.getTileItemContainer() != null) {
            potential = workTile.getTileItemContainer()
                .getTotalBonusPotential(primaryProduction.getType(), null,
                                        potential, false);
        }
        primaryProduction.setAmount(potential
            + Math.max(0, getColony().getProductionBonus()));
        return primaryProduction;
    }

    /**
     * Gets the secondary production of a colony center tile.  Only
     * natural tile improvements, such as rivers, contribute to the
     * production.  Artificial tile improvements, such as plowing, are
     * ignored.
     *
     * @return The secondary production, as an <code>AbstractGoods</code>.
     */
    private AbstractGoods getSecondaryProduction() {
        if (workTile.getType().getSecondaryGoods() == null) return null;
        AbstractGoods secondaryProduction
            = new AbstractGoods(workTile.getType().getSecondaryGoods());
        int potential = secondaryProduction.getAmount();
        if (workTile.getTileItemContainer() != null) {
            potential = workTile.getTileItemContainer()
                .getTotalBonusPotential(secondaryProduction.getType(), null,
                                        potential, true);
        }
        secondaryProduction.setAmount(potential
            + Math.max(0, getColony().getProductionBonus()));
        return secondaryProduction;
    }

    /**
     * Gets the basic production information for the colony tile,
     * ignoring any colony limited (which for now, should be irrelevant).
     *
     * @return The raw production of this colony tile.
     * @see ProductionCache#update
     */
    public ProductionInfo getBasicProductionInfo() {
        ProductionInfo pi = new ProductionInfo();
        if (isColonyCenterTile()) {
            AbstractGoods primaryProduction = getPrimaryProduction();
            if (primaryProduction != null) {
                pi.addProduction(primaryProduction);
            }
            AbstractGoods secondaryProduction = getSecondaryProduction();
            if (secondaryProduction != null) {
                pi.addProduction(secondaryProduction);
            }
        } else {
            for (Unit unit : getUnitList()) {
                GoodsType goodsType = unit.getWorkType();
                if (goodsType == null) continue;
                pi.addProduction(new AbstractGoods(goodsType,
                        getProductionOf(unit, goodsType)));
            }
        }
        return pi;
    }


    // Interface Location

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationName() {
        String name = getColony().getName();
        return (isColonyCenterTile()) ? StringTemplate.name(name)
            : StringTemplate.template("nearLocation")
                .add("%direction%", "direction."
                     + getTile().getDirection(workTile).toString())
                .addName("%location%", name);
    }

    // Omit getLocationNameFor

    /**
     * {@inheritDoc}
     */
    public boolean add(final Locatable locatable) {
        NoAddReason reason = getNoAddReason(locatable);
        if (reason != NoAddReason.NONE) {
            throw new IllegalStateException("Can not add " + locatable
                + " to " + toString() + " because " + reason);
        }
        Unit unit = (Unit) locatable;
        if (contains(unit)) return true;

        if (super.add(unit)) {
            unit.setState(Unit.UnitState.IN_COLONY);

            // Choose a sensible work type only if none already specified.
            if (unit.getWorkType() == null) {
                AbstractGoods goods = workTile.getType().getPrimaryGoods();
                if (goods == null) {
                    goods = workTile.getType().getSecondaryGoods();
                    if (goods == null
                        && !workTile.getType().getProduction().isEmpty()) {
                        goods = workTile.getType().getProduction().get(0);
                    }
                }
                if (goods != null) unit.setWorkType(goods.getType());
            }

            getColony().invalidateCache();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }
        Unit unit = (Unit) locatable;
        if (!contains(unit)) return true;

        if (super.remove(unit)) {
            unit.setState(Unit.UnitState.ACTIVE);
            unit.setMovesLeft(0);

            getColony().invalidateCache();
            return true;
        }
        return false;
    }


    // Interface UnitLocation

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        NoAddReason reason = getNoWorkReason();
        return (reason != NoAddReason.NONE) ? reason
            : super.getNoAddReason(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCapacity() {
        return (isColonyCenterTile()) ? 0 : UNIT_CAPACITY;
    }


    // Interface WorkLocation

    /**
     * {@inheritDoc}
     */
    public NoAddReason getNoWorkReason() {
        Tile tile = getWorkTile();
        NoClaimReason claim;

        return (isColonyCenterTile())
            ? NoAddReason.COLONY_CENTER
            : (!getColony().hasAbility(Ability.PRODUCE_IN_WATER)
                && !tile.isLand())
            ? NoAddReason.MISSING_ABILITY
            : (tile.getOwningSettlement() == getColony())
            ? NoAddReason.NONE
            : ((claim = getOwner().canClaimForSettlementReason(tile))
                == NoClaimReason.NONE)
            ? NoAddReason.CLAIM_REQUIRED
            : (claim == NoClaimReason.TERRAIN
                || claim == NoClaimReason.RUMOUR
                || claim == NoClaimReason.WATER)
            ? NoAddReason.MISSING_ABILITY
            : (claim == NoClaimReason.SETTLEMENT)
            ? ((tile.getSettlement().getOwner() == getOwner())
                ? NoAddReason.ANOTHER_COLONY
                : NoAddReason.OWNED_BY_ENEMY)
            : (claim == NoClaimReason.OCCUPIED)
            ? NoAddReason.OCCUPIED_BY_ENEMY
            : (claim == NoClaimReason.WORKED)
            ? NoAddReason.ANOTHER_COLONY
            : (claim == NoClaimReason.EUROPEANS)
            ? NoAddReason.OWNED_BY_ENEMY
            : (claim == NoClaimReason.NATIVES)
            ? NoAddReason.CLAIM_REQUIRED
            : NoAddReason.WRONG_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAutoProduce() {
        return isColonyCenterTile();
    }

    /**
     * {@inheritDoc}
     */
    public int getProductionOf(Unit unit, GoodsType goodsType) {
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        }
        return getPotentialProduction(goodsType, unit.getType());
    }

    /**
     * {@inheritDoc}
     */
    public int getPotentialProduction(GoodsType goodsType, UnitType unitType) {
        int production = 0;
        TileType tileType = workTile.getType();
        if (isColonyCenterTile()) {
            production = (unitType != null) ? 0
                : (tileType.getPrimaryGoods() != null
                    && tileType.getPrimaryGoods().getType() == goodsType)
                ? getPrimaryProduction().getAmount()
                : (tileType.getSecondaryGoods() != null
                    && tileType.getSecondaryGoods().getType() == goodsType)
                ? getSecondaryProduction().getAmount()
                : 0;
        } else if (workTile.isLand()
            || getColony().hasAbility(Ability.PRODUCE_IN_WATER)) {
            List<Modifier> mods = getProductionModifiers(goodsType, unitType);
            if (!mods.isEmpty()) {
                production = (int)FeatureContainer.applyModifiers(0f,
                    getGame().getTurn(), mods);
            }
        }
        return Math.max(0, production);
    }

    /**
     * {@inheritDoc}
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType, 
                                                 UnitType unitType) {
        if (goodsType == null) {
            throw new IllegalArgumentException("Null GoodsType.");
        }
        List<Modifier> result = new ArrayList<Modifier>();
        final Colony colony = getColony();
        final Player owner = colony.getOwner();
        final TileType tileType = getWorkTile().getType();
        final String id = goodsType.getId();
        final Turn turn = getGame().getTurn();
        if (isColonyCenterTile()) {
            if (tileType.isPrimaryGoodsType(goodsType)
                || tileType.isSecondaryGoodsType(goodsType)) {
                result.addAll(workTile.getProductionModifiers(goodsType, null));
                result.addAll(colony.getModifierSet(id, null, turn));
                result.add(colony.getProductionModifier(goodsType));
                if (owner != null) {
                    result.addAll(owner.getModifierSet(id, null, turn));
                }
            }
        } else {
            result.addAll(workTile.getProductionModifiers(goodsType, unitType));
            if (FeatureContainer.applyModifiers(0f, turn, result) > 0) {
                result.addAll(colony.getModifierSet(id, null, turn));
                if (unitType != null) {
                    result.add(colony.getProductionModifier(goodsType));
                    result.addAll(unitType.getModifierSet(id, tileType, turn));
                    if (owner != null) {
                        result.addAll(owner.getModifierSet(id, null, turn));
                    }
                } else {
                    if (owner != null) {
                        result.addAll(owner.getModifierSet(id, tileType, turn));
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public GoodsType getBestWorkType(Unit unit) {
        GoodsType workType = null;
        int amount = 0;
        for (GoodsType g : getSpecification().getFarmedGoodsTypeList()) {
            int newAmount = getPotentialProduction(g, unit.getType());
            if (newAmount > amount) {
                amount = newAmount;
                workType = g;
            }
        }
        return workType;
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getClaimTemplate() {
        return (isColonyCenterTile()) ? super.getClaimTemplate()
            : StringTemplate.template("workClaimColonyTile")
                .add("%direction%", "direction."
                    + getTile().getDirection(workTile).toString());
    }


    // Serialization

    private static final String WORK_TILE_TAG = "workTile";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        writeAttributes(out);
        super.writeChildren(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, WORK_TILE_TAG, workTile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        workTile = getFreeColGameObject(in, WORK_TILE_TAG, Tile.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields) throws XMLStreamException {
        toXMLPartialByClass(out, ColonyTile.class, fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXMLPartialImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXMLPartialByClass(in, ColonyTile.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ColonyTile" + getWorkTile().getPosition().toString()
            + "/" + getColony().getName();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "colonyTile".
     */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
