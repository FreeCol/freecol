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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.util.RandomChoice;

import org.w3c.dom.Element;

/**
 * This class implements the player's monarch, whose functions prior
 * to the revolution include raising taxes, declaring war on other
 * European countries, and occasionally providing military support.
 */
public final class Monarch extends FreeColGameObject implements Named {

    /** The name key of this monarch. */
    private String name;

    /** The player of this monarch. */
    private Player player;

    private static final Logger logger = Logger.getLogger(Monarch.class.getName());

    private static final EquipmentType muskets = FreeCol.getSpecification().getEquipmentType("model.equipment.muskets");
    private static final EquipmentType horses = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");

    /** Constants describing monarch actions. */
    public static enum MonarchAction {
        NO_ACTION,
            RAISE_TAX,
            ADD_TO_REF,
            DECLARE_WAR,
            SUPPORT_SEA,
            SUPPORT_LAND,
            OFFER_MERCENARIES,
            LOWER_TAX,
            WAIVE_TAX,
            ADD_UNITS }

    /** The space required to transport all land units. */
    int spaceRequired;

    /** The current naval transport capacity. */
    int capacity;

    /** The number of land units in the REF. */
    private List<AbstractUnit> landUnits = new ArrayList<AbstractUnit>();

    /** The number of naval units in the REF. */
    private List<AbstractUnit> navalUnits = new ArrayList<AbstractUnit>();

    /** The minimum price for mercenaries. */
    public static final int MINIMUM_PRICE = 100;

    /**
     * The maximum possible tax rate (given in percentage).
     */
    public static final int MAXIMUM_TAX_RATE = 75;

    /**
     * The minimum tax rate (given in percentage) from where it
     * can be lowered 
     */
    public static final int MINIMUM_TAX_RATE = 20;
    
    /** Whether a frigate has been provided. */
    private boolean supportSea = false;

    /**
     * Constructor.
     *
     * @param game The <code>Game</code> this <code>Monarch</code>
     *      should be created in.
     * @param player The <code>Player</code> to create the
     *      <code>Monarch</code> for.
     * @param name The name of the <code>Monarch</code>.
     */
    public Monarch(Game game, Player player, String name) {
        super(game);

        if (player == null) {
            throw new IllegalStateException("player == null");
        }

        this.player = player;
        this.name = name;

        int number = Specification.getSpecification().getIntegerOption("model.option.refStrength")
            .getValue() + 3;

        for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
            if (unitType.hasAbility("model.ability.refUnit")) {
                if (unitType.hasAbility("model.ability.navalUnit")) {
                    navalUnits.add(new AbstractUnit(unitType, Role.DEFAULT, number));
                    if (unitType.canCarryUnits()) {
                        capacity += unitType.getSpace() * number;
                    }
                } else if (unitType.hasAbility("model.ability.canBeEquipped")) {
                    landUnits.add(new AbstractUnit(unitType, Role.SOLDIER, number));
                    landUnits.add(new AbstractUnit(unitType, Role.DRAGOON, number));
                    spaceRequired += unitType.getSpaceTaken() * 2 * number;
                } else {
                    landUnits.add(new AbstractUnit(unitType, Role.DEFAULT, number));
                    spaceRequired += unitType.getSpaceTaken() * number;
                }
            }
        }
    }


    /**
     * Initiates a new <code>Monarch</code> from an <code>Element</code>
     * and registers this <code>Monarch</code> at the specified game.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Monarch(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>Monarch</code> from an <code>Element</code>
     * and registers this <code>Monarch</code> at the specified game.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public Monarch(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Monarch</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Monarch(Game game, String id) {
        super(game, id);
    }


    /**
     * Return the name key of this Monarch.
     *
     * @return a <code>String</code> value
     */
    public String getNameKey() {
        return name;
    }

    /**
     * Returns a monarch action. Not all actions are always
     * applicable, and their probability depends on the player's
     * difficulty settings. This method can only be called by the
     * server.
     *
     * @return A monarch action.
     */
    public MonarchAction getAction() {
        /**
         * Random numbers can be generated by this method since it is only
         * invoked by the endTurn method of the server's InGameController.
         */
        List<RandomChoice<MonarchAction>> choices = getActionChoices();
        if (choices == null) {
            return MonarchAction.NO_ACTION;
        } else {
            return RandomChoice.getWeightedRandom(getGame().getModelController().getPseudoRandom(),
                                                  choices);
        }                                              
    }

    public List<RandomChoice<MonarchAction>> getActionChoices() {
        int dx = Specification.getSpecification().getIntegerOption("model.option.monarchMeddling")
            .getValue() + 1;
        int turn = getGame().getTurn().getNumber();
        int grace = (6 - dx) * 10; // 10-50

        // nothing happens during the first few turns, nor after the
        // revolution
        if (turn < grace || player.getPlayerType() != PlayerType.COLONIAL) {
            return null;
        }

        boolean canDeclareWar = false;
        boolean atWar = false;
        // Benjamin Franklin puts an end to the monarch's interference
        if (!player.hasAbility("model.ability.ignoreEuropeanWars")) {
            for (Player enemy : getGame().getPlayers()) {
                if (!enemy.isEuropean() || enemy.isREF()) {
                    continue;
                }
                switch (player.getStance(enemy)) {
                case UNCONTACTED:
                    break;
                case WAR:
                    atWar = true;
                    break;
                case PEACE: case CEASE_FIRE:
                    canDeclareWar = true;
                    break;
                case ALLIANCE:
                    // we can neither have nor declare war.
                    break;
                }
            }
        }

        /** The probabilities of these actions. */
        List<RandomChoice<MonarchAction>> choices = new ArrayList<RandomChoice<MonarchAction>>();

        // the more time has passed, the less likely the monarch will
        // do nothing
        choices.add(new RandomChoice<MonarchAction>(MonarchAction.NO_ACTION, Math.max(200 - turn, 100)));

        if (player.getTax() < MAXIMUM_TAX_RATE) {
            choices.add(new RandomChoice<MonarchAction>(MonarchAction.RAISE_TAX, 10 + dx));
        }

        choices.add(new RandomChoice<MonarchAction>(MonarchAction.ADD_TO_REF, 10 + dx));

        if (canDeclareWar) {
            choices.add(new RandomChoice<MonarchAction>(MonarchAction.DECLARE_WAR, 5 + dx));
        }

        // provide no more than one frigate
        if (player.hasBeenAttackedByPrivateers() && !supportSea) {
            choices.add(new RandomChoice<MonarchAction>(MonarchAction.SUPPORT_SEA, 6 - dx));
        }

        if (atWar) {
            // disable for the moment
            //MonarchAction.SUPPORT_LAND = 6 - dx;
            if (player.getGold() > MINIMUM_PRICE) {
                choices.add(new RandomChoice<MonarchAction>(MonarchAction.OFFER_MERCENARIES, 6 - dx));
            }
        }

        if (player.getTax() > MINIMUM_TAX_RATE + 10) {
            // Play it safe: we don't want to lower the rate below the
            // minimum tax rate, since it might actually be raised by
            // getNewTax() in that case.
            choices.add(new RandomChoice<MonarchAction>(MonarchAction.LOWER_TAX, 10 - dx));
        }

        return choices;
    }

    /**
     * Returns a List of all REF units.
     *
     * @return a List of all REF units.
     */
    public List<AbstractUnit> getREF() {
        List<AbstractUnit> result = new ArrayList<AbstractUnit>(landUnits);
        result.addAll(navalUnits);
        return result;
    }

    /**
     * Returns only the naval units.
     *
     * @return the naval units
     */
    public List<AbstractUnit> getNavalUnits() {
        return navalUnits;
    }

    /**
     * Returns only the land units.
     *
     * @return the land units
     */
    public List<AbstractUnit> getLandUnits() {
        return landUnits;
    }

    /**
     * Returns the new increased tax.
     *
     * @return The increased tax.
     */
    public int getNewTax(MonarchAction taxChange) {
    	
    	int newTax = 110; // set bigger that 100% to catch errors
        int taxAdjustment = Specification.getSpecification().getIntegerOption("model.option.taxAdjustment")
            .getValue();
        
        switch(taxChange){
        case RAISE_TAX:
            int turn = getGame().getTurn().getNumber();
            int adjustment = Math.max(1, (6 - taxAdjustment) * 10); // 20-60
            // later in the game, the taxes will increase by more
            int increase = getGame().getModelController().getPseudoRandom().nextInt(5 + turn/adjustment) + 1;
            newTax = player.getTax() + increase;
            newTax = Math.min(newTax, MAXIMUM_TAX_RATE);
            break;
        case LOWER_TAX:
            adjustment = Math.max(1, 10 - taxAdjustment); // 5-10
            int decrease = getGame().getModelController().getPseudoRandom().nextInt(adjustment) + 1;
            newTax = player.getTax() - decrease;
            newTax = Math.max(newTax, MINIMUM_TAX_RATE);
            break;
        default:
            logger.warning("Wrong tax change type");
            return newTax;
        }
        
        return newTax;
    }

    /**
     * Returns units available as mercenaries.
     *
     * @return A troop of mercenaries.
     */
    public List<AbstractUnit> getMercenaries() {
        List<AbstractUnit> mercenaries = new ArrayList<AbstractUnit>();
        List<UnitType> unitTypes = new ArrayList<UnitType>();

        for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
            if (unitType.hasAbility("model.ability.mercenaryUnit")) {
                unitTypes.add(unitType);
            }
        }
        int gold = player.getGold();
        int price = 0;
        int limit = unitTypes.size();
        UnitType unitType = null;
        for (int count = 0; count < limit; count++) {
            int index = getGame().getModelController().getPseudoRandom().nextInt(unitTypes.size());
            unitType = unitTypes.get(index);
            if (unitType.hasAbility("model.ability.canBeEquipped")) {
                int newPrice = getPrice(unitType, Role.DRAGOON);
                for (int number = 3; number > 0; number--) {
                    if (price + newPrice * number <= gold) {
                        mercenaries.add(new AbstractUnit(unitType, Role.DRAGOON, number));
                        price += newPrice * number;
                        break;
                    }
                }
                newPrice = getPrice(unitType, Role.SOLDIER);
                for (int number = 3; number > 0; number--) {
                    if (price + newPrice * number <= gold) {
                        mercenaries.add(new AbstractUnit(unitType, Role.SOLDIER, number));
                        price += newPrice * number;
                        break;
                    }
                }
            } else {
                int newPrice = getPrice(unitType, Role.DEFAULT);
                for (int number = 3; number > 0; number--) {
                    if (price + newPrice * number <= gold) {
                        mercenaries.add(new AbstractUnit(unitType, Role.DEFAULT, number));
                        price += newPrice * number;
                        break;
                    }
                }
            }
            unitTypes.remove(index);
        }

        if (price == 0 && unitType != null) {
            if (unitType.hasAbility("model.ability.canBeEquipped")) {
                mercenaries.add(new AbstractUnit(unitType, Role.SOLDIER, 1));
            } else {
                mercenaries.add(new AbstractUnit(unitType, Role.DEFAULT, 1));
            }
        }

        return mercenaries;
    }



    /**
     * Returns units to be added to the Royal Expeditionary Force.
     *
     * @return An addition to the Royal Expeditionary Force.
     */
    public List<AbstractUnit> addToREF() {
        ArrayList<AbstractUnit> result = new ArrayList<AbstractUnit>();
        if (capacity < spaceRequired) {
            AbstractUnit unit = navalUnits.get(getGame().getModelController().getPseudoRandom().nextInt(navalUnits.size()));
            result.add(new AbstractUnit(unit.getUnitType(), unit.getRole(), 1));
        } else {
            int number = getGame().getModelController().getPseudoRandom().nextInt(3) + 1;
            AbstractUnit unit = landUnits.get(getGame().getModelController().getPseudoRandom().nextInt(landUnits.size()));
            result.add(new AbstractUnit(unit.getUnitType(), unit.getRole(), number));
        }
        return result;
    }

    /**
     * Adds units to the Royal Expeditionary Force.
     *
     * @param units The addition to the Royal Expeditionary Force.
     */
    public void addToREF(List<AbstractUnit> units) {
        for (AbstractUnit unitToAdd : units) {
            UnitType unitType = unitToAdd.getUnitType();
            if (unitType.hasAbility("model.ability.navalUnit")) {
                for (AbstractUnit refUnit : navalUnits) {
                    if (refUnit.getUnitType().equals(unitType)) {
                        refUnit.setNumber(refUnit.getNumber() + unitToAdd.getNumber());
                        if (unitType.canCarryUnits()) {
                            capacity += unitType.getSpace() * unitToAdd.getNumber();
                        }
                    }
                }
            } else {
                for (AbstractUnit refUnit : landUnits) {
                    if (refUnit.getUnitType().equals(unitType) &&
                        refUnit.getRole().equals(unitToAdd.getRole())) {
                        refUnit.setNumber(refUnit.getNumber() + unitToAdd.getNumber());
                        spaceRequired += unitType.getSpaceTaken() * unitToAdd.getNumber();
                    }
                }
            }
        }
    }

    /**
     * Returns the price for the given units.
     *
     * @param units The units to get a price for.
     * @param rebate Whether to grant a rebate.
     * @return The price fo the units.
     */
    public int getPrice(List<AbstractUnit> units, boolean rebate) {
        int price = 0;
        for (AbstractUnit unit : units) {
            int newPrice = getPrice(unit.getUnitType(), unit.getRole());
            price += newPrice * unit.getNumber();
        }
        if (price > player.getGold() && rebate) {
            return player.getGold();
        } else {
            return price;
        }
    }

    public int getPrice(UnitType unitType, Role role) {
        if (unitType.hasPrice()) {
            int price = player.getEurope().getUnitPrice(unitType);
            if (Role.SOLDIER.equals(role)) {
                price += getEquipmentPrice(muskets);
            } else if (Role.DRAGOON.equals(role)) {
                price += getEquipmentPrice(muskets);
                price += getEquipmentPrice(horses);
            }
            return price / 10 + Specification.getSpecification()
                .getIntegerOption("model.option.mercenaryPrice").getValue();
        } else {
            return 1000000;
        }
    }

    private int getEquipmentPrice(EquipmentType equipment) {
        int price = 0;
        for (AbstractGoods goods : equipment.getGoodsRequired()) {
            price += player.getMarket().getBidPrice(goods.getType(), goods.getAmount());
        }
        return price;
    }


    /**
     * Returns the nation of another player to declare war on.
     *
     * @return The enemy nation.
     */
    public Player declareWar() {
        ArrayList<Player> europeanPlayers = new ArrayList<Player>();
        for (Player enemy : getGame().getPlayers()) {
            if (enemy == player) {
                continue;
            } else if (!player.hasContacted(enemy)) {
                continue;
            } else if (!enemy.isEuropean() || enemy.isREF()) {
                continue;
            }
            Stance stance = player.getStance(enemy);
            if (stance == Stance.PEACE || stance == Stance.CEASE_FIRE) {
                europeanPlayers.add(enemy);
            }
        }
        if (europeanPlayers.size() > 0) {
            int randomInt = getGame().getModelController().getPseudoRandom().nextInt(europeanPlayers.size());
            Player enemy = europeanPlayers.get(randomInt);
            return enemy;
        }
        return null;
    }

    /**
     * Returns an addition to the colonial forces.
     *
     * @return An addition to the colonial forces.
     */
    /*
    public int[] supportLand() {
        int[] units = new int[NUMBER_OF_TYPES];
        switch (player.getDifficulty()) {
        case Player.VERY_EASY:
            units[ARTILLERY] = 1;
            units[DRAGOON] = 2;
            break;
        case Player.EASY:
            units[DRAGOON] = 2;
            units[INFANTRY] = 1;
            break;
        case Player.MEDIUM:
            units[DRAGOON] = 2;
            break;
        case Player.HARD:
            units[DRAGOON] = 1;
            units[INFANTRY] = 1;
            break;
        case Player.VERY_HARD:
            units[INFANTRY] = 1;
            break;
        }
        return units;
    }
    */


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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        out.writeAttribute("player", this.player.getId());
        out.writeAttribute("name", name);
        out.writeAttribute("supportSea", String.valueOf(supportSea));

        out.writeStartElement("navalUnits");
        for (AbstractUnit unit : navalUnits) {
            unit.toXMLImpl(out);
        }
        out.writeEndElement();

        out.writeStartElement("landUnits");
        for (AbstractUnit unit : landUnits) {
            unit.toXMLImpl(out);
        }
        out.writeEndElement();

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
        if (player == null) {
            player = new Player(getGame(), in.getAttributeValue(null, "player"));
        }
        name = getAttribute(in, "name", player.getNation().getRulerNameKey());
        supportSea = Boolean.valueOf(in.getAttributeValue(null, "supportSea")).booleanValue();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("navalUnits".equals(childName)) {
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    AbstractUnit newUnit = new AbstractUnit(in);
                    navalUnits.add(newUnit);
                }
            } else if ("landUnits".equals(childName)) {
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    AbstractUnit newUnit = new AbstractUnit(in);
                    landUnits.add(newUnit);
                }
            }
        }
        
        // sanity check: we should be on the closing tag
        if (!in.getLocalName().equals(Monarch.getXMLElementTagName())) {
            logger.warning("Error parsing xml: expecting closing tag </" + Monarch.getXMLElementTagName() + "> "+
                           "found instead: " +in.getLocalName());
        }
    }


    /**
     * Gets the tag name of the root element representing this object.
     * This method should be overwritten by any sub-class, preferably
     * with the name of the class with the first letter in lower case.
     *
     * @return "monarch".
     */
    public static String getXMLElementTagName() {
        return "monarch";
    }


}

