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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Element;

/**
 * Represents a colony. A colony contains {@link Building}s and
 * {@link ColonyTile}s. The latter represents the tiles around the
 * <code>Colony</code> where working is possible.
 */
public final class Colony extends Settlement implements Abilities, Location, Nameable, Modifiers {

    private static final Logger logger = Logger.getLogger(Colony.class.getName());

    private static final int BELLS_PER_REBEL = 100;

    /** The name of the colony. */
    private String name;

    /** A list of ColonyTiles. */
    private ArrayList<ColonyTile> colonyTiles = new ArrayList<ColonyTile>();

    /** A map of Buildings, indexed by the ID of their basic type. */
    private HashMap<String, Building> buildingMap = new HashMap<String, Building>();

    /** A map of ExportData, indexed by the IDs of GoodsTypes. */
    private HashMap<String, ExportData> exportData = new HashMap<String, ExportData>();

    private List<Building> delayedProduction = new ArrayList<Building>();

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
     * Stores the Features of this Colony.
     */
    private HashMap<String, Feature> features = new HashMap<String, Feature>();

    private int defenseBonus;

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
        Map map = game.getMap();
        tile.setOwner(owner);
        for (int direction = 0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
            Tile t = map.getNeighbourOrNull(direction, tile);
            if (t == null)
                continue;
            if (t.getOwner() == null) {
                t.setOwner(owner);
            }
            colonyTiles.add(new ColonyTile(game, this, t));
            if (t.getType().isWater()) {
                landLocked = false;
            }
        }
        colonyTiles.add(new ColonyTile(game, this, tile));
        List<BuildingType> buildingTypes = FreeCol.getSpecification().getBuildingTypeList();
        for (BuildingType buildingType : buildingTypes) {
            if (buildingType.getUpgradesFrom() == null &&
                buildingType.getGoodsRequired() == null) {
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
     * Add a Building to this Colony.
     *
     * @param building a <code>Building</code> value
     */
    public void addBuilding(Building building) {
        BuildingType buildingType = building.getType().getFirstLevel();
        buildingMap.put(buildingType.getId(), building);
        for (Feature feature : building.getType().getFeatures().values()) {
            setFeature(feature);
        }
    }

    /**
     * Updates SoL and builds stockade if possible.
     */
    public void updatePopulation() {
        Modifier priceBonus = getModifier("model.modifier.buildingPriceBonus");
        if (priceBonus != null && priceBonus.applyTo(100) == 0) {
            // this means we can get a building for free
            for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
                if (priceBonus.appliesTo(buildingType) &&
                    getBuilding(buildingType) == null &&
                    getUnitCount() >= buildingType.getPopulationRequired()) {
                    addBuilding(createBuilding(buildingType));
                }
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
    public final ExportData getExportData(GoodsType goodsType) {
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
    public final void setExportData(ExportData newExportData) {
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
     * Damages all ship located on this <code>Colony</code>'s
     * <code>Tile</code>. That is: they are sent to the closest location for
     * repair.
     * 
     * @see Unit#shipDamaged
     */
    public void damageAllShips() {
        Iterator<Unit> iter = getTile().getUnitIterator();
        while (iter.hasNext()) {
            Unit u = iter.next();
            if (u.isNaval()) {
                u.shipDamaged();
            }
        }
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
     * @param typeIndex The index of the building type to get.
     * @return The <code>Building</code>.
     */
    public Building getBuilding(int typeIndex) {
        return buildingMap.get(FreeCol.getSpecification().getBuildingType(typeIndex).getFirstLevel().getId());
    }
    public Building getBuilding(BuildingType type) {
        return buildingMap.get(type.getFirstLevel().getId());
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
                if (w != null) {
                    locatable.setLocation(w);
                } else {
                    logger.warning("Could not find a 'WorkLocation' for " + locatable + " in " + this);
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
        return 2 * getUnitCount();
    }

    /**
     * Gets the amount of one type of Goods at this Colony.
     * 
     * @param type The type of goods to look for.
     * @return The amount of this type of Goods at this Location.
     */
    public int getGoodsCount(GoodsType type) {
        if (type.getStoredAs() != null) {
            return goodsContainer.getGoodsCount(type.getStoredAs());
        } else {
            return goodsContainer.getGoodsCount(type);
        }
    }
    
    public int getGoodsCount(int goodsIndex) {
        return getGoodsCount(FreeCol.getSpecification().getGoodsType(goodsIndex));
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

    public void removeGoods(Goods goods) {
        goodsContainer.removeGoods(goods.getType(), goods.getAmount());
    }

    public void removeGoods(GoodsType type) {
        goodsContainer.removeGoods(type);
    }

    public void addGoods(GoodsType type, int amount) {
        if (type.getStoredAs() != null) {
            goodsContainer.addGoods(type.getStoredAs(), amount);
        } else {
            goodsContainer.addGoods(type, amount);
        }
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
    
    /**
     * Returns a list of all units in this colony of the given type.
     * 
     * @param type The type of the units to include in the list. For instance
     *            Unit.EXPERT_FARMER.
     * @return A list of all the units of the given type in this colony.
     */
    public List<Unit> getUnitList(int type) {
        ArrayList<Unit> units = new ArrayList<Unit>();
        for (WorkLocation wl : getWorkLocations()) {
            for (Unit unit : wl.getUnitList()) {
                if (unit.getType() == type) {
                    units.add(unit);
                }
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
        // throw new UnsupportedOperationException();
        if (locatable instanceof Unit && ((Unit) locatable).getOwner() == getOwner()) {
            return true;
        } else if (locatable instanceof Goods) {
            return true;
        }
        return false;
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
        return canTrain(unit.getUnitType());
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
            if (building.hasAbility("model.ability.teach") &&
                building.canAdd(unitType)) {
                return true;
            }
        }
        return false;
    }
    
    public List<Unit> getTeachers() {
        List<Unit> teachers = new ArrayList<Unit>();
        for (Building building : buildingMap.values()) {
            if (building.hasAbility("model.ability.teach")) {
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
        float defensePower = -1.0f;
        for (Unit nextUnit : getUnitList()) {
            float tmpPower = nextUnit.getDefensePower(attacker);
            if (tmpPower > defensePower) {
                defender = nextUnit;
                defensePower = tmpPower;
            }
        }
        if (defender == null) {
            throw new IllegalStateException("Colony " + name + " contains no units!");
        } else {
            return defender;
        }
    }

    /**
     * Adds to the hammer count of the colony.
     * 
     * @param amount The number of hammers to add.
     */
    public void addHammers(int amount) {
        if (getCurrentlyBuilding() == BuildableType.NOTHING) {
            addModelMessage(this, "model.colony.cannotBuild", new String[][] { { "%colony%", getName() } },
                    ModelMessage.WARNING, this);
            return;
        }
        // Building only:
        if (getCurrentlyBuilding().getPopulationRequired() > getUnitCount()) {
            addModelMessage(this, "model.colony.buildNeedPop",
                            new String[][] { { "%colony%", getName() },
                                             { "%building%", getCurrentlyBuilding().getName() } },
                            ModelMessage.WARNING);
            return;
        }

        if (getCurrentlyBuilding() instanceof BuildingType &&
            getBuilding((BuildingType) getCurrentlyBuilding()) != null) {
            addModelMessage(this, "model.colony.alreadyBuilt",
                            new String[][] {
                                { "%colony%", getName() },
                                { "%building%", getCurrentlyBuilding().getName() } },
                            ModelMessage.WARNING);
        }
        goodsContainer.addGoods(Goods.HAMMERS, amount);
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
            if (unitType.getGoodsRequired() != null) {
                if (canBuild(unitType)) {
                    buildableUnits.add(unitType);
                }
            }
        }
        return buildableUnits;
    }

    /**
     * Returns the hammer count of the colony.
     * 
     * @return The current hammer count of the colony.
     */
    public int getHammers() {
        return getGoodsCount(Goods.HAMMERS);
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
     * Adds to the bell count of the colony.
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
     * Adds to the bell count of the colony.
     * 
     * @param amount The percentage of SoL to add.
     */
    public void addSoL(int amount) {
        /*
         * The number of bells to be generated in order to get the appropriate
         * SoL is determined by the formula: int membership = ... in
         * "updateSoL()":
         */
	int requiredBells = ((sonsOfLiberty + amount) * BELLS_PER_REBEL * getUnitCount()) / 100;
        addGoods(Goods.BELLS, requiredBells - getGoodsCount(Goods.BELLS));
    }

    /**
     * Returns the bell count of the colony.
     * 
     * @return The current bell count of the colony.
     */
    public int getBells() {
        return getGoodsCount(Goods.BELLS);
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
        int membership = (getBells() * 100) / (BELLS_PER_REBEL * units);
        if (membership < 0)
            membership = 0;
        if (membership > 100)
            membership = 100;
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
     * Gets the production of food.
     * 
     * @return The same as <code>getProductionOf(Goods.FOOD)</code>.
     */
    public int getFoodProduction() {
        return getProductionOf(Goods.FOOD);
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
        WorkLocation result = getVacantColonyTileFor(unit, Goods.FOOD);
        if (result != null) {
            return result;
        } else {
            for (Building building : buildingMap.values()) {
                if (building.canAdd(unit)) {
                    return building;
                }
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
        return bestPick != null ? bestPick.getProductionOf(unit, goodsType) : 0;
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
        // TODO FIXME This should also take into account tools needed for a
        // current building project
        if (goodsType == Goods.FOOD) {
            used += getFoodConsumption();
        }
        count -= used;
        return count;
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
        } else if (buildableType.getGoodsRequired() == null) {
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
            if (colonyBuilding != null) {
                // a building of the same family already exists
                if (colonyBuilding.getType().getUpgradesTo() == null ||
                    colonyBuilding.getType().getUpgradesTo() != newBuildingType) {
                    // the existing building's next upgrade is not the new one we want to build
                    return false;
                }
            } else if (colonyBuilding == null) {
                // the colony has no similar building yet
                if (newBuildingType.getUpgradesFrom() != null) {
                    // we are trying to build an advanced factory, we should build lower level shop first
                    return false;
                }
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
                if (getGoodsCount(requiredGoodsType) < goodsRequired.getAmount()) {
                    if (!requiredGoodsType.isStorable()) {
                        // buildable is not complete and we don't care
                        // about missing goods because unstorable
                        // goods (e.g. hammers) are still missing
                        return;
                    }
                    messages.add(new ModelMessage(this, "model.colony.buildableNeedsGoods",
                                                  new String[][] {
                                                      { "%colony%", getName() },
                                                      { "%buildable%", buildable.getName() },
                                                      { "%goodsType%", requiredGoodsType.getName() } },
                                                  ModelMessage.MISSING_GOODS, 
                                                  requiredGoodsType));
                }
            }
            if (messages.size() == 0) {
                // no messages means all goods are present
                for (AbstractGoods goodsRequired : buildable.getGoodsRequired()) {
                    GoodsType requiredGoodsType = goodsRequired.getType();
                    if (requiredGoodsType.isStorable()) {
                        removeGoods(requiredGoodsType, goodsRequired.getAmount());
                    } else {
                        // waste excess unstorable goods
                        removeGoods(requiredGoodsType);
                    }
                }
                if (buildable instanceof UnitType) {
                    Unit unit = getGame().getModelController().createUnit(getId() + "buildUnit", getTile(), getOwner(),
                                                                          (UnitType) buildable);
                    addModelMessage(this, "model.colony.unitReady",
                                    new String[][] { { "%colony%", getName() },
                                                     { "%unit%", unit.getName() } },
                                    ModelMessage.UNIT_ADDED, unit);
                } else if (buildable instanceof BuildingType) {
                    BuildingType upgradesFrom = ((BuildingType) buildable).getUpgradesFrom();
                    if (upgradesFrom == null) {
                        addBuilding(createBuilding((BuildingType) buildable));
                    } else {
                        getBuilding(upgradesFrom).upgrade();
                    }
                    buildQueue.remove(0);
                    if (buildQueue.size() == 0) {
                        buildQueue.add(BuildableType.NOTHING);
                    }
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
                if (requiredGoodsType == Goods.HAMMERS) {
                    price += remaining * getGameOptions().getInteger(GameOptions.HAMMER_PRICE);
                } else {
                    price += (getOwner().getMarket().getBidPrice(Goods.TOOLS, remaining) * 110) / 100;
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
                if (requiredGoodsType == Goods.HAMMERS) {
                    getOwner().modifyGold(-remaining * getGameOptions().getInteger(GameOptions.HAMMER_PRICE));
                    addGoods(Goods.HAMMERS, remaining);
                } else {
                    getOwner().getMarket().buy(requiredGoodsType, remaining, getOwner());
                    addGoods(requiredGoodsType, remaining);
                }
            }
        }
    }

    /**
     * Bombard a unit with the given outcome.
     * 
     * @param defender The <code>Unit</code> defending against bombardment.
     * @param result The result of the bombardment.
     */
    public void bombard(Unit defender, int result) {
        if (defender == null) {
            throw new NullPointerException();
        }
        
        switch (result) {
        case Unit.ATTACK_EVADES:
            // send message to both parties
            addModelMessage(this, "model.unit.shipEvadedBombardment",
                            new String[][] {
                                { "%colony%", getName() },
                                { "%unit%", defender.getName() },
                                { "%nation%", defender.getOwner().getNationAsString() } }, 
                            ModelMessage.DEFAULT, this);
            addModelMessage(defender, "model.unit.shipEvadedBombardment",
                            new String[][] { { "%colony%", getName() },
                                             { "%unit%", defender.getName() },
                                             { "%nation%", defender.getOwner().getNationAsString() } }, 
                            ModelMessage.DEFAULT, this);
            break;
        case Unit.ATTACK_WIN:
            defender.shipDamaged(this);
            addModelMessage(this, "model.unit.enemyShipDamagedByBombardment",
                            new String[][] {
                                { "%colony%", getName() },
                                { "%unit%", defender.getName() },
                                { "%nation%", defender.getOwner().getNationAsString() } }, ModelMessage.UNIT_DEMOTED);
            break;
        case Unit.ATTACK_GREAT_WIN:
            defender.shipSunk(this);
            addModelMessage(this, "model.unit.shipSunkByBombardment",
                            new String[][] {
                                { "%colony%", getName() },
                                { "%unit%", defender.getName() },
                                { "%nation%", defender.getOwner().getNationAsString() } },
                            ModelMessage.UNIT_DEMOTED);
            break;
        default:
            logger.warning("Illegal result of bombardment!");
            throw new IllegalArgumentException("Illegal result of bombardment!");
        }
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


    // Eat food:
    private void updateFood() {
        int eat = getFoodConsumption();
        int food = getGoodsCount(Goods.FOOD);

        if (eat > food) {
            // Kill a colonist:
            getRandomUnit().dispose();
            removeGoods(Goods.FOOD, food);
            addModelMessage(this, "model.colony.colonistStarved", new String[][] { { "%colony%", getName() } },
                    ModelMessage.UNIT_LOST);
        } else {
            removeGoods(Goods.FOOD, eat);

            if (eat > getFoodProduction() && (food - eat) / (eat - getFoodProduction()) <= 3) {
                addModelMessage(this, "model.colony.famineFeared", new String[][] { { "%colony%", getName() },
                        { "%number%", Integer.toString((food - eat) / (eat - getFoodProduction())) } },
                        ModelMessage.WARNING);
            }
        }
    }

    // Create a new colonist if there is enough food:
    private void checkForNewColonist() {
        if (getGoodsCount(Goods.FOOD) >= 200) {
            List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.bornInColony");
            if (unitTypes.size() > 0) {
                int random = getGame().getModelController().getRandom(getId() + "bornInColony", unitTypes.size());
                Unit u = getGame().getModelController().createUnit(getId() + "newTurn200food",
                                                getTile(), getOwner(), unitTypes.get(random));
                removeGoods(Goods.FOOD, 200);
                addModelMessage(this, "model.colony.newColonist", new String[][] { { "%colony%", getName() } },
                                ModelMessage.UNIT_ADDED, u);
                logger.info("New colonist created in " + getName() + " with ID=" + u.getId());
            }
        }
    }


    // Update carpenter and blacksmith
    private void addHammersAndTools() {
        for (Building building : buildingMap.values()) {
            GoodsType output = building.getGoodsOutputType();
            if (output == Goods.HAMMERS || output == Goods.TOOLS) {
                logger.finest("Calling newTurn for building " + building.getName());
                building.newTurn();
            }
        }
    }



    /**
     * Update all buildings in given list which produce goods in
     * producedGoods. Buildings which don't produce goods in producedGoods
     * are added to delayedProduction.
     *
     * If producedGoods is null, update all buildings in given iterator
     */
    private void addBuildingProduction(List<Building> list, List<GoodsType> producedGoods) {
        // First auto production buildings
        addBuildingProduction(list, producedGoods, true);
        addBuildingProduction(list, producedGoods, false);
    }
    
    private void addBuildingProduction(List<Building> list,
            List<GoodsType> producedGoods, boolean autoProduction) {
        for (Building building : list) {
            if (building.hasAbility("model.ability.autoProduction") == autoProduction) {
                GoodsType output = building.getGoodsOutputType();
                if (producedGoods == null || producedGoods.contains(output)) {
                    logger.finest("Calling newTurn for building " + building.getName());
                    building.newTurn();
                } else {
                    delayedProduction.add(building);
                }
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
            if (getExportData(goods.getType()).isExported() &&
                owner.canTrade(goods, Market.CUSTOM_HOUSE)) {
                // capacity will never be exceeded
                continue;
            } else if (goods.getAmount() < getWarehouseCapacity()) {
                int waste = (goods.getAmount() + getProductionNetOf(goods.getType()) -
                             getWarehouseCapacity());
                if (waste > 0) {
                    addModelMessage(this, "model.building.warehouseSoonFull",
                                    new String [][] {{"%goods%", goods.getName()},
                                                     {"%colony%", getName()},
                                                     {"%amount%", String.valueOf(waste)}},
                                    ModelMessage.WAREHOUSE_CAPACITY, goods.getType());
                }
            }
        }
    }


    private void createSoLMessages() {
        final int difficulty = getOwner().getDifficulty();
        final int veryBadGovernment = 10 - difficulty;
        final int badGovernment = 6 - difficulty;
        if (sonsOfLiberty / 10 != oldSonsOfLiberty / 10) {
            if (sonsOfLiberty > oldSonsOfLiberty) {
                addModelMessage(this, "model.colony.SoLIncrease", new String[][] {
                        { "%oldSoL%", String.valueOf(oldSonsOfLiberty) },
                        { "%newSoL%", String.valueOf(sonsOfLiberty) }, { "%colony%", getName() } },
                    ModelMessage.SONS_OF_LIBERTY,
                    FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
            } else {
                addModelMessage(this, "model.colony.SoLDecrease", new String[][] {
                        { "%oldSoL%", String.valueOf(oldSonsOfLiberty) },
                        { "%newSoL%", String.valueOf(sonsOfLiberty) }, { "%colony%", getName() } },
                    ModelMessage.SONS_OF_LIBERTY,
                    FreeCol.getSpecification().getGoodsType("model.goods.Bells"));

            }
        }

        int bonus = 0;
        if (sonsOfLiberty == 100) {
            // there are no tories left
            bonus = 2;
            if (oldSonsOfLiberty < 100) {
                addModelMessage(this, "model.colony.SoL100", new String[][] { { "%colony%", getName() } },
                                ModelMessage.SONS_OF_LIBERTY,
                                FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
            }
        } else {
            if (sonsOfLiberty >= 50) {
                bonus += 1;
                if (oldSonsOfLiberty < 50) {
                    addModelMessage(this, "model.colony.SoL50", new String[][] { { "%colony%", getName() } },
                                    ModelMessage.SONS_OF_LIBERTY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
                }
            }
            if (tories > veryBadGovernment) {
                bonus -= 2;
                if (oldTories <= veryBadGovernment) {
                    // government has become very bad
                    addModelMessage(this, "model.colony.veryBadGovernment",
                            new String[][] { { "%colony%", getName() } }, ModelMessage.GOVERNMENT_EFFICIENCY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
                }
            } else if (tories > badGovernment) {
                bonus -= 1;
                if (oldTories <= badGovernment) {
                    // government has become bad
                    addModelMessage(this, "model.colony.badGovernment", new String[][] { { "%colony%", getName() } },
                                    ModelMessage.GOVERNMENT_EFFICIENCY, 
                                    FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
                } else if (oldTories > veryBadGovernment) {
                    // government has improved, but is still bad
                    addModelMessage(this, "model.colony.governmentImproved1",
                                    new String[][] { { "%colony%", getName() } }, 
                                    ModelMessage.GOVERNMENT_EFFICIENCY,
                    FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
                }
            } else if (oldTories > badGovernment) {
                // government was bad, but has improved
                addModelMessage(this, "model.colony.governmentImproved2", new String[][] { { "%colony%", getName() } },
                                ModelMessage.GOVERNMENT_EFFICIENCY, 
                                FreeCol.getSpecification().getGoodsType("model.goods.Bells"));
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
        }
        
        delayedProduction.clear();
        addBuildingProduction(getBuildings(), goodsForBuilding);

        // The following tasks consume hammers/tools,
        // or may do so in the future
        checkBuildableComplete();

        addBuildingProduction(delayedProduction, null);

        checkForNewColonist(); // must be after building production because horse production consumes some food
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
        int basicStorage = 0;
        Modifier storage = getModifier("model.modifier.warehouseStorage");
        if (storage != null) {
            basicStorage = (int) storage.applyTo(basicStorage);
        }
        return basicStorage;
    }
    
    public Building getWarehouse() {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (building.getType().getModifier("model.modifier.warehouseStorage") != null) {
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
        out.writeAttribute("defenseBonus", Integer.toString(defenseBonus));
        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            out.writeAttribute("sonsOfLiberty", Integer.toString(sonsOfLiberty));
            out.writeAttribute("oldSonsOfLiberty", Integer.toString(oldSonsOfLiberty));
            out.writeAttribute("tories", Integer.toString(tories));
            out.writeAttribute("oldTories", Integer.toString(oldTories));
            out.writeAttribute("productionBonus", Integer.toString(productionBonus));
            out.writeAttribute("currentlyBuilding", getCurrentlyBuilding().getId());
            out.writeAttribute("landLocked", Boolean.toString(landLocked));
            for (ExportData data : exportData.values()) {
                data.toXML(out);
            }
            /* Don't write features, they will be added from buildings in readFromXMLImpl
            for (Feature feature : features.values()) {
                if (feature instanceof Ability) {
                    ((Ability) feature).toXML(out, player);
                } else if (feature instanceof Modifier) {
                    ((Modifier) feature).toXML(out, player);
                }
            }
             */
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
        defenseBonus = getAttribute(in, "productionBonus", 0);
        String buildable = getAttribute(in, "currentlyBuilding", null);
        BuildableType buildableType = (BuildableType) FreeCol.getSpecification().getType(buildable);
        if (buildableType == null) {
            buildableType = BuildableType.NOTHING;
        }
        setCurrentlyBuilding(buildableType);
        landLocked = getAttribute(in, "landLocked", true);
        unitCount = getAttribute(in, "unitCount", -1);
        // Read child elements:
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(ColonyTile.getXMLElementTagName())) {
                ColonyTile ct = (ColonyTile) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (ct != null) {
                    ct.readFromXML(in);
                } else {
                    colonyTiles.add(new ColonyTile(getGame(), in));
                }
            } else if (in.getLocalName().equals(Building.getXMLElementTagName())) {
                Building b = (Building) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (b != null) {
                    b.readFromXML(in);
                } else {
                    addBuilding(new Building(getGame(), in));
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                GoodsContainer gc = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (gc != null) {
                    goodsContainer.readFromXML(in);
                } else {
                    goodsContainer = new GoodsContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals(ExportData.getXMLElementTagName())) {
                ExportData data = new ExportData();
                data.readFromXML(in);
                exportData.put(data.getId(), data);
            /* Features are not written, they will be added from buildings
            } else if (in.getLocalName().equals(Ability.getXMLElementTagName())) {
                Ability ability = new Ability(in);
                setFeature(ability);
            } else if (in.getLocalName().equals(Modifier.getXMLElementTagName())) {
                Modifier modifier = new Modifier(in);
                setFeature(modifier);
             */
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " loading colony " + name);
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
     * Returns the power for bombarding
     * 
     * @return the power for bombarding
     */
    public float getBombardingPower() {
        float attackPower = 0;
        Iterator<Unit> unitIterator = getTile().getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = unitIterator.next();
            if (unit.hasAbility("model.ability.bombard")) {
                attackPower += unit.getUnitType().getOffence();
            }
        }
        if (attackPower > 48) {
            attackPower = 48;
        }
        return attackPower;
    }
    
    /**
     * Returns a unit to bombard
     *
     * @return a unit to bombard
     */
    public Unit getBombardingAttacker() {
        Unit attacker = null;
        Iterator<Unit> unitIterator = getTile().getUnitIterator();
        int maxPower = -1;
        while (unitIterator.hasNext()) {
            Unit unit = unitIterator.next();
            logger.finest("Unit is " + unit.getName());
            if (unit.hasAbility("model.ability.bombard")) {
                int power = unit.getUnitType().getOffence();
                if (power > maxPower) {
                    power = maxPower;
                    attacker = unit;
                }
            }
        }
        return attacker;
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
            if (building.getType().getDefenseBonus() > 0) {
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
    public final Modifier getModifier(String id) {
        Modifier result;
        Modifier modifier = (Modifier) features.get(id);
        Modifier playerModifier = owner.getModifier(id);
        if (modifier != null) {
            if (playerModifier != null) {
                result = Modifier.combine(modifier, playerModifier);
            } else {
                result = modifier;
            }
        } else {
            result = playerModifier;
        }
        
        return result;
    }

    /**
     * Set the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @param newModifier a <code>Modifier</code> value
     */
    public final void setModifier(String id, final Modifier newModifier) {
        setFeature(newModifier);
    }
    
    public void removeModifier(final Modifier newModifier) {
        Modifier oldModifier = getModifier(newModifier.getId());
        if (newModifier == null || oldModifier == null) {
            return;
        } else if (newModifier == oldModifier) {
            features.remove(newModifier);
        } else {
            setFeature(oldModifier.remove(newModifier));
        }
    }

    public void removeFeature(String id, Feature feature) {
    }

    /**
     * Returns true if the Colony, or its owner has the ability
     * identified by <code>id</code.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return (features.containsKey(id) &&
            (features.get(id) instanceof Ability) &&
            ((Ability) features.get(id)).getValue()) ||
            owner.hasAbility(id);
    }

    /**
     * Sets the ability identified by <code>id</code.
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        features.put(id, new Ability(id, newValue));
    }

    public Feature getFeature(String id) {
        return features.get(id);
    }

    public void setFeature(Feature feature) {
        if (feature == null) {
            return;
        }
        Feature oldValue = features.get(feature.getId());
        if (oldValue instanceof Modifier && feature instanceof Modifier) {
            features.put(feature.getId(), Modifier.combine((Modifier) oldValue, (Modifier) feature));
        } else {
            features.put(feature.getId(), feature);
        }
    }

    /**
     * Sets the abilities given
     *
     * @param abilities the new abilities
     */
    public void putAbilities(java.util.Map<String, Boolean> abilities) {
        for (Entry<String, Boolean> entry : abilities.entrySet()) {
            features.put(entry.getKey(), new Ability(entry.getKey(), entry.getValue()));
        }
    }
    
    /**
     * Describe <code>getDefenseBonus</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getDefenseBonus() {
        return defenseBonus;
    }

    /**
     * Describe <code>setDefenseBonus</code> method here.
     *
     * @param newDefenseBonus an <code>int</code> value
     */
    public void setDefenseBonus(int newDefenseBonus) {
        defenseBonus = newDefenseBonus;
    }
}
