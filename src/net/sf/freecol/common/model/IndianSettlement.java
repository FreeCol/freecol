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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.Role;

import org.w3c.dom.Element;


/**
 * Represents an Indian settlement.
 */
public class IndianSettlement extends Settlement {

    private static final Logger logger = Logger.getLogger(IndianSettlement.class.getName());

    public static final int MISSIONARY_TENSION = -3;
    public static final int MAX_CONVERT_DISTANCE = 10;
    public static final int TURNS_PER_TRIBUTE = 5;
    public static final int ALARM_RADIUS = 2;

    public static final String UNITS_TAG_NAME = "units";

    /** The amount of goods a brave can produce a single turn. */
    //private static final int WORK_AMOUNT = 5;

    /** The amount of raw material that should be available before producing manufactured goods. */
    public static final int KEEP_RAW_MATERIAL = 50;

    /**
     * This is the skill that can be learned by Europeans at this
     * settlement.  At the server side its value will be null when the
     * skill has already been taught to a European.  At the client
     * side the value null is also possible in case the player hasn't
     * checked out the settlement yet.
     */
    private UnitType learnableSkill = null;

    private GoodsType[] wantedGoods = new GoodsType[] {null, null, null};

    /**
     * At the client side isVisited is true in case the player has
     * visited the settlement.
     */
    private boolean isVisited = false;

    /**
     * Whether this is the capital of the tribe.
     */
    private boolean isCapital = false;

    private List<Unit> units = Collections.emptyList();

    private ArrayList<Unit> ownedUnits = new ArrayList<Unit>();

    private Unit missionary = null;

    /** Used for monitoring the progress towards creating a convert. */
    private int convertProgress = 0;

    /** The number of the turn during which the last tribute was paid. */
    int lastTribute = 0;

    /**
     * Stores the alarm levels. <b>Only used by AI.</b>
     * 0-1000 with 1000 as the maximum alarm level.
     */
    private java.util.Map<Player, Tension> alarm = new HashMap<Player, Tension>();

    // sort goods types descending by price
    private final Comparator<GoodsType> wantedGoodsComparator = new Comparator<GoodsType>() {
        public int compare(GoodsType goodsType1, GoodsType goodsType2) {
            return getPrice(goodsType2, 100) - getPrice(goodsType1, 100);
        }
    };

    // sort goods descending by amount and price when amounts are equal
    private final Comparator<Goods> exportGoodsComparator = new Comparator<Goods>() {
        public int compare(Goods goods1, Goods goods2) {
            if (goods2.getAmount() == goods1.getAmount()) {
                return getPrice(goods2) - getPrice(goods1);
            } else {
                return goods2.getAmount() - goods1.getAmount();
            }
        }
    };

    /**
     * The constructor to use.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param player The <code>Player</code> owning this settlement.
     * @param tile The location of the <code>IndianSettlement</code>.
     * @param isCapital True if settlement is tribe's capital
     * @param learnableSkill The skill that can be learned by Europeans at this settlement.
     * @param isVisited Indicates if any European scout has asked to speak with the chief.
     * @param missionary The missionary in this settlement (or null).
     * @exception IllegalArgumentException if an invalid tribe or kind is given
     */
    public IndianSettlement(Game game, Player player, Tile tile, boolean isCapital,
                            UnitType learnableSkill, boolean isVisited, Unit missionary) {
        super(game, player, tile);

        if (tile == null) {
            throw new IllegalArgumentException("Parameter 'tile' must not be 'null'.");
        }
        
        tile.setOwner(player);
        tile.setSettlement(this);

        goodsContainer = new GoodsContainer(game, this);

        this.learnableSkill = learnableSkill;
        this.isCapital = isCapital;
        this.isVisited = isVisited;
        this.missionary = missionary;

        goodsContainer.addGoods(Goods.LUMBER, 300);
        convertProgress = 0;
        
        updateWantedGoods();
    }


    /**
     * Initiates a new <code>IndianSettlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public IndianSettlement(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>IndianSettlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public IndianSettlement(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>IndianSettlement</code> 
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public IndianSettlement(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns a suitable (non-unique) name.
     * @return The name of this settlement.
     */
    public String getLocationName() {
        if (isCapital()){
            return Messages.message("indianCapital", "%nation%", getOwner().getNationAsString());
        } else {
            return Messages.message("indianSettlement", "%nation%", getOwner().getNationAsString());
        }
    }

    /**
     * Returns the alarm Map.
     *
     * @return the alarm Map.
     */
    public java.util.Map<Player, Tension> getAlarm() {
        return alarm;
    }

    /**
     * Returns the amount of gold this settlement pays as a tribute.
     *
     * @param player a <code>Player</code> value
     * @return an <code>int</code> value
     */
    public int getTribute(Player player) {
        // increase tension whether we pay or not
        // apply tension directly to this settlement and let propagation works
        modifyAlarm(player, Tension.TENSION_ADD_NORMAL);

        int gold = 0;
        if (getGame().getTurn().getNumber() > lastTribute + TURNS_PER_TRIBUTE) {
            switch(getOwner().getTension(player).getLevel()) {
            case HAPPY:
            case CONTENT:
                gold = Math.min(getOwner().getGold() / 10, 100);
                break;
            case DISPLEASED:
                gold = Math.min(getOwner().getGold() / 20, 100);
                break;
            case ANGRY:
            case HATEFUL:
            default:
                // do nothing
            }

        }
        getOwner().modifyGold(-gold);
        lastTribute = getGame().getTurn().getNumber();
        return gold;
    }

    /**
     * Modifies the alarm level towards the given player.
     *
     * @param player The <code>Player</code>.
     * @param addToAlarm The amount to add to the current alarm level.
     */
    public void modifyAlarm(Player player, int addToAlarm) {
        Tension tension = alarm.get(player);
        if(tension != null) {
            tension.modify(addToAlarm);            
        }
        // propagate alarm upwards
        if (owner != null) {
            if (isCapital()) {
                // capital has a greater impact
                owner.modifyTension(player, addToAlarm, this);
            } else {
                owner.modifyTension(player, addToAlarm/2, this);
            }
        }
    }

    /**
     * Propagates the tension felt towards a given nation 
     * from the tribe down to each settlement that has already met that nation.
     * 
     * @param player The Player towards whom the alarm is felt.
     * @param addToAlarm The amount to add to the current alarm level.
     */
    public void propagatedAlarm(Player player, int addToAlarm) {
        Tension tension = alarm.get(player);
        // only applies tension if settlement has met europeans
        if (tension != null && isVisited) {
            tension.modify(addToAlarm);            
        }
    }

    /**
     * Sets alarm towards the given player.
     *
     * @param player The <code>Player</code>.
     * @param newAlarm The new alarm value.
     */
    public void setAlarm(Player player, Tension newAlarm) {
        alarm.put(player, newAlarm);
    }

    /**
     * Gets the alarm level towards the given player.
     * @param player The <code>Player</code> to get the alarm level for.
     * @return An object representing the alarm level.
     */    
    public Tension getAlarm(Player player) {
        return alarm.get(player);
    }

    /**
     * Gets the ID of the alarm message associated with the alarm
     * level of this player.
     *
     * @param player The other player.
     * @return The ID of an alarm level message.
     */
    public String getAlarmLevelMessage(Player player) {
        if (alarm.get(player) == null) {
            alarm.put(player, new Tension(0));
        }
        return "indianSettlement.alarm." + alarm.get(player).toString().toLowerCase();
    }

    /**
     * Returns true if a European player has visited this settlement to speak with the chief.
     * @return true if a European player has visited this settlement to speak with the chief.
     */
    public boolean hasBeenVisited() {
        return isVisited;
    }

    /**
     * Sets the visited status of this settlement to true, indicating
     * that a European has had a chat with the chief.
     *    
     * @param player a <code>Player</code> value
     */
    public void setVisited(Player player) {
        this.isVisited = true;
        if (alarm.get(player) == null) {
            alarm.put(player, new Tension(0));
        }
    }

    /**
     * Adds the given <code>Unit</code> to the list of units that belongs to this
     * <code>IndianSettlement</code>.
     * 
     * @param unit The <code>Unit</code> to be added.
     */
    public void addOwnedUnit(Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Parameter 'unit' must not be 'null'.");
        }

        if (!ownedUnits.contains(unit)) {
            ownedUnits.add(unit);
        }
    }


    /**
     * Gets an iterator over all the units this 
     * <code>IndianSettlement</code> is owning.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getOwnedUnitsIterator() {
        return ownedUnits.iterator();
    }


    /**
     * Removes the given <code>Unit</code> to the list of units that
     * belongs to this <code>IndianSettlement</code>. Returns true if
     * the Unit was removed.
     * 
     * @param unit The <code>Unit</code> to be removed from the
     *       list of the units this <code>IndianSettlement</code>
     *       owns.
     * @return a <code>boolean</code> value
     */
    public boolean removeOwnedUnit(Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Parameter 'unit' must not be 'null'.");
        }
        return ownedUnits.remove(unit);
    }


    /**
     * Returns the skill that can be learned at this settlement.
     * @return The skill that can be learned at this settlement.
     */
    public UnitType getLearnableSkill() {
        return learnableSkill;
    }


    /**
     * Returns this settlement's highly wanted goods.
     * @return This settlement's highly wanted goods.
     */
    public GoodsType getHighlyWantedGoods() {
        return wantedGoods[0];
    }


    /**
     * Returns this settlement's wanted goods 1.
     * @return This settlement's wanted goods 1.
     */
    public GoodsType getWantedGoods1() {
        return wantedGoods[1];
    }


    /**
     * Returns this settlement's wanted goods 2.
     * @return This settlement's wanted goods 2.
     */
    public GoodsType getWantedGoods2() {
        return wantedGoods[2];
    }


    /**
     * Returns the missionary from this settlement if there is one or null if there is none.
     * @return The missionary from this settlement if there is one or null if there is none.
     */
    public Unit getMissionary() {
        return missionary;
    }


    /**
     * Sets the missionary for this settlement.
     * @param missionary The missionary for this settlement.
     */
    public void setMissionary(Unit missionary) {
        if (missionary != null) {
            if (missionary.getRole() != Role.MISSIONARY) {
                throw new IllegalArgumentException("Specified unit is not a missionary.");
            }
            missionary.setLocation(null);
            if (alarm.get(missionary.getOwner()) == null) {
                alarm.put(missionary.getOwner(), new Tension(0));
            }
        }
        if (missionary != this.missionary) {
            convertProgress = 0;
        }
        if (this.missionary != null) {
            this.missionary.dispose();
        }
        this.missionary = missionary;
        getTile().updatePlayerExploredTiles();
    }



    public GoodsType[] getWantedGoods() {
        return wantedGoods;
    }

    public void setWantedGoods(int index, GoodsType type) {
        if (0 <= index && index <= 2) {
            wantedGoods[index] = type;
        }
    }

    /**
     * Sets the learnable skill for this Indian settlement.
     * @param skill The new learnable skill for this Indian settlement.
     */
    public void setLearnableSkill(UnitType skill) {
        learnableSkill = skill;
    }


    /**
     * Gets the kind of Indian settlement.
     */
    public SettlementType getTypeOfSettlement() {
        return ((IndianNationType) owner.getNationType()).getTypeOfSettlement();
    }

    /**
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.  Cities dominate 2 tiles, other settlements 1 tile.
     *
     * @return Settlement radius
     */
    @Override
    public int getRadius() {
        if (getTypeOfSettlement() == SettlementType.INCA_CITY ||
            getTypeOfSettlement() == SettlementType.AZTEC_CITY) {
            return 2;
        } else {
            return 1;
        }
    }

    public boolean isCapital() {
        return isCapital;
    }


    public void setCapital(boolean isCapital) {
        this.isCapital = isCapital;
    }


    /**
     * Adds a <code>Locatable</code> to this Location.
     *
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    @Override
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            if (units.equals(Collections.emptyList())) {
                units = new ArrayList<Unit>();
            }
            units.add((Unit) locatable);
        } else if (locatable instanceof Goods) {
            goodsContainer.addGoods((Goods)locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a IndianSettlement.");
        }
    }


    /**
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable The <code>Locatable</code> to remove from this Location.
     */
    @Override
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            units.remove((Unit) locatable);
        } else if (locatable instanceof Goods) {
            goodsContainer.removeGoods((Goods)locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a IndianSettlement.");
        }
    }


    /**
     * Returns the amount of Units at this Location.
     *
     * @return The amount of Units at this Location.
     */
    @Override
    public int getUnitCount() {
        return units.size();
    }

    public List<Unit> getUnitList() {
        return units;
    }
    
    public Iterator<Unit> getUnitIterator() {
        return units.iterator();
    }

    public Unit getFirstUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(0);
        }
    }

    public Unit getLastUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(units.size() - 1);
        }
    }

    /**
     * Gets the <code>Unit</code> that is currently defending this <code>IndianSettlement</code>.
     * @param attacker The unit that would be attacking this <code>IndianSettlement</code>.
     * @return The <code>Unit</code> that has been chosen to defend this <code>IndianSettlement</code>.
     */
    @Override
    public Unit getDefendingUnit(Unit attacker) {
        Unit defender = null;
        float defencePower = -1.0f;
        for (Unit nextUnit : units) {
            float tmpPower = attacker.getGame().getCombatModel().getDefencePower(attacker, nextUnit);
            if (tmpPower > defencePower) {
                defender = nextUnit;
                defencePower = tmpPower;
            }
        }
        return defender;
    }


    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to pay for the given <code>Goods</code>.
     *
     * <br><br>
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param goods The <code>Goods</code> to price.
     * @return The price.
     */
    public int getPrice(Goods goods) {
        return getPrice(goods.getType(), goods.getAmount());
    }


    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to pay for the given <code>Goods</code>.
     *
     * <br><br>
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param type The type of <code>Goods</code> to price.
     * @param amount The amount of <code>Goods</code> to price.
     * @return The price.
     */
    public int getPrice(GoodsType type, int amount) {
        int returnPrice = 0;

        if (amount > 100) {
            throw new IllegalArgumentException();
        }

        if (type == Goods.MUSKETS) {
            int need = 0;
            int supply = goodsContainer.getGoodsCount(Goods.MUSKETS);
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.MUSKETS_TO_ARM_INDIAN;
                if (ownedUnits.get(i).isArmed()) {
                    supply += Unit.MUSKETS_TO_ARM_INDIAN;
                }
            }

            int sets = ((goodsContainer.getGoodsCount(Goods.MUSKETS) + amount) / Unit.MUSKETS_TO_ARM_INDIAN)
                - (goodsContainer.getGoodsCount(Goods.MUSKETS) / Unit.MUSKETS_TO_ARM_INDIAN);
            int startPrice = (19+getPriceAddition()) - (supply / Unit.MUSKETS_TO_ARM_INDIAN);
            for (int i=0; i<sets; i++) {
                if ((startPrice-i) < 8 && (need > supply || goodsContainer.getGoodsCount(Goods.MUSKETS) < Unit.MUSKETS_TO_ARM_INDIAN * 2)) {
                    startPrice = 8+i;
                }
                returnPrice += Unit.MUSKETS_TO_ARM_INDIAN * (startPrice-i);
            }
        } else if (type == Goods.HORSES) {
            int need = 0;
            int supply = goodsContainer.getGoodsCount(Goods.HORSES);
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.HORSES_TO_MOUNT_INDIAN;
                if (ownedUnits.get(i).isMounted()) {
                    supply += Unit.HORSES_TO_MOUNT_INDIAN;
                }
            }

            int sets = (goodsContainer.getGoodsCount(Goods.HORSES) + amount) / Unit.HORSES_TO_MOUNT_INDIAN
                - (goodsContainer.getGoodsCount(Goods.HORSES) / Unit.HORSES_TO_MOUNT_INDIAN);
            int startPrice = (24+getPriceAddition()) - (supply/Unit.HORSES_TO_MOUNT_INDIAN);

            for (int i=0; i<sets; i++) {
                if ((startPrice-(i*4)) < 4 && (need > supply || goodsContainer.getGoodsCount(Goods.HORSES) < Unit.HORSES_TO_MOUNT_INDIAN * 2)) {
                    startPrice = 4+(i*4);
                }
                returnPrice += Unit.HORSES_TO_MOUNT_INDIAN * (startPrice-(i*4));
            }
        } else if (type.isFarmed()) {
            returnPrice = 0;
        } else {
            int currentGoods = goodsContainer.getGoodsCount(type);

            // Increase amount if raw materials are produced:
            GoodsType rawType = type.getRawMaterial();
            if (rawType != null) {
                int rawProduction = getMaximumProduction(rawType);
                if (currentGoods < 100) {
                    if (rawProduction < 5) {
                        currentGoods += rawProduction * 10;
                    } else if (rawProduction < 10) {
                        currentGoods += 50 + Math.max((rawProduction-5) * 5, 0);
                    } else if (rawProduction < 20) {
                        currentGoods += 75 + Math.max((rawProduction-10) * 2, 0);
                    } else {
                        currentGoods += 100;
                    }
                }
            }
            if (type == Goods.TRADEGOODS) {
                currentGoods += 20;
            }

            int valueGoods = Math.min(currentGoods + amount, 200) - currentGoods;
            if (valueGoods < 0) {
                valueGoods = 0;
            }

            returnPrice = (int) (((20.0+getPriceAddition())-(0.05*(currentGoods+valueGoods)))*(currentGoods+valueGoods)
                                 - ((20.0+getPriceAddition())-(0.05*(currentGoods)))*(currentGoods));
        }

        // Bonus for top 3 types of goods:
        if (type == wantedGoods[0]) {
            returnPrice = (returnPrice*12)/10;
        } else if (type == wantedGoods[1]) {
            returnPrice = (returnPrice*11)/10;
        } else if (type == wantedGoods[2]) {
            returnPrice = (returnPrice*105)/100;
        }

        return returnPrice;
    }

    /**
     * Gets the maximum possible production of the given type of goods.
     * @param goodsType The type of goods to check.
     * @return The maximum amount, of the given type of goods, that can
     *         be produced in one turn.
     */
    public int getMaximumProduction(GoodsType goodsType) {
        int amount = 0;
        Iterator<Position> it = getGame().getMap().getCircleIterator(getTile().getPosition(), true, getRadius());
        while (it.hasNext()) {
            Tile workTile = getGame().getMap().getTile(it.next());
            if (workTile.getOwningSettlement() == null || workTile.getOwningSettlement() == this) {
                amount += workTile.potential(goodsType);
            }
        }

        return amount;
    }


    /**
     * Updates the variables {@link #getHighlyWantedGoods wantedGoods[0]},
     * {@link #getWantedGoods1 wantedGoods[1]} and
     * {@link #getWantedGoods2 wantedGoods[2]}.
     *
     * <br><br>
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     */
    public void updateWantedGoods() {
        /* TODO: Try the different types goods in "random" order 
         * (based on the numbers of units on this tile etc): */
        List<GoodsType> goodsTypes = new ArrayList<GoodsType>(FreeCol.getSpecification().getGoodsTypeList());
        Collections.sort(goodsTypes, wantedGoodsComparator);
        int wantedIndex = 0;
        for (GoodsType goodsType : goodsTypes) {
            // Indians do not ask for horses or guns
            if (goodsType == Goods.HORSES || goodsType == Goods.MUSKETS) 
                continue;
            // no sense asking for bells or crosses
            if (goodsType.isStorable()==false)
                continue;
            if (wantedIndex < wantedGoods.length) {
                wantedGoods[wantedIndex] = goodsType;
                wantedIndex++;
            } else {
                break;
            }
        }
    }


    /**
     * Get the extra bonus if this is a <code>LONGHOUSE</code>,
     * <code>CITY</code> or a capital.
     */
    private int getPriceAddition() {
        return getBonusMultiplier() - 1;
    }


    /**
     * Get general bonus multiplier. This is >1 if this is a <code>LONGHOUSE</code>,
     * <code>CITY</code> or a capital.
     * 
     * @return The bonus multiplier.
     */
    public int getBonusMultiplier() {
        int multiplier = 0;
        switch (getTypeOfSettlement()) {
        case INDIAN_CAMP:
            multiplier = 1;
            break;
        case INDIAN_VILLAGE:
            multiplier = 2;
            break;
        case AZTEC_CITY:
        case INCA_CITY:
            multiplier = 3;
            break;
        }
        if (isCapital()) {
            multiplier++;
        }
        return multiplier;
    }


    @Override
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return units.contains((Unit) locatable);
        } else {
            return false;
        }
    }


    @Override
    public boolean canAdd(Locatable locatable) {
        return true;
    }

    public int getProductionOf(GoodsType type) {
        int potential = 0;
        Iterator<Position> it = getGame().getMap().getCircleIterator(getTile().getPosition(), true, getRadius());
        while (it.hasNext()) {
            Tile workTile = getGame().getMap().getTile(it.next());
            if ((workTile.getOwningSettlement() == null ||
                 workTile.getOwningSettlement() == this) && !workTile.isOccupied()) {
                potential += workTile.potential(type);
            }
        }
        
        if (type == Goods.FOOD) {
            potential = Math.min(potential, ownedUnits.size()*3);
        }
        
        return potential;
    }

    public int getProductionOf(int goodsIndex) {
        return getProductionOf(FreeCol.getSpecification().getGoodsType(goodsIndex));
    }


    @Override
    public void newTurn() {
        if (isUninitialized()) {
            logger.warning("Uninitialized when calling newTurn");
            return;
        }

        List<GoodsType> goodsList = FreeCol.getSpecification().getGoodsTypeList();
        int workers = ownedUnits.size();
        for (GoodsType g : goodsList) {
            /* Determine the maximum possible production for each type of goods: */
            goodsContainer.addGoods(g, getProductionOf(g));
        }

        /* Use tools (if available) to produce manufactured goods: */
        if (goodsContainer.getGoodsCount(Goods.TOOLS) > 0) {
            GoodsType typeWithSmallestAmount = null;
            for (GoodsType g : goodsList) {
                if (g == Goods.FOOD || g == Goods.LUMBER || g == Goods.ORE || g == Goods.TOOLS)
                    continue;
                if (g.isRawMaterial() && goodsContainer.getGoodsCount(g) > KEEP_RAW_MATERIAL) {
                    if (typeWithSmallestAmount == null ||
                        goodsContainer.getGoodsCount(g.getProducedMaterial()) < goodsContainer.getGoodsCount(typeWithSmallestAmount)) {
                        typeWithSmallestAmount = g.getProducedMaterial();
                    }
                }
            }
            /*
              if (potential[Goods.SUGAR]-KEEP_RAW_MATERIAL > 0) {
              typeWithSmallestAmount = Goods.RUM;
              }
              if (potential[Goods.TOBACCO]-KEEP_RAW_MATERIAL > 0 && goodsContainer.getGoodsCount(Goods.CIGARS) < goodsContainer.getGoodsCount(typeWithSmallestAmount)) {
              typeWithSmallestAmount = Goods.CIGARS;
              }
              if (potential[Goods.COTTON]-KEEP_RAW_MATERIAL > 0 && goodsContainer.getGoodsCount(Goods.CLOTH) < goodsContainer.getGoodsCount(typeWithSmallestAmount)) {
              typeWithSmallestAmount = Goods.CLOTH;
              }
              if (potential[Goods.FURS]-KEEP_RAW_MATERIAL > 0 && goodsContainer.getGoodsCount(Goods.COATS) < goodsContainer.getGoodsCount(typeWithSmallestAmount)) {
              typeWithSmallestAmount = Goods.COATS;
              }
            */
            if (typeWithSmallestAmount != null) {
                int production = Math.min(goodsContainer.getGoodsCount(typeWithSmallestAmount.getRawMaterial()),
                                          Math.min(10, goodsContainer.getGoodsCount(Goods.TOOLS)));
                goodsContainer.removeGoods(Goods.TOOLS, production);
                goodsContainer.removeGoods(typeWithSmallestAmount.getRawMaterial(), production);
                goodsContainer.addGoods(typeWithSmallestAmount, production * 5);
            }
        }

        /* Consume goods: */
        for (int i=0; i<workers; i++) {
            consumeGoods(Goods.FOOD, 2);
            consumeGoods(Goods.TOBACCO, 1);
            consumeGoods(Goods.COTTON, 1);
            consumeGoods(Goods.FURS, 1);
            consumeGoods(Goods.ORE, 1);
            consumeGoods(Goods.SILVER, 1);
            consumeGoods(Goods.RUM, 2);
            consumeGoods(Goods.CIGARS, 1);
            consumeGoods(Goods.COATS, 1);
            consumeGoods(Goods.CLOTH, 1);
            consumeGoods(Goods.TRADEGOODS, 2);
        }
        goodsContainer.removeAbove(500);

        checkForNewIndian();

        /* Increase alarm: */
        if (getUnitCount() > 0) {
            java.util.Map<Player, Integer> extraAlarm = new HashMap<Player, Integer>();
            for (Player enemy : getGame().getEuropeanPlayers()) {
                extraAlarm.put(enemy, new Integer(0));
            }
            CombatModel combatModel = getGame().getCombatModel();
            
            int alarmRadius = getRadius() + ALARM_RADIUS; // the radius in which Europeans cause alarm
            Iterator<Position> ci = getGame().getMap().getCircleIterator(getTile().getPosition(), true, alarmRadius);
            while (ci.hasNext()) {
                Tile t = getGame().getMap().getTile(ci.next());
                
                // Nearby military units:
                if (t.getSettlement() == null &&
                    t.getFirstUnit() != null) {
                    Player owner =  t.getFirstUnit().getOwner();
                    if (owner.isEuropean()) {
                        int alarm = extraAlarm.get(owner);
                        for (Unit unit : t.getUnitList()) {
                            if (unit.isOffensiveUnit() && !unit.isNaval()) {
                                alarm += combatModel.getOffencePower(unit, getTile().getDefendingUnit(unit));
                            }
                        }
                        extraAlarm.put(owner, alarm);
                    }
                }
                
                // Land being used by another settlement:
                if (t.getOwningSettlement() != null) {
                    Player owner = t.getOwningSettlement().getOwner();
                    if (owner.isEuropean()) {
                        extraAlarm.put(owner, extraAlarm.get(owner).intValue() + 2);
                    }
                }

                // Settlement:
                Settlement settlement = t.getSettlement();
                if (settlement != null) {
                    Player owner = settlement.getOwner();
                    if (owner.isEuropean()) {
                        extraAlarm.put(owner, extraAlarm.get(owner).intValue() + settlement.getUnitCount());
                    }
                }
            }

            // Missionary helps reducing alarm a bit, here and to the tribe as a whole.
            // No reduction effect on other settlements (1/4 of this) unless this is capital. 
            if (missionary != null) {
                Player owner = missionary.getOwner();
                extraAlarm.put(owner, extraAlarm.get(owner).intValue() + MISSIONARY_TENSION);
            }

            for (Entry<Player, Integer> entry : extraAlarm.entrySet()) {
                Integer alarm = entry.getValue();
                if (alarm != null && alarm.intValue() > 0) {
                    Player player = entry.getKey();
                    int modifiedAlarm = (int) player.getFeatureContainer()
                        .applyModifier(alarm.intValue(), "model.modifier.nativeAlarmModifier",
                                       null, getGame().getTurn());
                    modifyAlarm(player, modifiedAlarm);
                }
            }

            /* Decrease alarm slightly - independent from nation level */
            for (Tension tension : alarm.values()) {
                if (tension.getValue() > 0) {
                    int newAlarm = 4 + tension.getValue()/100;
                    tension.modify(-newAlarm);
                }
            }
        }

        /* Increase convert progress and generate convert if needed. */
        if (missionary != null && getGame().getViewOwner() == null) {
            int increment = 8;

            // Update increment if missionary is an expert.
            if (missionary.hasAbility("model.ability.expertMissionary")) {
                increment = 13;
            }

            // Increase increment if alarm level is high.
            increment += 2 * alarm.get(missionary.getOwner()).getValue() / 100;
            convertProgress += increment;

            int extra = Math.max(0, 8-getUnitCount()*getUnitCount());
            extra *= extra;
            extra *= extra;
            
            if (convertProgress >= 100 + extra && getUnitCount() > 2) {
                Tile targetTile = null;
                Iterator<Position> ffi = getGame().getMap().getFloodFillIterator(getTile().getPosition());
                while (ffi.hasNext()) {
                    Tile t = getGame().getMap().getTile(ffi.next());
                    if (getTile().getDistanceTo(t) > MAX_CONVERT_DISTANCE) {
                        break;
                    }
                    if (t.getSettlement() != null && t.getSettlement().getOwner() == missionary.getOwner()) {
                        targetTile = t;
                        break;
                    }
                }

                if (targetTile != null) {
                    convertProgress = 0;
                    
                    List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                    if (converts.size() > 0) {
                        getUnitIterator().next().dispose();

                        ModelController modelController = getGame().getModelController();
                        int random = modelController.getRandom(getId() + "getNewConvertType", converts.size());
                        Unit u = modelController.createUnit(getId() + "newTurn100missionary", targetTile,
                                                            missionary.getOwner(), converts.get(random));
                        addModelMessage(u, ModelMessage.MessageType.UNIT_ADDED, u,
                                "model.colony.newConvert",
                                "%nation%", getOwner().getNationAsString(),
                                "%colony%", targetTile.getColony().getName());
                        logger.info("New convert created for " + missionary.getOwner().getName() + " with ID=" + u.getId());
                    }
                }
            }
        }
        
        updateWantedGoods();
    }

    // Create a new colonist if there is enough food:
    private void checkForNewIndian() {
        // Alcohol also contributes to create children. 
        if (goodsContainer.getGoodsCount(Goods.FOOD) + 4*goodsContainer.getGoodsCount(Goods.RUM) > 200+KEEP_RAW_MATERIAL ) {
            if (ownedUnits.size() <= 6 + getTypeOfSettlement().ordinal()) {
                // up to a limit. Anyway cities produce more children than camps
                List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.bornInIndianSettlement");
                if (unitTypes.size() > 0) {
                    int random = getGame().getModelController().getRandom(getId() + "bornInIndianSettlement", unitTypes.size());
                    Unit u = getGame().getModelController().createUnit(getId() + "newTurn200food",
                                                                       getTile(), getOwner(), unitTypes.get(random));
                    consumeGoods(Goods.FOOD, 200);  // All food will be consumed, even if RUM helped
                    consumeGoods(Goods.RUM, 200/4);    // Also, some available RUM is consumed
                    // I know that consumeGoods will produce gold, which is explained because children are always a gift

                    addOwnedUnit(u);    // New indians quickly go out of their city and start annoying.
                    logger.info("New indian native created in " + getTile() + " with ID=" + u.getId());
                }
            }
        }
    }

    private void consumeGoods(GoodsType type, int amount) {
        if (goodsContainer.getGoodsCount(type) > 0) {
            amount = Math.min(amount, goodsContainer.getGoodsCount(type));
            getOwner().modifyGold(amount);
            goodsContainer.removeGoods(type, amount);
        }
    }

    /**
     * Disposes this settlement and removes its claims to adjacent
     * tiles.
     */
    @Override
    public void dispose() {
        while (ownedUnits.size() > 0) {
            ownedUnits.remove(0).setIndianSettlement(null);
        }
        for (Unit unit : units) {
            unit.dispose();
        }

        Tile settlementTile = getTile();     
        
        Map map = getGame().getMap();
        Position position = settlementTile.getPosition();
        Iterator<Position> circleIterator = map.getCircleIterator(position, true, getRadius());
        
        super.dispose();
    }

    /**
     * Creates the {@link GoodsContainer}.
     * <br><br>
     * DO NOT USE OTHER THAN IN {@link net.sf.freecol.server.FreeColServer#loadGame}:
     * Only for compatibility when loading savegames with pre-0.0.3 protocols.
     */
    public void createGoodsContainer() {
        goodsContainer = new GoodsContainer(getGame(), this);
    }

    private void unitsToXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        if (!units.isEmpty()) {
            out.writeStartElement(UNITS_TAG_NAME);
            for (Unit unit : units) {
                unit.toXML(out, player, showAll, toSavedGame);
            }
            out.writeEndElement();
        }
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
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", tile.getId());
        out.writeAttribute("owner", owner.getId());
        out.writeAttribute("lastTribute", Integer.toString(lastTribute));
        out.writeAttribute("isCapital", Boolean.toString(isCapital));

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            String ownedUnitsString = "";
            for (int i=0; i<ownedUnits.size(); i++) {
                ownedUnitsString += ownedUnits.get(i).getId();
                if (i != ownedUnits.size() - 1) {
                    ownedUnitsString += ", ";
                }
            }
            if (!ownedUnitsString.equals("")) {
                out.writeAttribute("ownedUnits", ownedUnitsString);
            }

            out.writeAttribute("hasBeenVisited", Boolean.toString(isVisited));
            out.writeAttribute("convertProgress", Integer.toString(convertProgress));
            writeAttribute(out, "learnableSkill", learnableSkill);

            for (int i = 0; i < wantedGoods.length; i++) {
                String tag = "wantedGoods" + Integer.toString(i);
                out.writeAttribute(tag, wantedGoods[i].getId());
            }

        }
        
        for (Entry<Player, Tension> entry : alarm.entrySet()) {
            out.writeStartElement("alarm");
            out.writeAttribute("player", entry.getKey().getId());
            out.writeAttribute("value", String.valueOf(entry.getValue().getValue()));
            out.writeEndElement();
        }

        if (missionary != null) {
            out.writeStartElement("missionary");
            missionary.toXML(out, player, showAll, toSavedGame);
            out.writeEndElement();
        }

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            unitsToXML(out, player, showAll, toSavedGame);
            goodsContainer.toXML(out, player, showAll, toSavedGame);
        } else {
            GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
            emptyGoodsContainer.setFakeID(goodsContainer.getId());
            emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }
        owner = (Player)getGame().getFreeColGameObject(in.getAttributeValue(null, "owner"));
        if (owner == null) {
            owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
        }
        isCapital = (new Boolean(in.getAttributeValue(null, "isCapital"))).booleanValue();

        owner.addSettlement(this);
        featureContainer.addModifier(Settlement.DEFENCE_MODIFIER);
        
        ownedUnits.clear();
        
        final String ownedUnitsStr = in.getAttributeValue(null, "ownedUnits");
        if (ownedUnitsStr != null) {
            StringTokenizer st = new StringTokenizer(ownedUnitsStr, ", ", false);
            while (st.hasMoreTokens()) {
                final String token = st.nextToken();
                Unit u = (Unit) getGame().getFreeColGameObject(token);
                if (u == null) {
                    u = new Unit(getGame(), token);
                    owner.setUnit(u);
                }
                ownedUnits.add(u);
            }
        }

        for (int i = 0; i < wantedGoods.length; i++) {
            String tag = "wantedGoods" + Integer.toString(i);
            String wantedGoodsId = getAttribute(in, tag, null);
            if (wantedGoodsId != null) {
                wantedGoods[i] = FreeCol.getSpecification().getGoodsType(wantedGoodsId);
            }
        }

        isVisited = getAttribute(in, "hasBeenVisited", false);
        convertProgress = getAttribute(in, "convertProgress", 0);
        lastTribute = getAttribute(in, "lastTribute", 0);
        String learnableSkillStr = getAttribute(in, "learnableSkill", null);
        if (learnableSkillStr != null) {
            learnableSkill = FreeCol.getSpecification().getUnitType(learnableSkillStr);
        }

        alarm = new HashMap<Player, Tension>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("alarm")) {
                Player player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
                alarm.put(player, new Tension(getAttribute(in, "value", 0)));
                in.nextTag(); // close element
            } else if (in.getLocalName().equals("wantedGoods")) {
                String[] wantedGoodsID = readFromArrayElement("wantedGoods", in, new String[0]);
                for (int i = 0; i < wantedGoodsID.length; i++) {
                    if (i == 3)
                        break;
                    wantedGoods[i] = FreeCol.getSpecification().getGoodsType(wantedGoodsID[i]);
                }
            } else if (in.getLocalName().equals("missionary")) {
                in.nextTag();
                missionary = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (missionary == null) {
                    missionary = new Unit(getGame(), in);
                } else {
                    missionary.readFromXML(in);
                }
                in.nextTag();                
            } else if (in.getLocalName().equals(UNITS_TAG_NAME)) {
                units = new ArrayList<Unit>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                        Unit unit = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                        if (unit != null) {
                            unit.readFromXML(in);
                            units.add(unit);
                        } else {
                            unit = new Unit(getGame(), in);
                            units.add(unit);
                        }
                    }
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                goodsContainer = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (goodsContainer != null) {
                    goodsContainer.readFromXML(in);
                } else {
                    goodsContainer = new GoodsContainer(getGame(), this, in);
                }                
            }
        }
    }


    /**
     * Returns the tag name of the root element representing this object.
     * @return "indianSettlement".
     */
    public static String getXMLElementTagName() {
        return "indianSettlement";
    }

    /**
     * An Indian settlement is no colony.
     * 
     * @return null
     */
    public Colony getColony() {
        return null;
    }
    
    /**
     * Returns an array with goods to sell
     */
    public Goods[] getSellGoods() {
        List<Goods> settlementGoods = getCompactGoods();
        for(Goods goods : settlementGoods) {
            if (goods.getAmount() > 100) {
                goods.setAmount(100);
            }
        }
        Collections.sort(settlementGoods, exportGoodsComparator);
        Goods sellGoods[] = {null, null, null};
        
        int i = 0;
        for(Goods goods : settlementGoods) {
            // Only sell raw materials but never sell food and lumber
            if (goods.getType() != Goods.FOOD && goods.getType() != Goods.LUMBER &&
                goods.getType().isStorable()) {
                sellGoods[i] = goods;
                i++;
                if (i == sellGoods.length) break;
            }
        }
        
        return sellGoods;
    }

    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to pay for the given <code>Goods</code>.
     *
     * <br><br>
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param goods The <code>Goods</code> to price.
     * @return The price.
     */
    public int getPriceToSell(Goods goods) {
        return getPriceToSell(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the amount of gold this <code>IndianSettlment</code>
     * is willing to pay for the given <code>Goods</code>.
     *
     * <br><br>
     *
     * It is only meaningful to call this method from the
     * server, since the settlement's {@link GoodsContainer}
     * is hidden from the clients.
     *
     * @param type The type of <code>Goods</code> to price.
     * @param amount The amount of <code>Goods</code> to price.
     * @return The price.
     */
    public int getPriceToSell(GoodsType type, int amount) {
        if (amount > 100) {
            throw new IllegalArgumentException();
        }

        int price = 10 - getProductionOf(type);
        if (price < 1) price = 1;
        return amount * price;
    }
}
