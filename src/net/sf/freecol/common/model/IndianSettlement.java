
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import net.sf.freecol.FreeCol;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Position;

import org.w3c.dom.Element;


/**
 * Represents an Indian settlement.
 */
public class IndianSettlement extends Settlement {
    private static final Logger logger = Logger.getLogger(IndianSettlement.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int INCA = 0;
    public static final int AZTEC = 1;
    public static final int ARAWAK = 2;
    public static final int CHEROKEE = 3;
    public static final int IROQUOIS = 4;
    public static final int SIOUX = 5;
    public static final int APACHE = 6;
    public static final int TUPI = 7;
    public static final int LAST_TRIBE = 7;


    public static final int CAMP = 0;
    public static final int VILLAGE = 1;
    public static final int CITY = 2;
    public static final int LAST_KIND = 2;

    public static final int MISSIONARY_TENSION = -3;
    public static final int MAX_CONVERT_DISTANCE = 10;
    public static final int TURNS_PER_TRIBUTE = 5;

    /** The amount of goods a brave can produce a single turn. */
    //private static final int WORK_AMOUNT = 5;

    /** The amount of raw material that should be available before producing manufactured goods. */
    public static final int KEEP_RAW_MATERIAL = 50;

    // These are the learnable skills for an Indian settlement.
    // They are fully compatible with the types from the Unit class!
    //
    // Note: UNKNOWN is used for both 'skill' and 'wanted goods' - Depreciated
    public static final int UNKNOWN = -2,
                            NONE = -1,
                            EXPERT_FARMER = Unit.EXPERT_FARMER,
                            EXPERT_FISHERMAN = Unit.EXPERT_FISHERMAN,
                            EXPERT_SILVER_MINER = Unit.EXPERT_SILVER_MINER,
                            MASTER_SUGAR_PLANTER = Unit.MASTER_SUGAR_PLANTER,
                            MASTER_COTTON_PLANTER = Unit.MASTER_COTTON_PLANTER,
                            MASTER_TOBACCO_PLANTER = Unit.MASTER_TOBACCO_PLANTER,
                            SEASONED_SCOUT = Unit.SEASONED_SCOUT,
                            EXPERT_ORE_MINER = Unit.EXPERT_ORE_MINER,
                            EXPERT_LUMBER_JACK = Unit.EXPERT_LUMBER_JACK,
                            EXPERT_FUR_TRAPPER = Unit.EXPERT_FUR_TRAPPER;


    /** The kind of settlement this is.
        TODO: this information should be moved to the IndianPlayer class (that doesn't
              exist yet). */
    private int kind;

    /** The tribe that owns this settlement. */
    private int tribe;

    /**
    * This is the skill that can be learned by Europeans at this settlement.
    * At the server side its value will be null when the skill has already been 
    * taught to a European.
    * At the client side the value null is also possible in case the player hasn't
    * checked out the settlement yet.
    */
    private UnitType learnableSkill = null;

    private GoodsType[] wantedGoods = new GoodsType[] {null, null, null};

    /**
    * At the client side isVisited is true in case the player has visited
    * the settlement.
    */
    private boolean isCapital,
                    isVisited; /* true if a European player has asked to speak with the chief. */

    private UnitContainer unitContainer;

    private ArrayList<Unit> ownedUnits = new ArrayList<Unit>();

    private Unit missionary;

    /** Used for monitoring the progress towards creating a convert. */
    private int  convertProgress;

    /** The number of the turn during which the last tribute was paid. */
    int lastTribute = 0;

    /**
    * Stores the alarm levels. <b>Only used by AI.</b>
    * 0-1000 with 1000 as the maximum alarm level.
    */
    private Tension[] alarm = new Tension[Player.NUMBER_OF_NATIONS];

    // sort goods descending by price
    private final Comparator<Goods> wantedGoodsComparator = new Comparator<Goods>() {
        public int compare(Goods goods1, Goods goods2) {
            return getPrice(goods2) - getPrice(goods1);
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
     * @param tribe Tribe of settlement
     * @param kind Kind of settlement
     * @param isCapital True if settlement is tribe's capital
     * @param learnableSkill The skill that can be learned by Europeans at this settlement.
     * @param isVisited Indicates if any European scout has asked to speak with the chief.
     * @param missionary The missionary in this settlement (or null).
     * @exception IllegalArgumentException if an invalid tribe or kind is given
     */
    public IndianSettlement(Game game, Player player, Tile tile, int tribe, int kind,
            boolean isCapital, UnitType learnableSkill, boolean isVisited, Unit missionary) {
        super(game, player, tile);

        if (tile == null) {
            throw new NullPointerException();
        }
        
        tile.setNationOwner(player.getNation());

        unitContainer = new UnitContainer(game, this);
        goodsContainer = new GoodsContainer(game, this);

        if (tribe < 0 || tribe > LAST_TRIBE) {
            throw new IllegalArgumentException("Invalid tribe provided");
        }

        this.tribe = tribe;

        if (kind < 0 || kind > LAST_KIND) {
            throw new IllegalArgumentException("Invalid settlement kind provided");
        }

        this.kind = kind;
        this.learnableSkill = learnableSkill;
        this.isCapital = isCapital;
        this.isVisited = isVisited;
        this.missionary = missionary;

        /*
        for (int goodsType=0; goodsType<Goods.NUMBER_OF_TYPES; goodsType++) {
            if (goodsType == Goods.LUMBER || goodsType == Goods.HORSES || goodsType == Goods.MUSKETS
                    || goodsType == Goods.TRADE_GOODS || goodsType == Goods.TOOLS) {
                continue;
            }
            goodsContainer.addGoods(goodsType, (int) (Math.random() * 300));
        }
        */        

        goodsContainer.addGoods(Goods.LUMBER, 300);
        convertProgress = 0;
        
        for (int k = 0; k < alarm.length; k++) {
            alarm[k] = new Tension(0);
        }

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

        // The client doesn't know a lot at first.
        this.learnableSkill = null;
        this.wantedGoods[0] = null;
        this.wantedGoods[1] = null;
        this.wantedGoods[2] = null;
        isVisited = false;
        missionary = null;
        convertProgress = 0;

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

        // The client doesn't know a lot at first.
        this.learnableSkill = null;
        this.wantedGoods[0] = null;
        this.wantedGoods[1] = null;
        this.wantedGoods[2] = null;
        isVisited = false;
        missionary = null;
        convertProgress = 0;

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
        
        // The client doesn't know a lot at first.
        this.learnableSkill = null;
        this.wantedGoods[0] = null;
        this.wantedGoods[1] = null;
        this.wantedGoods[2] = null;
        isVisited = false;
        missionary = null;
        convertProgress = 0;
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
            case Tension.HAPPY:
            case Tension.CONTENT:
                gold = Math.min(getOwner().getGold() / 10, 100);
                break;
            case Tension.DISPLEASED:
                gold = Math.min(getOwner().getGold() / 20, 100);
                break;
            case Tension.ANGRY:
            case Tension.HATEFUL:
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
        modifyAlarm(player.getNation(), addToAlarm);
    }

    public void modifyAlarm(int nation, int addToAlarm) {
        Tension tension = alarm[nation];
        if(tension != null) {
            tension.modify(addToAlarm);            
        }
        // propagate alarm upwards
        if(owner != null) {
                if (isCapital())
                        // capital has a greater impact
                    owner.modifyTension(nation, addToAlarm, this); 
                else
                owner.modifyTension(nation, addToAlarm/2, this);            
        }
    }

    /**
     * Propagates the tension felt towards a given nation 
     * from the tribe down to each settlement that has already met that nation.
     * 
     * @param nation The nation towards whom the alarm is felt.
     * @param addToAlarm The amount to add to the current alarm level.
     */
    public void propagatedAlarm(int nation, int addToAlarm) {
        Tension tension = alarm[nation];
        // only applies tension if settlement has met europeans
        if(tension != null && isVisited) {
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
        alarm[player.getNation()] = newAlarm;
    }



    /**
    * Gets the alarm level towards the given player.
    * @param nation The nation to get the alarm level for.
    * @return An object representing the alarm level.
    */
    public Tension getAlarm(int nation) {
        return alarm[nation];
    }

    /**
     * Gets the alarm level towards the given player.
     * @param player The <code>Player</code> to get the alarm level for.
     * @return An object representing the alarm level.
     */    
    public Tension getAlarm(Player player) {
        return getAlarm(player.getNation());
    }

    /**
     * Gets the ID of the alarm message associated with the alarm
     * level of this player.
     *
     * @param player The other player.
     * @return The ID of an alarm level message.
     */
    public String getAlarmLevelMessage(Player player) {
        return "indianSettlement.alarm." + alarm[player.getNation()].getCodeString();
    }

    /**
    * Returns true if a European player has visited this settlement to speak with the chief.
    * @return true if a European player has visited this settlement to speak with the chief.
    */
    public boolean hasBeenVisited() {
        return isVisited;
    }

    /**
    * Sets the visited status of this settlement to true, indicating that a European has had
    * a chat with the chief.
    */
    public void setVisited() {
        this.isVisited = true;
    }

    /**
    * Adds the given <code>Unit</code> to the list of units that belongs to this
    * <code>IndianSettlement</code>.
    * 
    * @param u The <code>Unit</code> to be added.
    */
    public void addOwnedUnit(Unit u) {
        if (u == null) {
            throw new NullPointerException();
        }

        if (!ownedUnits.contains(u)) {
            ownedUnits.add(u);
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
    * Removes the given <code>Unit</code> to the list of units 
    * that belongs to this <code>IndianSettlement</code>.
    * 
    * @param u The <code>Unit</code> to be removed from the
    *       list of the units this <code>IndianSettlement</code>
    *       owns.
    */
    public void removeOwnedUnit(Unit u) {
        if (u == null) {
            throw new NullPointerException();
        }

        int index = ownedUnits.indexOf(u);

        if (index >= 0) {
            ownedUnits.remove(index);
        }
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
            if (!missionary.isMissionary()) {
                throw new IllegalArgumentException("Specified unit is not a missionary.");
            }
            missionary.setLocation(null);
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

    public void setWantedGoods(int index, int goodsIndex) {
        if (0 <= index && index <= 2) {
            wantedGoods[index] = FreeCol.getSpecification().getGoodsType(goodsIndex);
        }
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
     * @return {@link #CAMP}, {@link #VILLAGE} or {@link #CITY}.
     */
    public int getKind() {
        return kind;
    }

    /**
     * Gets the kind of <code>Settlment</code> being used
     * by the given tribe.
     *
     * @param tribe The tribe.
     * @return {@link #CAMP}, {@link #VILLAGE} or {@link #CITY}.
     */
    public static int getKind(int tribe) {
        switch (tribe) {
        case INCA:
        case AZTEC:
            return CITY;
        case CHEROKEE:
        case ARAWAK:
        case IROQUOIS:
            return VILLAGE;
        case SIOUX:
        case APACHE:
        case TUPI:
            return CAMP;
        default:
            logger.warning("Unknown tribe: " + tribe);
            return CAMP;
        }
    }

    /**
    * Gets the tribe of the Indian settlement.
    * @return The tribe.
    */
    public int getTribe() {
        return tribe;
    }

    /**
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.  Cities dominate 2 tiles, other settlements 1 tile.
     *
     * @return Settlement radius
     */
    @Override
    public int getRadius() {
        return getRadius(kind);
    }
    
    /**
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.  Cities dominate 2 tiles, other settlements 1 tile.
     * 
     * @param kind The kind of settlement. One of: {@link #CAMP},
     *      {@link #VILLAGE} or {@link #CITY}.
     * @return Settlement radius
     */
    public static int getRadius(int kind) {
        if (kind == CITY) {
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
            unitContainer.addUnit((Unit) locatable);
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
            unitContainer.removeUnit((Unit) locatable);
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
        return unitContainer.getUnitCount();
    }

    public List<Unit> getUnitList() {
        return unitContainer.getUnitsClone();
    }
    
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }


    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }


    /**
    * Gets the <code>Unit</code> that is currently defending this <code>IndianSettlement</code>.
    * @param attacker The target that would be attacking this <code>IndianSettlement</code>.
    * @return The <code>Unit</code> that has been choosen to defend this <code>IndianSettlement</code>.
    */
    @Override
    public Unit getDefendingUnit(Unit attacker) {
        Iterator<Unit> unitIterator = getUnitIterator();

        Unit defender = null;
        if (unitIterator.hasNext()) {
            defender = unitIterator.next();
        } else {
            return null;
        }

        while (unitIterator.hasNext()) {
            Unit nextUnit = unitIterator.next();

            if (nextUnit.getDefensePower(attacker) > defender.getDefensePower(attacker)) {
                defender = nextUnit;
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
        } else if (type == Goods.FOOD || type == Goods.LUMBER || type == Goods.SUGAR ||
                type == Goods.TOBACCO || type == Goods.COTTON || type == Goods.FURS ||
                type == Goods.ORE || type == Goods.SILVER) {
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
            if (type == Goods.TRADE_GOODS) {
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
            if (workTile.getOwner() == null || workTile.getOwner() == this) {
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
        Goods[] goodsType = new Goods[Goods.NUMBER_OF_TYPES];
        for (int index = 0; index < Goods.NUMBER_OF_TYPES; index++) {
            goodsType[index] = new Goods(index);
            goodsType[index].setAmount(100);
        }
        Arrays.sort(goodsType, wantedGoodsComparator);
        int wantedIndex = 0;
        for (int index = 0; index < goodsType.length; index++) {
            GoodsType type = goodsType[index].getType();
            if (type != Goods.HORSES && type != Goods.MUSKETS) {
                if (wantedIndex < wantedGoods.length) {
                    wantedGoods[wantedIndex] = type;
                    wantedIndex++;
                } else {
                    break;
                }
            }
        }
    }


    /**
    * Get the extra bonus if this is a <code>VILLAGE</code>,
    * <code>CITY</code> or a capital.
    */
    private int getPriceAddition() {
        return getBonusMultiplier() - 1;
    }


    /**
    * Get general bonus multiplier. This is >1 if this is a <code>VILLAGE</code>,
    * <code>CITY</code> or a capital.
    * 
    * @return The bonus multiplier.
    */
    public int getBonusMultiplier() {
        int addition = getKind() + 1;
        if (isCapital()) {
            addition++;
        }
        return addition;
    }


    @Override
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
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
            if ((workTile.getOwner() == null || workTile.getOwner() == this) && !workTile.isOccupied()) {
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
        int[] potential = new int[Goods.NUMBER_OF_TYPES];
        int workers = ownedUnits.size();
        for (GoodsType g : goodsList) {
            int index = g.getIndex();
            /* Determine the maximum possible production for each type of goods: */
            potential[index] = getProductionOf(g);
            /* Produce the goods: */
            goodsContainer.addGoods(g, potential[index]);
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
            consumeGoods(Goods.TRADE_GOODS, 2);
        }
        goodsContainer.removeAbove(500);


        // TODO: Create a unit if food>=200, but not if a maximum number of units is reaced.
        // DONE Create Indian when enough food
        // Alcohol also contributes to create children. 
        if (goodsContainer.getGoodsCount(Goods.FOOD) + 4*goodsContainer.getGoodsCount(Goods.RUM) > 200+KEEP_RAW_MATERIAL ) {
            if (workers <= 6 + getKind()) { // up to a limit. Anyway cities produce more children than camps
                // TODO: search in specification units which can be born in a settlement and choose one
                Unit u = getGame().getModelController().createUnit(getID() + "newTurn200food", getTile(),
                                            getOwner(), FreeCol.getSpecification().unitType(Unit.BRAVE));
                consumeGoods(Goods.FOOD, 200);  // All food will be consumed, even if RUM helped
                consumeGoods(Goods.RUM, 200/4);    // Also, some available RUM is consumed
                // I know that consumeGoods will produce gold, which is explained because children are always a gift
                addOwnedUnit(u);    // New indians quickly go out of their city and start annoying.
                /* Should I do something like this? Seems to work without it.
                 addModelMessage(u, "model.colony.newColonist",
                                    new String[][] {{"%nation%", getOwner().getNationAsString()}},
                                    ModelMessage.UNIT_ADDED);
                 */
                logger.info("New indian native created in " + getTile() + " with ID=" + u.getID());
            }
        }
        // end Create Indian when enough food


        /* Increase alarm: */
        if (getUnitCount() > 0) {
            int[] extraAlarm = new int[Player.NUMBER_OF_NATIONS];
            
            int alarmRadius = getRadius() + 2; // the radius in which Europeans cause alarm
            Iterator<Position> ci = getGame().getMap().getCircleIterator(getTile().getPosition(), true, alarmRadius);
            while (ci.hasNext()) {
                Tile t = getGame().getMap().getTile(ci.next());
                
                // Nearby military units:
                if (t.getFirstUnit() != null 
                        && t.getFirstUnit().getOwner().isEuropean()
                        && t.getSettlement() == null) {                    
                    Iterator<Unit> ui = t.getUnitIterator();
                    while (ui.hasNext()) {
                        Unit u = ui.next();
                        if (u.isOffensiveUnit() && !u.isNaval()) {                          
                            extraAlarm[u.getOwner().getNation()] += u.getOffensePower(getTile().getDefendingUnit(u));
                        }
                    }
                }
                
                // Land being used by another settlement:
                if (t.getOwner() != null && t.getOwner().getOwner().isEuropean()) {                    
                    extraAlarm[t.getOwner().getOwner().getNation()] += 2;
                }

                // Settlement:
                if (t.getSettlement() != null && t.getSettlement().getOwner().isEuropean()) {
                    extraAlarm[t.getSettlement().getOwner().getNation()] += t.getSettlement().getUnitCount();
                }
            }

            // Missionary helps reducing alarm a bit, here and to the tribe as a whole.
            // No reduction effect on other settlements (1/4 of this) unless this is capital. 
            if (missionary != null) {
                extraAlarm[missionary.getOwner().getNation()] += MISSIONARY_TENSION;
            }

            for (int i=0; i<extraAlarm.length; i++) {
                Player p = getGame().getPlayer(i);
                if (p.isEuropean() && extraAlarm[i] != 0) {
                    if (extraAlarm[i] > 0) {
                        int d = (p.hasFather(FoundingFather.POCAHONTAS)) ? 2 : 1;
                        if (p.getNation() == Player.FRENCH) {
                            d *= 2;
                        }
                        modifyAlarm(p, extraAlarm[i] / d);
                    } else {
                        modifyAlarm(p, extraAlarm[i]);
                    }
                }
            }

            /* Decrease alarm slightly - independent from nation level */
            for (int i=0; i<alarm.length; i++) {
                if (alarm[i] != null && alarm[i].getValue() > 0) {
                    int newAlarm = 4 + alarm[i].getValue()/100;
                    alarm[i].modify(-newAlarm);
                }
            }
        }

        /* Increase convert progress and generate convert if needed. */
        if (missionary != null && getGame().getViewOwner() == null) {
            int increment = 8;

            // Update increment if missionary is an expert.
            if (missionary.hasAbility("model.ability.expertMissionary") ||
                    missionary.getOwner().hasFather(FoundingFather.FATHER_JEAN_DE_BREBEUF)) {
                increment = 13;
            }

            // Increase increment if alarm level is high.
            increment += 2 * alarm[missionary.getOwner().getNation()].getValue() / 100;
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
                        int random = modelController.getRandom(getID() + "getNewConvertType", converts.size());
                        Unit u = modelController.createUnit(getID() + "newTurn100missionary", targetTile,
                                missionary.getOwner(), converts.get(random));
                        addModelMessage(u, "model.colony.newConvert",
                                        new String[][] {{"%nation%", getOwner().getNationAsString()}},
                                        ModelMessage.UNIT_ADDED);
                        logger.info("New convert created for " + missionary.getOwner().getName() + " with ID=" + u.getID());
                    }
                }
            }
        }
        
        updateWantedGoods();
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
        unitContainer.dispose();

        int nation = owner.getNation();
        
        Tile settlementTile = getTile();     
        
        Map map = getGame().getMap();
        Position position = settlementTile.getPosition();
        Iterator<Position> circleIterator = map.getCircleIterator(position, true, getRadius());
        
        settlementTile.setClaim(Tile.CLAIM_NONE);
        while (circleIterator.hasNext()) {
            Tile tile = map.getTile(circleIterator.next());
            if (tile.getNationOwner() == nation) {
                tile.setClaim(Tile.CLAIM_NONE);
            }
        }

        super.dispose();
    }

    public UnitContainer getUnitContainer() {
        return unitContainer;
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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        out.writeAttribute("ID", getID());
        out.writeAttribute("tile", tile.getID());
        out.writeAttribute("owner", owner.getID());
        out.writeAttribute("tribe", Integer.toString(tribe));
        out.writeAttribute("kind", Integer.toString(kind));
        out.writeAttribute("lastTribute", Integer.toString(lastTribute));
        out.writeAttribute("isCapital", Boolean.toString(isCapital));

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            String ownedUnitsString = "";
            for (int i=0; i<ownedUnits.size(); i++) {
                ownedUnitsString += ownedUnits.get(i).getID();
                if (i != ownedUnits.size() - 1) {
                    ownedUnitsString += ", ";
                }
            }
            if (!ownedUnitsString.equals("")) {
                out.writeAttribute("ownedUnits", ownedUnitsString);
            }

            out.writeAttribute("hasBeenVisited", Boolean.toString(isVisited));
            out.writeAttribute("convertProgress", Integer.toString(convertProgress));
            if (learnableSkill != null) {
                out.writeAttribute("learnableSkill", learnableSkill.getId());
            }
            for (int i = 0; i < wantedGoods.length; i++) {
                String tag = "wantedGoods" + Integer.toString(i);
                out.writeAttribute(tag, wantedGoods[i].getName());
            }

        }
        
        int[] tensionArray = new int[alarm.length];
        for (int i = 0; i < alarm.length; i++) {
            tensionArray[i] = alarm[i].getValue();
        }
        toArrayElement("alarm", tensionArray, out);

        if (missionary != null) {
            out.writeStartElement("missionary");
            missionary.toXML(out, player, showAll, toSavedGame);
            out.writeEndElement();
        }

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            unitContainer.toXML(out, player, showAll, toSavedGame);
            goodsContainer.toXML(out, player, showAll, toSavedGame);
        } else {
            UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
            emptyUnitContainer.setFakeID(unitContainer.getID());
            emptyUnitContainer.toXML(out, player, showAll, toSavedGame);

            GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
            emptyGoodsContainer.setFakeID(goodsContainer.getID());
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
        setID(in.getAttributeValue(null, "ID"));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }
        owner = (Player)getGame().getFreeColGameObject(in.getAttributeValue(null, "owner"));
        if (owner == null) {
            owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
        }
        tribe = Integer.parseInt(in.getAttributeValue(null, "tribe"));
        kind = Integer.parseInt(in.getAttributeValue(null, "kind"));
        isCapital = (new Boolean(in.getAttributeValue(null, "isCapital"))).booleanValue();

        owner.addSettlement(this);
        
        ownedUnits.clear();
        
        final String ownedUnitsStr = in.getAttributeValue(null, "ownedUnits");
        if (ownedUnitsStr != null) {
            StringTokenizer st = new StringTokenizer(ownedUnitsStr, ", ", false);
            while (st.hasMoreTokens()) {
                final String token = st.nextToken();
                Unit u = (Unit) getGame().getFreeColGameObject(token);
                if (u == null) {
                    u = new Unit(getGame(), token);
                }
                ownedUnits.add(u);
            }
        }

        for (int i = 0; i < wantedGoods.length; i++) {
            String tag = "wantedGoods" + Integer.toString(i);
            wantedGoods[i] = FreeCol.getSpecification().getGoodsType(getAttribute(in, tag, null));
        }

        isVisited = getAttribute(in, "hasBeenVisisted", false);
        convertProgress = getAttribute(in, "convertProgress", UNKNOWN);
        lastTribute = getAttribute(in, "lastTribute", 0);
        String learnableSkillId = getAttribute(in, "learnableSkill", null);
        learnableSkill = FreeCol.getSpecification().getUnitType(learnableSkillId);

        alarm = new Tension[Player.NUMBER_OF_NATIONS];
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("alarm")) {
                int[] tensionArray = readFromArrayElement("alarm", in, new int[0]);
                for (int i = 0; i < tensionArray.length; i++) {
                    alarm[i] = new Tension(tensionArray[i]);
                }
            } else if (in.getLocalName().equals("wantedGoods")) {
                int[] wantedGoodsIndex = readFromArrayElement("wantedGoods", in, new int[0]);
                for (int i = 0; i < wantedGoodsIndex.length; i++) {
                    if (i == 3)
                        break;
                    wantedGoods[i] = FreeCol.getSpecification().getGoodsType(wantedGoodsIndex[i]);
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
            } else if (in.getLocalName().equals(UnitContainer.getXMLElementTagName())) {
                unitContainer = (UnitContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (unitContainer != null) {
                    unitContainer.readFromXML(in);
                } else {
                    unitContainer = new UnitContainer(getGame(), this, in);
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
