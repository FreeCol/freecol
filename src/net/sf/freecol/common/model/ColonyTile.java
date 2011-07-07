/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Player.NoClaimReason;

import org.w3c.dom.Element;


/**
 * Represents a work location on a tile. Each ColonyTile except the
 * colony center tile provides a work place for a single unit and
 * produces a single type of goods. The colony center tile generally
 * produces two different of goods, one food type and one new world
 * raw material.
 */
public class ColonyTile extends FreeColGameObject
    implements WorkLocation, Ownable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The colony this colony tile belongs to. */
    protected Colony colony;

    /** The tile to work. */
    protected Tile workTile;

    /** The unit working the tile. */
    protected Unit unit;

    /** Is this colony tile at the center of the colony. */
    protected boolean colonyCenterTile;


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
     */
    public ColonyTile(Game game) {
        super(game);
    }

    /**
     * Initiates a new <code>Building</code> from an
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
     * Initiates a new <code>Building</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public ColonyTile(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>ColonyTile</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public ColonyTile(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns the (non-unique) name of this <code>ColonyTile</code>.
     * @return The name of this ColonyTile.
     */
    public StringTemplate getLocationName() {
        String name = getColony().getName();
        if (isColonyCenterTile()) {
            return StringTemplate.name(name);
        } else {
            return StringTemplate.template("nearLocation")
                .addName("%location%", name);
        }
    }

    /**
     * Returns the name of this ColonyTile for a particular player.
     *
     * @param player The <code>Player</code> to prepare the name for.
     * @return The name of this ColonyTile.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return getLocationName();
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
    * Gets the owner of this <code>Ownable</code>.
    *
    * @return The <code>Player</code> controlling this
    *         {@link Ownable}.
    */
    public Player getOwner() {
        return colony.getOwner();
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership
     *      of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *      this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }

    /**
    * Checks if this is the tile where the <code>Colony</code> is located.
    * @return The result.
    */
    public boolean isColonyCenterTile() {
        return colonyCenterTile;
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
     * Gets the tile where the colony is located.
     * @return The <code>Tile</code>.
     */
    public Tile getTile() {
        return colony.getTile();
    }

    public GoodsContainer getGoodsContainer() {
        return null;
    }


    /**
     * Gets a pointer to the settlement containing this tile.
     *
     * @return This settlement.
     */
    public Settlement getSettlement() {
        return colony;
    }

    /**
     * Gets a pointer to the colony containing this tile.
     *
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Gets the <code>Unit</code> currently working on this <code>ColonyTile</code>.
     *
     * @return The <code>Unit</code> or <i>null</i> if no unit is present.
     * @see #setUnit
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Sets a <code>Unit</code> to this <code>ColonyTile</code>.
     *
     * @param unit The <code>Unit</code>.
     * @see #getUnit
     */
    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    /**
     * Gets the amount of Units at this <code>ColonyTile</code>.
     * @return The amount of Units at this <code>ColonyTile</code>.
     */
    public int getUnitCount() {
        return (getUnit() != null) ? 1 : 0;
    }

    /**
     * Relocates any worker on this <code>ColonyTile</code>.
     * The workers are added to another {@link WorkLocation}
     * within the {@link Colony}.
     */
    public void relocateWorkers() {
        if (getUnit() != null) {
            for (WorkLocation wl : getColony().getWorkLocations()) {
                if (wl != this && wl.canAdd(getUnit())) {
                    getUnit().setLocation(wl);
                    break;
                }
            }
        }
    }

    /**
       Reasons why a colony tile can not be worked or added to.
    */
    public static enum NoAddReason {
        NONE,            // Actually, tile can be worked
        SETTLEMENT,      // Settlement centre tile
        OCCUPIED,        // Occupying unit fortified
        PRODUCE,         // Does not have produceInWater ability
        CLAIM,           // Does not own the tile, see NoClaimReason
        FULL,            // Workable tile is full
        NOTUNIT,         // Adding non-unit
        NOSKILL,         // Adding unit with no skill
    };

    /**
     * Checks if this colony tile is available to the colony to be worked.
     * Checks ownership last because this can trigger a tile claim,
     * which we should be conservative about.
     *
     * @return The reason why/not the tile can be worked.
     */
    public NoAddReason canBeWorkedReason() {
        Player player = getOwner();
        Tile tile = getWorkTile();

        return (tile.getSettlement() != null) ? NoAddReason.SETTLEMENT
            : (tile.getOccupyingUnit() != null) ? NoAddReason.OCCUPIED
            : (!colony.hasAbility("model.ability.produceInWater")
                && !tile.isLand()) ? NoAddReason.PRODUCE
            : (tile.getOwningSettlement() != colony
                && !player.canClaimForSettlement(tile)) ? NoAddReason.CLAIM
            : NoAddReason.NONE;
    }

    /**
     * Check if this <code>WorkLocation</code> is available to the colony.
     * Used by canAdd() and the gui to decide whether to draw a border
     * on this tile in the colony panel.
     *
     * @return True if the location can be worked.
     */
    public boolean canBeWorked() {
        return canBeWorkedReason() == NoAddReason.NONE;
    }

    /**
     * Checks if this tile can have a specified locatable added to it.
     *
     * @param locatable The <code>Locatable</code> to add.
     * @return The reason why/not the tile can be added to.
     */
    public NoAddReason canAddReason(Locatable locatable) {
        NoAddReason reason;
        Unit unit;

        return (!(locatable instanceof Unit)) ? NoAddReason.NOTUNIT
            : (!(unit = (Unit) locatable).getType()
                .hasSkill()) ? NoAddReason.NOSKILL
            : (getUnit() != null && getUnit() != unit) ? NoAddReason.FULL
            : ((reason = canBeWorkedReason()) != NoAddReason.NONE) ? reason
            : NoAddReason.NONE;
    }

    /**
     * Checks if the specified <code>Locatable</code> may be added to
     * this <code>WorkLocation</code>.
     *
     * @param locatable the <code>Locatable</code>.
     * @return <code>true</code> if the <code>Unit</code> may be added
     *         and <code>false</code> otherwise.
     */
    public boolean canAdd(Locatable locatable) {
        return canAddReason(locatable) == NoAddReason.NONE;
    }

    /**
     * Add the specified locatable to this colony tile.
     *
     * @param locatable The <code>Locatable</code> to add.
     */
    public void add(final Locatable locatable) {
        NoAddReason reason = canAddReason(locatable);
        if (reason != NoAddReason.NONE) {
            throw new IllegalStateException("Can not add to colony tile: "
                + toString() + " reason: " + reason);
        }

        final Unit unit = (Unit) locatable;
        setUnit(unit);
        unit.setState(Unit.UnitState.IN_COLONY);
        getColony().invalidateCache();

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

        Unit teacher = unit.getTeacher();
        if (teacher == null
            && (teacher = getColony().findTeacher(unit)) != null) {
            unit.setTeacher(teacher);
            teacher.setStudent(unit);
        }
    }

    /**
     * Remove the specified locatable from this colony tile.
     *
     * @param locatable The <code>Locatable</code> to be removed.
     */
    public void remove(final Locatable locatable) {
        if (getUnit() == null || !getUnit().equals(locatable)) {
            return;
        }

        final Unit unit = getUnit();
        setUnit(null);
        unit.setMovesLeft(0);
        unit.setState(Unit.UnitState.ACTIVE);
        getColony().invalidateCache();

        Unit teacher = unit.getTeacher();
        if (teacher != null) {
            teacher.setStudent(null);
            unit.setTeacher(null);
        }
    }

    /**
     * Gets a list of units in this colony tile.
     *
     * @return A list of units in this colony tile.
     */
    public List<Unit> getUnitList() {
        List<Unit> units = new ArrayList<Unit>();
        if (getUnit() != null) units.add(getUnit());
        return units;
    }

    public Iterator <Unit> getUnitIterator() {
        return getUnitList().iterator();
    }


    /**
     * Checks if this <code>ColonyTile</code> contains the given <code>Locatable</code>.
     *
     * @param locatable The <code>Locatable</code>.
     * @return The result.
     */
    public boolean contains(Locatable locatable) {
        return (locatable == unit) ? true:false;
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
     * Returns a worktype for a unit.
     *
     * @param unit a <code>Unit</code> value
     * @return a workType
     */
    public GoodsType getWorkType(Unit unit) {
        GoodsType workType = unit.getWorkType();
        int amount = (workType == null) ? 0 : getProductionOf(unit, workType);
        if (amount == 0) {
            List<GoodsType> farmedGoodsTypes = getSpecification().getFarmedGoodsTypeList();
            for(GoodsType farmedGoods : farmedGoodsTypes) {
                int newAmount = getProductionOf(unit, farmedGoods);
                if (newAmount > amount) {
                    amount = newAmount;
                    workType = farmedGoods;
                }
            }
        }
        return workType;
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
            AbstractGoods primaryProduction = new AbstractGoods(workTile.getType().getPrimaryGoods());
            int potential = primaryProduction.getAmount();
            if (workTile.getTileItemContainer() != null) {
                potential = workTile.getTileItemContainer()
                    .getTotalBonusPotential(primaryProduction.getType(), null,
                                            potential, false);
            }
            primaryProduction.setAmount(potential + Math.max(0, getColony().getProductionBonus()));
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
            AbstractGoods secondaryProduction = new AbstractGoods(workTile.getType().getSecondaryGoods());
            int potential = secondaryProduction.getAmount();
            if (workTile.getTileItemContainer() != null) {
                potential = workTile.getTileItemContainer()
                    .getTotalBonusPotential(secondaryProduction.getType(), null,
                                            potential, true);
            }
            secondaryProduction.setAmount(potential + Math.max(0, getColony().getProductionBonus()));
            return secondaryProduction;
        }
    }

    public List<AbstractGoods> getProduction() {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>(2);
        if (isColonyCenterTile()) {
            AbstractGoods primaryProduction = getPrimaryProduction();
            if (primaryProduction != null) {
                result.add(primaryProduction);
            }
            AbstractGoods secondaryProduction = getSecondaryProduction();
            if (secondaryProduction != null) {
                result.add(secondaryProduction);
            }
        } else if (getUnit() != null) {
            GoodsType goodsType = getUnit().getWorkType();
            result.add(new AbstractGoods(goodsType, getProductionOf(getUnit(), goodsType)));
        }
        return result;
    }


    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getProductionOf(GoodsType goodsType) {
        if (isColonyCenterTile()) {
            if (workTile.getType().getPrimaryGoods() != null
                && workTile.getType().getPrimaryGoods().getType() == goodsType) {
                return getPrimaryProduction().getAmount();
            } else if (workTile.getType().getSecondaryGoods() != null
                       && workTile.getType().getSecondaryGoods().getType() == goodsType) {
                return getSecondaryProduction().getAmount();
            } else {
                return 0;
            }
        } else if (goodsType == null) {
            throw new IllegalArgumentException("GoodsType must not be 'null'.");
        } else if (getUnit() == null) {
            return 0;
        } else if (goodsType.equals(getUnit().getWorkType())) {
            return getProductionOf(getUnit(), goodsType);
        } else {
            return 0;
        }
    }

    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param unitType a <code>unitType</code> value
     * @return an <code>int</code> value
     */
    public Set<Modifier> getProductionModifiers(GoodsType goodsType, UnitType unitType) {
        if (goodsType == null) {
            throw new IllegalArgumentException("GoodsType must not be 'null'.");
        } else {
            Set<Modifier> result = new HashSet<Modifier>();
            if (getUnit() == null) {
                if (isColonyCenterTile() &&
                    (workTile.getType().isPrimaryGoodsType(goodsType)
                     || workTile.getType().isSecondaryGoodsType(goodsType))) {
                    result.addAll(workTile.getProductionBonus(goodsType, null));
                    result.addAll(getColony().getFeatureContainer().getModifierSet(goodsType.getId()));
                }
            } else if (goodsType.equals(getUnit().getWorkType())) {
                result.addAll(workTile.getProductionBonus(goodsType, unitType));
                result.addAll(getUnit().getModifierSet(goodsType.getId()));
            }
            return result;
        }
    }

    /**
     * Returns the production of the given type of goods which would
     * be produced by the given unit
     *
     * @param unit an <code>Unit</code> value
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getProductionOf(Unit unit, GoodsType goodsType) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be 'null'.");
        } else if (workTile.isLand() || colony.hasAbility("model.ability.produceInWater")) {
            Set<Modifier> modifiers = workTile.getProductionBonus(goodsType, unit.getType());
            if (FeatureContainer.applyModifierSet(0f, getGame().getTurn(), modifiers) > 0) {
                modifiers.addAll(unit.getModifierSet(goodsType.getId()));
                modifiers.add(colony.getProductionModifier(goodsType));
                modifiers.addAll(getColony().getModifierSet(goodsType.getId()));
                List<Modifier> modifierList = new ArrayList<Modifier>(modifiers);
                Collections.sort(modifierList);
                return Math.max(1, (int) FeatureContainer.applyModifiers(0, getGame().getTurn(), modifierList));
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Removes all references to this object.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        if (unit != null) {
            objects.addAll(unit.disposeList());
        }
        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Dispose of this ColonyTile.
     */
    public void dispose() {
        disposeList();
    }

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
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("colony", colony.getId());
        out.writeAttribute("workTile", workTile.getId());

        if (unit != null) unit.toXML(out, player, showAll, toSavedGame);

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        colony = getFreeColGameObject(in, "colony", Colony.class);
        workTile = getFreeColGameObject(in, "workTile", Tile.class);
        colonyCenterTile = (colony.getTile() == workTile);

        unit = null;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                unit = updateFreeColGameObject(in, Unit.class);
            }
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
