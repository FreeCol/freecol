
package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Vector;


/**
 * Represents an Indian settlement.
 */
public class IndianSettlement extends Settlement {
    private static final Logger logger = Logger.getLogger(IndianSettlement.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
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


    public static final int ADD_ALARM_UNIT_DESTROYED = 400;
    public static final int ADD_ALARM_SETTLEMENT_ATTACKED = 500;
    
    public static final int MAX_CONVERT_DISTANCE = 10;


    /** The amount of goods a brave can produce a single turn. */
    private static final int WORK_AMOUNT = 5;

    /** The amount of raw material that should be available before producing manufactured goods: */
    public static final int KEEP_RAW_MATERIAL = 50;

    // These are the learnable skills for an Indian settlement.
    // They are fully compatible with the types from the Unit class!
    //
    // Note: UNKNOWN is used for both 'skill' and 'wanted goods'
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
    * At the server side its value will always be NONE or any of the skills above.
    * At the client side the value UNKNOWN is also possible in case the player hasn't
    * checked out the settlement yet.
    * The value NONE is used when the skill has already been taught to a European.
    */
    private int learnableSkill = UNKNOWN;

    private int highlyWantedGoods = UNKNOWN,
                wantedGoods1 = UNKNOWN,
                wantedGoods2 = UNKNOWN;

    private boolean isCapital,
                    isVisited; /* true if a European player has asked to speak with the chief. */

    private UnitContainer unitContainer;
    private GoodsContainer goodsContainer;

    private ArrayList ownedUnits = new ArrayList();

    private Unit missionary;

    /** Used for moitoring the progress towards creating a convert. */
    private int  convertProgress;


    /**
    * Only used by AI - stores the alarm levels,
    * 0-1000 with 1000 as the maximum alarm level:
    */
    private int[] alarm = new int[Player.NUMBER_OF_NATIONS];



    /**
     * The constructor to use.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param tile The location of the <code>IndianSettlement</code>.
     * @param tribe Tribe of settlement
     * @param kind Kind of settlement
     * @param isCapital True if settlement is tribe's capital
     * @param learnableSkill The skill that can be learned by Europeans at this settlement.
     * @param highlyWantedGoods The goods that are very much wanted by the people from this settlement.
     * @param wantedGoods1 Goods that wanted by the people from this settlement.
     * @param wantedGoods2 Goods that wanted by the people from this settlement.
     * @param isVisited Indicates if any European scout has asked to speak with the chief.
     * @param missionary The missionary in this settlement (or null).
     * @exception IllegalArgumentException if an invalid tribe or kind is given
     */
    public IndianSettlement(Game game, Player player, Tile tile, int tribe, int kind,
            boolean isCapital, int learnableSkill, int highlyWantedGoods, int wantedGoods1, int wantedGoods2, boolean isVisited, Unit missionary) {

        // TODO: Change 'null' to the indian AI-player:

        super(game, player, tile);

        if (tile == null) {
            throw new NullPointerException();
        }

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
        this.highlyWantedGoods = highlyWantedGoods;
        this.wantedGoods1 = wantedGoods1;
        this.wantedGoods2 = wantedGoods2;
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
        //updateWantedGoods();
        convertProgress = 0;
    }


    /**
    * Initiates a new <code>IndianSettlement</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> in which this object belong.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public IndianSettlement(Game game, Element element) {
        super(game, element);

        // The client doesn't know a lot at first.
        this.learnableSkill = UNKNOWN;
        this.highlyWantedGoods = UNKNOWN;
        this.wantedGoods1 = UNKNOWN;
        this.wantedGoods2 = UNKNOWN;
        isVisited = false;
        missionary = null;
        convertProgress = 0;

        readFromXMLElement(element);
    }



    /**
    * Modifies the alarm level towards the given player.
    *
    * @param player The <code>Player</code>.
    * @param addToAlarm The amount to add to the current alarm level.
    */
    public void modifyAlarm(Player player, int addToAlarm) {
        alarm[player.getNation()] += addToAlarm;

        if (alarm[player.getNation()]>1000) {
            alarm[player.getNation()] = 1000;
        }
    }


    /**
    * Sets alarm towards the given player.
    *
    * @param player The <code>Player</code>.
    * @param alarmLevel The alarm level.
    */
    public void setAlarm(Player player, int alarmLevel) {
        alarm[player.getNation()] = alarmLevel;

        if (alarm[player.getNation()]>1000) {
            alarm[player.getNation()] = 1000;
        }
    }



    /**
    * Gets the alarm level towards the given player.
    */
    public int getAlarm(Player player) {
        return alarm[player.getNation()];
    }


    /**
    * Returns true if a European player has visited this settlement to speak with the chief.
    * @return true if a European player has visited this settlement to speak with the chief.
    */
    public boolean hasBeenVisited() {
        return isVisited;
    }

    /**
    * Adds the given <code>Unit</code> to the list of units that belongs to this
    * <code>IndianSettlement</code>.
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
    * Gets an iterator over all the units this <code>IndianSettlement</code> is owning.
    */
    public Iterator getOwnedUnitsIterator() {
        return ownedUnits.iterator();
    }


    /**
    * Removes the given <code>Unit</code> to the list of units that belongs to this
    * <code>IndianSettlement</code>.
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
    public int getLearnableSkill() {
        return learnableSkill;
    }


    /**
    * Returns this settlement's highly wanted goods.
    * @return This settlement's highly wanted goods.
    */
    public int getHighlyWantedGoods() {
        return highlyWantedGoods;
    }


    /**
    * Returns this settlement's wanted goods 1.
    * @return This settlement's wanted goods 1.
    */
    public int getWantedGoods1() {
        return wantedGoods1;
    }


    /**
    * Returns this settlement's wanted goods 2.
    * @return This settlement's wanted goods 2.
    */
    public int getWantedGoods2() {
        return wantedGoods2;
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
        if (missionary != this.missionary) {
            convertProgress = 0;
        }
        this.missionary = missionary;
    }


    /**
    * Sets the visited status of this settlement to true, indicating that a European has had
    * a chat with the chief.
    */
    public void setVisited() {
        this.isVisited = true;
    }


    /**
    * Sets this settlement's highly wanted goods.
    * @param goods This settlement's highly wanted goods.
    */
    public void setHighlyWantedGoods(int goods) {
        highlyWantedGoods = goods;
    }


    /**
    * Sets this settlement's wanted goods 1.
    * @param goods This settlement's wanted goods 1.
    */
    public void setWantedGoods1(int goods) {
        wantedGoods1 = goods;
    }


    /**
    * Sets this settlement's wanted goods 2.
    * @param goods This settlement's wanted goods 2.
    */
    public void setWantedGoods2(int goods) {
        wantedGoods2 = goods;
    }


    /**
    * Sets the learnable skill for this Indian settlement.
    * @param skill The new learnable skill for this Indian settlement.
    */
    public void setLearnableSkill(int skill) {
        learnableSkill = skill;
    }


    /**
    * Gets the kind of Indian settlement.
    */
    public int getKind() {
        return kind;
    }


    /**
    * Gets the tribe of the Indian settlement.
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
    public int getRadius() {
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
    * @param locatable The code>Locatable</code> to add to this Location.
    */
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
    * Removes a code>Locatable</code> from this Location.
    *
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
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
    public int getUnitCount() {
        return unitContainer.getUnitCount();
    }


    public Iterator getUnitIterator() {
        return unitContainer.getUnitIterator();
    }


    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
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
    public Unit getDefendingUnit(Unit attacker) {
        Iterator unitIterator = getUnitIterator();

        Unit defender = null;
        if (unitIterator.hasNext()) {
            defender = (Unit) unitIterator.next();
        } else {
            return null;
        }

        while (unitIterator.hasNext()) {
            Unit nextUnit = (Unit) unitIterator.next();

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
    * @param goods The <code>Goods</code> to price.
    * @return The price.
    */
    public int getPrice(int type, int amount) {
        int returnPrice = 0;

        if (amount > 100) {
            throw new IllegalArgumentException();
        }

        if (type == Goods.MUSKETS) {
            int need = 0;
            int supply = 0;
            supply += goodsContainer.getGoodsCount(Goods.MUSKETS) / Unit.MUSKETS_TO_ARM_INDIAN;
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.MUSKETS_TO_ARM_INDIAN;
                if (((Unit) ownedUnits.get(i)).isArmed()) {
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
                returnPrice += 25 * (startPrice-i);
            }
        } else if (type == Goods.HORSES) {
            int need = 0;
            int supply = 0;
            supply += goodsContainer.getGoodsCount(Goods.HORSES) / Unit.HORSES_TO_MOUNT_INDIAN;
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.HORSES_TO_MOUNT_INDIAN;
                if (((Unit) ownedUnits.get(i)).isMounted()) {
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
                returnPrice += 25 * (startPrice-(i*4));
            }
        } else if (type == Goods.FOOD || type == Goods.LUMBER || type == Goods.SUGAR ||
                type == Goods.TOBACCO || type == Goods.COTTON || type == Goods.FURS ||
                type == Goods.ORE || type == Goods.SILVER) {
            returnPrice = 0;
        } else {
            int currentGoods = goodsContainer.getGoodsCount(type);

            // Increase amount if raw materials are produced:
            int rawType = Goods.getRawMaterial(type);
            if (rawType != -1) {
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
        if (type == highlyWantedGoods) {
            returnPrice = (returnPrice*12)/10;
        } else if (type == wantedGoods1) {
            returnPrice = (returnPrice*11)/10;
        } else if (type == wantedGoods2) {
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
    public int getMaximumProduction(int goodsType) {
        int amount = 0;
        Iterator it = getGame().getMap().getCircleIterator(getTile().getPosition(), true, getRadius());
        while (it.hasNext()) {
            Tile workTile = getGame().getMap().getTile((Map.Position) it.next());
            if (workTile.getOwner() == null || workTile.getOwner() == this) {
                amount += workTile.potential(goodsType);
            }
        }

        return amount;
    }


    /**
    * Updates the variables {@link #getHighlyWantedGoods highlyWantedGoods},
    * {@link #getWantedGoods1 wantedGoods1} and
    * {@link #getWantedGoods2 wantedGoods2}.
    *
    * <br><br>
    *
    * It is only meaningful to call this method from the
    * server, since the settlement's {@link GoodsContainer}
    * is hidden from the clients.
    *
    * <br><br>
    *
    * This method should only get called when
    * a scout enters this settlement.
    */
    public void updateWantedGoods() {
        highlyWantedGoods = Goods.TRADE_GOODS;
        wantedGoods1 = Goods.TOOLS;
        wantedGoods2 = Goods.RUM;
        int highlyWantedGoodsAmount = 0;
        int wantedGoods1Amount = 0;
        int wantedGoods2Amount = 0;

        /* TODO: Try the different types goods in random order: */
        for (int type=0; type<Goods.NUMBER_OF_TYPES; type++) {
            if (type == Goods.MUSKETS || type == Goods.HORSES) {
                continue;
            }

            int price = getPrice(type, 100);
            if (price > highlyWantedGoodsAmount) {
                wantedGoods2 = wantedGoods1;
                wantedGoods1 = highlyWantedGoods;
                highlyWantedGoods  = type;
                highlyWantedGoodsAmount = price;
            } else if (price > wantedGoods1Amount) {
                wantedGoods2 = wantedGoods1;
                wantedGoods1 = type;
                wantedGoods1Amount = price;
            } else if (price > wantedGoods2Amount) {
                wantedGoods2 = type;
                wantedGoods2Amount = price;
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
    */
    public int getBonusMultiplier() {
        int addition = getKind() + 1;
        if (isCapital()) {
            addition++;
        }
        return addition;
    }


    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        } else {
            return false;
        }
    }


    public boolean canAdd(Locatable locatable) {
        return true;
    }


    public void newTurn() {
        /* Determine the maximum possible production for each type of goods: */
        int totalGoods = 0;
        int[] potential = new int[Goods.NUMBER_OF_TYPES];
        Iterator it = getGame().getMap().getCircleIterator(getTile().getPosition(), true, getRadius());
        while (it.hasNext()) {
            Tile workTile = getGame().getMap().getTile((Map.Position) it.next());
            if (workTile.getOwner() == null || workTile.getOwner() == this) {
                for (int i=0; i<Goods.NUMBER_OF_TYPES;i++) {
                    potential[i] += workTile.potential(i);
                    totalGoods += potential[i];
                }
            }
        }

        /* Produce the goods: */
        int workers = ownedUnits.size();
        for (int i=1; i<Goods.NUMBER_OF_TYPES;i++) {
            goodsContainer.addGoods(i, potential[i]);
        }
        goodsContainer.addGoods(Goods.FOOD, Math.min(potential[Goods.FOOD], workers*3));

        /* Use tools (if available) to produce manufactured goods: */
        if (goodsContainer.getGoodsCount(Goods.TOOLS) > 0) {
            int typeWithSmallestAmount = -1;
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

            if (typeWithSmallestAmount != -1) {
                int production = Math.min(goodsContainer.getGoodsCount(Goods.getRawMaterial(typeWithSmallestAmount)), Math.min(10, goodsContainer.getGoodsCount(Goods.TOOLS)));
                goodsContainer.removeGoods(Goods.TOOLS, production);
                goodsContainer.removeGoods(Goods.getRawMaterial(typeWithSmallestAmount), production);
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


        /* Increase alarm: */
        if (getUnitCount() > 0) {
            Iterator ci = getGame().getMap().getCircleIterator(getTile().getPosition(), true, getRadius());
            while (ci.hasNext()) {
                Tile t = getGame().getMap().getTile((Map.Position) ci.next());
                if (t.getFirstUnit() != null && t.getFirstUnit().getOwner().isEuropean()) {
                    // Nearby military units.
                    Iterator ui = t.getUnitIterator();
                    while (ui.hasNext()) {
                        Unit u = (Unit) ui.next();
                        if (u.isOffensiveUnit() && !u.isNaval()) {
                            int d = (u.getOwner().hasFather(FoundingFather.POCAHONTAS)) ? 2 : 1;
                            modifyAlarm(u.getOwner(), u.getOffensePower(getTile().getDefendingUnit(u)) / d);
                        }
                    }
                } else if (t.getOwner() != null && t.getOwner().getOwner().isEuropean()) {
                    // Land being used by another settlement:
                    int d = (t.getOwner().getOwner().hasFather(FoundingFather.POCAHONTAS)) ? 2 : 1;
                    modifyAlarm(t.getOwner().getOwner(), 2 / d);
                }

                // Settlement:
                if (t.getSettlement() != null && t.getSettlement().getOwner().isEuropean()) {
                    int d = (t.getSettlement().getOwner().hasFather(FoundingFather.POCAHONTAS)) ? 2 : 1;
                    modifyAlarm(t.getSettlement().getOwner(), (t.getSettlement().getUnitCount() / 2) / d);
                }
            }

            /* Decrease alarm slightly: */
            for (int i=0; i<alarm.length; i++) {
                alarm[i] -= 4 + alarm[i]/100;
                if (alarm[i] < 0) {
                    alarm[i] = 0;
                }
            }
        }

        /* Increase convert progress. And generate convert if needed. */
        if (missionary != null && getGame().getViewOwner() == null) {
            int increment = 8;

            // Update increment if missionary is an expert.
            if (missionary.getType() == Unit.JESUIT_MISSIONARY || missionary.getOwner().hasFather(FoundingFather.FATHER_JEAN_DE_BREBEUF)) {
                increment = 13;
            }

            // Increase increment if alarm level is high.
            increment += 2 * alarm[missionary.getOwner().getNation()] / 100;
            convertProgress += increment;

            int extra = Math.max(0, 8-getUnitCount()*getUnitCount());
            extra *= extra;
            extra *= extra;
            
            if (convertProgress >= 100 + extra && getUnitCount() > 2) {
                Tile targetTile = null;
                Iterator ffi = getGame().getMap().getFloodFillIterator(getTile().getPosition());
                while (ffi.hasNext()) {
                    Tile t = getGame().getMap().getTile((Map.Position) ffi.next());
                    if (getTile().getDistanceTo(t) > MAX_CONVERT_DISTANCE) {
                        break;
                    }
                    if (t.getSettlement() != null && t.getSettlement().getOwner() == missionary.getOwner()) {
                        targetTile = t;
                        break;
                    }
                }

                if (targetTile != null) {
                    ((Unit) getUnitIterator().next()).dispose();
                    convertProgress = 0;

                    Unit u = getGame().getModelController().createUnit(getID() + "newTurn100missionary", targetTile, missionary.getOwner(), Unit.INDIAN_CONVERT);
                    //addModelMessage(this, "model.colony.newConvert", new String[][] {{"%nation%", getOwner().getNationAsString()}});
                    logger.info("New convert created for " + missionary.getOwner().getName() + " with ID=" + u.getID());
                }
            }
        }
    }


    private void consumeGoods(int type, int amount) {
        if (goodsContainer.getGoodsCount(type) > 0) {
            amount = Math.min(amount, goodsContainer.getGoodsCount(type));
            getOwner().modifyGold(amount);
            goodsContainer.removeGoods(type, amount);
        }
    }


    public void dispose() {
        unitContainer.dispose();
        goodsContainer.dispose();
        getTile().setSettlement(null);
        super.dispose();
    }


    /**
    * Creates the {@link GoodsContainer}.
    * <br><br>
    * DO NOT USE OTHER THAN IN {@link FreeColServer#loadGame}: Only for compatibility when loading
    * save games with pre-0.0.3 protocols.
    */
    public void createGoodsContainer() {
        goodsContainer = new GoodsContainer(getGame(), this);
    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "IndianSettlement".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        Element indianSettlementElement = document.createElement(getXMLElementTagName());

        indianSettlementElement.setAttribute("ID", getID());
        indianSettlementElement.setAttribute("tile", tile.getID());
        indianSettlementElement.setAttribute("owner", owner.getID());
        indianSettlementElement.setAttribute("tribe", Integer.toString(tribe));
        indianSettlementElement.setAttribute("kind", Integer.toString(kind));
        indianSettlementElement.setAttribute("isCapital", Boolean.toString(isCapital));

        String ownedUnitsString = "";
        for (int i=0; i<ownedUnits.size(); i++) {
            ownedUnitsString += ((Unit) ownedUnits.get(i)).getID();
            if (i != ownedUnits.size() - 1) {
                ownedUnitsString += ", ";
            }
        }
        if (!ownedUnitsString.equals("")) {
            indianSettlementElement.setAttribute("ownedUnits", ownedUnitsString);
        }

        if (showAll || player == getOwner()) {
            indianSettlementElement.setAttribute("hasBeenVisted", Boolean.toString(isVisited));
        }

        indianSettlementElement.setAttribute("learnableSkill", Integer.toString(learnableSkill));
        indianSettlementElement.setAttribute("highlyWantedGoods", Integer.toString(highlyWantedGoods));
        indianSettlementElement.setAttribute("wantedGoods1", Integer.toString(wantedGoods1));
        indianSettlementElement.setAttribute("wantedGoods2", Integer.toString(wantedGoods2));
        indianSettlementElement.appendChild(toArrayElement("alarm", alarm, document));
        
        if (missionary != null && (showAll || player == getOwner() || (missionary != null && player == missionary.getOwner()))) {
            indianSettlementElement.setAttribute("missionary", missionary.getID());
        }

        if (showAll || player == getOwner()) {
            indianSettlementElement.setAttribute("convertProgress", Integer.toString(convertProgress));
            indianSettlementElement.appendChild(unitContainer.toXMLElement(player, document, showAll, toSavedGame));
            indianSettlementElement.appendChild(goodsContainer.toXMLElement(player, document, showAll, toSavedGame));
        } else {
            UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
            emptyUnitContainer.setFakeID(unitContainer.getID());
            indianSettlementElement.appendChild(emptyUnitContainer.toXMLElement(player, document, showAll, toSavedGame));

            GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
            emptyGoodsContainer.setFakeID(goodsContainer.getID());
            indianSettlementElement.appendChild(emptyGoodsContainer.toXMLElement(player, document, showAll, toSavedGame));
        }

        return indianSettlementElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param indianSettlementElement The DOM-element ("Document Object Model") made to represent this "IndianSettlement".
    */
    public void readFromXMLElement(Element indianSettlementElement) {
        setID(indianSettlementElement.getAttribute("ID"));

        tile = (Tile) getGame().getFreeColGameObject(indianSettlementElement.getAttribute("tile"));
        owner = (Player)getGame().getFreeColGameObject(indianSettlementElement.getAttribute("owner"));
        tribe = Integer.parseInt(indianSettlementElement.getAttribute("tribe"));
        kind = Integer.parseInt(indianSettlementElement.getAttribute("kind"));
        isCapital = (new Boolean(indianSettlementElement.getAttribute("isCapital"))).booleanValue();

        ownedUnits.clear();
        if (indianSettlementElement.hasAttribute("ownedUnits")) {
            StringTokenizer st = new StringTokenizer(indianSettlementElement.getAttribute("ownedUnits"), ", ", false);
            while (st.hasMoreTokens()) {
                Unit u = (Unit) getGame().getFreeColGameObject(st.nextToken());
                if (u != null) {
                    ownedUnits.add(u);
                }
            }
        }

        if (getChildElement(indianSettlementElement, "alarm") != null) {
            alarm = readFromArrayElement("alarm", getChildElement(indianSettlementElement, "alarm"), new int[0]);
        } else {
            alarm = new int[Player.NUMBER_OF_NATIONS];
        }

        if (indianSettlementElement.hasAttribute("learnableSkill")) {
            learnableSkill = Integer.parseInt(indianSettlementElement.getAttribute("learnableSkill"));
        }
        if (indianSettlementElement.hasAttribute("highlyWantedGoods")) {
            highlyWantedGoods = Integer.parseInt(indianSettlementElement.getAttribute("highlyWantedGoods"));
        }
        if (indianSettlementElement.hasAttribute("wantedGoods1")) {
            wantedGoods1 = Integer.parseInt(indianSettlementElement.getAttribute("wantedGoods1"));
        }
        if (indianSettlementElement.hasAttribute("wantedGoods2")) {
            wantedGoods2 = Integer.parseInt(indianSettlementElement.getAttribute("wantedGoods2"));
        }
        if (indianSettlementElement.hasAttribute("hasBeenVisited")) {
            isVisited = Boolean.getBoolean(indianSettlementElement.getAttribute("hasBeenVisited"));
        }
        if (indianSettlementElement.hasAttribute("missionary")) {
            missionary = (Unit) getGame().getFreeColGameObject(indianSettlementElement.getAttribute("missionary"));
        }
        if (indianSettlementElement.hasAttribute("convertProgress")) {
            convertProgress = Integer.parseInt(indianSettlementElement.getAttribute("convertProgress"));
        }

        Element unitContainerElement = getChildElement(indianSettlementElement, UnitContainer.getXMLElementTagName());
        if (unitContainer != null) {
            unitContainer.readFromXMLElement(unitContainerElement);
        } else {
            unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
        }

        Element goodsContainerElement = getChildElement(indianSettlementElement, GoodsContainer.getXMLElementTagName());
        if (goodsContainerElement != null) {  // "if" used to ensure compatibility with PRE-0.0.3 FreeCol-protocols.
            GoodsContainer gc = (GoodsContainer) getGame().getFreeColGameObject(goodsContainerElement.getAttribute("ID"));

            if (gc != null) {
                goodsContainer.readFromXMLElement(goodsContainerElement);
            } else {
                goodsContainer = new GoodsContainer(getGame(), this, goodsContainerElement);
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
}
