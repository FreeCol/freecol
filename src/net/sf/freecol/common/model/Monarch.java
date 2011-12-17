/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.option.UnitListOption;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;


/**
 * This class implements the player's monarch, whose functions prior
 * to the revolution include raising taxes, declaring war on other
 * European countries, and occasionally providing military support.
 */
public final class Monarch extends FreeColGameObject implements Named {

    private static final Logger logger = Logger.getLogger(Monarch.class.getName());

    /** The minimum price for mercenaries. */
    public static final int MINIMUM_PRICE = 300;

    /**
     * The minimum tax rate (given in percentage) from where it
     * can be lowered.
     */
    public static final int MINIMUM_TAX_RATE = 20;

    /** The name key of this monarch. */
    private String name;

    /** The player of this monarch. */
    private Player player;

    /** The number of land units in the REF. */
    private final List<AbstractUnit> landUnits = new ArrayList<AbstractUnit>();

    /** The number of naval units in the REF. */
    private final List<AbstractUnit> navalUnits = new ArrayList<AbstractUnit>();

    /** Whether a frigate has been provided. */
    private boolean supportSea = false;

    /** Whether displeasure has been incurred. */
    private boolean displeasure = false;

    // Internal variables that do not need serialization.
    /** The space required to transport all land units. */
    private int spaceRequired;

    /** The current naval transport capacity. */
    private int capacity;


    /** Constants describing monarch actions. */
    public static enum MonarchAction {
        NO_ACTION,
        RAISE_TAX_ACT,
        RAISE_TAX_WAR,
        FORCE_TAX,
        LOWER_TAX_WAR,
        LOWER_TAX_OTHER,
        WAIVE_TAX,
        ADD_TO_REF,
        DECLARE_WAR,
        SUPPORT_LAND,
        SUPPORT_SEA,
        OFFER_MERCENARIES, DISPLEASURE,
    }


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

        Specification spec = getSpecification();
        List<AbstractUnit> ref =
            ((UnitListOption) spec.getOption("model.option.refSize")).getOptionValues();
        for (AbstractUnit unit : ref) {
            UnitType unitType = unit.getUnitType(spec);
            if (unitType.hasAbility("model.ability.refUnit")) {
                if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                    navalUnits.add(unit);
                } else {
                    landUnits.add(unit);
                }
            } else {
                logger.warning("Found REF unit lacking ability \"model.ability.refUnit\": "
                               + unit.toString());
            }
        }
        //updateSpaceAndCapacity();
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
     * Initiates a new <code>Monarch</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Monarch(Game game, String id) {
        super(game, id);
    }

    /**
     * Update the space and capacity variables.
     */
    private void updateSpaceAndCapacity() {
        Specification spec = getSpecification();
        capacity = 0;
        for (AbstractUnit nu : navalUnits) {
            if (nu.getUnitType(spec).canCarryUnits()) {
                capacity += nu.getUnitType(spec).getSpace() * nu.getNumber();
            }
        }
        spaceRequired = 0;
        for (AbstractUnit lu : landUnits) {
            spaceRequired += lu.getUnitType(spec).getSpaceTaken() * lu.getNumber();
        }
    }

    /**
     * Gets the sea support status.
     *
     * @return Gets the sea support status.
     */
    public boolean getSupportSea() {
        return supportSea;
    }

    /**
     * Sets the sea support status.
     *
     * @param supportSea The new sea support status.
     */
    public void setSupportSea(boolean supportSea) {
        this.supportSea = supportSea;
    }

    /**
     * Gets the displeasure status.
     *
     * @return Gets the displeasure status.
     */
    public boolean getDispleasure() {
        return displeasure;
    }

    /**
     * Sets the displeasure status.
     *
     * @param displeasure The new displeasure status.
     */
    public void setDispleasure(boolean displeasure) {
        this.displeasure = displeasure;
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
     * Gets the REF units.
     *
     * @return A list of all REF units.
     */
    public List<AbstractUnit> getREF() {
        List<AbstractUnit> result = new ArrayList<AbstractUnit>(landUnits);
        result.addAll(navalUnits);
        return result;
    }

    /**
     * Gets the REF naval units.
     *
     * @return A list of the REF naval units.
     */
    public List<AbstractUnit> getREFNavalUnits() {
        return navalUnits;
    }

    /**
     * Gets the REF land units.
     *
     * @return A list of the REF land units.
     */
    public List<AbstractUnit> getREFLandUnits() {
        return landUnits;
    }

    /**
     * Gets the maximum tax rate in this game.
     */
    private int taxMaximum() {
        return getSpecification().getIntegerOption("model.option.maximumTax")
            .getValue();
    }

    /**
     * Collects a list of potential enemies for this player.
     */
    public List<Player> collectPotentialEnemies() {
        List<Player> enemies = new ArrayList<Player>();
        // Benjamin Franklin puts an end to the monarch's interference
        if (!player.hasAbility("model.ability.ignoreEuropeanWars")) {
            for (Player enemy : getGame().getLiveEuropeanPlayers()) {
                if (enemy.isREF()) continue;
                switch (player.getStance(enemy)) {
                case PEACE: case CEASE_FIRE:
                    enemies.add(enemy);
                    break;
                }
            }
        }
        return enemies;
    }

    /**
     * Checks if a specified action is valid at present.
     *
     * @param action The <code>MonarchAction</code> to check.
     */
    public boolean actionIsValid(MonarchAction action) {
        switch (action) {
        case NO_ACTION:
            return true;
        case RAISE_TAX_ACT:
        case RAISE_TAX_WAR:
            return player.getTax() < taxMaximum();
        case FORCE_TAX:
            return false;
        case LOWER_TAX_WAR:
        case LOWER_TAX_OTHER:
            return player.getTax() > MINIMUM_TAX_RATE + 10;
        case WAIVE_TAX:
            return true;
        case ADD_TO_REF:
            return true;
        case DECLARE_WAR:
            return !collectPotentialEnemies().isEmpty();
        case SUPPORT_SEA:
            return player.getAttackedByPrivateers() && !getSupportSea()
                && !getDispleasure();
        case SUPPORT_LAND: case OFFER_MERCENARIES:
            return player.isAtWar() && !getDispleasure();
        case DISPLEASURE:
            return false;
        default:
            throw new IllegalArgumentException("Bogus monarch action: "
                                               + action);
        }
    }

    /**
     * Builds a weighted list of monarch actions.
     *
     * @return A weighted list of monarch actions.
     */
    public List<RandomChoice<MonarchAction>> getActionChoices() {
        List<RandomChoice<MonarchAction>> choices
            = new ArrayList<RandomChoice<MonarchAction>>();
        int dx = 1 + getSpecification()
            .getIntegerOption("model.option.monarchMeddling").getValue();
        int turn = getGame().getTurn().getNumber();
        int grace = (6 - dx) * 10; // 10-50

        // Nothing happens during the first few turns, if there are no
        // colonies, or after the revolution begins.
        if (turn < grace
            || player.getSettlements().size() == 0
            || player.getPlayerType() != PlayerType.COLONIAL) {
            return choices;
        }

        // The more time has passed, the less likely the monarch will
        // do nothing.
        addIfValid(choices, MonarchAction.NO_ACTION, Math.max(200 - turn, 100));
        addIfValid(choices, MonarchAction.RAISE_TAX_ACT, 5 + dx);
        addIfValid(choices, MonarchAction.RAISE_TAX_WAR, 5 + dx);
        addIfValid(choices, MonarchAction.LOWER_TAX_WAR, 5 - dx);
        addIfValid(choices, MonarchAction.LOWER_TAX_OTHER, 5 - dx);
        addIfValid(choices, MonarchAction.ADD_TO_REF, 10 + dx);
        addIfValid(choices, MonarchAction.DECLARE_WAR, 5 + dx);
        if (player.checkGold(MINIMUM_PRICE)) {
            addIfValid(choices, MonarchAction.OFFER_MERCENARIES, 6 - dx);
        } else if (dx < 3) {
            addIfValid(choices, MonarchAction.SUPPORT_LAND, 3 - dx);
        }
        addIfValid(choices, MonarchAction.SUPPORT_SEA, 6 - dx);

        return choices;
    }

    private void addIfValid(List<RandomChoice<MonarchAction>> choices,
                            MonarchAction action, int weight) {
        if (actionIsValid(action)) {
            choices.add(new RandomChoice<MonarchAction>(action, weight));
        }
    }

    /**
     * Calculates a tax raise.
     *
     * @param random The <code>Random</code> number source to use.
     * @return The new tax rate.
     */
    public int raiseTax(Random random) {
        int taxAdjustment = getSpecification()
            .getIntegerOption("model.option.taxAdjustment").getValue();
        int turn = getGame().getTurn().getNumber();
        int oldTax = player.getTax();
        int adjust = Math.max(1, (6 - taxAdjustment) * 10); // 20-60
        adjust = 1 + Utils.randomInt(logger, "Tax rise", random,
                                     5 + turn/adjust);
        return Math.min(oldTax + adjust, taxMaximum());
    }

    /**
     * Calculates a tax reduction.
     *
     * @param random The <code>Random</code> number source to use.
     * @return The new tax rate.
     */
    public int lowerTax(Random random) {
        int taxAdjustment = getSpecification()
            .getIntegerOption("model.option.taxAdjustment").getValue();
        int oldTax = player.getTax();
        int adjust = Math.max(1, 10 - taxAdjustment); // 5-10
        adjust = 1 + Utils.randomInt(logger, "Tax reduction", random, adjust);
        return Math.max(oldTax - adjust, Monarch.MINIMUM_TAX_RATE);
    }

    /**
     * Returns units to be added to the Royal Expeditionary Force.
     *
     * @param random The <code>Random</code> number source to use.
     * @return An addition to the Royal Expeditionary Force.
     */
    public List<AbstractUnit> chooseForREF(Random random) {
        ArrayList<AbstractUnit> result = new ArrayList<AbstractUnit>();
        // Preserve some extra naval capacity so that not all the REF
        // navy is completely loaded
        logger.info("Add to REF: capacity=" + capacity
                    + " spaceRequired=" + spaceRequired + " => "
                    + ((capacity < spaceRequired + 15) ? "naval" : "land")
                    + " unit");
        // TODO: magic number 2.5 * Manowar-capacity = 15
        if (capacity < spaceRequired + 15) {
            AbstractUnit unit = Utils.getRandomMember(logger, "Choose naval",
                navalUnits, random);
            result.add(new AbstractUnit(unit.getId(), unit.getRole(), 1));
        } else {
            AbstractUnit unit = Utils.getRandomMember(logger, "Choose land",
                    landUnits, random);
            result.add(new AbstractUnit(unit.getId(), unit.getRole(),
                    Utils.randomInt(logger, "Choose land#", random, 3) + 1));
        }
        return result;
    }

    /**
     * Adds units to the Royal Expeditionary Force.
     *
     * @param units The addition to the Royal Expeditionary Force.
     */
    public void addToREF(List<AbstractUnit> units) {
        Specification spec = getSpecification();
        for (AbstractUnit unitToAdd : units) {
            UnitType unitType = spec.getUnitType(unitToAdd.getId());
            int n = unitToAdd.getNumber();
            if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                for (AbstractUnit refUnit : navalUnits) {
                    if (refUnit.getId().equals(unitToAdd.getId())) {
                        refUnit.setNumber(refUnit.getNumber() + n);
                        if (unitType.canCarryUnits()) {
                            capacity += unitType.getSpace() * n;
                        }
                        break;
                    }
                }
            } else {
                for (AbstractUnit refUnit : landUnits) {
                    if (refUnit.getId().equals(unitToAdd.getId())
                        && refUnit.getRole().equals(unitToAdd.getRole())) {
                        refUnit.setNumber(refUnit.getNumber() + n);
                        spaceRequired += unitType.getSpaceTaken() * n;
                        break;
                    }
                }
            }
        }
        updateSpaceAndCapacity();
    }

    /**
     * Gets a additions to the colonial forces.
     *
     * @param random The <code>Random</code> number source to use.
     * @param naval If the addition should be a naval unit.
     * @return An addition to the colonial forces.
     */
    public List<AbstractUnit> getSupport(Random random, boolean naval) {
        Specification spec = getSpecification();
        List<AbstractUnit> support = new ArrayList<AbstractUnit>();
        List<UnitType> navalTypes = new ArrayList<UnitType>();
        List<UnitType> bombardTypes = new ArrayList<UnitType>();
        List<UnitType> mountedTypes = new ArrayList<UnitType>();
        for (UnitType unitType : spec.getUnitTypeList()) {
            if (unitType.hasAbility("model.ability.supportUnit")) {
                if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                    navalTypes.add(unitType);
                } else if (unitType.hasAbility(Ability.BOMBARD)) {
                    bombardTypes.add(unitType);
                } else if (unitType.hasAbility(Ability.CAN_BE_EQUIPPED)) {
                    mountedTypes.add(unitType);
                }
            }
        }
        if (naval) {
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose naval support", navalTypes, random),
                        Role.DEFAULT, 1));
            setSupportSea(true);
            return support;
        }

        /**
         * TODO: we should use a total strength value, and randomly
         * add individual units until that limit has been
         * reached. E.g. given a value of 10, we might select two
         * units with strength 5, or three units with strength 3 (plus
         * one with strength 1, if there is one).
         */
        int difficulty = spec.getInteger("model.option.monarchSupport");
        switch (difficulty) {
        case 4:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose bombard", bombardTypes, random),
                        Role.DEFAULT, 1));
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        Role.DRAGOON, 2));
            break;
        case 3:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        Role.DRAGOON, 2));
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose soldier", mountedTypes, random),
                        Role.SOLDIER, 1));
            break;
        case 2:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        Role.DRAGOON, 2));
            break;
        case 1:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        Role.DRAGOON, 1));
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose soldier", mountedTypes, random),
                        Role.SOLDIER, 1));
            break;
        case 0:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose soldier",mountedTypes, random),
                        Role.SOLDIER, 1));
            break;
        default:
            break;
        }
        return support;
    }

    /**
     * Returns units available as mercenaries.
     *
     * @param random The <code>Random</code> number source to use.
     * @return A troop of mercenaries.
     */
    public List<AbstractUnit> getMercenaries(Random random) {
        Specification spec = getSpecification();
        List<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitType unitType : spec.getUnitTypeList()) {
            if (unitType.hasAbility("model.ability.mercenaryUnit")) {
                unitTypes.add(unitType);
            }
        }

        int mercPrice = spec.getIntegerOption("model.option.mercenaryPrice")
            .getValue();
        List<AbstractUnit> mercs = new ArrayList<AbstractUnit>();
        int price = 0;
        int limit = unitTypes.size();
        UnitType unitType = null;
        AbstractUnit au;
        for (int count = 0; count < limit; count++) {
            unitType = Utils.getRandomMember(logger, "Choose unit", unitTypes,
                                             random);
            if (unitType.hasAbility(Ability.CAN_BE_EQUIPPED)) {
                for (int number = 3; number > 0; number--) {
                    au = new AbstractUnit(unitType, Role.DRAGOON, number);
                    int newPrice = player.getPrice(au) * mercPrice / 100;
                    if (player.checkGold(price + newPrice)) {
                        mercs.add(au);
                        price += newPrice;
                        break;
                    }
                }
                for (int number = 3; number > 0; number--) {
                    au = new AbstractUnit(unitType, Role.SOLDIER, number);
                    int newPrice = player.getPrice(au) * mercPrice / 100;
                    if (player.checkGold(price + newPrice)) {
                        mercs.add(au);
                        price += newPrice;
                        break;
                    }
                }
            } else {
                for (int number = 3; number > 0; number--) {
                    au = new AbstractUnit(unitType, Role.DEFAULT, number);
                    int newPrice = player.getPrice(au) * mercPrice / 100;;
                    if (player.checkGold(price + newPrice)) {
                        mercs.add(au);
                        price += newPrice;
                        break;
                    }
                }
            }
            unitTypes.remove(unitType);
        }

        /* Try to always return something, even if it is not affordable */
        if (mercs.isEmpty() && unitType != null) {
            Role role = (unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? Role.SOLDIER
                : Role.DEFAULT;
            mercs.add(new AbstractUnit(unitType, role, 1));
        }
        return mercs;
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
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("player", this.player.getId());
        out.writeAttribute("name", name);
        out.writeAttribute("supportSea", String.valueOf(supportSea));
        out.writeAttribute("displeasure", String.valueOf(displeasure));

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
     *
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
        if (player == null) {
            player = new Player(getGame(), in.getAttributeValue(null, "player"));
        }
        name = getAttribute(in, "name", player.getNation().getRulerNameKey());
        supportSea = getAttribute(in, "supportSea", false);
        displeasure = getAttribute(in, "displeasure", false);

        navalUnits.clear();
        landUnits.clear();
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
        updateSpaceAndCapacity();

        // sanity check: we should be on the closing tag
        if (!in.getLocalName().equals(Monarch.getXMLElementTagName())) {
            logger.warning("Error parsing xml: expecting closing tag </"
                           + Monarch.getXMLElementTagName()
                           + "> found instead: " + in.getLocalName());
        }
    }

    /**
     * Partial writer, so that simple updates can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that simple updates can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    protected void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "monarch".
     */
    public static String getXMLElementTagName() {
        return "monarch";
    }
}
