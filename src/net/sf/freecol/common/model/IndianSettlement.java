
package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


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

    /** The amount of goods a brave can produce a single turn. */
    private static final int WORK_AMOUNT = 5;

    // These are the learnable skills for an Indian settlement.
    // They are fully compatible with the types from the Unit class!
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
    private int learnableSkill;
    
    private boolean isCapital;
    private UnitContainer unitContainer;
    private GoodsContainer goodsContainer;

    private ArrayList ownedUnits = new ArrayList();




    /**
     * The constructor to use.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param tile The location of the <code>IndianSettlement</code>.
     * @param tribe Tribe of settlement
     * @param kind Kind of settlement
     * @param isCapital True if settlement is tribe's capital
     * @param learnableSkill The skill that can be learned by Europeans at this settlement.
     * @exception IllegalArgumentException if an invalid tribe or kind is given
     */
    public IndianSettlement(Game game, Player player, Tile tile, int tribe, int kind, boolean isCapital, int learnableSkill) {
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
        this.isCapital = isCapital;
        
        for (int goodsType=0; goodsType<Goods.NUMBER_OF_TYPES; goodsType++) {
            if (goodsType == Goods.LUMBER || goodsType == Goods.HORSES || goodsType == Goods.MUSKETS 
                    || goodsType == Goods.TRADE_GOODS || goodsType == Goods.TOOLS) {
                continue;
            }
            goodsContainer.addGoods(goodsType, (int) Math.random() * 300);
        }

        goodsContainer.addGoods(Goods.LUMBER, 300);
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

        // The client doesn't know the skill at first.
        this.learnableSkill = UNKNOWN;

        readFromXMLElement(element);
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
    * @param goods THe <code>Goods</code> to price.
    * @return The price.
    */
    public int getPrice(Goods goods) {
        int returnPrice = 0;

        if (goods.getAmount() > 100) {
            throw new IllegalArgumentException();
        }

        if (goods.getType() == Goods.MUSKETS) {
            int need = 0;
            int supply = 0;
            supply += goodsContainer.getGoodsCount(Goods.MUSKETS) / Unit.MUSKETS_TO_ARM_INDIAN;
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.MUSKETS_TO_ARM_INDIAN;
                if (((Unit) ownedUnits.get(i)).isArmed()) {
                    supply += Unit.MUSKETS_TO_ARM_INDIAN;
                }
            }

            int sets = ((goodsContainer.getGoodsCount(Goods.MUSKETS) + goods.getAmount()) / Unit.MUSKETS_TO_ARM_INDIAN)
                        - (goodsContainer.getGoodsCount(Goods.MUSKETS) / Unit.MUSKETS_TO_ARM_INDIAN);
            int startPrice = (19+getPriceAddition()) - (supply / Unit.MUSKETS_TO_ARM_INDIAN);
            for (int i=0; i<sets; i++) {
                if ((startPrice-i) < 8 && (need > supply || goodsContainer.getGoodsCount(Goods.MUSKETS) < Unit.MUSKETS_TO_ARM_INDIAN * 2)) {
                    startPrice = 8+i;
                }
                returnPrice += 25 * (startPrice-i);
            }
        } else if (goods.getType() == Goods.HORSES) {
            int need = 0;
            int supply = 0;
            supply += goodsContainer.getGoodsCount(Goods.HORSES) / Unit.HORSES_TO_MOUNT_INDIAN;
            for (int i=0; i<ownedUnits.size(); i++) {
                need += Unit.HORSES_TO_MOUNT_INDIAN;
                if (((Unit) ownedUnits.get(i)).isMounted()) {
                    supply += Unit.HORSES_TO_MOUNT_INDIAN;
                }
            }

            int sets = (goodsContainer.getGoodsCount(Goods.HORSES) + goods.getAmount()) / Unit.HORSES_TO_MOUNT_INDIAN
                        - (goodsContainer.getGoodsCount(Goods.HORSES) / Unit.HORSES_TO_MOUNT_INDIAN);
            int startPrice = (24+getPriceAddition()) - (supply/Unit.HORSES_TO_MOUNT_INDIAN);

            for (int i=0; i<sets; i++) {
                if ((startPrice-(i*4)) < 4 && (need > supply || goodsContainer.getGoodsCount(Goods.HORSES) < Unit.HORSES_TO_MOUNT_INDIAN * 2)) {
                    startPrice = 4+(i*4);
                }
                returnPrice += 25 * (startPrice-(i*4));
            }
        } else if (goods.getType() == Goods.FOOD || goods.getType() == Goods.LUMBER || goods.getType() == Goods.SUGAR ||
                goods.getType() == Goods.TOBACCO || goods.getType() == Goods.COTTON || goods.getType() == Goods.FURS ||
                goods.getType() == Goods.ORE || goods.getType() == Goods.SILVER) {
            returnPrice = 0;
        } else {
            int currentGoods = goodsContainer.getGoodsCount(goods.getType());
            int valueGoods = Math.min(currentGoods + goods.getAmount(), 200) - currentGoods;
            if (valueGoods < 0) {
                valueGoods = 0;
            }

            returnPrice = (int) (((20.0+getPriceAddition())-(0.05*(currentGoods+valueGoods)))*(currentGoods+valueGoods)
                        - ((20.0+getPriceAddition())-(0.05*(currentGoods)))*(currentGoods));
        }

        return returnPrice;
    }

    
    /**
    * Get the extra bonus if this is a <code>VILLAGE</code>,
    * <code>CITY</code> or a capital.
    */
    private int getPriceAddition() {
        int addition = getKind();
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
        /* Determine the maximum possible production of each type of goods: */
        int[] potential = new int[Goods.NUMBER_OF_TYPES];
        Iterator it = getGame().getMap().getCircleIterator(getTile().getPosition(), true, getRadius());
        while (it.hasNext()) {
            Tile workTile = getGame().getMap().getTile((Map.Position) it.next());
            if (workTile.getOwner() == null || workTile.getOwner() == this) {
                for (int i=0; i<Goods.NUMBER_OF_TYPES;i++) {
                    potential[i] += workTile.potential(i);
                }
            }
        }

        /* Produce goods: */
        int workers = ownedUnits.size();
        goodsContainer.addGoods(Goods.FOOD, Math.min(potential[Goods.FOOD], workers*3));
        for (int i=0; i<workers*WORK_AMOUNT; i++) {
            int typeWithSmallestAmount = -1;
            for (int j=1; j<Goods.NUMBER_OF_TYPES; j++) {
                if (potential[j] > 0 && (typeWithSmallestAmount == -1 ||
                        goodsContainer.getGoodsCount(j) < goodsContainer.getGoodsCount(typeWithSmallestAmount))) {
                    typeWithSmallestAmount = j;
                }
            }
            if (typeWithSmallestAmount != -1) {
                if (goodsContainer.getGoodsCount(Goods.TOOLS) > 0) {
                    goodsContainer.addGoods(typeWithSmallestAmount, 3);
                    goodsContainer.removeGoods(Goods.TOOLS, 1);
                } else {
                    goodsContainer.addGoods(typeWithSmallestAmount, 1);
                }
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
            consumeGoods(Goods.SILVER, 1);
            consumeGoods(Goods.RUM, 2);
            consumeGoods(Goods.CIGARS, 1);
            consumeGoods(Goods.COATS, 1);
            consumeGoods(Goods.CLOTH, 1);
            consumeGoods(Goods.TRADE_GOODS, 5);
        }

        goodsContainer.removeAbove(300);

        // TODO: Create a unit if food>=200, but not if a maximum number of units is reaced.
    }

    
    private void consumeGoods(int type, int amount) {
        if (goodsContainer.getGoodsCount(type) > 0) {
            goodsContainer.removeGoods(type, Math.min(amount, goodsContainer.getGoodsCount(type)));
            getOwner().modifyGold(Math.min(amount, goodsContainer.getGoodsCount(type)));
        }
    }


    public void dispose() {
        unitContainer.dispose();
        goodsContainer.dispose();
        getTile().setSettlement(null);
        super.dispose();
    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "IndianSettlement".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
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
            indianSettlementElement.setAttribute("learnableSkill", Integer.toString(learnableSkill));
        }

        if (showAll || player == getOwner()) {
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

        if (indianSettlementElement.hasAttribute("learnableSkill")) {
            learnableSkill = Integer.parseInt(indianSettlementElement.getAttribute("learnableSkill"));
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
