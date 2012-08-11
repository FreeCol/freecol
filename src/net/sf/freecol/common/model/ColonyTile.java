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
     * Constructor for ServerColonyTile.
     */
    protected ColonyTile() {
        // empty constructor
    }

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
     * Initiates a new <code>ColonyTile</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public ColonyTile(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns a description of the tile, with the name of the tile
     * and any improvements made to it (road/plow).
     *
     * @return The description label for this tile
     */
    public StringTemplate getLabel() {
        return workTile.getLabel();
    }

    /**
     * Checks if this is the tile where the <code>Colony</code> is located.
     *
     * @return True if this is the colony center tile.
     */
    public boolean isColonyCenterTile() {
        return getWorkTile() == getTile();
    }

    /**
     * Gets the work tile.
     *
     * @return The tile in which this <code>ColonyTile</code> represents a
     *         <code>WorkLocation</code> for.
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
     * Returns the unit who is occupying the tile
     * @return the unit who is occupying the tile
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        return workTile.getOccupyingUnit();
    }

    /**
     * Checks whether there is a fortified enemy unit in the tile.
     * Units can't produce in occupied tiles
     * @return <code>true</code> if an fortified enemy unit is in the tile
     */
    public boolean isOccupied() {
        return workTile.isOccupied();
    }

    /**
     * Returns the primary production of a colony center tile. In the
     * standard rule sets, this is always some kind of food and all
     * tile improvements contribute to the production.
     *
     * @return an <code>AbstractGoods</code> value
     */
    private AbstractGoods getPrimaryProduction() {
        if (workTile.getType().getPrimaryGoods() == null) {
            return null;
        } else {
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
    }

    /**
     * Returns the secondary production of a colony center tile. Only
     * natural tile improvements, such as rivers, contribute to the
     * production. Artificial tile improvements, such as plowing, are
     * ignored.
     *
     * @return an <code>int</code> value
     */
    private AbstractGoods getSecondaryProduction() {
        if (workTile.getType().getSecondaryGoods() == null) {
            return null;
        } else {
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
        if (isColonyCenterTile()) {
            return StringTemplate.name(name);
        } else {
            return StringTemplate.template("nearLocation")
                .add("%direction%", "direction."
                    + getTile().getDirection(workTile).toString())
                .addName("%location%", name);
        }
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
            : (tile.getOccupyingUnit() != null)
            ? NoAddReason.OCCUPIED_BY_ENEMY
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
                if (owner != null) {
                    result.addAll(owner.getModifierSet(id, null, turn));
                }
            }
        } else {
            result.addAll(workTile.getProductionModifiers(goodsType, unitType));
            if (FeatureContainer.applyModifiers(0f, turn, result) > 0) {
                result.addAll(colony.getModifierSet(id, null, turn));
                if (unitType != null) {
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
        GoodsType workType = unit.getWorkType();
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

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start
        out.writeStartElement(getXMLElementTagName());

        // Attributes
        super.writeAttributes(out);
        out.writeAttribute("workTile", workTile.getId());

        // Children
        super.writeChildren(out, player, showAll, toSavedGame);

        // End
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        workTile = getFreeColGameObject(in, "workTile", Tile.class);
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
    public void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * Will return the position of the tile and the name of the colony in
     * addition to the FreeColObject.toString().
     *
     * @return A representation of a colony-tile that can be used for
     *    debugging.
     */
    public String toString() {
        return "ColonyTile " + getWorkTile().getPosition().toString()
            + " in '" + getColony().getName() + "'";
    }

    /**
     * Gets the tag name of the root element representing this object.
     * @return "colonyTile".
     */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
