
package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.logging.Logger;

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

    private int food = 0;

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

        readFromXMLElement(element);

        // The client doesn't know the skill at first.
        this.learnableSkill = UNKNOWN;
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
        int workers = unitContainer.getUnitCount();
        for (int direction=0; direction<8 && workers > 0; direction++) {
            if (getGame().getMap().getNeighbourOrNull(direction, getTile()) != null &&
                    (getGame().getMap().getNeighbourOrNull(direction, getTile()).getOwner() == null
                    || getGame().getMap().getNeighbourOrNull(direction, getTile()).getOwner() == this)) {
                food += 5;
                workers--;
            }
        }

        // TODO: Create a unit if food>=300, but not if a maximum number of units is reaced.
    }


    public void dispose() {
        unitContainer.dispose();
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
        // By default learnableSkill is not sent over the network. The user needs to visit
        // the settlement with a free colonist or a scout in order to obtain this information.
        indianSettlementElement.setAttribute("isCapital", Boolean.toString(isCapital));
        indianSettlementElement.setAttribute("food", Integer.toString(food));

        indianSettlementElement.appendChild(unitContainer.toXMLElement(player, document, showAll, toSavedGame));

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
        // learnableSkill is not in the network message. See toXMLElement for details.
        isCapital = (new Boolean(indianSettlementElement.getAttribute("isCapital"))).booleanValue();

        if (indianSettlementElement.hasAttribute("food")) {
            food = Integer.parseInt(indianSettlementElement.getAttribute("food"));
        } else {
            food = 0;
        }

        Element unitContainerElement = getChildElement(indianSettlementElement, UnitContainer.getXMLElementTagName());
        if (unitContainer != null) {
            unitContainer.readFromXMLElement(unitContainerElement);
        } else {
            unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
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
