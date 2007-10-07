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
import net.sf.freecol.common.model.Map.Position;

import org.w3c.dom.Element;

/**
 * Represents a colony. A colony contains {@link Building}s and
 * {@link ColonyTile}s. The latter represents the tiles around the
 * <code>Colony</code> where working is possible.
 */
public final class Colony extends Settlement implements Abilities, Location, Nameable, Modifiers {
    private static final Logger logger = Logger.getLogger(Colony.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final int BUILDING_UNIT_ADDITION = 1000;

    private static final int BELLS_PER_REBEL = 100;

    /** The name of the colony. */
    private String name;

    /**
     * Places a unit may work. Either a <code>Building</code> or a
     * <code>ColonyTile</code>.
     */
    private ArrayList<WorkLocation> workLocations = new ArrayList<WorkLocation>();

    private HashMap<String, Building> buildingMap = new HashMap<String, Building>();
    
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

    private int[] highLevel, lowLevel, exportLevel;
    private boolean[] exports;

    /**
     * Stores the Modifiers of this Player.
     */
    private HashMap<String, Modifier> modifiers = new HashMap<String, Modifier>();

    private int defenseBonus;

    /**
     * A list of Buildable items, which is NEVER empty.
     */
    private List<BuildableType> buildQueue = new ArrayList<BuildableType>();
    
    private HashMap<String, Boolean> abilities = new HashMap<String, Boolean>();

    /** Cannot do it this way as NUMBER_OF_TYPES is not a constant
    private int[] highLevel = new int[Goods.NUMBER_OF_TYPES];
    private int[] lowLevel = new int[Goods.NUMBER_OF_TYPES];
    private int[] exportLevel = new int[Goods.NUMBER_OF_TYPES];
    private boolean[] exports = new boolean[Goods.NUMBER_OF_TYPES];
    */

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
        Iterator<Position> exploreIt = getGame().getMap().getCircleIterator(getTile().getPosition(), true,
                getLineOfSight());
        while (exploreIt.hasNext()) {
            Tile t = getGame().getMap().getTile(exploreIt.next());
            t.setExploredBy(owner, true);
        }
        owner.invalidateCanSeeTiles();
        goodsContainer = new GoodsContainer(game, this);
        initializeGoodsTypeArrays();
        this.name = name;
        sonsOfLiberty = 0;
        oldSonsOfLiberty = 0;
        Map map = game.getMap();
        tile.setOwner(owner);
        for (int direction = 0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
            Tile t = map.getNeighbourOrNull(direction, tile);
            if (t.getOwner() == null) {
                t.setOwner(owner);
            }
            addWorkLocation(new ColonyTile(game, this, t));
            if (t.getType().isWater()) {
                landLocked = false;
            }
        }
        addWorkLocation(new ColonyTile(game, this, tile));
        List<BuildingType> buildingTypes = FreeCol.getSpecification().getBuildingTypeList();
        for (BuildingType buildingType : buildingTypes) {
            if (buildingType.getUpgradesFrom() == null &&
                buildingType.getHammersRequired() == 0) {
                addWorkLocation(new Building(game, this, buildingType));
            }
        }
        setCurrentlyBuilding(BuildableType.NOTHING);
        /*
        if (landLocked)
            currentlyBuilding = Building.WAREHOUSE;
        else
            currentlyBuilding = Building.DOCK;
         */
    }

    /**
     * Initiates a new <code>Colony</code> from an XML representation.
     * 
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occured during parsing.
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
     * Add a WorkLocation to this Colony.
     *
     * @param workLocation a <code>WorkLocation</code> value
     */
    public void addWorkLocation(WorkLocation workLocation) {
        workLocations.add(workLocation);
        if (workLocation instanceof Building) {
            Building building = (Building) workLocation;
            BuildingType buildingType = building.getType().getFirstLevel();
            buildingMap.put(buildingType.getID(), building);
            modifiers.putAll(buildingType.getModifiers());
        }
    }

    /**
     * Initializes warehouse/export settings.
     */
    private void initializeGoodsTypeArrays() {
        int numberOfGoods = FreeCol.getSpecification().numberOfGoodsTypes();
        highLevel = new int[numberOfGoods];
        lowLevel = new int[numberOfGoods];
        exportLevel = new int[numberOfGoods];
        exports = new boolean[numberOfGoods];
        
        for (int goodsIndex = 0; goodsIndex < numberOfGoods; goodsIndex++) {
            exports[goodsIndex] = false;
            lowLevel[goodsIndex] = 10;
            highLevel[goodsIndex] = 90;
            exportLevel[goodsIndex] = 50;
        }
    }

    /**
     * Updates SoL and builds stockade if possible.
     */
    public void updatePopulation() {
        if (getUnitCount() >= 3 && getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.laSalle"))) {
            if (!hasStockade()) {
                getStockade().upgrade();
            }
        }
        getTile().updatePlayerExploredTiles();
        if (getUnitCount() > 0) {
            updateSoL();
        }
    }

    /**
     * Get the <code>HighLevel</code> value.
     * 
     * @return an <code>int[]</code> value
     */
    public int[] getHighLevel() {
        return highLevel;
    }

    /**
     * Set the <code>HighLevel</code> value.
     * 
     * @param newHighLevel The new HighLevel value.
     */
    public void setHighLevel(final int[] newHighLevel) {
        this.highLevel = newHighLevel;
    }

    /**
     * Get the <code>LowLevel</code> value.
     * 
     * @return an <code>int[]</code> value
     */
    public int[] getLowLevel() {
        return lowLevel;
    }

    /**
     * Set the <code>LowLevel</code> value.
     * 
     * @param newLowLevel The new LowLevel value.
     */
    public void setLowLevel(final int[] newLowLevel) {
        this.lowLevel = newLowLevel;
    }

    /**
     * Get the <code>ExportLevel</code> value.
     * 
     * @return an <code>int[]</code> value
     */
    public int[] getExportLevel() {
        return exportLevel;
    }

    /**
     * Set the <code>ExportLevel</code> value.
     * 
     * @param newExportLevel The new ExportLevel value.
     */
    public void setExportLevel(final int[] newExportLevel) {
        this.exportLevel = newExportLevel;
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
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
            if (building != null && building.getGoodsOutputType() == goodsType) {
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
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
            if (building.getGoodsInputType() == goodsType) {
                return building;
            }
        }
        return null;
    }

    /**
     * Gets an <code>Iterator</code> of every location in this
     * <code>Colony</code> where a {@link Unit} can work.
     * 
     * @return The <code>Iterator</code>.
     * @see WorkLocation
     */
    public Iterator<WorkLocation> getWorkLocationIterator() {
        return workLocations.iterator();
    }

    /**
     * Gets a <code>List</code> of every {@link Building} in this
     * <code>Colony</code>.
     * 
     * @return The <code>List</code>.
     * @see Building
     */
    public List<Building> getBuildingList() {
        ArrayList<Building> b = new ArrayList<Building>();
        for (WorkLocation location : workLocations) {
            if (location instanceof Building) {
                b.add((Building) location);
            }
        }
        return b;
    }

    /**
     * Gets an <code>Iterator</code> of every {@link Building} in this
     * <code>Colony</code>.
     * 
     * @return The <code>Iterator</code>.
     * @see Building
     */
    public Iterator<Building> getBuildingIterator() {
        return getBuildingList().iterator();
    }

    /**
     * Gets an <code>Iterator</code> of every {@link ColonyTile} in this
     * <code>Colony</code>.
     * 
     * @return The <code>Iterator</code>.
     * @see ColonyTile
     */
    public Iterator<ColonyTile> getColonyTileIterator() {
        ArrayList<ColonyTile> b = new ArrayList<ColonyTile>();
        for (WorkLocation location : workLocations) {
            if (location instanceof ColonyTile) {
                b.add((ColonyTile) location);
            }
        }
        return b.iterator();
    }

    /**
     * Gets a <code>Building</code> of the specified type.
     * 
     * @param type The type of building to get.
     * @return The <code>Building</code>.
     */
    public Building getBuilding(int typeIndex) {
        return buildingMap.get(FreeCol.getSpecification().getBuildingType(typeIndex).getFirstLevel().getID());
    }
    public Building getBuilding(BuildingType type) {
        return buildingMap.get(type.getFirstLevel().getID());
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
        Iterator<ColonyTile> i = getColonyTileIterator();
        while (i.hasNext()) {
            ColonyTile c = i.next();
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
        Iterator<ColonyTile> i = getColonyTileIterator();
        while (i.hasNext()) {
            ColonyTile c = i.next();
            if (c.getWorkTile() == t) {
                return c;
            }
        }
        return null;
    }

    /**
     * Gets a vacant <code>WorkLocation</code> for the given <code>Unit</code>.
     * 
     * @param locatable The <code>Unit</code>
     * @return A vacant <code>WorkLocation</code> for the given
     *         <code>Unit</code> or <code>null</code> if there is no such
     *         location.
     */
    public WorkLocation getVacantWorkLocationFor(Unit locatable) {
        WorkLocation w = getVacantColonyTileFor(locatable, Goods.FOOD);
        if (w != null && w.canAdd(locatable) && getVacantColonyTileProductionFor(locatable, Goods.FOOD) > 0) {
            return w;
        }
        Iterator<Building> i = getBuildingIterator();
        while (i.hasNext()) {
            w = i.next();
            if (w.canAdd(locatable)) {
                return w;
            }
        }
        Iterator<WorkLocation> it = getWorkLocationIterator();
        while (it.hasNext()) {
            w = it.next();
            if (w.canAdd(locatable)) {
                return w;
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
            Iterator<WorkLocation> i = getWorkLocationIterator();
            while (i.hasNext()) {
                WorkLocation w = i.next();
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
        Iterator<WorkLocation> i = getWorkLocationIterator();
        while (i.hasNext()) {
            WorkLocation w = i.next();
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
     * Removes a specified amount of a type of Goods from this containter.
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
        for (WorkLocation wl : workLocations) {
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
        for (WorkLocation wl : workLocations) {
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

    /**
     * Returns true if the custom house should export this type of goods.
     * 
     * @param type The type of goods.
     * @return True if the custom house should export this type of goods.
     */
    public boolean getExports(GoodsType type) {
        return exports[type.getIndex()];
    }
    public boolean getExports(int goodsIndex) {
        return exports[goodsIndex];
    }

    /**
     * Returns true if the custom house should export these goods.
     * 
     * @param goods The goods.
     * @return True if the custom house should export these goods.
     */
    public boolean getExports(Goods goods) {
        return exports[goods.getType().getIndex()];
    }

    /**
     * Sets whether the custom house should export these goods.
     * 
     * @param type the type of goods.
     * @param value a <code>boolean</code> value
     */
    public void setExports(GoodsType type, boolean value) {
        exports[type.getIndex()] = value;
    }
    public void setExports(int goodsIndex, boolean value) {
        exports[goodsIndex] = value;
    }

    /**
     * Sets whether the custom house should export these goods.
     * 
     * @param goods the goods.
     * @param value a <code>boolean</code> value
     */
    public void setExports(Goods goods, boolean value) {
        setExports(goods.getType().getIndex(), value);
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
     * @param unitType The unit type to add as a teacher.
     * @return <code>true</code> if this unit type could be added.
    */
    public boolean canTrain(Unit unit) {
        if (!hasAbility("model.ability.teach")) {
            return false;
        }
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
            if (building.hasAbility("model.ability.teach") && building.canAdd(unit)) {
                return true;
            }
        }
        return false;
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
        
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
            if (building.hasAbility("model.ability.teach") &&
                building.canAdd(unitType)) {
                return true;
            }
        }
        return false;
    }
    
    public List<Unit> getTeachers() {
        List<Unit> teachers = new ArrayList<Unit>();
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
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
     * Note! Several callers fail to handle null as a return value. Return an
     * arbitrary unarmed land unit unless Paul Revere is present as founding
     * father, in which case the unit can be armed as well. Also note that the
     * colony would typically be defended by a unit outside it.
     * 
     * @param attacker The target that would be attacking this colony.
     * @return The <code>Unit</code> that has been choosen to defend this
     *         colony.
     * @see Tile#getDefendingUnit(Unit)
     */
    @Override
    public Unit getDefendingUnit(Unit attacker) {
        return getDefendingUnit();
    }

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Colony</code>. Note that the colony will normally be defended by
     * units outside of the colony (@see Tile#getDefendingUnit).
     * <p>
     * Note! Several callers fail to handle null as a return value. Return an
     * arbitrary unarmed land unit.
     * 
     * @return The <code>Unit</code> that has been choosen to defend this
     *         colony.
     */
    private Unit getDefendingUnit() {
        // Sanity check - are there units available?
        List<Unit> unitList = getUnitList();
        if (unitList.isEmpty()) {
            throw new IllegalStateException("Colony " + name + " contains no units!");
        }
        // Get the first unit
        // TODO should return an experienced soldier rather if available.
        return unitList.get(0);
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
            if (unitType.getHammersRequired() > 0) {
                // TODO: check requirements
                buildableUnits.add(unitType);
            }
        }
        return buildableUnits;
    }

    /**
     * Checks if this colony may build the given unit type.
     * 
     * @param unitType The unit type to test against.
     * @return The result.
     */
    public boolean canBuildUnit(UnitType unitType) {
        List<UnitType> buildableUnits = getBuildableUnits();
        for (UnitType buildable : buildableUnits) {
            if (unitType == buildable) {
                return true;
            }
        }
        return false;
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
     * @param type The type of building to be built.
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
        int bells = getGoodsCount(Goods.BELLS);
        addGoods(Goods.BELLS, (bells * amount) / 100);
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
        int membership = (getBells() * 100) / (BELLS_PER_REBEL * getUnitCount());
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
        return Math.min(getBells() / BELLS_PER_REBEL, getUnitCount());
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
        Iterator<WorkLocation> workLocationIterator = getWorkLocationIterator();
        while (workLocationIterator.hasNext()) {
            amount += workLocationIterator.next().getProductionOf(goodsType);
        }
        return amount;
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
        int highestProduction = -2;
        Iterator<ColonyTile> colonyTileIterator = getColonyTileIterator();
        while (colonyTileIterator.hasNext()) {
            ColonyTile colonyTile = colonyTileIterator.next();
            if (colonyTile.canAdd(unit)) {
                Tile workTile = colonyTile.getWorkTile();
                /*
                 * canAdd ensures workTile it's empty or unit it's working in it
                 * so unit can work in it if it's owned by none, by europeans or
                 * unit's owner has the founding father Peter Minuit
                 */

                boolean ourLand = (owner.getLandPrice(workTile) == 0);
                int potential = colonyTile.getProductionOf(unit, goodsType);
                if (ourLand && potential > highestProduction) {
                    highestProduction = potential;
                    bestPick = colonyTile;
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
     * Returns the horse production (given that enough food is being produced
     * and a sufficient storage capacity).
     * 
     * @return The number of producable horses.
     *//* deprecated
    public int getPotentialHorseProduction() {
        if (getGoodsCount(Goods.HORSES) < 2) {
            return 0;
        }
        int maxAmount = getGoodsCount(Goods.HORSES) / 10;
        // TODO: put production bonus for horses in specification
        if (!getStables().isBuilt()) {
            maxAmount /= 2;
        }
        return Math.max(1, maxAmount);
    }*/

    /**
     * Gets the production of horses in this <code>Colony</code>.
     * 
     * @return The total production of horses in this <code>Colony</code>.
     *//* deprecated
    public int getHorseProduction() {
        int surplus = getFoodProduction() - getFoodConsumption();
        int maxSpace = getWarehouseCapacity() - getGoodsCount(Goods.HORSES);
        int potential = Math.min(getPotentialHorseProduction(), maxSpace);
        return Math.max(Math.min(surplus / 2, potential), 0); // store the half of surplus
    }*/

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
        if (goodsType == Goods.FOOD ) {
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

    public boolean canBuild(BuildableType buildableType) {
        if (buildableType == BuildableType.NOTHING) {
            return false;
        } else if (buildableType.getHammersRequired() <= 0) {
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
        return true;
    }

    private int getNextHammersForBuilding() {
        if (canBuild()) {
            return getCurrentlyBuilding().getHammersRequired();
        } else {
            return -1;
        }
    }

    private int getNextToolsForBuilding() {
        if (canBuild()) {
            return getCurrentlyBuilding().getToolsRequired();
        } else {
            return -1;
        }
    }
    
    private void checkBuildingComplete() {
        // In order to avoid duplicate messages:
        if (lastVisited == getGame().getTurn().getNumber()) {
            return;
        }
        lastVisited = getGame().getTurn().getNumber();
        if (canBuild()) {
            BuildableType buildable = getCurrentlyBuilding();
            if (buildable.getHammersRequired() != -1 &&
                buildable.getHammersRequired() <= getHammers()) {
                if (buildable.getToolsRequired() <= getGoodsCount(Goods.TOOLS)) {
                    // waste excess hammers
                    removeGoods(Goods.HAMMERS);
                    removeGoods(Goods.TOOLS, buildable.getToolsRequired());
                    if (buildable instanceof UnitType) {
                        Unit unit = getGame().getModelController().createUnit(getID() + "buildUnit", getTile(), getOwner(),
                                                                              (UnitType) buildable);
                        addModelMessage(this, "model.colony.unitReady",
                                        new String[][] { { "%colony%", getName() },
                                                         { "%unit%", unit.getName() } },
                                        ModelMessage.UNIT_ADDED, unit);
                    } else if (buildable instanceof BuildingType) {
                        BuildingType upgradesFrom = ((BuildingType) buildable).getUpgradesFrom();
                        if (upgradesFrom == null) {
                            addWorkLocation(new Building(getGame(), this, (BuildingType) buildable));
                        } else {
                            getBuilding(upgradesFrom).upgrade();
                        }
                        buildQueue.remove(0);
                        if (buildQueue.size() == 0) {
                            buildQueue.add(BuildableType.NOTHING);
                        }
                    }
                } else {
                    addModelMessage(this, "model.colony.itemNeedTools",
                                    new String[][] {
                                        { "%colony%", getName() },
                                        { "%item%", buildable.getName() } },
                                    ModelMessage.MISSING_GOODS, 
                                    FreeCol.getSpecification().getGoodsType("model.goods.tools"));
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
        int hammersRemaining = Math.max(getCurrentlyBuilding().getHammersRequired() - getHammers(), 0);
        int toolsRemaining = Math.max(getCurrentlyBuilding().getToolsRequired() - getGoodsCount(Goods.TOOLS), 0);
        int price = hammersRemaining * getGameOptions().getInteger(GameOptions.HAMMER_PRICE)
                + (getOwner().getMarket().getBidPrice(Goods.TOOLS, toolsRemaining) * 110) / 100;
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
        int hammersRemaining = getCurrentlyBuilding().getHammersRequired() - getHammers();
        int toolsRemaining = getCurrentlyBuilding().getToolsRequired() - getGoodsCount(Goods.TOOLS);

        if (hammersRemaining > 0) {
            getOwner().modifyGold(-hammersRemaining * getGameOptions().getInteger(GameOptions.HAMMER_PRICE));
            addGoods(Goods.HAMMERS, hammersRemaining);
        }
        if (toolsRemaining > 0) {
            getOwner().getMarket().buy(Goods.TOOLS, toolsRemaining, getOwner());
            addGoods(Goods.TOOLS, toolsRemaining);
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
        Iterator<WorkLocation> wli = getWorkLocationIterator();
        while (wli.hasNext()) {
            WorkLocation wl = wli.next();
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
        Iterator<ColonyTile> tileIterator = getColonyTileIterator();
        while (tileIterator.hasNext()) {
            ColonyTile tile = tileIterator.next();
            logger.finest("Calling newTurn for colony tile " + tile.toString());
            tile.newTurn();
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

    /* deprecated
    // Breed horses:
    private void updateHorses() {
        int horseProduction = getHorseProduction();
        if (horseProduction != 0) {
            removeGoods(Goods.FOOD, horseProduction);
            addGoods(Goods.HORSES, horseProduction);
        }
    }*/

    // Create a new colonist if there is enough food:
    private void checkForNewColonist() {
        if (getGoodsCount(Goods.FOOD) >= 200) {
            List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.bornInColony");
            if (unitTypes.size() > 0) {
                int random = getGame().getModelController().getRandom(getID() + "bornInColony", unitTypes.size());
                Unit u = getGame().getModelController().createUnit(getID() + "newTurn200food",
                                                getTile(), getOwner(), unitTypes.get(random));
                removeGoods(Goods.FOOD, 200);
                addModelMessage(this, "model.colony.newColonist", new String[][] { { "%colony%", getName() } },
                                ModelMessage.UNIT_ADDED, u);
                logger.info("New colonist created in " + getName() + " with ID=" + u.getID());
            }
        }
    }


    // Update carpenter and blacksmith
    private void addHammersAndTools() {
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
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
                if (getExports(goods) && (owner.canTrade(goods, Market.CUSTOM_HOUSE))) {
                    GoodsType type = goods.getType();
                    int amount = goods.getAmount() - getExportLevel()[type.getIndex()];
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
            if (getExports(goods)  && (owner.canTrade(goods, Market.CUSTOM_HOUSE))) {
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
                                    ModelMessage.WAREHOUSE_CAPACITY, goods);
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
                    FreeCol.getSpecification().getGoodsType("model.goods.bells"));
            } else {
                addModelMessage(this, "model.colony.SoLDecrease", new String[][] {
                        { "%oldSoL%", String.valueOf(oldSonsOfLiberty) },
                        { "%newSoL%", String.valueOf(sonsOfLiberty) }, { "%colony%", getName() } },
                    ModelMessage.SONS_OF_LIBERTY,
                    FreeCol.getSpecification().getGoodsType("model.goods.bells"));

            }
        }

        int bonus = 0;
        if (sonsOfLiberty == 100) {
            // there are no tories left
            bonus = 2;
            if (oldSonsOfLiberty < 100) {
                addModelMessage(this, "model.colony.SoL100", new String[][] { { "%colony%", getName() } },
                                ModelMessage.SONS_OF_LIBERTY,
                                FreeCol.getSpecification().getGoodsType("model.goods.bells"));
            }
        } else {
            if (sonsOfLiberty >= 50) {
                bonus += 1;
                if (oldSonsOfLiberty < 50) {
                    addModelMessage(this, "model.colony.SoL50", new String[][] { { "%colony%", getName() } },
                                    ModelMessage.SONS_OF_LIBERTY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"));
                }
            }
            if (tories > veryBadGovernment) {
                bonus -= 2;
                if (oldTories <= veryBadGovernment) {
                    // government has become very bad
                    addModelMessage(this, "model.colony.veryBadGovernment",
                            new String[][] { { "%colony%", getName() } }, ModelMessage.GOVERNMENT_EFFICIENCY,
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"));
                }
            } else if (tories > badGovernment) {
                bonus -= 1;
                if (oldTories <= badGovernment) {
                    // government has become bad
                    addModelMessage(this, "model.colony.badGovernment", new String[][] { { "%colony%", getName() } },
                                    ModelMessage.GOVERNMENT_EFFICIENCY, 
                                    FreeCol.getSpecification().getGoodsType("model.goods.bells"));
                } else if (oldTories > veryBadGovernment) {
                    // government has improved, but is still bad
                    addModelMessage(this, "model.colony.governmentImproved1",
                                    new String[][] { { "%colony%", getName() } }, 
                                    ModelMessage.GOVERNMENT_EFFICIENCY,
                    FreeCol.getSpecification().getGoodsType("model.goods.bells"));
                }
            } else if (oldTories > badGovernment) {
                // government was bad, but has improved
                addModelMessage(this, "model.colony.governmentImproved2", new String[][] { { "%colony%", getName() } },
                                ModelMessage.GOVERNMENT_EFFICIENCY, 
                                FreeCol.getSpecification().getGoodsType("model.goods.bells"));
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

        // TODO: this needs to be made future-proof by considering all
        // materials that might be used for construction work. This
        // will require a Buildable interface, and suitable methods
        // for the *Type classes.
        List<GoodsType> goodsForBuilding = new ArrayList<GoodsType>();
        if (getNextHammersForBuilding() > 0) {
            goodsForBuilding.add(Goods.HAMMERS);
        }
        if (getNextToolsForBuilding() > 0) {
            goodsForBuilding.add(Goods.TOOLS);
        }
        
        delayedProduction.clear();
        addBuildingProduction(getBuildingList(), goodsForBuilding);

        // The following tasks consume hammers/tools,
        // or may do so in the future
        checkBuildingComplete();

        addBuildingProduction(delayedProduction, null);

        checkForNewColonist(); // must be after building production because horse production consumes some food
        exportGoods();
        // Throw away goods there is no room for.
        goodsContainer.cleanAndReport(getWarehouseCapacity(), getLowLevel(), getHighLevel());
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
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
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
        Iterator<WorkLocation> i = getWorkLocationIterator();
        while (i.hasNext()) {
            ((FreeColGameObject) i.next()).dispose();
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
        out.writeAttribute("ID", getID());
        out.writeAttribute("name", name);
        out.writeAttribute("owner", owner.getID());
        out.writeAttribute("tile", tile.getID());
        out.writeAttribute("defenseBonus", Integer.toString(defenseBonus));
        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            out.writeAttribute("sonsOfLiberty", Integer.toString(sonsOfLiberty));
            out.writeAttribute("oldSonsOfLiberty", Integer.toString(oldSonsOfLiberty));
            out.writeAttribute("tories", Integer.toString(tories));
            out.writeAttribute("oldTories", Integer.toString(oldTories));
            out.writeAttribute("productionBonus", Integer.toString(productionBonus));
            out.writeAttribute("currentlyBuilding", getCurrentlyBuilding().getID());
            out.writeAttribute("landLocked", Boolean.toString(landLocked));
            char[] exportsCharArray = new char[exports.length];
            for (int i = 0; i < exports.length; i++) {
                exportsCharArray[i] = (exports[i] ? '1' : '0');
            }
            out.writeAttribute("exports", new String(exportsCharArray));
            toArrayElement("lowLevel", lowLevel, out);
            toArrayElement("highLevel", highLevel, out);
            toArrayElement("exportLevel", exportLevel, out);
            for (Modifier modifier : modifiers.values()) {
                modifier.toXML(out, player);
            }
            Iterator<WorkLocation> workLocationIterator = workLocations.iterator();
            while (workLocationIterator.hasNext()) {
                ((FreeColGameObject) workLocationIterator.next()).toXML(out, player, showAll, toSavedGame);
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
        initializeGoodsTypeArrays();
        setID(in.getAttributeValue(null, "ID"));
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
        String currently = getAttribute(in, "currentlyBuilding", null);
        setCurrentlyBuilding(BuildableType.NOTHING);
        landLocked = getAttribute(in, "landLocked", true);
        unitCount = getAttribute(in, "unitCount", -1);
        final String exportString = in.getAttributeValue(null, "exports");
        if (exportString != null) {
            for (int i = 0; i < exportString.length(); i++) {
                exports[i] = ((exportString.charAt(i) == '1') ? true : false);
            }
        }
        // Read child elements:
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(ColonyTile.getXMLElementTagName())) {
                ColonyTile ct = (ColonyTile) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (ct != null) {
                    ct.readFromXML(in);
                } else {
                    addWorkLocation(new ColonyTile(getGame(), in));
                }
            } else if (in.getLocalName().equals(Building.getXMLElementTagName())) {
                Building b = (Building) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (b != null) {
                    b.readFromXML(in);
                } else {
                    addWorkLocation(new Building(getGame(), in));
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                GoodsContainer gc = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (gc != null) {
                    goodsContainer.readFromXML(in);
                } else {
                    goodsContainer = new GoodsContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals("lowLevel")) {
                lowLevel = readFromArrayElement("lowLevel", in, new int[0]);
            } else if (in.getLocalName().equals("highLevel")) {
                highLevel = readFromArrayElement("highLevel", in, new int[0]);
            } else if (in.getLocalName().equals("exportLevel")) {
                exportLevel = readFromArrayElement("exportLevel", in, new int[0]);
            } else if (in.getLocalName().equals(Modifier.getXMLElementTagName())) {
                Modifier modifier = new Modifier(in);
                modifiers.put(modifier.getId(), modifier);
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
        Iterator<Building> buildingIterator = getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
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
        Modifier modifier = modifiers.get(id);
        Modifier playerModifier = owner.getModifier(id);
        if (modifier != null) {
            if (playerModifier != null) {
                result = new Modifier(modifier);
                result.combine(playerModifier);
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
        modifiers.put(id, newModifier);
    }
    
    public void addModifier(String id, final Modifier newModifier) {
        if (modifiers.get(id) == null) {
            setModifier(id, newModifier);
        } else {
            modifiers.get(id).combine(newModifier);
        }
    }
    
    public void removeModifier(String id, final Modifier newModifier) {
        if (modifiers.get(id) != null) {
            modifiers.get(id).combine(newModifier.getInverse());
        }
    }

    /**
     * Returns true if the Building has the ability identified by
     * <code>id</code.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        // TODO: search player abilities too
        return modifiers.containsKey(id) && modifiers.get(id).getBooleanValue();
    }

    /**
     * Sets the ability identified by <code>id</code.
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        modifiers.put(id, new Modifier(id, newValue));
    }

    /**
     * Sets the abilities given
     *
     * @param abilities the new abilities
     */
    public void putAbilities(java.util.Map<String, Boolean> abilities) {
        this.abilities.putAll(abilities);
    }
    
    public int getDefenseBonus() {
        return defenseBonus;
    }

    public void setDefenseBonus(int newDefenseBonus) {
        defenseBonus = newDefenseBonus;
    }
}
