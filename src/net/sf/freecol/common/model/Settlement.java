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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;

import org.w3c.dom.Element;


/**
 * The super class of all settlements on the map (that is colonies and indian settlements).
 */
abstract public class Settlement extends FreeColGameObject implements Location, Named, Ownable {

    private static final Logger logger = Logger.getLogger(Settlement.class.getName());

    public static final int FOOD_PER_COLONIST = 200;

    public static enum SettlementType {
        SMALL_COLONY, MEDIUM_COLONY, LARGE_COLONY,
        SMALL_STOCKADE, MEDIUM_STOCKADE, MEDIUM_FORT,
        LARGE_STOCKADE, LARGE_FORT, LARGE_FORTRESS,
        UNDEAD,
        INDIAN_CAMP, INDIAN_VILLAGE, AZTEC_CITY, INCA_CITY
    }

    /** The <code>Player</code> owning this <code>Settlement</code>. */
    protected Player owner;

    /** The name of the Settlement. */
    private String name;

    /** The <code>Tile</code> where this <code>Settlement</code> is located. */
    protected Tile tile;

    protected GoodsContainer goodsContainer;

    /**
     * Whether this is the capital of the nation.
     */
    private boolean isCapital = false;

    /**
     * Contains the abilities and modifiers of this Colony.
     */
    private FeatureContainer featureContainer;


    /**
     * Empty constructor needed for Colony -> ServerColony.
     */
    protected Settlement() {
        // empty constructor
    }

    /**
     * Creates a new <code>Settlement</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The owner of this <code>Settlement</code>.
     * @param name The name for this <code>Settlement</code>.
     * @param tile The location of the <code>Settlement</code>.
     */
    public Settlement(Game game, Player owner, String name, Tile tile) {
        super(game);
        this.owner = owner;
        this.name = name;
        this.tile = tile;

        // Relocate any worker already on the Tile (from another Settlement):
        if (tile.getOwningSettlement() != null) {
            if (tile.getOwningSettlement() instanceof Colony) {
                Colony oc = (Colony) tile.getOwningSettlement();
                ColonyTile ct = oc.getColonyTile(tile);
                ct.relocateWorkers();
            } else if (tile.getOwningSettlement() instanceof IndianSettlement) {
                logger.warning("An indian settlement is already owning the tile.");
            } else {
                logger.warning("An unknown type of settlement is already owning the tile.");
            }
        }
        featureContainer = new FeatureContainer(game.getSpecification());
        owner.addSettlement(this);
    }



    /**
     * Initiates a new <code>Settlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Settlement(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
    }

    /**
     * Initiates a new <code>Settlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public Settlement(Game game, Element e) {
        super(game, e);
    }

    /**
     * Initiates a new <code>Settlement</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Settlement(Game game, String id) {
        super(game, id);
    }


    // TODO: remove this again
    public String getNameKey() {
        return getName();
    }

    /**
     * Gets the name of this <code>Settlement</code>.
     *
     * @return The name as a <code>String</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of this <code>Settlement</code> for a particular player.
     *
     * @param player A <code>Player</code> to return the name for.
     * @return The name as a <code>String</code>.
     */
    abstract public String getNameFor(Player player);

    /**
     * Sets the name of this <code>Settlement</code>.
     *
     * @param newName The new name.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Returns <code>true</code> if this is the Nation's capital.
     *
     * @return <code>true</code> if this is the Nation's capital.
     */
    public boolean isCapital() {
        return isCapital;
    }

    /**
     * Sets the capital value.
     *
     * @param isCapital a <code>boolean</code> value
     */
    public void setCapital(boolean isCapital) {
        this.isCapital = isCapital;
    }


    /**
     * Describe <code>getFeatureContainer</code> method here.
     *
     * @return a <code>FeatureContainer</code> value
     */
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    /**
     * Describe <code>setFeatureContainer</code> method here.
     *
     * @param container a <code>FeatureContainer</code> value
     */
    protected void setFeatureContainer(FeatureContainer container) {
        featureContainer = container;
    }

    /**
     * Gets this colony's line of sight.
     * @return The line of sight offered by this
     *       <code>Colony</code>.
     * @see Player#canSee(Tile)
     */
    public int getLineOfSight() {
        return 2;
    }


    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Settlement</code>.
     *
     * @param attacker The unit be attacking this <code>Settlement</code>.
     * @return The <code>Unit</code> that has been chosen to defend
     * this <code>Settlement</code>.
     */
    abstract public Unit getDefendingUnit(Unit attacker);

    /**
     * Get the amount of gold plundered when this settlement is captured.
     */
    abstract public int getPlunder();

    /**
     * Gets the <code>Tile</code> where this <code>Settlement</code> is located.
     * @return The <code>Tile</code> where this <code>Settlement</code> is located.
     */
    public Tile getTile() {
        return tile;
    }

    /**
     * Claim ownership of a tile for this settlement.
     *
     * @param tile The <code>Tile</code> to claim.
     * @return True if some aspect of the tile changed.
     */
    public boolean claimTile(Tile tile) {
        boolean change = false;
        if (tile.getOwningSettlement() != this) {
            tile.setOwningSettlement(this);
            change = true;
        }
        if (tile.getOwner() != owner) {
            tile.setOwner(owner);
            change = true;
        }
        if (change) {
            tile.updatePlayerExploredTiles();
        }
        return change;
    }

    /**
     * Disclaim ownership of a tile for this settlement.
     *
     * @param tile The <code>Tile</code> to disclaim.
     * @return True if some aspect of the tile changed.
     */
    public boolean disclaimTile(Tile tile) {
        if (tile.getOwningSettlement() == this) {
            tile.setOwner(null);
            tile.setOwningSettlement(null);
            tile.updatePlayerExploredTiles();
            return true;
        }
        return false;
    }

    /**
     * Put a prepared settlement onto the map.
     */
    public void placeSettlement() {
        List<Tile> tiles = getGame().getMap().getClaimableTiles(owner, tile,
                                                                getRadius());
        if (!tiles.contains(tile)) {
            throw new IllegalStateException("Can not claim center tile");
        }
        tile.setSettlement(this);
        for (Tile t : tiles) {
            t.setOwner(owner);
            t.setOwningSettlement(this);
        }
        for (Tile t : tile.getSurroundingTiles(getLineOfSight())) {
            owner.setExplored(t);
        }
        owner.invalidateCanSeeTiles();
    }

    /**
     * Gets the owner of this <code>Settlement</code>.
     *
     * @return The owner of this <code>Settlement</code>.
     * @see #setOwner
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this <code>Settlement</code>.
     *
     * @param player The new owner of this <code>Settlement</code>.
     */
    public void setOwner(Player player) {
        owner = player;
    }

    /**
     * Change the owner of this <code>Settlement</code>.
     *
     * @param newOwner The <code>Player</code> that shall own this
     *            <code>Settlement</code>.
     * @see #getOwner
     */
    public void changeOwner(Player newOwner) {
        Player oldOwner = this.owner;
        setOwner(newOwner);

        if (oldOwner.hasSettlement(this)) {
            oldOwner.removeSettlement(this);
        }
        if (!newOwner.hasSettlement(this)) {
            newOwner.addSettlement(this);
        }

        List<Unit> units = getUnitList();
        units.addAll(getTile().getUnitList());
        for (Unit u : units) {
            u.setState(UnitState.ACTIVE);
            UnitType type = u.getTypeChange((newOwner.isUndead())
                                            ? ChangeType.UNDEAD
                                            : ChangeType.CAPTURE, newOwner);
            if (type != null) u.setType(type);
            u.setOwner(newOwner);
        }

        for (Tile t : getOwnedTiles()) t.setOwner(newOwner);
        oldOwner.invalidateCanSeeTiles();
        newOwner.invalidateCanSeeTiles();

        if (getGame().getFreeColGameObjectListener() != null) {
            getGame().getFreeColGameObjectListener()
                .ownerChanged(this, oldOwner, newOwner);
        }
    }

    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Goods</code> in this
     * <code>GoodsContainer</code>. Each <code>Goods</code> have a maximum
     * amount of 100.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Goods> getGoodsIterator() {
        return goodsContainer.getGoodsIterator();
    }

    /**
     * Gets an <code>List</code> with every <code>Goods</code> in this
     * <code>Colony</code>. There is only one <code>Goods</code> for each
     * type of goods.
     *
     * @return The <code>Iterator</code>.
     */
    public List<Goods> getCompactGoods() {
        return goodsContainer.getCompactGoods();
    }

    /**
     * Get the tiles this settlement owns.
     *
     * @return A list of tiles.
     */
    public List<Tile> getOwnedTiles() {
        Tile settlementTile = getTile();
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        for (Tile t : settlementTile.getSurroundingTiles(getRadius())) {
            if (t.getOwningSettlement() == this) tiles.add(t);
        }
        tiles.add(settlementTile);
        return tiles;
    }

    /**
     * Dispose of this settlement.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        if (owner != null && goodsContainer != null) {
            objects.addAll(goodsContainer.disposeList());
            goodsContainer = null;
        }

        if (owner != null && getTile() != null
            && getTile().getSettlement() != null) {
            // Defensive tests to handle transition from calling dispose()
            // on both sides to when it is only called on server-side.

            // Get off the map
            Tile settlementTile = getTile();
            List<Tile> lostTiles = getOwnedTiles();
            for (Tile tile : lostTiles) {
                tile.setOwningSettlement(null);
                tile.setOwner(null);
                tile.updatePlayerExploredTiles();
            }
            settlementTile.setSettlement(null);

            // The owner forgets about the settlement.
            Player oldOwner = owner;
            setOwner(null);
            oldOwner.removeSettlement(this);
            oldOwner.invalidateCanSeeTiles();
        }

        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Dispose of this <code>Settlement</code>.
     */
    public void dispose() {
        disposeList();
    }

    /**
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.
     *
     * @return Settlement radius
     */
    public int getRadius() {
        if (isCapital) {
            return owner.getNationType().getCapitalRadius();
        } else {
            return owner.getNationType().getSettlementRadius();
        }
    }

    /**
     * Gets the current Sons of Liberty in this settlement.
     */
    public abstract int getSoL();

    /**
     * Propagates a global change in tension down to a settlement.
     * Only apply the change if the settlement is aware of the player
     * causing alarm.
     *
     * @param player The <code>Player</code> towards whom the alarm is felt.
     * @param addToAlarm The amount to add to the current alarm level.
     * @return True if the settlement alarm level changes as a result
     *     of this change.
     */
    public abstract boolean propagateAlarm(Player player, int addToAlarm);

    /**
     * Removes a specified amount of a type of Goods from this Settlement.
     *
     * @param type The type of Goods to remove from this settlement.
     * @param amount The amount of Goods to remove from this settlement.
     */
    public void removeGoods(GoodsType type, int amount) {
        goodsContainer.removeGoods(type, amount);
    }

    /**
     * Removes the given Goods from the Settlement.
     *
     * @param goods a <code>Goods</code> value
     */
    public void removeGoods(AbstractGoods goods) {
        goodsContainer.removeGoods(goods);
    }

    /**
     * Removes all Goods of the given type from the Settlement.
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
        goodsContainer.addGoods(type.getStoredAs(), amount);
    }

    public void addGoods(AbstractGoods goods) {
        addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the amount of one type of Goods at this Settlement.
     *
     * @param type The type of goods to look for.
     * @return The amount of this type of Goods at this Location.
     */
    public int getGoodsCount(GoodsType type) {
        if (type != null && !type.isStoredAs()) {
            return goodsContainer.getGoodsCount(type);
        } else {
            return 0;
        }
    }

    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType The type of goods to get the production for.
     * @return The production of the given type of goods the current turn by the
     * <code>Settlement</code>
     */
    public abstract int getProductionOf(GoodsType goodsType);

    /**
     * Returns the number of goods of a given type used by the settlement
     * each turn.
     *
     * @param goodsTypes <code>GoodsType</code> values
     * @return an <code>int</code> value
     */
    public int getConsumptionOf(GoodsType goodsType) {
        int result = 0;
        for (Unit unit : getUnitList()) {
            result += unit.getType().getConsumptionOf(goodsType);
        }
        return Math.max(0, result);
    }

    /**
     * Gives the food needed to keep all units alive in this Settlement.
     *
     * @return The amount of food eaten in this colony each this turn.
     */
    public int getFoodConsumption() {
        int result = 0;
        for (GoodsType foodType : getSpecification().getFoodGoodsTypeList()) {
            result += getConsumptionOf(foodType);
        }
        return result;
    }

    /**
     * Gets food consumption by type
     */
    public int getFoodConsumptionByType(GoodsType type) {
    	// Since for now only model.goods.food are needed for other
    	// purposes we will hard code the preference of consumption
    	// for other types of food If later other requirements appear,
    	// an allocation algorithm needs to be implemented

    	if(!type.isFoodType()){
            logger.warning("Good type given isnt food type");
            return 0;
    	}

    	int required = getFoodConsumption();
    	int consumed = 0;
    	GoodsType corn = getSpecification().getGoodsType("model.goods.grain");

    	for (GoodsType foodType : getSpecification().getGoodsFood()) {
            if(foodType == corn){
                // consumption of corn calculated last
                continue;
            }

            consumed = Math.min(getProductionOf(foodType),required);
            if(type == foodType){
                return consumed;
            }
            required -= consumed;
        }

    	// type asked is corn, calculate consumption and return
    	consumed = Math.min(getProductionOf(corn),required);

    	return consumed;
    }

    protected void removeFood(final int amount) {
        int rest = amount;
        List<AbstractGoods> backlog = new ArrayList<AbstractGoods>();
        for (GoodsType foodType : getSpecification().getGoodsFood()) {
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

    /**
     * Returns the total amount of food present.
     *
     * @return an <code>int</code> value
     */
    public int getFoodCount() {
        int result = 0;
        for (GoodsType foodType : getSpecification().getGoodsFood()) {
            result += getGoodsCount(foodType);
        }
        return result;
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

}
