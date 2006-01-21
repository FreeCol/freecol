
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
* Represents a colony. A colony contains {@link Building}s and {@link ColonyTile}s.
* The latter represents the tiles around the <code>Colony</code> where working is
* possible.
*/
public final class Colony extends Settlement implements Location {

    private static final Logger logger = Logger.getLogger(Colony.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int BUILDING_UNIT_ADDITION = 1000;

    /** The name of the colony. */
    private String name;

    /** Places a unit may work. Either a <code>Building</code> or a <code>ColonyTile</code>. */
    private ArrayList workLocations = new ArrayList();

    private GoodsContainer goodsContainer;
    private boolean[] exports;
    
    private int hammers;
    private int bells;
    private int sonsOfLiberty, oldSonsOfLiberty;
    
    /**
    * Identifies what this colony is currently building.
    *
    * This is the type of the "Building" that is being built,
    * or if <code>currentlyBuilding >= BUILDING_UNIT_ADDITION</code>
    * the type of the <code>Unit</code> (+BUILDING_UNIT_ADDITION)
    * that is currently beeing build
    */
    private int currentlyBuilding;

    // Whether this colony is landlocked
    private boolean landLocked = true;
    
    // Will only be used on enemy colonies:
    private int unitCount = -1;

    // Temporary variable:
    private int lastVisited = -1;


    /**
    * Creates a new <code>Colony</code>.
    *
    * @param game The <code>Game</code> in which this object belongs.
    * @param owner The <code>Player</code> owning this <code>Colony</code>.
    * @param name The name of the new <code>Colony</code>.
    * @param tile The location of the <code>Colony</code>.
    */
    public Colony(Game game, Player owner, String name, Tile tile) {
        this(game, owner, name, tile, true);
    }


    /**
    * Creates a new <code>Colony</code>.
    *
    * @param game The <code>Game</code> in which this object belongs.
    * @param owner The <code>Player</code> owning this <code>Colony</code>.
    * @param name The name of the new <code>Colony</code>.
    * @param tile The location of the <code>Colony</code>.
    */
    public Colony(Game game, Player owner, String name, Tile tile, boolean initializeWorkLocations) {
        super(game, owner, tile);

        goodsContainer = new GoodsContainer(game, this);
        exports = new boolean[Goods.NUMBER_OF_TYPES];
        for (int i = 0; i < exports.length; i++) {
            exports[i] = false;
        }
        
        this.name = name;
        
        hammers = 0;
        bells = 0;
        sonsOfLiberty = 0;
        oldSonsOfLiberty = 0;
        currentlyBuilding = Building.DOCK;

        Map map = game.getMap();
        int ownerNation = owner.getNation();
        tile.setNationOwner(ownerNation);
        for (int direction = 0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
            Tile t = map.getNeighbourOrNull(direction, tile);
            if (t.getNationOwner() == Player.NO_NATION) {
                t.setNationOwner(ownerNation);
                //t.setOwner(this);
            }
            if (initializeWorkLocations) {
                workLocations.add(new ColonyTile(game, this, t));
            }
            if (t.getType() == Tile.OCEAN) {
                landLocked = false;
            }
        }
            
        if (initializeWorkLocations) {
            workLocations.add(new ColonyTile(game, this, tile));

            workLocations.add(new Building(game, this, Building.TOWN_HALL, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.CARPENTER, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.BLACKSMITH, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.TOBACCONIST, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.WEAVER, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.DISTILLER, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.FUR_TRADER, Building.HOUSE));
            workLocations.add(new Building(game, this, Building.STOCKADE, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.ARMORY, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.DOCK, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.SCHOOLHOUSE, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.WAREHOUSE, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.STABLES, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.CHURCH, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.PRINTING_PRESS, Building.NOT_BUILT));
            workLocations.add(new Building(game, this, Building.CUSTOM_HOUSE, Building.NOT_BUILT));
        }
    }


    public void updatePopulation() {
        if (getUnitCount() >= 3 && getOwner().hasFather(FoundingFather.LA_SALLE)) {
            if (!getBuilding(Building.STOCKADE).isBuilt()) {
                getBuilding(Building.STOCKADE).setLevel(Building.HOUSE);
            }
        }
    }
    

    /**
    * Initiates a new <code>Colony</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public Colony(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }


    /**
    * Gets this colony's line of sight.
    */
    public int getLineOfSight() {
        return 2;
    }

    /**
     * Returns whether this colony is landlocked, or has access to the
     * ocean.
     */
    public boolean isLandLocked() {
        return landLocked;
    }

    /**
    * Sets the owner of this <code>Colony</code>, including all units within.
    *
    * @param owner The <code>Player</code> that shall own this <code>Settlement</code>.
    * @see Settlement#getOwner
    */
    public void setOwner(Player owner) {
        this.owner = owner;

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            ((Unit)unitIterator.next()).setOwner(owner);
        }

        Iterator tileUnitIterator = getTile().getUnitIterator();
        while (tileUnitIterator.hasNext()) {
            Unit target = (Unit) tileUnitIterator.next();
            target.setOwner(getOwner());
        }
    }


    /**
    * Returns the building for producing the given type of goods.
    * @return The <code>Building</code> which produces the given type
    *         of goods, or <code>null</code> if such a building cannot
    *         be found.
    */
    public Building getBuildingForProducing(int goodsType) {
        Building b;
        switch (goodsType) {
            case Goods.MUSKETS:
                b = getBuilding(Building.ARMORY);
                break;
            case Goods.RUM:
                b = getBuilding(Building.DISTILLER);
                break;
            case Goods.CIGARS:
                b = getBuilding(Building.TOBACCONIST);
                break;
            case Goods.CLOTH:
                b = getBuilding(Building.WEAVER);
                break;
            case Goods.COATS:
                b = getBuilding(Building.FUR_TRADER);
                break;
            case Goods.TOOLS:
                b = getBuilding(Building.BLACKSMITH);
                break;
            case Goods.CROSSES:
                b = getBuilding(Building.CHURCH);
                break;
            case Goods.HAMMERS:
                b = getBuilding(Building.CARPENTER);
                break;
            case Goods.BELLS:
                b = getBuilding(Building.TOWN_HALL);
                break;
            default:
                b = null;
        }

        return (b != null && b.isBuilt()) ? b : null;
    }
    
    /**
     * Returns the colony's existing building for the given goods type.
     *
     * @param goodsType The goods type.
     * @return The Building for the <code>goodsType</code>, or 
     *      <code>null</code> if not exists or not fully built.
     * @see Goods
     */
    public Building getBuildingForConsuming(int goodsType) {
        Building b;
        switch (goodsType) {
            case Goods.TOOLS:
                b = getBuilding(Building.ARMORY);
                break;
            case Goods.LUMBER:
                b = getBuilding(Building.CARPENTER);
                break;
            case Goods.SUGAR:
                b = getBuilding(Building.DISTILLER);
                break;
            case Goods.TOBACCO:
                b = getBuilding(Building.TOBACCONIST);
                break;
            case Goods.COTTON:
                b = getBuilding(Building.WEAVER);
                break;
            case Goods.FURS:
                b = getBuilding(Building.FUR_TRADER);
                break;
            case Goods.ORE:
                b = getBuilding(Building.BLACKSMITH);
                break;
            default:
                b = null;
        }

        if (b != null && b.isBuilt()) {
            return b;
        }
        return null;
    } 

    /**
    * Gets an <code>Iterator</code> of every location in this <code>Colony</code>
    * where a {@link Unit} can work.
    *
    * @return The <code>Iterator</code>.
    * @see WorkLocation
    */
    public Iterator getWorkLocationIterator() {
        return workLocations.iterator();
    }


    /**
    * Gets an <code>Iterator</code> of every {@link Building} in this <code>Colony</code>.
    *
    * @return The <code>Iterator</code>.
    * @see Building
    */
    public Iterator getBuildingIterator() {
        ArrayList b = new ArrayList();

        Iterator w = getWorkLocationIterator();
        while (w.hasNext()) {
            Object o = w.next();

            if (o instanceof Building) {
                b.add(o);
            }
        }

        return b.iterator();
    }


    /**
    * Gets an <code>Iterator</code> of every {@link ColonyTile} in this <code>Colony</code>.
    *
    * @return The <code>Iterator</code>.
    * @see ColonyTile
    */
    public Iterator getColonyTileIterator() {
        ArrayList b = new ArrayList();

        Iterator w = getWorkLocationIterator();
        while (w.hasNext()) {
            Object o = w.next();

            if (o instanceof ColonyTile) {
                b.add(o);
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
    public Building getBuilding(int type) {
        Iterator buildingIterator = getBuildingIterator();

        while (buildingIterator.hasNext()) {
            Building building = (Building) buildingIterator.next();
            if (building.isType(type)) {
                return building;
            }
        }

        return null;
    }

    /**
    * Gets a <code>Tile</code> of this<code>Colony</code>.
    * @return The <code>Tile</code>.
    */
    public Tile getTile() {
            return tile;
    }
    
    /**
    * Gets a <code>Tile</code> from the neighbourhood of this <code>Colony</code>.
    * @return The <code>Tile</code>.
    */
    public Tile getTile(int x, int y) {
        if (x==0 && y==0) {
            return getGame().getMap().getNeighbourOrNull(Map.N, tile);
        } else if (x==0 && y== 1) {
            return getGame().getMap().getNeighbourOrNull(Map.NE, tile);
        } else if (x==0 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.E, tile);
        } else if (x==1 && y== 0) {
            return getGame().getMap().getNeighbourOrNull(Map.NW, tile);
        } else if (x==1 && y== 1) {
            return tile;
        } else if (x==1 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.SE, tile);
        } else if (x==2 && y== 0) {
            return getGame().getMap().getNeighbourOrNull(Map.W, tile);
        } else if (x==2 && y== 1) {
            return getGame().getMap().getNeighbourOrNull(Map.SW, tile);
        } else if (x==2 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.S, tile);
        } else {
            return null;
        }
    }


    /**
    * Gets the specified <code>ColonyTile</code>.
    */
    public ColonyTile getColonyTile(int x, int y) {
        Tile t = getTile(x, y);

        Iterator i = getColonyTileIterator();
        while (i.hasNext()) {
            ColonyTile c = (ColonyTile) i.next();

            if (c.getWorkTile() == t) {
                return c;
            }
        }

        return null;
    }


    /**
    * Adds a <code>Locatable</code> to this Location.
    * @param locatable The <code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            if (((Unit) locatable).isColonist()) {
                WorkLocation w = getVacantColonyTileFor(((Unit) locatable), Goods.FOOD);
                if (w != null && w.canAdd(locatable) &&
                    getVacantColonyTileProductionFor((Unit) locatable, Goods.FOOD) > 0) {
                    locatable.setLocation(w);
                    updatePopulation();
                    return;
                }

                Iterator i = getBuildingIterator();
                while (i.hasNext()) {
                    w = (WorkLocation) i.next();
                    if (w.canAdd(locatable)) {
                        locatable.setLocation(w);
                        updatePopulation();
                        return;
                    }
                }

                Iterator it = getWorkLocationIterator();
                while (it.hasNext()) {
                    w = (WorkLocation) it.next();
                    if (w.canAdd(locatable)) {
                        locatable.setLocation(w);
                        updatePopulation();
                        return;
                    }
                }

                logger.warning("Could not find a 'WorkLocation' for " + locatable + " in " + this);
            } else {
                locatable.setLocation(getTile());
            }

            updatePopulation();
        } else if (locatable instanceof Goods) {
            goodsContainer.addGoods((Goods)locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a 'Colony'.");
        }
    }


    /**
    * Removes a <code>Locatable</code> from this Location.
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Iterator i = getWorkLocationIterator();
            while (i.hasNext()) {
                WorkLocation w = (WorkLocation) i.next();
                if (w.contains(locatable)) {
                    w.remove(locatable);
                    updatePopulation();
                    return;
                }
            }
        } else if (locatable instanceof Goods) {
            goodsContainer.removeGoods((Goods)locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a 'Colony'.");
        }
    }


    /**
    * Gets the amount of Units at this Location. These units are
    * located in a {@link WorkLocation} in this <code>Colony</code>.
    *
    * @return The amount of Units at this Location.
    */
    public int getUnitCount() {
        int count = 0;

        if (unitCount != -1) {
            return unitCount;
        }

        Iterator i = getWorkLocationIterator();
        while (i.hasNext()) {
            WorkLocation w = (WorkLocation) i.next();
            count += w.getUnitCount();
        }

        return count;
    }
    
    /**
     * Gives the food needed to keep all current colonists alive in this colony.
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
    public int getGoodsCount(int type) {
        return goodsContainer.getGoodsCount(type);
    }

    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }
       
    /**
    * Removes a specified amount of a type of Goods from this containter.
    *
    * @param type The type of Goods to remove from this container.
    * @param amount The amount of Goods to remove from this container.
    */
    public void removeGoods(int type, int amount) {
        goodsContainer.removeGoods(type, amount);
    }

    public void removeGoods(Goods goods) {
        goodsContainer.removeGoods(goods.getType(), goods.getAmount());
    }
    
    
    public void addGoods(int type, int amount) {
        goodsContainer.addGoods(type, amount);
    }


    public Iterator getUnitIterator() {
        ArrayList units = new ArrayList();

        Iterator wli = getWorkLocationIterator();
        while (wli.hasNext()) {
            WorkLocation wl = (WorkLocation) wli.next();

            Iterator unitIterator = wl.getUnitIterator();
            while (unitIterator.hasNext()) {
                Object o = unitIterator.next();
                if (o != null) {
                    units.add(o);
                }
            }
        }

        return units.iterator();
    }

    public Iterator getGoodsIterator() {
        return goodsContainer.getGoodsIterator();
    }

    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>Colony</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getCompactGoodsIterator() {
        return goodsContainer.getCompactGoodsIterator();
    }

    /**
     * Returns true if the custom house should export this type of
     * goods.
     *
     * @param type The type of goods.
     * @return True if the custom house should export this type of
     * goods.
     */
    public boolean getExports(int type) {
        return exports[type];
    }

    /**
     * Returns true if the custom house should export these goods.
     *
     * @param goods The goods.
     * @return True if the custom house should export these goods.
     */
    public boolean getExports(Goods goods) {
        return exports[goods.getType()];
    }

    /**
     * Toggles the custom house's export settings for this type of
     * goods.
     *
     * @param type The type of goods.
     */
    public void toggleExports(int type) {
        if (exports[type]) {
            exports[type] = false;
        } else {
            exports[type] = true;
        }
    }

    /**
     * Toggles the custom house's export settings for these goods.
     *
     * @param goods The goods.
     */
    public void toggleExports(Goods goods) {
        toggleExports(goods.getType());
    }
    
    public boolean contains(Locatable locatable) {
        throw new UnsupportedOperationException();
    }


    public boolean canAdd(Locatable locatable) {
        //throw new UnsupportedOperationException();
        if (locatable instanceof Unit &&
            ((Unit) locatable).getOwner() == getOwner()) {
            return true;
        }

        return false;
    }

    /**
    * Gets the <code>Unit</code> that is currently defending this <code>Colony</code>.
    * @param attacker The target that would be attacking this colony.
    * @return The <code>Unit</code> that has been choosen to defend this colony.
    */
    public Unit getDefendingUnit(Unit attacker) {
        return getDefendingUnit();
    }


    /**
    * Gets the <code>Unit</code> that is currently defending this <code>Colony</code>.
    * @return The <code>Unit</code> that has been choosen to defend this colony.
    */
    public Unit getDefendingUnit() {
        Iterator ui = getUnitIterator();
        if (ui.hasNext()) {
            return (Unit) ui.next();
        }

        return null;
    }

    /**
    * Adds to the hammer count of the colony.
    * @param amount The number of hammers to add.
    */
    public void addHammers(int amount) {
        if (currentlyBuilding == -1) {
            addModelMessage(this, "model.colony.cannotBuild", new String[][] {{"%colony%", getName()}});
            return;
        }

        // Building only:
        if (currentlyBuilding < BUILDING_UNIT_ADDITION) {
            if (getBuilding(currentlyBuilding).getNextPop() > getUnitCount()) {
                addModelMessage(this, "model.colony.buildNeedPop", new String[][] {{"%colony%", getName()}, {"%building%", getBuilding(currentlyBuilding).getNextName()}});
                return;
            }

            if (getBuilding(currentlyBuilding).getNextHammers() == -1) {
                addModelMessage(this, "model.colony.alreadyBuilt", new String[][] {{"%colony%", getName()}, {"%building%", getBuilding(currentlyBuilding).getName()}});
            }
        }
        
        hammers += amount;
        checkBuildingComplete();
    }


    /**
    * Returns an <code>Iterator</code> of every unit type this colony may build.
    */
    public Iterator getBuildableUnitIterator() {
        ArrayList buildableUnits = new ArrayList();
        buildableUnits.add(new Integer(Unit.WAGON_TRAIN));
        
        if (getBuilding(Building.ARMORY).isBuilt()) {
            buildableUnits.add(new Integer(Unit.ARTILLERY));
        }

        if (getBuilding(Building.DOCK).getLevel() >= Building.FACTORY) {
            buildableUnits.add(new Integer(Unit.CARAVEL));
            buildableUnits.add(new Integer(Unit.MERCHANTMAN));
            buildableUnits.add(new Integer(Unit.GALLEON));
            buildableUnits.add(new Integer(Unit.PRIVATEER));
            buildableUnits.add(new Integer(Unit.FRIGATE));
            if(owner.getRebellionState() >= Player.REBELLION_IN_WAR)
                buildableUnits.add(new Integer(Unit.MAN_O_WAR));
        }
        
        return buildableUnits.iterator();
    }

   
    /**
    * Checks if this colony may build the given unit type.
    *
    * @param unitType The unit type to test against.
    * @return The result.
    */
    public boolean canBuildUnit(int unitType) {
        Iterator buildableUnitIterator = getBuildableUnitIterator();
        while (buildableUnitIterator.hasNext()) {
            if (unitType == ((Integer) buildableUnitIterator.next()).intValue()) {
                return true;
            }
        }

        return false;
    }


    /**
    * Returns the hammer count of the colony.
    * @return The current hammer count of the colony.
    */
    public int getHammers() {
        return hammers;
    }
    
    /**
    * Returns the type of building currently being built.
    * @return The type of building currently being built.
    */
    public int getCurrentlyBuilding() {
        return currentlyBuilding;
    }

    /**
    * Sets the type of building to be built.
    * @param type The type of building to be built.
    */
    public void setCurrentlyBuilding(int type) {
        currentlyBuilding = type;
    }

    
    /**
    * Adds to the bell count of the colony.
    * @param amount The number of bells to add.
    */
    public void addBells(int amount) {
        bells += amount;

        // This is by 51 now because the removal of bells
        // happens *after* the bells are added;
        // each unit will eat one bell at 100% membership,
        // hence the extra 1.
        if (bells >= ((getUnitCount() + 1) * 51)) {
            bells = ((getUnitCount() + 1) * 51);
        } else if (bells <= 0) {
            bells = 0;
        }
    }
    
    
    /**
    * Adds to the bell count of the colony.
    * @param amount The percentage of SoL to add.
    */
    public void addSoL(int amount) {
        bells += (bells * amount) / 100;
    }


    /**
    * Returns the bell count of the colony.
    * @return The current bell count of the colony.
    */
    public int getBells() {
        return bells;
    }
    
    /**
    * Returns the current SoL membership of the colony.
    * @return The current SoL membership of the colony.
    */
    public int getSoL() {
        return sonsOfLiberty;
    }

    /**
    * Returns the previous SoL membership of the colony.
    * @return The previous SoL membership of the colony.
    */
    public int getOldSoL() {
        return oldSonsOfLiberty;
    }

    /**
     * Calculates the current SoL membership of the colony.
     */
    public void updateSoL() {
        int membership = (bells * 2) / (getUnitCount() + 1);
        if (membership < 0) membership = 0;
        if (membership > 100) membership = 100;
        oldSonsOfLiberty = sonsOfLiberty;
        sonsOfLiberty = membership;
        if (sonsOfLiberty/10 != oldSonsOfLiberty/10) {
            if (sonsOfLiberty > oldSonsOfLiberty) {
                addModelMessage(this, "model.colony.SoLIncrease",
                                new String [][] {{"%oldSoL%", String.valueOf(oldSonsOfLiberty)},
                                                 {"%newSoL%", String.valueOf(sonsOfLiberty)},
                                                 {"%colony%", getName()}});
            } else {
                addModelMessage(this, "model.colony.SoLDecrease",
                                new String [][] {{"%oldSoL%", String.valueOf(oldSonsOfLiberty)},
                                                 {"%newSoL%", String.valueOf(sonsOfLiberty)},
                                                 {"%colony%", getName()}});
            }
        }
    }
    
    /**
    * Returns the Tory membership of the colony.
    * @return The current Tory membership of the colony.
    */
    public int getTory() {
        return 100 - getSoL();
    }
    
    /**
    * Returns the production bonus, if any, of the colony.
    * @return The current production bonus of the colony.
    */
    public int getProductionBonus() {
        int bonus = 0;
	int tories = (getTory() * getUnitCount()) / 100;
	int difficulty = getOwner().getDifficulty();

        if (tories > 10 - difficulty) {
            bonus -= 2;
        } else if (tories > 6 - difficulty) {
            bonus -= 1;
        }

        if (getSoL() == 100) {
            bonus += 2;
        } else if (getSoL() >= 50) {
            bonus += 1;
        }
        
        // TODO-LATER: REMOVE THIS WHEN THE AI CAN HANDLE PRODUCTION PENALTIES:
        if (getOwner().isAI()) {
            bonus = Math.max(0, bonus);
        }
        
        return bonus;
    }

    
    /**
    * Gets a string representation of the Colony. Currently this method
    * just returns the name of the <code>Colony</code>, but that may
    * change later.
    *
    * @return The name of the colony.
    * @see #getName
    */
    public String toString() {
        return name;
    }

    
    /**
    * Gets the name of this <code>Colony</code>.
    * @return The name as a <code>String</code>.
    */
    public String getName() {
        return name;
    }


    /**
    * Gets the production of food.
    */
    public int getFoodProduction() {
        return getProductionOf(Goods.FOOD);
    }


    /**
    * Returns the production of the given type of goods.
    */
    public int getProductionOf(int goodsType) {
        int amount = 0;

        if (goodsType == Goods.HORSES) {
            return getHorseProduction();
        }

        Iterator workLocationIterator = getWorkLocationIterator();
        while (workLocationIterator.hasNext()) {
            amount += ((WorkLocation) workLocationIterator.next()).getProductionOf(goodsType);
        }

        return amount;
    }


    /**
    * Returns a vacant <code>ColonyTile</code> where the
    * given <code>unit</code> produces the maximum output of
    * the given <code>goodsType</code>.
    */
    public ColonyTile getVacantColonyTileFor(Unit unit, int goodsType) {
        ColonyTile bestPick = null;
        int highestProduction = -2;

        Iterator colonyTileIterator = getColonyTileIterator();
        while (colonyTileIterator.hasNext()) {
            ColonyTile colonyTile = (ColonyTile) colonyTileIterator.next();              
            
            if (colonyTile.canAdd(unit)) {
                Tile workTile = colonyTile.getWorkTile();
                boolean ourLand = workTile.getNationOwner() == Player.NO_NATION 
                                  || workTile.getNationOwner() == unit.getNation();
                if (ourLand && unit.getFarmedPotential(goodsType, colonyTile.getWorkTile()) > highestProduction) {
                    highestProduction = unit.getFarmedPotential(goodsType, colonyTile.getWorkTile());
                    bestPick = colonyTile;
                }
            }
        }

        return bestPick;
    }


    /**
    * Returns the production of a vacant <code>ColonyTile</code>
    * where the given <code>unit</code> produces the maximum output
    * of the given <code>goodsType</code>.
    */
    public int getVacantColonyTileProductionFor(Unit unit, int goodsType) {
        ColonyTile bestPick = getVacantColonyTileFor(unit, goodsType);
        return unit.getFarmedPotential(goodsType, bestPick.getWorkTile());
    }


    /**
    * Returns the horse production (given that enough food
    * is being produced and a sufficient storage capacity).
    *
    * @return The number of producable horses.
    */
    public int getPotentialHorseProduction() {
        if (getGoodsCount(Goods.HORSES) < 2) {
            return 0;
        }
        int maxAmount = Math.max(1, getGoodsCount(Goods.HORSES) / 10);
        int maxSpace = Math.max(0, getWarehouseCapacity() - getGoodsCount(Goods.HORSES));

        return Math.min(maxAmount, maxSpace);
        
    }

    /**
    * Gets the production of horses in this <code>Colony</code>.
    */
    public int getHorseProduction() {
        int surplus = getFoodProduction() - getFoodConsumption();
        int potential = getPotentialHorseProduction();

        if (getGoodsCount(Goods.HORSES) >= 2 && surplus > 1) {
            if (!getBuilding(Building.STABLES).isBuilt()) {
                return Math.min(surplus / 2, potential);
            }

            return Math.min(surplus, potential);
        }

        return 0;
    }

    
    
    /**
     * Returns how much of a Good will be produced by this colony this turn, 
     * taking into account how much is consumed - by workers, horses, etc.
     *
     * @param goodsType The goods' type.
     * @return The amount of the given goods currently unallocated for next 
     *      turn.
     */
    public int getProductionNetOf(int goodsType) {
        int count = getProductionOf( goodsType );
        int used = 0;
        switch (goodsType) {
            case Goods.FOOD:
                used = getFoodConsumption();
                used += getHorseProduction();
                break;
            default:
                Building bldg = getBuildingForConsuming(goodsType);
                if (bldg != null) {
                    used = bldg.getGoodsInput();
                }
                // TODO FIXME  This should also take into account tools needed for a current building project
        }
        count -= used;
        return count;
    }

    private void checkBuildingComplete() {
        // In order to avoid duplicate messages:
        if (lastVisited == getGame().getTurn().getNumber()) {
            return;
        }

        lastVisited = getGame().getTurn().getNumber();

        if (getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) {
            int unitType = getCurrentlyBuilding() - BUILDING_UNIT_ADDITION;

            if (canBuildUnit(unitType) && Unit.getNextHammers(unitType) <= getHammers() && Unit.getNextHammers(unitType) != -1) {
                if (Unit.getNextTools(unitType) <= getGoodsCount(Goods.TOOLS)) {
                    Unit unit = getGame().getModelController().createUnit(getID() + "buildUnit", getTile(), getOwner(), unitType);
                    hammers = 0;
                    removeGoods(Goods.TOOLS, Unit.getNextTools(unit.getType()));
                    addModelMessage(this, "model.colony.unitReady", new String[][] {{"%colony%", getName()}, {"%unit%", unit.getName()}});
                } else {
                    addModelMessage(this, "model.colony.itemNeedTools", new String[][] {{"%colony%", getName()}, {"%item%", Unit.getName(unitType)}});
                }
            }
        } else if (currentlyBuilding != -1) {
            int hammersRequired = getBuilding(currentlyBuilding).getNextHammers();
            int toolsRequired = getBuilding(currentlyBuilding).getNextTools();

            if ((hammers >= hammersRequired) && (hammersRequired != -1)) {
                hammers = hammersRequired;
                if (getGoodsCount(Goods.TOOLS) >= toolsRequired) {
                    //TODO: Adam Smith check for factory level buildings
                    if (!getBuilding(currentlyBuilding).canBuildNext()) {
                        throw new IllegalStateException("Cannot build the selected building.");
                    }
                    if (toolsRequired > 0) {
                        removeGoods(Goods.TOOLS, toolsRequired);
                    }
                    hammers = 0;
                    getBuilding(currentlyBuilding).setLevel(getBuilding(currentlyBuilding).getLevel() + 1);
                    addModelMessage(this, "model.colony.buildingReady", new String[][] {{"%colony%", getName()}, {"%building%", getBuilding(currentlyBuilding).getName()}});
                } else {
                    addModelMessage(this, "model.colony.itemNeedTools", new String[][] {{"%colony%", getName()}, {"%item%", getBuilding(currentlyBuilding).getNextName()}});
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
        // Any changes in this method should also be reflected in "payForBuilding()"

        int hammersRemaining = 0;
        int toolsRemaining = 0;
        if (getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) {
            int unitType = getCurrentlyBuilding() - BUILDING_UNIT_ADDITION;
            hammersRemaining = Math.max(Unit.getNextHammers(unitType) - hammers, 0);
            toolsRemaining = Math.max(Unit.getNextTools(unitType) - getGoodsCount(Goods.TOOLS), 0);
        } else if (getCurrentlyBuilding() != -1) {
            hammersRemaining = Math.max(getBuilding(currentlyBuilding).getNextHammers() - hammers, 0);
            toolsRemaining = Math.max(getBuilding(currentlyBuilding).getNextTools() - getGoodsCount(Goods.TOOLS), 0);
        }

        int price = hammersRemaining * getGameOptions().getInteger(GameOptions.HAMMER_PRICE)
                    + (getGame().getMarket().getBidPrice(Goods.TOOLS, toolsRemaining) * 110) / 100;

        return price;
    }


    /**
    * Buys the remaining hammers and tools for the {@link Building} that is
    * currently being built.
    *
    * @exception IllegalStateException If the owner of this <code>Colony</code>
    *            has an insufficient amount of gold.
    * @see #getPriceForBuilding
    */
    public void payForBuilding() {
        // Any changes in this method should also be reflected in "getPriceForBuilding()"

        if (getPriceForBuilding() > getOwner().getGold()) {
            throw new IllegalStateException("Not enough gold.");
        }

        int hammersRemaining = 0;
        int toolsRemaining = 0;
        if (getCurrentlyBuilding() >= Colony.BUILDING_UNIT_ADDITION) {
            int unitType = getCurrentlyBuilding() - BUILDING_UNIT_ADDITION;
            hammersRemaining = Math.max(Unit.getNextHammers(unitType) - hammers, 0);
            toolsRemaining = Math.max(Unit.getNextTools(unitType) - getGoodsCount(Goods.TOOLS), 0);
            hammers = Math.max(Unit.getNextHammers(unitType), hammers);
        } else if (getCurrentlyBuilding() != -1) {
            hammersRemaining = Math.max(getBuilding(currentlyBuilding).getNextHammers() - hammers, 0);
            toolsRemaining = Math.max(getBuilding(currentlyBuilding).getNextTools() - getGoodsCount(Goods.TOOLS), 0);
            hammers = Math.max(getBuilding(currentlyBuilding).getNextHammers(), hammers);
        }

        if (hammersRemaining > 0) {
            getOwner().modifyGold(-hammersRemaining * getGameOptions().getInteger(GameOptions.HAMMER_PRICE));
        }
        if (toolsRemaining > 0) {
            getGame().getMarket().buy(Goods.TOOLS, toolsRemaining, getOwner());
            getGoodsContainer().addGoods(Goods.TOOLS, toolsRemaining);
        }
    }


    /**
    * Returns a random unit from this colony.
    *
    * At this moment, this method always returns the first unit
    * in the colony.
    */
    public Unit getRandomUnit() {
        if (getUnitIterator().hasNext()) {
            return ((Unit) getUnitIterator().next());
        } else {
            return null;
        }
    }


    /**
    * Prepares this <code>Colony</code> for a new turn.
    */
    public void newTurn() {
        // Skip doing work in enemy colonies.
        if (unitCount != -1) {
            return;
        }

        // Repair any damaged ships:
        Iterator unitIterator = getTile().getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            if (unit.isNaval() && unit.isUnderRepair()) {
                unit.setHitpoints(unit.getHitpoints()+1);
            }
        }

        // Eat food:
        int eat = getFoodConsumption();
        int food = getGoodsCount(Goods.FOOD);

        if (eat > food) {
            // Kill a colonist:
            getRandomUnit().dispose();
            removeGoods(Goods.FOOD, food);
            addModelMessage(this, "model.colony.colonistStarved", new String[][] {{"%colony%", getName()}});
        } else {
            removeGoods(Goods.FOOD, eat);

            if (eat > getFoodProduction() && (food-eat) / (eat - getFoodProduction()) <= 3) {
                addModelMessage(this, "model.colony.famineFeared", new String[][] {{"%colony%", getName()}, {"%number%", Integer.toString((food-eat) / (eat - getFoodProduction()))}});
            }
        }

        // Breed horses:
        int horseProduction = getHorseProduction();
        if (horseProduction != 0) {
            if (!getBuilding(Building.STABLES).isBuilt()) {
                removeGoods(Goods.FOOD, horseProduction);
                addGoods(Goods.HORSES, horseProduction);
            } else {
                removeGoods(Goods.FOOD, horseProduction/2);
                addGoods(Goods.HORSES, horseProduction);
            }
        }

        // Create a new colonist if there is enough food:
        if (getGoodsCount(Goods.FOOD) >= 200) {
            Unit u = getGame().getModelController().createUnit(getID() + "newTurn200food", getTile(), getOwner(), Unit.FREE_COLONIST);
            removeGoods(Goods.FOOD, 200);
            addModelMessage(this, "model.colony.newColonist", new String[][] {{"%colony%", getName()}});
            logger.info("New colonist created in " + getName() + " with ID=" + u.getID());
        }

        // Build:
        checkBuildingComplete();

        // Export goods if custom house is built
        if (getBuilding(Building.CUSTOM_HOUSE).isBuilt()) {
            Iterator goodsIterator = getCompactGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = (Goods) goodsIterator.next();
                if (getExports(goods) && owner.canTrade(goods)) {
                    getGame().getMarket().sell(goods, owner);
                }
            }
        }                
        
        // Throw away goods there is no room for.
        goodsContainer.cleanAndReport(getWarehouseCapacity(), new int [] {200, 100});

        // Remove bells:
        bells -= (getSoL() * getUnitCount()) / 100;
        if (bells < 0) {
            bells = 0;
        }
    }


    /**
    * Returns the capacity of this colony's warehouse. All goods above this limit,
    * except {@link Goods#FOOD}, will be removed when calling {@link #newTurn}.
    */
    public int getWarehouseCapacity() {
        return 100 + getBuilding(Building.WAREHOUSE).getLevel() * 100;
    }


    public void dispose() {
        Iterator i = getWorkLocationIterator();
        while (i.hasNext()) {
            WorkLocation w = (WorkLocation) i.next();
            ((FreeColGameObject) w).dispose();
        }

        getTile().setSettlement(null);
        super.dispose();
    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Colony".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element colonyElement = document.createElement(getXMLElementTagName());

        colonyElement.setAttribute("ID", getID());
        colonyElement.setAttribute("name", name);
        colonyElement.setAttribute("owner", owner.getID());
        colonyElement.setAttribute("tile", tile.getID());

        if (showAll || player == getOwner()) {
            colonyElement.setAttribute("hammers", Integer.toString(hammers));
            colonyElement.setAttribute("bells", Integer.toString(bells));
            colonyElement.setAttribute("sonsOfLiberty", Integer.toString(sonsOfLiberty));
            colonyElement.setAttribute("oldSonsOfLiberty", Integer.toString(oldSonsOfLiberty));
            colonyElement.setAttribute("currentlyBuilding", Integer.toString(currentlyBuilding));
            colonyElement.setAttribute("landLocked", Boolean.toString(landLocked));

            char[] exportsCharArray = new char[exports.length];
            for(int i = 0; i < exports.length; i++) {
                exportsCharArray[i] = (exports[i] ? '1' : '0');
            }
            colonyElement.setAttribute("exports", new String(exportsCharArray));

            Iterator workLocationIterator = workLocations.iterator();
            while (workLocationIterator.hasNext()) {
                colonyElement.appendChild(((FreeColGameObject) workLocationIterator.next()).toXMLElement(player, document, showAll, toSavedGame));
            }
        } else {
            colonyElement.setAttribute("unitCount", Integer.toString(getUnitCount()));
            colonyElement.appendChild(getBuilding(Building.STOCKADE).toXMLElement(player, document, showAll, toSavedGame));
        }

        colonyElement.appendChild(goodsContainer.toXMLElement(player, document, showAll, toSavedGame));

        return colonyElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    * @param colonyElement The DOM-element ("Document Object Model") made to represent this "Colony".
    */
    public void readFromXMLElement(Element colonyElement) {
        setID(colonyElement.getAttribute("ID"));

        name = colonyElement.getAttribute("name");
        owner = (Player) getGame().getFreeColGameObject(colonyElement.getAttribute("owner"));
        tile = (Tile) getGame().getFreeColGameObject(colonyElement.getAttribute("tile"));

        if (colonyElement.hasAttribute("hammers")) {
            hammers = Integer.parseInt(colonyElement.getAttribute("hammers"));
        } else {
            hammers = 0;
        }

        if (colonyElement.hasAttribute("bells")) {
            bells = Integer.parseInt(colonyElement.getAttribute("bells"));
        } else {
            bells = 0;
        }

        if (colonyElement.hasAttribute("sonsOfLiberty")) {
            sonsOfLiberty = Integer.parseInt(colonyElement.getAttribute("sonsOfLiberty"));
        } else {
            sonsOfLiberty = 0;
        }

        if (colonyElement.hasAttribute("oldSonsOfLiberty")) {
            oldSonsOfLiberty = Integer.parseInt(colonyElement.getAttribute("oldSonsOfLiberty"));
        } else {
            oldSonsOfLiberty = 0;
        }

        if (colonyElement.hasAttribute("currentlyBuilding")) {
            currentlyBuilding = Integer.parseInt(colonyElement.getAttribute("currentlyBuilding"));
        } else {
            currentlyBuilding = -1;
        }

        if (colonyElement.hasAttribute("landLocked")) {
            landLocked = Boolean.valueOf(colonyElement.getAttribute("landLocked")).booleanValue();
        } else {
            landLocked = true;
        }

        if (colonyElement.hasAttribute("unitCount")) {
            unitCount = Integer.parseInt(colonyElement.getAttribute("unitCount"));
        } else {
            unitCount = -1;
        }

        if (colonyElement.hasAttribute("exports")) {
            exports = new boolean[Goods.NUMBER_OF_TYPES];
            String exportString = colonyElement.getAttribute("exports");
            for(int i = 0; i < exportString.length(); i++) {
                exports[i] = ( (exportString.charAt(i) == '1') ? true : false );
            }
        }

        NodeList childNodes = colonyElement.getChildNodes();
        for (int i=0; i<childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }        
            Element childElement = (Element) node;

            if (childElement.getTagName().equals(ColonyTile.getXMLElementTagName())) {
                ColonyTile ct = (ColonyTile) getGame().getFreeColGameObject(childElement.getAttribute("ID"));

                if (ct != null) {
                    ct.readFromXMLElement(childElement);
                } else {
                    workLocations.add(new ColonyTile(getGame(), childElement));
                }
            } else if (childElement.getTagName().equals(Building.getXMLElementTagName())) {
                Building b = (Building) getGame().getFreeColGameObject(childElement.getAttribute("ID"));

                if (b != null) {
                    b.readFromXMLElement(childElement);
                } else {
                    workLocations.add(new Building(getGame(), childElement));
                }
            } else if (childElement.getTagName().equals(GoodsContainer.getXMLElementTagName())) {
                GoodsContainer gc = (GoodsContainer) getGame().getFreeColGameObject(childElement.getAttribute("ID"));

                if (gc != null) {
                    goodsContainer.readFromXMLElement(childElement);
                } else {
                    goodsContainer = new GoodsContainer(getGame(), this, childElement);
                }
            } 
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "colony".
    */
    public static String getXMLElementTagName() {
        return "colony";
    }
}
