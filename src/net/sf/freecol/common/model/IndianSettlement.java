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
    public static final int ALARM_TILE_IN_USE = 2;
    public static final int ALARM_NEW_MISSIONARY = -100;

    public static final String UNITS_TAG_NAME = "units";
    public static final String OWNED_UNITS_TAG_NAME = "ownedUnits";
    public static final String ALARM_TAG_NAME = "alarm";
    public static final String MISSIONARY_TAG_NAME = "missionary";
    public static final String WANTED_GOODS_TAG_NAME = "wantedGoods";

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
        return "indianSettlement.alarm." + alarm.get(player).getLevel().toString().toLowerCase();
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
            Tension currentAlarm = alarm.get(missionary.getOwner());
            if (currentAlarm == null) {
                alarm.put(missionary.getOwner(), new Tension(0));
            } else {
                currentAlarm.modify(ALARM_NEW_MISSIONARY);
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

    /**
     * Gets the response to an attempt to create a mission
     * @return response
     */
    public String getResponseToMissionaryAttempt(Tension.Level tension, String success) {
    	String response = null;
    	
    	// Attempt Successful
    	if(success.equals("true")){
            switch(tension){
            case HAPPY:
                response = "indianSettlement.mission.Happy";
                break;
            case CONTENT:
                response = "indianSettlement.mission.Content";
                break;
            case DISPLEASED:
                response = "indianSettlement.mission.Displeased";
                break;
            default:
                logger.warning("Unknown response for tension " + tension);
            }
    				
    	} else {
            switch(tension){
            case ANGRY:
                response = "indianSettlement.mission.Angry";
                break;	
            case HATEFUL:
                response = "indianSettlement.mission.Hateful";
                break;	
            default:
                logger.warning("Requesting reaction when no mission was established");
            }
    	}
    	return response;
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

    /**
     * Returns <code>true</code> if this is the Nation's capital.
     *
     * @return <code>true</code> if this is the Nation's capital.
     */
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
            if (!units.contains(locatable)) {
                if (units.equals(Collections.emptyList())) {
                    units = new ArrayList<Unit>();
                }
                units.add((Unit) locatable);
            }
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
            if (!units.remove((Unit) locatable)) {
                logger.warning("Failed to remove unit " + ((Unit)locatable).getId() + " from IndianSettlement");
            }
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
            int supply = getGoodsCount(Goods.MUSKETS);
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.MUSKETS_TO_ARM_INDIAN;
                if (ownedUnits.get(i).isArmed()) {
                    supply += Unit.MUSKETS_TO_ARM_INDIAN;
                }
            }

            int sets = ((getGoodsCount(Goods.MUSKETS) + amount) / Unit.MUSKETS_TO_ARM_INDIAN)
                - (getGoodsCount(Goods.MUSKETS) / Unit.MUSKETS_TO_ARM_INDIAN);
            int startPrice = (19+getPriceAddition()) - (supply / Unit.MUSKETS_TO_ARM_INDIAN);
            for (int i=0; i<sets; i++) {
                if ((startPrice-i) < 8 && (need > supply || getGoodsCount(Goods.MUSKETS) < Unit.MUSKETS_TO_ARM_INDIAN * 2)) {
                    startPrice = 8+i;
                }
                returnPrice += Unit.MUSKETS_TO_ARM_INDIAN * (startPrice-i);
            }
        } else if (type == Goods.HORSES) {
            int need = 0;
            int supply = getGoodsCount(Goods.HORSES);
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.HORSES_TO_MOUNT_INDIAN;
                if (ownedUnits.get(i).isMounted()) {
                    supply += Unit.HORSES_TO_MOUNT_INDIAN;
                }
            }

            int sets = (getGoodsCount(Goods.HORSES) + amount) / Unit.HORSES_TO_MOUNT_INDIAN
                - (getGoodsCount(Goods.HORSES) / Unit.HORSES_TO_MOUNT_INDIAN);
            int startPrice = (24+getPriceAddition()) - (supply/Unit.HORSES_TO_MOUNT_INDIAN);

            for (int i=0; i<sets; i++) {
                if ((startPrice-(i*4)) < 4 &&
                    (need > supply ||
                     getGoodsCount(Goods.HORSES) < Unit.HORSES_TO_MOUNT_INDIAN * 2)) {
                    startPrice = 4+(i*4);
                }
                returnPrice += Unit.HORSES_TO_MOUNT_INDIAN * (startPrice-(i*4));
            }
        } else if (type.isFarmed()) {
            returnPrice = 0;
        } else {
            int currentGoods = getGoodsCount(type);

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
            if (type.isTradeGoods()) {
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
     * Updates the variable wantedGoods.
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
            if (goodsType.isMilitaryGoods()) 
                continue;
            // no sense asking for bells or crosses
            if (!goodsType.isStorable())
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


    /**
     * Provide some variation in unit count for different types of
     * <code>IndianSettlement</code>.
     * 
     * @return The number of units to generate for this settlement.
     */
    public int getGeneratedUnitCount() {
        int n;
        switch (getTypeOfSettlement()) {
        case INDIAN_CAMP:
            n = 0;
            break;
        case INDIAN_VILLAGE:
            n = 1;
            break;
        case AZTEC_CITY: case INCA_CITY:
            n = 2;
            break;
        default:
            throw new IllegalArgumentException("getTypeOfSettlement() out of range (" + getTypeOfSettlement() + ") in IndianSettlement.getGeneratedUnitCount()");
        }
        return 2 * n + 4;
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
        
        if (type.isFoodType()) {
            potential = Math.min(potential, ownedUnits.size()*3);
        }
        return potential;
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
        if (getGoodsCount(Goods.TOOLS) > 0) {
            GoodsType typeWithSmallestAmount = null;
            for (GoodsType g : goodsList) {
                if (g.isFoodType() || g.isBuildingMaterial() || g.isRawBuildingMaterial()) {
                    continue;
                }
                if (g.isRawMaterial() && getGoodsCount(g) > KEEP_RAW_MATERIAL) {
                    if (typeWithSmallestAmount == null ||
                        getGoodsCount(g.getProducedMaterial()) < getGoodsCount(typeWithSmallestAmount)) {
                        typeWithSmallestAmount = g.getProducedMaterial();
                    }
                }
            }
            if (typeWithSmallestAmount != null) {
                int production = Math.min(getGoodsCount(typeWithSmallestAmount.getRawMaterial()),
                                          Math.min(10, getGoodsCount(Goods.TOOLS)));
                goodsContainer.removeGoods(Goods.TOOLS, production);
                goodsContainer.removeGoods(typeWithSmallestAmount.getRawMaterial(), production);
                goodsContainer.addGoods(typeWithSmallestAmount, production * 5);
            }
        }

        /* Consume goods: TODO: make this more generic */
        consumeGoods(Goods.FOOD, getFoodConsumption());
        consumeGoods(Goods.RUM, 2 * workers);
        consumeGoods(Goods.TRADEGOODS, 2 * workers);
        /* TODO: do we need this at all? At the moment, most Indian Settlements
           consume more than they produce.
        for (GoodsType goodsType : FreeCol.getSpecification().getNewWorldGoodsTypeList()) {
            consumeGoods(goodsType, workers);
        }
        */
        consumeGoods(Goods.ORE, workers);
        consumeGoods(Goods.SILVER, workers);
        consumeGoods(Goods.CIGARS, workers);
        consumeGoods(Goods.COATS, workers);
        consumeGoods(Goods.CLOTH, workers);
        goodsContainer.removeAbove(500);

        checkForNewIndian();

        /* Increase alarm: */
        if (getUnitCount() > 0) {
            increaseAlarm();
        }
        
        updateWantedGoods();
    }

    public boolean checkForNewMissionnaryConvert() {
        
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
                convertProgress = 0;
                return true;
            }
        }
        return false;
    }

    // Create a new colonist if there is enough food:
    private void checkForNewIndian() {
        // Alcohol also contributes to create children. 
        if (getFoodCount() + 4*getGoodsCount(Goods.RUM) > 200+KEEP_RAW_MATERIAL ) {
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
                    u.setIndianSettlement(this);
                    logger.info("New indian native created in " + getTile() + " with ID=" + u.getId());
                }
            }
        }
    }

    private void increaseAlarm() {

        java.util.Map<Player, Integer> extraAlarm = new HashMap<Player, Integer>();
        for (Player enemy : getGame().getEuropeanPlayers()) {
            extraAlarm.put(enemy, new Integer(0));
        }
        int alarmRadius = getRadius() + ALARM_RADIUS; // the radius in which Europeans cause alarm
        Iterator<Position> ci = getGame().getMap().getCircleIterator(getTile().getPosition(), true, alarmRadius);
        while (ci.hasNext()) {
            Tile tile = getGame().getMap().getTile(ci.next());
            Colony colony = tile.getColony();
                
            if (colony == null) {
                // Nearby military units:
                if (tile.getFirstUnit() != null) {
                    Player enemy =  tile.getFirstUnit().getOwner();
                    if (enemy.isEuropean()) {
                        int alarm = extraAlarm.get(enemy);
                        for (Unit unit : tile.getUnitList()) {
                            if (unit.isOffensiveUnit() && !unit.isNaval()) {
                                alarm += unit.getType().getOffence();
                            }
                        }
                        extraAlarm.put(enemy, alarm);
                    }
                }
                
                // Land being used by another settlement:
                if (tile.getOwningSettlement() != null) {
                    Player enemy = tile.getOwningSettlement().getOwner();
                    if (enemy!=null && enemy.isEuropean()) {
                        extraAlarm.put(enemy, extraAlarm.get(enemy).intValue() + ALARM_TILE_IN_USE);
                    }
                }
            } else {
                // Settlement:
                Player enemy = colony.getOwner();
                extraAlarm.put(enemy, extraAlarm.get(enemy).intValue() + colony.getUnitCount());
            }
        }

        // Missionary helps reducing alarm a bit, here and to the tribe as a whole.
        // No reduction effect on other settlements (1/4 of this) unless this is capital. 
        if (missionary != null) {
            Player enemy = missionary.getOwner();
            int missionaryAlarm = MISSIONARY_TENSION;
            if (missionary.hasAbility("model.ability.expertMissionary")) {
                missionaryAlarm *= 2;
            }
            extraAlarm.put(enemy, extraAlarm.get(enemy).intValue() + missionaryAlarm);
        }

        for (Entry<Player, Integer> entry : extraAlarm.entrySet()) {
            Integer newAlarm = entry.getValue();
            if (alarm != null) {
                Player player = entry.getKey();
                int modifiedAlarm = (int) player.getFeatureContainer()
                    .applyModifier(newAlarm.intValue(), "model.modifier.nativeAlarmModifier",
                                   null, getGame().getTurn());
                Tension oldAlarm = alarm.get(player);
                if (oldAlarm != null) {
                    modifiedAlarm -= 4 + oldAlarm.getValue()/100;
                }
                modifyAlarm(player, modifiedAlarm);
            }
        }
    }


    private void consumeGoods(GoodsType type, int amount) {
        if (getGoodsCount(type) > 0) {
            amount = Math.min(amount, getGoodsCount(type));
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

        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("tile", tile.getId());
        out.writeAttribute("owner", owner.getId());
        out.writeAttribute("lastTribute", Integer.toString(lastTribute));
        out.writeAttribute("isCapital", Boolean.toString(isCapital));

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            out.writeAttribute("hasBeenVisited", Boolean.toString(isVisited));
            out.writeAttribute("convertProgress", Integer.toString(convertProgress));
            writeAttribute(out, "learnableSkill", learnableSkill);

            for (int i = 0; i < wantedGoods.length; i++) {
                String tag = "wantedGoods" + Integer.toString(i);
                out.writeAttribute(tag, wantedGoods[i].getId());
            }
        }

        // attributes end here
        
        for (Entry<Player, Tension> entry : alarm.entrySet()) {
            out.writeStartElement(ALARM_TAG_NAME);
            out.writeAttribute("player", entry.getKey().getId());
            out.writeAttribute("value", String.valueOf(entry.getValue().getValue()));
            out.writeEndElement();
        }

        if (missionary != null) {
            out.writeStartElement(MISSIONARY_TAG_NAME);
            missionary.toXML(out, player, showAll, toSavedGame);
            out.writeEndElement();
        }

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            if (!units.isEmpty()) {
                out.writeStartElement(UNITS_TAG_NAME);
                for (Unit unit : units) {
                    unit.toXML(out, player, showAll, toSavedGame);
                }
                out.writeEndElement();
            }
            goodsContainer.toXML(out, player, showAll, toSavedGame);
            for (Unit unit : ownedUnits) {
                out.writeStartElement(OWNED_UNITS_TAG_NAME);
                out.writeAttribute(ID_ATTRIBUTE, unit.getId());
                out.writeEndElement();
            }
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
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }
        owner = (Player)getGame().getFreeColGameObject(in.getAttributeValue(null, "owner"));
        if (owner == null) {
            owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
        }
        isCapital = getAttribute(in, "isCapital", false);

        owner.addSettlement(this);
        featureContainer.addModifier(Settlement.DEFENCE_MODIFIER);
        
        ownedUnits.clear();
        
        // TODO: this is support for 0.7 savegames, remove sometime
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
        // end TODO

        for (int i = 0; i < wantedGoods.length; i++) {
            String tag = WANTED_GOODS_TAG_NAME + Integer.toString(i);
            String wantedGoodsId = getAttribute(in, tag, null);
            if (wantedGoodsId != null) {
                wantedGoods[i] = FreeCol.getSpecification().getGoodsType(wantedGoodsId);
            }
        }

        isVisited = getAttribute(in, "hasBeenVisited", false);
        convertProgress = getAttribute(in, "convertProgress", 0);
        lastTribute = getAttribute(in, "lastTribute", 0);
        learnableSkill = FreeCol.getSpecification().getType(in, "learnableSkill", UnitType.class, null);

        alarm = new HashMap<Player, Tension>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (ALARM_TAG_NAME.equals(in.getLocalName())) {
                Player player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
                alarm.put(player, new Tension(getAttribute(in, "value", 0)));
                in.nextTag(); // close element
            } else if (WANTED_GOODS_TAG_NAME.equals(in.getLocalName())) {
                String[] wantedGoodsID = readFromArrayElement(WANTED_GOODS_TAG_NAME, in, new String[0]);
                for (int i = 0; i < wantedGoodsID.length; i++) {
                    if (i == 3)
                        break;
                    wantedGoods[i] = FreeCol.getSpecification().getGoodsType(wantedGoodsID[i]);
                }
            } else if (MISSIONARY_TAG_NAME.equals(in.getLocalName())) {
                in.nextTag();
                missionary = updateFreeColGameObject(in, Unit.class);
                in.nextTag();                
            } else if (UNITS_TAG_NAME.equals(in.getLocalName())) {
                units = new ArrayList<Unit>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                        Unit unit = updateFreeColGameObject(in, Unit.class);
                        if (unit.getLocation() != this) {
                            logger.warning("fixing unit location");
                            unit.setLocation(this);
                        }
                        units.add(unit);
                    }
                }
            } else if (OWNED_UNITS_TAG_NAME.equals(in.getLocalName())) {
                Unit unit = getFreeColGameObject(in, ID_ATTRIBUTE, Unit.class);
                ownedUnits.add(unit);
                owner.setUnit(unit);
                in.nextTag();
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                goodsContainer = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
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
    public List<Goods> getSellGoods() {
        List<Goods> settlementGoods = getCompactGoods();
        for(Goods goods : settlementGoods) {
            if (goods.getAmount() > 100) {
                goods.setAmount(100);
            }
        }
        Collections.sort(settlementGoods, exportGoodsComparator);

        List<Goods> result = new ArrayList<Goods>();
        int count = 0;
        for (Goods goods : settlementGoods) {
            if (goods.getType().isNewWorldGoodsType() && goods.getAmount() > 0) {
                result.add(goods);
                count++;
                if (count > 2) {
                    return result;
                }
            }
        }
        
        return result;
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
