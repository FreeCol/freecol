/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Direction;

import org.w3c.dom.Element;

/**
 * Represents a colony. A colony contains {@link Building}s and
 * {@link ColonyTile}s. The latter represents the tiles around the
 * <code>Colony</code> where working is possible.
 */
public final class Colony extends Settlement implements Location, Nameable {

    private static final Logger logger = Logger.getLogger(Colony.class.getName());

    public static final int BELLS_PER_REBEL = 200;
    private static final int FOOD_PER_COLONIST = 200;
    private static final int FOOD_CONSUMPTION = 2;

    public static final Ability HAS_PORT = new Ability("model.ability.hasPort");

    /** The name of the colony. */
    private String name;

    /** A list of ColonyTiles. */
    private final List<ColonyTile> colonyTiles = new ArrayList<ColonyTile>();

    /** A map of Buildings, indexed by the ID of their basic type. */
    private final java.util.Map<String, Building> buildingMap = new HashMap<String, Building>();

    /** A map of ExportData, indexed by the IDs of GoodsTypes. */
    private final java.util.Map<String, ExportData> exportData = new HashMap<String, ExportData>();

    /** The SoL membership this turn. */
    private int sonsOfLiberty;

    /** The SoL membership last turn. */
    private int oldSonsOfLiberty;

    /** The number of tories this turn. */
    private int tories;

    /** The number of tories last turn. */
    private int oldTories;

    /** The current production bonus. */
    private int productionBonus;

    // Whether this colony is landlocked
    private boolean landLocked = true;

    // Will only be used on enemy colonies:
    private int unitCount = -1;

    // Temporary variable:
    private int lastVisited = -1;

    /**
     * A list of Buildable items, which is NEVER empty.
     */
    private List<BuildableType> buildQueue = new ArrayList<BuildableType>();
    

    /**
     * Creates a new <code>Colony</code>.
     * 
     * @param game The <code>Game</code> in which this object belongs.
     * @param owner The <code>Player</code> owning this <code>Colony</code>.
     * @param name The name of the new <code>Colony</code>.
     * @param tile The location of the <code>Colony</code>.
     */
    public Colony(Game game, Player owner, String name, Tile tile) {
        super(game, owner, tile);
        goodsContainer = new GoodsContainer(game, this);
        this.name = name;
        sonsOfLiberty = 0;
        oldSonsOfLiberty = 0;
        final Map map = game.getMap();
        tile.setOwner(owner);
        TileImprovement road = new TileImprovement(game, tile, FreeCol.getSpecification()
                                                   .getTileImprovementType("model.improvement.Road"));
        road.setTurnsToComplete(0);
        tile.add(road);
        for (Direction direction : Direction.values()) {
            Tile t = map.getNeighbourOrNull(direction, tile);
            if (t == null) {
                continue;
            }
            if (t.getOwner() == null) {
                t.setOwner(owner);
            }
            colonyTiles.add(new ColonyTile(game, this, t));
            if (t.getType().isWater()) {
                landLocked = false;
            }
        }
        if (!landLocked) {
            featureContainer.addAbility(HAS_PORT);
        }
        colonyTiles.add(new ColonyTile(game, this, tile));
        List<BuildingType> buildingTypes = FreeCol.getSpecification().getBuildingTypeList();
        for (BuildingType buildingType : buildingTypes) {
            if (buildingType.getUpgradesFrom() == null &&
                buildingType.getGoodsRequired().isEmpty()) {
                addBuilding(new Building(getGame(), this, buildingType));
            } else if (isFree(buildingType)) {
                addBuilding(new Building(getGame(), this, buildingType));
            }
        }
        setCurrentlyBuilding(BuildableType.NOTHING);
    }

    /**
     * Initiates a new <code>Colony</code> from an XML representation.
     * 
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public Colony(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>Colony</code> from an XML representation.
     * 
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Colony(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Colony</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Colony(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns <code>true</code> if a building of the given type can
     * be built for free.
     *
     * @param buildingType a <code>BuildingType</code> value
     * @return a <code>boolean</code> value
     */
    private boolean isFree(BuildingType buildingType) {
        float value = owner.getFeatureContainer().applyModifier(100f, 
                "model.modifier.buildingPriceBonus", buildingType, getGame().getTurn());
        return (value == 0f);
    }

    /**
     * Add a Building to this Colony.
     *
     * @param building a <code>Building</code> value
     */
    public void addBuilding(final Building building) {
        BuildingType buildingType = building.getType().getFirstLevel();
        buildingMap.put(buildingType.getId(), building);
        featureContainer.add(building.getType().getFeatureContainer());
    }

    /**
     * Updates SoL and builds Buildings that are free if possible.
     */
    public void updatePopulation() {
        // this means we might get a building for free
        for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
            if (isFree(buildingType)) {
                addBuilding(createBuilding(buildingType));
            }
        }
        getTile().updatePlayerExploredTiles();
        if (getUnitCount() > 0) {
            updateSoL();
        }
    }

    /**
     * Describe <code>getExportData</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>ExportData</code> value
     */
    public ExportData getExportData(final GoodsType goodsType) {
        ExportData result = exportData.get(goodsType.getId());
        if (result == null) {
            result = new ExportData(goodsType);
            setExportData(result);
        }
        return result;
    }

    /**
     * Describe <code>setExportData</code> method here.
     *
     * @param newExportData an <code>ExportData</code> value
     */
    public final void setExportData(final ExportData newExportData) {
        exportData.put(newExportData.getId(), newExportData);
    }

    /**
     * Returns whether this colony is landlocked, or has access to the ocean.
     * 
     * @return <code>true</code> if there are no adjacent tiles to this
     *         <code>Colony</code>'s tile being ocean tiles.
     */
    public boolean isLandLocked() {
        return landLocked;
    }

    /**
     * Returns whether this colony has undead units.
     * 
     * @return whether this colony has undead units.
     */
    public boolean isUndead() {
        final Iterator<Unit> unitIterator = getUnitIterator();
        return unitIterator.hasNext() && unitIterator.next().isUndead();
    }

    /**
     * Sets the owner of this <code>Colony</code>, including all units
     * within, and change main tile nation ownership.
     * 
     * @param owner The <code>Player</code> that shall own this
     *            <code>Settlement</code>.
     * @see Settlement#getOwner
     */
    @Override
    public void setOwner(Player owner) {
        // TODO: Erik - this only works if called on the server!
        super.setOwner(owner);
        tile.setOwner(owner);
        for (Unit unit : getUnitList()) {
            unit.setOwner(owner);
            if (unit.getLocation() instanceof ColonyTile) {
                ((ColonyTile) unit.getLocation()).getWorkTile().setOwner(owner);
            }
        }
        for (Unit target : tile.getUnitList()) {
            target.setOwner(getOwner());
        }
        // Changing the owner might alter bonuses applied by founding fathers:
        updatePopulation();
    }

    /**
     * Sets the number of units inside the colony, used in enemy colonies
     * 
     * @param unitCount The units inside the colony
     * @see #getUnitCount
     */
    public void setUnitCount(int unitCount) {
        this.unitCount = unitCount;
    }

    /**
     * Returns the building for producing the given type of goods.
     * 
     * @param goodsType The type of goods.
     * @return The <code>Building</code> which produces the given type of
     *         goods, or <code>null</code> if such a building cannot be found.
     */
    public Building getBuildingForProducing(GoodsType goodsType) {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (building.getGoodsOutputType() == goodsType) {
                return building;
            }
        }
        return null;
    }

    /** COMEBACKHEREs
     * Returns the colony's existing building for the given goods type.
     * 
     * @param goodsType The goods type.
     * @return The Building for the <code>goodsType</code>, or
     *         <code>null</code> if not exists or not fully built.
     * @see Goods
     */
    public Building getBuildingForConsuming(GoodsType goodsType) {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (building.getGoodsInputType() == goodsType) {
                return building;
            }
        }
        return null;
    }

    /**
     * Gets a <code>List</code> of every {@link WorkLocation} in this
     * <code>Colony</code>.
     * 
     * @return The <code>List</code>.
     * @see WorkLocation
     */
    public List<WorkLocation> getWorkLocations() {
        List<WorkLocation> result = new ArrayList<WorkLocation>(colonyTiles);
        result.addAll(buildingMap.values());
        return result;
    }

    /**
     * Gets a <code>List</code> of every {@link Building} in this
     * <code>Colony</code>.
     * 
     * @return The <code>List</code>.
     * @see Building
     */
    public List<Building> getBuildings() {
        return new ArrayList<Building>(buildingMap.values());
    }

    /**
     * Gets a <code>List</code> of every {@link ColonyTile} in this
     * <code>Colony</code>.
     * 
     * @return The <code>List</code>.
     * @see ColonyTile
     */
    public List<ColonyTile> getColonyTiles() {
        return colonyTiles;
    }

    /**
     * Gets a <code>Building</code> of the specified type.
     * 
     * @param type The type of the building to get.
     * @return The <code>Building</code>.
     */
    public Building getBuilding(BuildingType type) {
        return buildingMap.get(type.getFirstLevel().getId());
    }


    /**
     * Returns a <code>Building</code> with the given
     * <code>Ability</code>, or null, if none exists.
     *
     * @param ability a <code>String</code> value
     * @return a <code>Building</code> value
     */
    public Building getBuildingWithAbility(String ability) {
        for (Building building : buildingMap.values()) {
            if (building.getType().hasAbility(ability)) {
                return building;
            }
        }
        return null;
    }

    /**
     * Gets the specified <code>ColonyTile</code>.
     * 
     * @param x The x-coordinate of the <code>Tile</code>.
     * @param y The y-coordinate of the <code>Tile</code>.
     * @return The <code>ColonyTile</code> for the <code>Tile</code>
     *         returned by {@link #getTile(int, int)}.
     */
    public ColonyTile getColonyTile(int x, int y) {
        Tile t = getTile(x, y);
        for (ColonyTile c : colonyTiles) {
            if (c.getWorkTile() == t) {
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the <code>ColonyTile</code> matching the given
     * <code>Tile</code>.
     * 
     * @param t The <code>Tile</code> to get the <code>ColonyTile</code>
     *            for.
     * @return The <code>ColonyTile</code>
     */
    public ColonyTile getColonyTile(Tile t) {
        for (ColonyTile c : colonyTiles) {
            if (c.getWorkTile() == t) {
                return c;
            }
        }
        return null;
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     * 
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    @Override
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            if (((Unit) locatable).isColonist()) {
                WorkLocation w = getVacantWorkLocationFor((Unit) locatable);
                if (w == null) {
                    logger.warning("Could not find a 'WorkLocation' for " + locatable + " in " + this);
                } else {
                    locatable.setLocation(w);
                }
            } else {
                locatable.setLocation(getTile());
            }
            updatePopulation();
        } else if (locatable instanceof Goods) {
            goodsContainer.addGoods((Goods) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a 'Colony'.");
        }
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     * 
     * @param locatable The <code>Locatable</code> to remove from this
     *            Location.
     */
    @Override
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            for (WorkLocation w : getWorkLocations()) {
                if (w.contains(locatable)) {
                    w.remove(locatable);
                    updatePopulation();
                    return;
                }
            }
        } else if (locatable instanceof Goods) {
            goodsContainer.removeGoods((Goods) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a 'Colony'.");
        }
    }

    /**
     * Gets the amount of Units at this Location. These units are located in a
     * {@link WorkLocation} in this <code>Colony</code>.
     * 
     * @return The amount of Units at this Location.
     */
    @Override
    public int getUnitCount() {
        int count = 0;
        if (unitCount != -1) {
            return unitCount;
        }
        for (WorkLocation w : getWorkLocations()) {
            count += w.getUnitCount();
        }
        return count;
    }

    /**
     * Gives the food needed to keep all current colonists alive in this colony.
     * 
     * @return The amount of food eaten in this colony each this turn.
     */
    public int getFoodConsumption() {
        return FOOD_CONSUMPTION * getUnitCount();
    }

    /**
     * Gets the amount of one type of Goods at this Colony.
     * 
     * @param type The type of goods to look for.
     * @return The amount of this type of Goods at this Location.
     */
    public int getGoodsCount(GoodsType type) {
        if (type.getStoredAs() == null) {
            return goodsContainer.getGoodsCount(type);
        } else {
            return goodsContainer.getGoodsCount(type.getStoredAs());
        }
    }

    /**
     * Removes a specified amount of a type of Goods from this container.
     * 
     * @param type The type of Goods to remove from this container.
     * @param amount The amount of Goods to remove from this container.
     */
    public void removeGoods(GoodsType type, int amount) {
        goodsContainer.removeGoods(type, amount);
    }

    /**
     * Removes the given Goods from the Colony.
     *
     * @param goods a <code>Goods</code> value
     */
    public void removeGoods(AbstractGoods goods) {
        goodsContainer.removeGoods(goods);
    }

    /**
     * Removes all Goods of the given type from the Colony.
     *
     * @param type a <code>GoodsType</code> value
     */
    public void removeGoods(GoodsType type) {
        goodsContainer.removeGoods(type);
    }

    /**
     * Describe <code>addGoods</code> method here.
     *
     * @param type a <code>GoodsType</code> value
     * @param amount an <code>int</code> value
     */
    public void addGoods(GoodsType type, int amount) {
        if (type.getStoredAs() != null) {
            goodsContainer.addGoods(type.getStoredAs(), amount);
        } else {
            goodsContainer.addGoods(type, amount);
        }
    }

    public void addGoods(AbstractGoods goods) {
        addGoods(goods.getType(), goods.getAmount());
    }

    public List<Unit> getUnitList() {
        ArrayList<Unit> units = new ArrayList<Unit>();
        for (WorkLocation wl : getWorkLocations()) {
            for (Unit unit : wl.getUnitList()) {
                units.add(unit);
            }
        }
        return units;
    }
    
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    @Override
    public boolean contains(Locatable locatable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAdd(Locatable locatable) {
        if (locatable instanceof Unit && ((Unit) locatable).getOwner() == getOwner()) {
            return true;
        } else if (locatable instanceof Goods) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this colony has a schoolhouse and the unit type is a
     * skilled unit type with a skill level not exceeding the level of the
     * schoolhouse. @see Building#canAdd
     * 
     * @param unit The unit to add as a teacher.
     * @return <code>true</code> if this unit type could be added.
    */
    public boolean canTrain(Unit unit) {
        return canTrain(unit.getType());
    }

    /**
     * Returns true if this colony has a schoolhouse and the unit type is a
     * skilled unit type with a skill level not exceeding the level of the
     * schoolhouse. The number of units already in the schoolhouse and
     * the availability of pupils are not taken into account. @see
     * Building#canAdd
     * 
     * @param unitType The unit type to add as a teacher.
     * @return <code>true</code> if this unit type could be added.
    */
    public boolean canTrain(UnitType unitType) {
        if (!hasAbility("model.ability.teach")) {
            return false;
        }
        
        for (Building building : buildingMap.values()) {
            if (building.getType().hasAbility("model.ability.teach") &&
                building.canAdd(unitType)) {
                return true;
            }
        }
        return false;
    }
    
    public List<Unit> getTeachers() {
        List<Unit> teachers = new ArrayList<Unit>();
        for (Building building : buildingMap.values()) {
            if (building.getType().hasAbility("model.ability.teach")) {
                teachers.addAll(building.getUnitList());
            }
        }
        return teachers;
    }

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Colony</code>.
     * <p>
     * Note that this function will only return a unit working inside the colony.
     * Typically, colonies are also defended by units outside the colony on the same tile.
     * To consider units outside the colony as well, use (@see Tile#getDefendingUnit) instead.
     * <p>
     * Returns an arbitrary unarmed land unit unless Paul Revere is present 
     * as founding father, in which case the unit can be armed as well. 
     * 
     * @param attacker The unit that would be attacking this colony.
     * @return The <code>Unit</code> that has been chosen to defend this
     *         colony.
     * @see Tile#getDefendingUnit(Unit)
     * @throws IllegalStateException if there are units in the colony
     */
    @Override
    public Unit getDefendingUnit(Unit attacker) {
        Unit defender = null;
        float defencePower = -1.0f;
        for (Unit nextUnit : getUnitList()) {
            float tmpPower = getGame().getCombatModel().getDefencePower(attacker, nextUnit);
            if (tmpPower > defencePower) {
                defender = nextUnit;
                defencePower = tmpPower;
            }
        }
        if (defender == null) {
            throw new IllegalStateException("Colony " + name + " contains no units!");
        } else {
            return defender;
        }
    }

    /**
     * Returns a <code>List</code> with every unit type this colony may
     * build.
     * 
     * @return A <code>List</code> with <code>UnitType</code>
     */
    public List<UnitType> getBuildableUnits() {
        ArrayList<UnitType> buildableUnits = new ArrayList<UnitType>();
        List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();
        for (UnitType unitType : unitTypes) {
            if (unitType.getGoodsRequired().isEmpty() == false && canBuild(unitType)) {
                buildableUnits.add(unitType);
            }
        }
        return buildableUnits;
    }

    /**
     * Returns the type of building currently being built.
     * 
     * @return The type of building currently being built.
     */
    public BuildableType getCurrentlyBuilding() {
        return buildQueue.get(0);
    }

    /**
     * Sets the type of building to be built.
     * 
     * @param buildable The type of building to be built.
     */
    public void setCurrentlyBuilding(BuildableType buildable) {
        buildQueue.add(0, buildable);
    }

    /**
     * Sets the type of building to None, so no building is done.
     */
    public void stopBuilding() {
        if (getCurrentlyBuilding() != BuildableType.NOTHING) {
            setCurrentlyBuilding(BuildableType.NOTHING);
        }
    }

    /**
     * Get the <code>BuildQueue</code> value.
     *
     * @return a <code>List<Buildable></code> value
     */
    public List<BuildableType> getBuildQueue() {
        return buildQueue;
    }

    /**
     * Set the <code>BuildQueue</code> value.
     *
     * @param newBuildQueue The new BuildQueue value.
     */
    public void setBuildQueue(final List<BuildableType> newBuildQueue) {
        this.buildQueue = newBuildQueue;
    }

    /**
     * Adds to the bell count of the colony. Used only by DebugMenu.
     * 
     * @param amount The number of bells to add.
     */
    public void addBells(int amount) {
        getOwner().incrementBells(amount);
        if (getMembers() <= getUnitCount() + 1 && amount > 0) {
            addGoods(Goods.BELLS, amount);
        }
    }

    /**
     * Returns the current SoL membership of the colony.
     * 
     * @return The current SoL membership of the colony.
     */
    public int getSoL() {
        return sonsOfLiberty;
    }

    /**
     * Calculates the current SoL membership of the colony based on the number
     * of bells and colonists.
     */
    private void updateSoL() {
        int units = getUnitCount();
        if (units <= 0) {
            return;
        }
        // Update "addSol(int)" and "getMembers()" if this formula gets changed:
        int membership = (getGoodsCount(Goods.BELLS) * 100) / (BELLS_PER_REBEL * units);
        if (membership < 0) {
            membership = 0;
        } else if (membership > 100) {
            membership = 100;
        }
        sonsOfLiberty = membership;
        tories = (units - getMembers());
    }

    /**
     * Return the number of sons of liberty
     */
    public int getMembers() {
	float result = (sonsOfLiberty * getUnitCount()) / 100f;
        return Math.round(result);
    }

    /**
     * Returns the Tory membership of the colony.
     * 
     * @return The current Tory membership of the colony.
     */
    public int getTory() {
        return 100 - getSoL();
    }

    /**
     * Returns the production bonus, if any, of the colony.
     * 
     * @return The current production bonus of the colony.
     */
    public int getProductionBonus() {
        return productionBonus;
    }

    /**
     * Gets a string representation of the Colony. Currently this method just
     * returns the name of the <code>Colony</code>, but that may change
     * later.
     * 
     * @return The name of the colony.
     * @see #getName
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the name of this <code>Colony</code>.
     * 
     * @return The name as a <code>String</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this <code>Colony</code>.
     * 
     * @param newName The new name of this Colony.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public String getLocationName() {
        return name;
    }

    /**
     * Gets the combined production of all food types.
     *
     * @return an <code>int</code> value
     */
    public int getFoodProduction() {
        int result = 0;
        for (GoodsType foodType : FreeCol.getSpecification().getGoodsFood()) {
            result += getProductionOf(foodType);
        }
        return result;
    }

    /**
     * Returns the production of the given type of goods.
     * 
     * @param goodsType The type of goods to get the production for.
     * @return The production of the given type of goods the current turn by all
     *         of the <code>Colony</code>'s {@link Building buildings} and
     *         {@link ColonyTile tiles}.
     */
    public int getProductionOf(GoodsType goodsType) {
        int amount = 0;
        for (WorkLocation workLocation : getWorkLocations()) {
            amount += workLocation.getProductionOf(goodsType);
        }
        return amount;
    }

    /**
     * Gets a vacant <code>WorkLocation</code> for the given <code>Unit</code>.
     * 
     * @param unit The <code>Unit</code>
     * @return A vacant <code>WorkLocation</code> for the given
     *         <code>Unit</code> or <code>null</code> if there is no such
     *         location.
     */
    public WorkLocation getVacantWorkLocationFor(Unit unit) {
        for (GoodsType foodType : FreeCol.getSpecification().getGoodsFood()) {
            WorkLocation colonyTile = getVacantColonyTileFor(unit, foodType);
            if (colonyTile != null) {
                return colonyTile;
            }
        }
        for (Building building : buildingMap.values()) {
            if (building.canAdd(unit)) {
                return building;
            }
        }
        return null;
    }

    /**
     * Returns a vacant <code>ColonyTile</code> where the given
     * <code>unit</code> produces the maximum output of the given
     * <code>goodsType</code>.
     * 
     * @param unit The <code>Unit</code> to find a vacant
     *            <code>ColonyTile</code> for.
     * @param goodsType The type of goods that should be produced.
     * @return The <code>ColonyTile</code> giving the highest production of
     *         the given goods for the given unit or <code>null</code> if
     *         there is no available <code>ColonyTile</code> for producing
     *         that goods.
     */
    public ColonyTile getVacantColonyTileFor(Unit unit, GoodsType goodsType) {
        ColonyTile bestPick = null;
        int highestProduction = 0;
        for (ColonyTile colonyTile : colonyTiles) {
            if (colonyTile.canAdd(unit)) {
                Tile workTile = colonyTile.getWorkTile();
                /*
                 * canAdd ensures workTile it's empty or unit it's working in it
                 * so unit can work in it if it's owned by none, by europeans or
                 * unit's owner has the founding father Peter Minuit
                 */

                if (owner.getLandPrice(workTile) == 0) {
                    int potential = colonyTile.getProductionOf(unit, goodsType);
                    if (potential > highestProduction) {
                        highestProduction = potential;
                        bestPick = colonyTile;
                    }
                }
            }
        }
        return bestPick;
    }

    /**
     * Returns the production of a vacant <code>ColonyTile</code> where the
     * given <code>unit</code> produces the maximum output of the given
     * <code>goodsType</code>.
     * 
     * @param unit The <code>Unit</code> to find the highest possible
     *            <code>ColonyTile</code>-production for.
     * @param goodsType The type of goods that should be produced.
     * @return The highest possible production on a vacant
     *         <code>ColonyTile</code> for the given goods and the given unit.
     */
    public int getVacantColonyTileProductionFor(Unit unit, GoodsType goodsType) {
        ColonyTile bestPick = getVacantColonyTileFor(unit, goodsType);
        return bestPick == null ? 0 : bestPick.getProductionOf(unit, goodsType);
    }

    /**
     * Returns how much of a Good will be produced by this colony this turn
     * 
     * @param goodsType The goods' type.
     * @return The amount of the given goods will be produced for next turn.
     */
    public int getProductionNextTurn(GoodsType goodsType) {
        int count = 0;
        Building building = getBuildingForProducing(goodsType);
        if (building == null) {
            count = getProductionOf(goodsType);
        } else {
            count = building.getProductionNextTurn();
        }
        return count;
    }

    /**
     * Returns how much of a Good will be produced by this colony this turn,
     * taking into account how much is consumed - by workers, horses, etc.
     * 
     * @param goodsType The goods' type.
     * @return The amount of the given goods currently unallocated for next
     *         turn.
     */
    public int getProductionNetOf(GoodsType goodsType) {
        int count = getProductionNextTurn(goodsType);
        int used = 0;
        Building bldg = getBuildingForConsuming(goodsType);
        if (bldg != null) {
            used = bldg.getGoodsInputNextTurn();
        }

        if (goodsType.isStorable()) {
            if (goodsType.isFoodType()) {
                used += getFoodConsumption();
            }
            BuildableType currentBuildable = getCurrentlyBuilding();
            if (currentBuildable != null &&
                currentBuildable != BuildableType.NOTHING &&
                currentBuildable.getGoodsRequired().isEmpty() == false) {
                boolean willBeFinished = true;
                int possiblyUsed = 0;
                for (AbstractGoods goodsRequired : currentBuildable.getGoodsRequired()) {
                    GoodsType requiredType = goodsRequired.getType();
                    int requiredAmount = goodsRequired.getAmount();
                    int presentAmount = getGoodsCount(requiredType);
                    if (requiredType.equals(goodsType)) {
                        if (presentAmount + (count - used) < requiredAmount) {
                            willBeFinished = false;
                            break;
                        } else if (presentAmount < requiredAmount) {
                            possiblyUsed = requiredAmount - presentAmount;
                        }
                    } else if (getGoodsCount(requiredType) + getProductionNextTurn(requiredType) <
                               goodsRequired.getAmount()) {
                        willBeFinished = false;
                        break;
                    }
                }
                if (willBeFinished && possiblyUsed > 0) {
                    used += possiblyUsed;
                }
            }
        }
        return count - used;
    }

    /**
     * Returns <code>true</code> if this Colony can breed the given
     * type of Goods. Only animals (such as horses) are expected to be
     * breedable.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBreed(GoodsType goodsType) {
        int breedingNumber = goodsType.getBreedingNumber();
        return (breedingNumber != GoodsType.NO_BREEDING &&
                breedingNumber <= getGoodsCount(goodsType));
    }


    /**
     * Describe <code>canBuild</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBuild() {
        return canBuild(getCurrentlyBuilding());
    }

    /**
     * Returns true if this Colony can build the given BuildableType.
     *
     * @param buildableType a <code>BuildableType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBuild(BuildableType buildableType) {
        if (buildableType == null || buildableType == BuildableType.NOTHING) {
            return false;
        } else if (buildableType.getGoodsRequired().isEmpty()) {
            return false;
        } else if (buildableType.getPopulationRequired() > getUnitCount()) {
            return false;
        } else {
            java.util.Map<String, Boolean> requiredAbilities = buildableType.getAbilitiesRequired();
            for (Entry<String, Boolean> entry : requiredAbilities.entrySet()) {
                if (hasAbility(entry.getKey()) != entry.getValue()) {
                    return false;
                }
            }
        }
        if (buildableType instanceof BuildingType) {
            BuildingType newBuildingType = (BuildingType) buildableType;
            Building colonyBuilding = this.getBuilding(newBuildingType);
            if (colonyBuilding == null) {
                // the colony has no similar building yet
                if (newBuildingType.getUpgradesFrom() != null) {
                    // we are trying to build an advanced factory, we should build lower level shop first
                    return false;
                }
            } else {
                // a building of the same family already exists
                if (colonyBuilding.getType().getUpgradesTo() != newBuildingType) {
                    // the existing building's next upgrade is not the new one we want to build
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if this Colony could build at least one item of the
     * given EquipmentType.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBuildEquipment(EquipmentType equipmentType) {
        for (AbstractGoods requiredGoods : equipmentType.getGoodsRequired()) {
            if (getGoodsCount(requiredGoods.getType()) < requiredGoods.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private Building createBuilding(BuildingType buildingType) {
        return getGame().getModelController().createBuilding(getId() + "buildBuilding", this, buildingType);
    }

    private void checkBuildableComplete() {
        // In order to avoid duplicate messages:
        if (lastVisited == getGame().getTurn().getNumber()) {
            return;
        }
        lastVisited = getGame().getTurn().getNumber();
        if (canBuild()) {
            BuildableType buildable = getCurrentlyBuilding();
            ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
            for (AbstractGoods goodsRequired : buildable.getGoodsRequired()) {
                GoodsType requiredGoodsType = goodsRequired.getType();
                int available = getGoodsCount(requiredGoodsType);
                int required = goodsRequired.getAmount();
                if (available < required) {
                    if (!requiredGoodsType.isStorable()) {
                        // buildable is not complete and we don't care
                        // about missing goods because unstorable
                        // goods (e.g. hammers) are still missing
                        return;
                    }
                    messages.add(new ModelMessage(this, ModelMessage.MessageType.MISSING_GOODS,
                                                  requiredGoodsType,
                                                  "model.colony.buildableNeedsGoods",
                                                  "%colony%", getName(),
                                                  "%buildable%", buildable.getName(),
                                                  "%amount%", String.valueOf(required - available),
                                                  "%goodsType%", requiredGoodsType.getName()));
                }
            }
            if (messages.isEmpty()) {
                // no messages means all goods are present
                for (AbstractGoods goodsRequired : buildable.getGoodsRequired()) {
                    if (getGameOptions().getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW) ||
                        goodsRequired.getType().isStorable()) {
                        removeGoods(goodsRequired);
                    } else {
                        // waste excess goods
                        removeGoods(goodsRequired.getType());
                    }
                }
                if (buildable instanceof UnitType) {
                    // artillery/ship/wagon completed
                    Unit unit = getGame().getModelController().createUnit(getId() + "buildUnit", getTile(), getOwner(),
                                                                          (UnitType) buildable);
                    addModelMessage(this, ModelMessage.MessageType.UNIT_ADDED, unit,
                            "model.colony.unitReady",
                            "%colony%", getName(),
                            "%unit%", unit.getName());
                } else if (buildable instanceof BuildingType) {
                    // building completed
                    BuildingType upgradesFrom = ((BuildingType) buildable).getUpgradesFrom();
                    if (upgradesFrom == null) {
                        addBuilding(createBuilding((BuildingType) buildable));
                    } else {
                        getBuilding(upgradesFrom).upgrade();
                    }
                    addModelMessage(this, ModelMessage.MessageType.BUILDING_COMPLETED, this,
                            "model.colony.buildingReady", 
                            "%colony%", getName(),
                            "%building%", buildable.getName());
                    buildQueue.remove(0);
                    if (buildQueue.isEmpty()) {
                        buildQueue.add(BuildableType.NOTHING);
                    }
                    if (buildQueue.size()==1 && buildQueue.get(0)==BuildableType.NOTHING) {
                        addModelMessage(this, ModelMessage.MessageType.WARNING, this, 
                                "model.colony.cannotBuild", 
                                "%colony%", getName());
                    }
                }
            } else {
                for (ModelMessage message : messages) {
                    owner.addModelMessage(message);
                }
            }
        }
    }

    /**
     * Returns the price for the remaining hammers and tools for the
     * {@link Building} that is currently being built.
     * 
     * @return The price.
     * @see #payForBuilding
     */
    public int getPriceForBuilding() {
        // Any changes in this method should also be reflected in
        // "payForBuilding()"
        int price = 0;
        for (AbstractGoods goodsRequired : getCurrentlyBuilding().getGoodsRequired()) {
            GoodsType requiredGoodsType = goodsRequired.getType();
            int remaining = goodsRequired.getAmount() - getGoodsCount(requiredGoodsType);
            if (remaining > 0) {
                if (requiredGoodsType.isStorable()) {
                    price += (getOwner().getMarket().getBidPrice(requiredGoodsType, remaining) * 110) / 100;
                } else {
                    price += requiredGoodsType.getPrice() * remaining;
                }
            }
        }
        return price;
    }

    /**
     * Buys the remaining hammers and tools for the {@link Building} that is
     * currently being built.
     * 
     * @exception IllegalStateException If the owner of this <code>Colony</code>
     *                has an insufficient amount of gold.
     * @see #getPriceForBuilding
     */
    public void payForBuilding() {
        // Any changes in this method should also be reflected in
        // "getPriceForBuilding()"
        if (getPriceForBuilding() > getOwner().getGold()) {
            throw new IllegalStateException("Not enough gold.");
        }
        for (AbstractGoods goodsRequired : getCurrentlyBuilding().getGoodsRequired()) {
            GoodsType requiredGoodsType = goodsRequired.getType();
            int remaining = goodsRequired.getAmount() - getGoodsCount(requiredGoodsType);
            if (remaining > 0) {
                if (requiredGoodsType.isStorable()) {
                    getOwner().getMarket().buy(requiredGoodsType, remaining, getOwner());
                } else {
                    getOwner().modifyGold(-remaining * requiredGoodsType.getPrice());
                }
                addGoods(requiredGoodsType, remaining);
            }
        }
    }

    /**
     * determine if there is a problem with the production of the specified good
     *
     * @param goodsType  for this good
     * @param amount     warehouse amount
     * @param production production per turn
     * @return all warnings
     */
    public Collection<String> getWarnings(GoodsType goodsType, int amount, int production) {
        List<String> result = new LinkedList<String>();

        if (goodsType.isFoodType() && goodsType.isStorable()) {
            if (amount + production < 0) {
                result.add(Messages.message("model.colony.famineFeared",
                        "%colony%", getName(),
                        "%number%", "0"));
            }
        } else {
            //food is never wasted -> new settler is produced
            int waste = (amount + production - getWarehouseCapacity());
            if (waste > 0 && !getExportData(goodsType).isExported() && !goodsType.limitIgnored()) {
                result.add(Messages.message("model.building.warehouseSoonFull",
                        "%goods%", goodsType.getName(),
                        "%colony%", getName(),
                        "%amount%", String.valueOf(waste)));

            }
        }

        BuildableType currentlyBuilding = getCurrentlyBuilding();
        if (currentlyBuilding != BuildingType.NOTHING) {
            for (AbstractGoods goods : currentlyBuilding.getGoodsRequired()) {
                if (goods.getType().equals(goodsType) && amount < goods.getAmount()) {
                    result.add(Messages.message("model.colony.buildableNeedsGoods",
                                                "%colony%", getName(),
                                                "%buildable%", currentlyBuilding.getName(),
                                                "%amount%", String.valueOf(goods.getAmount() - amount),
                                                "%goodsType%", goodsType.getName()));
                }
            }
        }

        addInsufficientProductionMessage(result, getBuildingForProducing(goodsType));

        Building buildingForConsuming = getBuildingForConsuming(goodsType);
        if (buildingForConsuming != null && !buildingForConsuming.getGoodsOutputType().isStorable()) {
            //the warnings are for a non-storable good, which is not displayed in the trade report
            addInsufficientProductionMessage(result, buildingForConsuming);
        }

        return result;
    }

    /**
     * adds a message about insufficient production for a building
     *
     * @param warnings where to add the warnings
     * @param building for this building
     */
    private void addInsufficientProductionMessage(List<String> warnings, Building building) {
        if (building != null) {
            int delta = building.getMaximumProduction() - building.getProductionNextTurn();
            if (delta > 0) {
                warnings.add(createInsufficientProductionMessage(
                        building.getGoodsOutputType(),
                        delta,
                        building.getGoodsInputType(),
                        building.getMaximumGoodsInput() - building.getGoodsInputNextTurn()));
            }
        }
    }

    /**
     * create a message about insufficient production
     *
     * @param outputType    output type
     * @param missingOutput missing output
     * @param inputType     input type
     * @param missingInput  missing input
     * @return message
     */
    private String createInsufficientProductionMessage(GoodsType outputType, int missingOutput,
                                                       GoodsType inputType, int missingInput) {
        return Messages.message("model.colony.insufficientProduction",
                "%outputAmount%", String.valueOf(missingOutput),
                "%outputType%", outputType.getName(),
                "%colony%", getName(),
                "%inputAmount%", String.valueOf(missingInput),
                "%inputType%", inputType.getName());
    }

    /**
     * Returns a random unit from this colony. At this moment, this method
     * always returns the first unit in the colony.
     * 
     * @return A random unit from this <code>Colony</code>. This
     *         <code>Unit</code> will either be working in a {@link Building}
     *         or a {@link ColonyTile}.
     */
    public Unit getRandomUnit() {
        return getFirstUnit();
        // return getUnitIterator().hasNext() ? getUnitIterator().next() : null;
    }

    private Unit getFirstUnit() {
        for (WorkLocation wl : getWorkLocations()) {
            Iterator<Unit> unitIterator = wl.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit o = unitIterator.next();
                if (o != null) {
                    return o;
                }
            }
        }
        return null;
    }

    // Save state of warehouse
    private void saveWarehouseState() {
        logger.finest("Saving state of warehouse in " + getName());
        getGoodsContainer().saveState();
    }


    // Update all colony tiles
    private void addColonyTileProduction() {
        for (ColonyTile colonyTile : colonyTiles) {
            logger.finest("Calling newTurn for colony tile " + colonyTile.toString());
            colonyTile.newTurn();
        }
    }

    private void removeFood(final int amount) {
        int rest = amount;
        List<AbstractGoods> backlog = new ArrayList<AbstractGoods>();
        for (GoodsType foodType : FreeCol.getSpecification().getGoodsFood()) {
            int available = getGoodsCount(foodType);
            if (available >= rest) {
                removeGoods(foodType, rest);
                for (AbstractGoods food : backlog) {
                    removeGoods(food.getType(), food.getAmount());
                }
                rest = 0;
            } else {
                backlog.add(new AbstractGoods(foodType, available));
                rest -= available;
            }
        }
        if (rest > 0) {
            throw new IllegalStateException("Attempted to remove more food than was present.");
        }
    }
            
    public int getFoodCount() {
        int result = 0;
        for (GoodsType foodType : FreeCol.getSpecification().getGoodsFood()) {
            result += getGoodsCount(foodType);
        }
        return result;
    }


    // Eat food:
    void updateFood() {
        int required = getFoodConsumption();
        int available = getFoodCount();
        int production = getFoodProduction();

        if (required > available) {
            // Kill a colonist:
            getRandomUnit().dispose();
            removeFood(available);
            addModelMessage(this, ModelMessage.MessageType.UNIT_LOST,
                            "model.colony.colonistStarved", "%colony%", getName());
        } else {
            removeFood(required);
            if (required > production){
            	int turnsToLive = (available - required) / (required - production);
            	if(turnsToLive <= 3) {
            		addModelMessage(this, ModelMessage.MessageType.WARNING,
                                "model.colony.famineFeared", "%colony%", getName(),
                                "%number%", String.valueOf(turnsToLive));
            	}
            }
        }
    }

    // Create a new colonist if there is enough food:
    private void checkForNewColonist() {
        if (getFoodCount() >= FOOD_PER_COLONIST) {
            List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.bornInColony");
            if (!unitTypes.isEmpty()) {
                int random = getGame().getModelController().getRandom(getId() + "bornInColony", unitTypes.size());
                Unit u = getGame().getModelController().createUnit(getId() + "newTurn200food",
                                                getTile(), getOwner(), unitTypes.get(random));
                removeFood(FOOD_PER_COLONIST);
                addModelMessage(this, ModelMessage.MessageType.UNIT_ADDED, u,
                                "model.colony.newColonist", "%colony%", getName());
                logger.info("New colonist created in " + getName() + " with ID=" + u.getId());
            }
        }
    }

    // Export goods if custom house is built
    private void exportGoods() {
        if (hasAbility("model.ability.export")) {
            List<Goods> exportGoods = getCompactGoods();
            for (Goods goods : exportGoods) {
                GoodsType type = goods.getType();
                ExportData data = getExportData(type);
                if (data.isExported() && (owner.canTrade(goods, Market.CUSTOM_HOUSE))) {
                    int amount = goods.getAmount() - data.getExportLevel();
                    if (amount > 0) {
                        removeGoods(type, amount);
                        getOwner().getMarket().sell(type, amount, owner, Market.CUSTOM_HOUSE);
                    }
                }
            }
        }
    }


    // Warn about levels that will be exceeded next turn
    private void createWarehouseCapacityWarning() {
        List<Goods> storedGoods = getGoodsContainer().getFullGoods();
        for (Goods goods : storedGoods) {
            if (!goods.getType().isStorable()) {
                // no need to do anything about bells or crosses
                continue;
            }
            if (getExportData(goods.getType()).isExported() &&
                owner.canTrade(goods, Market.CUSTOM_HOUSE)) {
                // capacity will never be exceeded
                continue;
            } else if (goods.getAmount() < getWarehouseCapacity()) {
                int waste = (goods.getAmount() + getProductionNetOf(goods.getType()) -
                             getWarehouseCapacity());
                if (waste > 0) {
                    addModelMessage(this, ModelMessage.MessageType.WAREHOUSE_CAPACITY, goods.getType(),
                                    "model.building.warehouseSoonFull",
                                    "%goods%", goods.getName(),
                                    "%colony%", getName(),
                                    "%amount%", String.valueOf(waste));
                }
            }
        }
    }


    private void createSoLMessages() {
        final int veryBadGovernment = getOwner().getDifficulty().getVeryBadGovernmentLimit();
        final int badGovernment = getOwner().getDifficulty().getBadGovernmentLimit();
        if (sonsOfLiberty / 10 != oldSonsOfLiberty / 10) {
            if (sonsOfLiberty > oldSonsOfLiberty) {
                addModelMessage(this, ModelMessage.MessageType.SONS_OF_LIBERTY,
                                FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                "model.colony.SoLIncrease", 
                                "%oldSoL%", String.valueOf(oldSonsOfLiberty),
                                "%newSoL%", String.valueOf(sonsOfLiberty),
                                "%colony%", getName());
            } else {
                addModelMessage(this, ModelMessage.MessageType.SONS_OF_LIBERTY,
                                FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                "model.colony.SoLDecrease", 
                                "%oldSoL%", String.valueOf(oldSonsOfLiberty),
                                "%newSoL%", String.valueOf(sonsOfLiberty),
                                "%colony%", getName());

            }
        }

        int bonus = 0;
        if (sonsOfLiberty == 100) {
            // there are no tories left
            bonus = 2;
            if (oldSonsOfLiberty < 100) {
                addModelMessage(this, ModelMessage.MessageType.SONS_OF_LIBERTY,
                                FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                "model.colony.SoL100", "%colony%", getName());
            }
        } else {
            if (sonsOfLiberty >= 50) {
                bonus += 1;
                if (oldSonsOfLiberty < 50) {
                    addModelMessage(this, ModelMessage.MessageType.SONS_OF_LIBERTY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                    "model.colony.SoL50", "%colony%", getName());
                }
            }
            if (tories > veryBadGovernment) {
                bonus -= 2;
                if (oldTories <= veryBadGovernment) {
                    // government has become very bad
                    addModelMessage(this, ModelMessage.MessageType.GOVERNMENT_EFFICIENCY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                    "model.colony.veryBadGovernment", "%colony%", getName());
                }
            } else if (tories > badGovernment) {
                bonus -= 1;
                if (oldTories <= badGovernment) {
                    // government has become bad
                    addModelMessage(this, ModelMessage.MessageType.GOVERNMENT_EFFICIENCY, 
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                    "model.colony.badGovernment", "%colony%", getName());
                } else if (oldTories > veryBadGovernment) {
                    // government has improved, but is still bad
                    addModelMessage(this, ModelMessage.MessageType.GOVERNMENT_EFFICIENCY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                    "model.colony.governmentImproved1", "%colony%", getName());
                }
            } else if (oldTories > badGovernment) {
                // government was bad, but has improved
                addModelMessage(this, ModelMessage.MessageType.GOVERNMENT_EFFICIENCY, 
                                FreeCol.getSpecification().getGoodsType("model.goods.bells"),
                                "model.colony.governmentImproved2", "%colony%", getName());
            }
        }

        // TODO-LATER: REMOVE THIS WHEN THE AI CAN HANDLE PRODUCTION PENALTIES:
        if (getOwner().isAI()) {
            productionBonus = Math.max(0, bonus);
        } else {
            productionBonus = bonus;
        }

    }


    /**
     * Prepares this <code>Colony</code> for a new turn.
     */
    @Override
    public void newTurn() {
        // Skip doing work in enemy colonies.
        if (unitCount != -1) {
            return;
        }

        if (getTile() == null) {
            // Fix NullPointerException below
            logger.warning("Colony " + getName() + " lacks a tile!");
            return;
        }

        // TODO: make warehouse a building
        saveWarehouseState();

        addColonyTileProduction();
        updateFood();
        if (getUnitCount() == 0) {
            dispose();
            return;
        }

        List<GoodsType> goodsForBuilding = new ArrayList<GoodsType>();
        if (canBuild()) {
            for (AbstractGoods goodsRequired : getCurrentlyBuilding().getGoodsRequired()) {
                goodsForBuilding.add(goodsRequired.getType());
            }
        } else {
            // something is preventing construction
            BuildableType currentlyBuilding = getCurrentlyBuilding();
            if (currentlyBuilding == null || currentlyBuilding == BuildableType.NOTHING) {
                // production idle
                addModelMessage(this, ModelMessage.MessageType.WARNING, this, 
                        "model.colony.cannotBuild", 
                        "%colony%", getName());
            } else if (currentlyBuilding.getPopulationRequired() > getUnitCount()) {
                // not enough units
                addModelMessage(this, ModelMessage.MessageType.WARNING, this, 
                        "model.colony.buildNeedPop", 
                        "%colony%", getName(), 
                        "%building%", currentlyBuilding.getName());
            }
        }
        
        List<Building> buildings1 = new ArrayList<Building>();
        List<Building> buildings2 = new ArrayList<Building>();
        for (Building building : getBuildings()) {
            if (building.getType().hasAbility("model.ability.autoProduction")) {
                // call auto-producing buildings immediately
                logger.finest("Calling newTurn for building " + building.getName());
                building.newTurn();
            } else if (goodsForBuilding.contains(building.getGoodsOutputType())) {
                buildings1.add(building);
            } else {
                buildings2.add(building);
            }
        }

        // buildings that produce building materials
        for (Building building : buildings1) {
            logger.finest("Calling newTurn for building " + building.getName());
            building.newTurn();
        }

        // The following tasks consume building materials or may do so
        // in the future
        checkBuildableComplete();

        // buildings that do not produce building materials, but might
        // consume them
        for (Building building : buildings2) {
            logger.finest("Calling newTurn for building " + building.getName());
            building.newTurn();
        }

        // must be after building production because horse production
        // consumes some food
        checkForNewColonist();
        exportGoods();
        // Throw away goods there is no room for.
        goodsContainer.cleanAndReport();
        // TODO: make warehouse a building
        createWarehouseCapacityWarning();

        // Remove bells:
        removeGoods(Goods.BELLS, Math.max(0, getUnitCount() - 2));

        // Update SoL:
        updateSoL();
        createSoLMessages();
        // Remember current SoL and tories for check changes at the next turn
        oldSonsOfLiberty = sonsOfLiberty;
        oldTories = tories;

    }

    /**
     * Returns the capacity of this colony's warehouse. All goods above this
     * limit, except {@link Goods#FOOD}, will be removed when calling
     * {@link #newTurn}.
     * 
     * @return The capacity of this <code>Colony</code>'s warehouse.
     */
    public int getWarehouseCapacity() {
        return (int) featureContainer.applyModifier(0, "model.modifier.warehouseStorage",
                                                    null, getGame().getTurn());
    }
    
    public Building getWarehouse() {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (!building.getType().getModifierSet("model.modifier.warehouseStorage").isEmpty()) {
                return building;
            }
        }
        return null;
    }

    /**
     * Disposes this <code>Colony</code>. All <code>WorkLocation</code>s
     * owned by this <code>Colony</code> will also be destroyed.
     */
    @Override
    public void dispose() {
        for (WorkLocation workLocation : getWorkLocations()) {
            ((FreeColGameObject) workLocation).dispose();
        }
        super.dispose();
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream. <br>
     * <br>
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
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        // Add attributes:
        out.writeAttribute("ID", getId());
        out.writeAttribute("name", name);
        out.writeAttribute("owner", owner.getId());
        out.writeAttribute("tile", tile.getId());
        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            out.writeAttribute("sonsOfLiberty", Integer.toString(sonsOfLiberty));
            out.writeAttribute("oldSonsOfLiberty", Integer.toString(oldSonsOfLiberty));
            out.writeAttribute("tories", Integer.toString(tories));
            out.writeAttribute("oldTories", Integer.toString(oldTories));
            out.writeAttribute("productionBonus", Integer.toString(productionBonus));
            if (!BuildableType.NOTHING.equals(getCurrentlyBuilding())) {
                out.writeAttribute("currentlyBuilding", getCurrentlyBuilding().getId());
            }
            out.writeAttribute("landLocked", Boolean.toString(landLocked));
            for (ExportData data : exportData.values()) {
                data.toXML(out);
            }
            /** 
             * Don't write other features, they will be added from buildings in readFromXMLImpl
             */
            for (Modifier modifier : featureContainer.getModifierSet("model.goods.bells",
                                                                     null, getGame().getTurn())) {
                if (Modifier.COLONY_GOODS_PARTY.equals(modifier.getSource())) {
                    modifier.toXML(out);
                }
            }

            for (WorkLocation workLocation : getWorkLocations()) {
                ((FreeColGameObject) workLocation).toXML(out, player, showAll, toSavedGame);
            }
        } else {
            out.writeAttribute("unitCount", Integer.toString(getUnitCount()));
            if (getStockade() != null) {
                getStockade().toXML(out, player, showAll, toSavedGame);
            }
        }
        goodsContainer.toXML(out, player, showAll, toSavedGame);
        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        name = in.getAttributeValue(null, "name");
        owner = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "owner"));
        if (owner == null) {
            owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
        }
        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "owner"));
        }
        owner.addSettlement(this);
        sonsOfLiberty = getAttribute(in, "sonsOfLiberty", 0);
        oldSonsOfLiberty = getAttribute(in, "oldSonsOfLiberty", 0);
        tories = getAttribute(in, "tories", 0);
        oldTories = getAttribute(in, "oldTories", 0);
        productionBonus = getAttribute(in, "productionBonus", 0);
        BuildableType buildableType = BuildableType.NOTHING;
        String buildable = getAttribute(in, "currentlyBuilding", null);
        if (buildable != null) {
            buildableType = (BuildableType) FreeCol.getSpecification().getType(buildable);
        }
        setCurrentlyBuilding(buildableType);
        landLocked = getAttribute(in, "landLocked", true);
        if (!landLocked) {
            featureContainer.addAbility(HAS_PORT);
        }
        unitCount = getAttribute(in, "unitCount", -1);
        featureContainer.addModifier(Settlement.DEFENCE_MODIFIER);
        // Read child elements:
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(ColonyTile.getXMLElementTagName())) {
                ColonyTile ct = (ColonyTile) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (ct == null) {
                    colonyTiles.add(new ColonyTile(getGame(), in));
                } else {
                    ct.readFromXML(in);
                }
            } else if (in.getLocalName().equals(Building.getXMLElementTagName())) {
                Building b = (Building) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (b == null) {
                    addBuilding(new Building(getGame(), in));
                } else {
                    b.readFromXML(in);
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                GoodsContainer gc = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (gc == null) {
                    goodsContainer = new GoodsContainer(getGame(), this, in);
                } else {
                    goodsContainer.readFromXML(in);
                }
            } else if (in.getLocalName().equals(ExportData.getXMLElementTagName())) {
                ExportData data = new ExportData();
                data.readFromXML(in);
                exportData.put(data.getId(), data);
            } else if (Modifier.getXMLElementTagName().equals(in.getLocalName())) {
                Modifier modifier = new Modifier(in);
                if (Modifier.COLONY_GOODS_PARTY.equals(modifier.getSource())) {
                    featureContainer.addModifier(modifier);
                }
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " loading colony " + name);
                in.nextTag();
            }
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "colony".
     */
    public static String getXMLElementTagName() {
        return "colony";
    }

    /**
     * Returns just this Colony itself.
     * 
     * @return this colony.
     */
    public Colony getColony() {
        return this;
    }
    
    /**
     * Returns true when colony has a stockade
     *
     * @return whether the colony has a stockade
     */
    public boolean hasStockade() {
        return (getStockade() != null);
    }
    
    /**
     * Returns the stockade building
     *
     * @return a <code>Building</code>
     */ 
    public Building getStockade() {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (!building.getType().getModifierSet("model.modifier.defence").isEmpty()) {
                return building;
            }
        }
        return null;
    }

    /**
     * Get the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public final Set<Modifier> getModifierSet(String id) {
        Set<Modifier> result = featureContainer.getModifierSet(id, null, getGame().getTurn());
        result.addAll(owner.getFeatureContainer().getModifierSet(id, null, getGame().getTurn()));
        return result;
    }

    /**
     * Returns true if the Colony, or its owner has the ability
     * identified by <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        HashSet<Ability> colonyAbilities = 
            new HashSet<Ability>(featureContainer.getAbilitySet(id, null, getGame().getTurn()));
        Set<Ability> playerAbilities = owner.getFeatureContainer().getAbilitySet(id, null, getGame().getTurn());
        colonyAbilities.addAll(playerAbilities);
        return FeatureContainer.hasAbility(colonyAbilities);
    }

    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

}
