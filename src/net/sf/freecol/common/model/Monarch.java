/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Player.PlayerType;
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

    /** Whether a frigate has been provided. */
    private boolean supportSea = false;

    /** Whether displeasure has been incurred. */
    private boolean displeasure = false;

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
        DECLARE_PEACE,
        DECLARE_WAR,
        SUPPORT_LAND,
        SUPPORT_SEA,
        OFFER_MERCENARIES, DISPLEASURE,
    }

    /**
     * The Royal Expeditionary Force, which the Monarch will send to
     * crush the player's rebellion.
     */
    private Force expeditionaryForce = new Force();

    /**
     * The Foreign Intervention Force, which some random country will
     * send to support the player's rebellion.
     */
    private Force interventionForce = new Force();

    /**
     * A force of mercenaries, which some random country will offer to
     * send to support the player's rebellion.
     */
    private Force mercenaryForce = new Force();

    /** The naval unit types suitable for the REF. */
    private List<UnitType> navalREFUnitTypes = null;
    /** The land unit types suitable for the REF. */
    private List<UnitType> landREFUnitTypes = null;


    /**
     * Constructor.
     *
     * @param game The enclosing <code>Game</code>.
     * @param player The <code>Player</code> to create the
     *     <code>Monarch</code> for.
     * @param name The name of the <code>Monarch</code>.
     */
    public Monarch(Game game, Player player, String name) {
        super(game);

        if (player == null) {
            throw new IllegalStateException("player == null");
        }

        this.player = player;
        this.name = name;

        final Specification spec = getSpecification();
        UnitListOption op;
        op = (UnitListOption)spec.getOption(GameOptions.REF_FORCE);
        expeditionaryForce = new Force(op, Ability.REF_UNIT);
        op = (UnitListOption)spec.getOption(GameOptions.INTERVENTION_FORCE);
        interventionForce = new Force(op, null);
    }

    /**
     * Initiates a new <code>Monarch</code> with the given identifier.
     * The object should later be initialized by calling
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Monarch(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the name key of this Monarch.
     *
     * @return The monarch name key.
     */
    public String getNameKey() {
        return name;
    }

    /**
     * Get the force describing the REF.
     *
     * @return The REF.
     */
    public Force getExpeditionaryForce() {
        return expeditionaryForce;
    }

    /**
     * Get the force describing the Intervention Force.
     *
     * @return The Intervention Force.
     */
    public Force getInterventionForce() {
        return interventionForce;
    }

    /**
     * Gets the force describing the Mercenary Force.
     *
     * @return The Mercenary Force.
     */
    public Force getMercenaryForce() {
        return interventionForce;
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
     * Gets the maximum tax rate in this game.
     *
     * @return The maximum tax rate in the game.
     */
    private int taxMaximum() {
        return getSpecification().getInteger(GameOptions.MAXIMUM_TAX);
    }

    /**
     * Collect the REF unit types.
     *
     * @param naval If true, choose naval unit types, if not, land
     *     unit types.
     * @return A list of possible REF unit types.
     */
    public List<UnitType> collectREFUnitTypes(boolean naval) {
        if (naval) {
            if (navalREFUnitTypes == null) {
                navalREFUnitTypes = getSpecification().getREFUnitTypes(true);
            }
            return navalREFUnitTypes;
        } else {
            if (landREFUnitTypes == null) {
                landREFUnitTypes = getSpecification().getREFUnitTypes(false);
            }
            return landREFUnitTypes;
        }
    }

    /**
     * Collects a list of potential enemies for this player.
     *
     * @return A list of potential enemy <code>Player</code>s.
     */
    public List<Player> collectPotentialEnemies() {
        List<Player> enemies = new ArrayList<Player>();
        // Benjamin Franklin puts an end to the monarch's interference
        if (!player.hasAbility(Ability.IGNORE_EUROPEAN_WARS)) {
            for (Player enemy : getGame().getLiveEuropeanPlayers()) {
                if (enemy.isREF()) continue;
                switch (player.getStance(enemy)) {
                case PEACE: case CEASE_FIRE:
                    if (!enemy.hasAbility(Ability.IGNORE_EUROPEAN_WARS)) {
                        enemies.add(enemy);
                    }
                    break;
                }
            }
        }
        return enemies;
    }

    /**
     * Collects a list of potential friends for this player.
     *
     * @return A list of potential friendly <code>Player</code>s.
     */
    public List<Player> collectPotentialFriends() {
        List<Player> friends = new ArrayList<Player>();
        // Benjamin Franklin puts an end to the monarch's interference
        if (!player.hasAbility(Ability.IGNORE_EUROPEAN_WARS)) {
            for (Player enemy : getGame().getLiveEuropeanPlayers()) {
                if (enemy.isREF()) continue;
                switch (player.getStance(enemy)) {
                case WAR: case CEASE_FIRE:
                    if (!enemy.hasAbility(Ability.IGNORE_EUROPEAN_WARS)) {
                        friends.add(enemy);
                    }
                    break;
                }
            }
        }
        return friends;
    }

    /**
     * Checks if a specified action is valid at present.
     *
     * @param action The <code>MonarchAction</code> to check.
     * @return True if the action is valid.
     */
    public boolean actionIsValid(MonarchAction action) {
        switch (action) {
        case NO_ACTION:
            return true;
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
            return player.getTax() < taxMaximum();
        case FORCE_TAX:
            return false;
        case LOWER_TAX_WAR: case LOWER_TAX_OTHER:
            return player.getTax() > MINIMUM_TAX_RATE + 10;
        case WAIVE_TAX:
            return true;
        case ADD_TO_REF:
            return !(collectREFUnitTypes(true).isEmpty()
                || collectREFUnitTypes(false).isEmpty());
        case DECLARE_PEACE:
            return !collectPotentialFriends().isEmpty();
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
        final Specification spec = getSpecification();
        List<RandomChoice<MonarchAction>> choices
            = new ArrayList<RandomChoice<MonarchAction>>();
        int dx = 1 + spec.getInteger(GameOptions.MONARCH_MEDDLING);
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
        addIfValid(choices, MonarchAction.DECLARE_PEACE, 6 - dx);
        addIfValid(choices, MonarchAction.DECLARE_WAR, 5 + dx);
        if (player.checkGold(MINIMUM_PRICE)) {
            addIfValid(choices, MonarchAction.OFFER_MERCENARIES, 6 - dx);
        } else if (dx < 3) {
            addIfValid(choices, MonarchAction.SUPPORT_LAND, 3 - dx);
        }
        addIfValid(choices, MonarchAction.SUPPORT_SEA, 6 - dx);

        return choices;
    }

    /**
     * Convenience hack to check if an action is valid, and if so add
     * it to a choice list with a given weight.
     *
     * @param choices The list of choices.
     * @param action The <code>MonarchAction</code> to check.
     * @param weight The weight to add the action with if valid.
     */
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
        final Specification spec = getSpecification();
        int taxAdjustment = spec.getInteger(GameOptions.TAX_ADJUSTMENT);
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
        final Specification spec = getSpecification();
        int taxAdjustment = spec.getInteger(GameOptions.TAX_ADJUSTMENT);
        int oldTax = player.getTax();
        int adjust = Math.max(1, 10 - taxAdjustment); // 5-10
        adjust = 1 + Utils.randomInt(logger, "Tax reduction", random, adjust);
        return Math.max(oldTax - adjust, Monarch.MINIMUM_TAX_RATE);
    }

    /**
     * Gets units to be added to the Royal Expeditionary Force.
     *
     * @param random The <code>Random</code> number source to use.
     * @return An addition to the Royal Expeditionary Force.
     */
    public AbstractUnit chooseForREF(Random random) {
        final Specification spec = getSpecification();
        // Preserve some extra naval capacity so that not all the REF
        // navy is completely loaded
        // TODO: magic number 2.5 * Manowar-capacity = 15
        boolean needNaval = expeditionaryForce.getCapacity()
            < expeditionaryForce.getSpaceRequired() + 15;
        List<UnitType> types = collectREFUnitTypes(needNaval);
        if (types.isEmpty()) return null;
        UnitType unitType = Utils.getRandomMember(logger, "Choose REF unit",
                                                  types, random);
        String roleId = (needNaval
            || !unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
            ? "model.role.default"
            : (Utils.randomInt(logger, "Choose land role", random, 2) == 0)
            ? "model.role.infantry"
            : "model.role.cavalry";
        int number = (needNaval) ? 1
            : Utils.randomInt(logger, "Choose land#", random, 3) + 1;
        AbstractUnit result = new AbstractUnit(unitType, roleId, number);
        logger.info("Add to REF: capacity=" + expeditionaryForce.getCapacity()
            + " spaceRequired=" + expeditionaryForce.getSpaceRequired()
            + " => " + ((needNaval) ? "naval" : "land") + " unit"
            + " => " + result);
        return result;
    }

    /**
     * Update the intervention force, adding land units depending on
     * turns passed, and naval units sufficient to transport all land
     * units.
     */
    public void updateInterventionForce() {
        Specification spec = getSpecification();
        int interventionTurns = spec.getInteger(GameOptions.INTERVENTION_TURNS);
        if (interventionTurns > 0) {
            int updates = getGame().getTurn().getNumber() / interventionTurns;
            for (AbstractUnit unit : interventionForce.getLandUnits()) {
                // add units depending on current turn
                int value = unit.getNumber() + updates;
                unit.setNumber(value);
            }
            interventionForce.updateSpaceAndCapacity();
            while (interventionForce.getCapacity() < interventionForce.getSpaceRequired()) {
                boolean progress = false;
                for (AbstractUnit ship : interventionForce.getNavalUnits()) {
                    // add ships until all units can be transported at once
                    if (ship.getType(spec).canCarryUnits()
                        && ship.getType(spec).getSpace() > 0) {
                        int value = ship.getNumber() + 1;
                        ship.setNumber(value);
                        progress = true;
                    }
                }
                if (!progress) break;
                interventionForce.updateSpaceAndCapacity();
            }
        }
    }

    /**
     * Gets a additions to the colonial forces.
     *
     * @param random The <code>Random</code> number source to use.
     * @param naval If the addition should be a naval unit.
     * @return An addition to the colonial forces.
     */
    public List<AbstractUnit> getSupport(Random random, boolean naval) {
        final Specification spec = getSpecification();
        List<AbstractUnit> support = new ArrayList<AbstractUnit>();
        List<UnitType> navalTypes = new ArrayList<UnitType>();
        List<UnitType> bombardTypes = new ArrayList<UnitType>();
        List<UnitType> mountedTypes = new ArrayList<UnitType>();
        for (UnitType unitType : spec.getUnitTypeList()) {
            if (unitType.hasAbility(Ability.SUPPORT_UNIT)) {
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
                        "model.role.default", 1));
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
        int difficulty = spec.getInteger(GameOptions.MONARCH_SUPPORT);
        switch (difficulty) {
        case 4:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose bombard", bombardTypes, random),
                        "model.role.default", 1));
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        "model.role.dragoon", 2));
            break;
        case 3:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        "model.role.dragoon", 2));
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose soldier", mountedTypes, random),
                        "model.role.soldier", 1));
            break;
        case 2:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        "model.role.dragoon", 2));
            break;
        case 1:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose mounted", mountedTypes, random),
                        "model.role.dragoon", 1));
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose soldier", mountedTypes, random),
                        "model.role.soldier", 1));
            break;
        case 0:
            support.add(new AbstractUnit(Utils.getRandomMember(logger,
                        "Choose soldier", mountedTypes, random),
                        "model.role.soldier", 1));
            break;
        default:
            break;
        }
        return support;
    }

    /**
     * Gets some units available as mercenaries.
     *
     * @param random The <code>Random</code> number source to use.
     * @return A troop of mercenaries.
     */
    public List<AbstractUnit> getMercenaries(Random random) {
        Specification spec = getSpecification();
        List<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitType unitType : spec.getUnitTypeList()) {
            if (unitType.hasAbility(Ability.MERCENARY_UNIT)) {
                unitTypes.add(unitType);
            }
        }

        int mercPrice = spec.getInteger(GameOptions.MERCENARY_PRICE);
        List<AbstractUnit> mercs = new ArrayList<AbstractUnit>();
        // FIXME: magic numbers for 2-4 mercs
        int count = Utils.randomInt(logger, "Mercenary count", random, 2) + 2;
        int price = 0;
        UnitType unitType = null;
        while (!unitTypes.isEmpty() && count > 0) {
            unitType = Utils.getRandomMember(logger, "Choose unit", unitTypes,
                                             random);
            unitTypes.remove(unitType);

            String[] roleIds = (unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? ((Utils.randomInt(logger, "Swap role", random, 2) == 0)
                    ? new String[] { "model.role.dragoon", "model.role.soldier" }
                    : new String[] { "model.role.soldier", "model.role.dragoon" })
                : new String[] { "model.role.default" };
            for (int r = 0; r < roleIds.length; r++) {
                int n = Utils.randomInt(logger, "Choose number " + unitType,
                                        random, Math.min(count, 2)) + 1;
                AbstractUnit au = new AbstractUnit(unitType, roleIds[r], n);
                for (;;) {
                    int newPrice = player.getPrice(au) * mercPrice / 100;
                    if (player.checkGold(price + newPrice)) {
                        mercs.add(au);
                        price += newPrice;
                        count -= n;
                        break;
                    }
                    if (--n <= 0) break;
                    au.setNumber(n);
                }
                if (count <= 0) break;
            }
        }

        /* Try to always return something, even if it is not affordable */
        if (mercs.isEmpty() && unitType != null) {
            String roleId = (unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? "model.role.soldier"
                : "model.role.default";
            mercs.add(new AbstractUnit(unitType, roleId, 1));
        }
        return mercs;
    }


    /**
     * A group of units with a common origin and purpose.
     */
    public class Force {

        /** The number of land units in the REF. */
        private final List<AbstractUnit> landUnits
            = new ArrayList<AbstractUnit>();

        /** The number of naval units in the REF. */
        private final List<AbstractUnit> navalUnits
            = new ArrayList<AbstractUnit>();

        // Internal variables that do not need serialization.
        /** The space required to transport all land units. */
        private int spaceRequired;

        /** The current naval transport capacity. */
        private int capacity;


        /**
         * Empty constructor.
         */
        public Force() {}

        /**
         * Create a new Force.
         *
         * @param option The <code>Option</code> defining the force.
         * @param ability An optional ability name required of the units
         *     in the force.
         */
        public Force(UnitListOption option, String ability) {
            final Specification spec = getSpecification();
            List<AbstractUnit> units = option.getOptionValues();
            for (AbstractUnit unit : units) {
                UnitType unitType = unit.getType(spec);
                if (ability == null || unitType.hasAbility(ability)) {
                    if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                        navalUnits.add(unit);
                    } else {
                        landUnits.add(unit);
                    }
                } else {
                    logger.warning("Found unit lacking required ability \""
                        + ability + "\": " + unit.toString());
                }
            }
            updateSpaceAndCapacity();
        }

        public int getSpaceRequired() {
            return spaceRequired;
        }

        public int getCapacity() {
            return capacity;
        }

        /**
         * Update the space and capacity variables.
         */
        public void updateSpaceAndCapacity() {
            final Specification spec = getSpecification();
            capacity = 0;
            for (AbstractUnit nu : navalUnits) {
                if (nu.getType(spec).canCarryUnits()) {
                    capacity += nu.getType(spec).getSpace() * nu.getNumber();
                }
            }
            spaceRequired = 0;
            for (AbstractUnit lu : landUnits) {
                spaceRequired += lu.getType(spec).getSpaceTaken() * lu.getNumber();
            }
        }

        /**
         * Gets all units.
         *
         * @return A list of all units.
         */
        public List<AbstractUnit> getUnits() {
            List<AbstractUnit> result = new ArrayList<AbstractUnit>(landUnits);
            result.addAll(navalUnits);
            return result;
        }

        /**
         * Gets the naval units.
         *
         * @return A list of the naval units.
         */
        public List<AbstractUnit> getNavalUnits() {
            return navalUnits;
        }

        /**
         * Gets the land units.
         *
         * @return A list of the  land units.
         */
        public List<AbstractUnit> getLandUnits() {
            return landUnits;
        }

        /**
         * Is this Force empty?
         *
         * @return True if there are no land or naval units.
         */
        public boolean isEmpty() {
            return landUnits.isEmpty() && navalUnits.isEmpty();
        }

        /**
         * Adds units to this Force.
         *
         * @param au The addition to this Force.
         */
        public void add(AbstractUnit au) {
            Specification spec = getSpecification();
            UnitType unitType = au.getType(spec);
            int n = au.getNumber();
            boolean added = false;
            if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                for (AbstractUnit refUnit : navalUnits) {
                    if (spec.getUnitType(refUnit.getId()) == unitType) {
                        refUnit.setNumber(refUnit.getNumber() + n);
                        if (unitType.canCarryUnits()) {
                            capacity += unitType.getSpace() * n;
                        }
                        added = true;
                        break;
                    }
                }
                if (!added) navalUnits.add(au);
            } else {
                for (AbstractUnit refUnit : landUnits) {
                    if (spec.getUnitType(refUnit.getId()) == unitType
                        && refUnit.getRoleId().equals(au.getRoleId())) {
                        refUnit.setNumber(refUnit.getNumber() + n);
                        spaceRequired += unitType.getSpaceTaken() * n;
                        added = true;
                        break;
                    }
                }
                if (!added) landUnits.add(au);
            }
            updateSpaceAndCapacity();
        }


        // Serialization

        public static final String LAND_UNITS_TAG = "landUnits";
        public static final String NAVAL_UNITS_TAG = "navalUnits";
        // @compat 0.10.5
        // public for now, revert to private
        // end @compat


        public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
            xw.writeStartElement(tag);

            xw.writeStartElement(NAVAL_UNITS_TAG);

            for (AbstractUnit unit : navalUnits) unit.toXML(xw);

            xw.writeEndElement();

            xw.writeStartElement(LAND_UNITS_TAG);

            for (AbstractUnit unit : landUnits) unit.toXML(xw);

            xw.writeEndElement();

            xw.writeEndElement();
        }

        public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
            // Clear containers.
            navalUnits.clear();
            landUnits.clear();

            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                final String tag = xr.getLocalName();

                if (LAND_UNITS_TAG.equals(tag)) {
                    while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        landUnits.add(new AbstractUnit(xr));
                    }
                } else if (NAVAL_UNITS_TAG.equals(tag)) {
                    while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        navalUnits.add(new AbstractUnit(xr));
                    }
                } else {
                    logger.warning("Bogus Force tag: " + tag);
                }
            }
        }
    }


    // Serialization

    private static final String DISPLEASURE_TAG = "displeasure";
    private static final String EXPEDITIONARY_FORCE_TAG = "expeditionaryForce";
    private static final String INTERVENTION_FORCE_TAG = "interventionForce";
    private static final String MERCENARY_FORCE_TAG = "mercenaryForce";
    private static final String NAME_TAG = "name";
    private static final String PLAYER_TAG = "player";
    private static final String SUPPORT_SEA_TAG = "supportSea";


    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(PLAYER_TAG, this.player);

        xw.writeAttribute(NAME_TAG, name);

        if (xw.validFor(this.player)) {

            xw.writeAttribute(SUPPORT_SEA_TAG, supportSea);

            xw.writeAttribute(DISPLEASURE_TAG, displeasure);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(this.player)) {

            expeditionaryForce.toXML(xw, EXPEDITIONARY_FORCE_TAG);

            interventionForce.toXML(xw, INTERVENTION_FORCE_TAG);

            mercenaryForce.toXML(xw, MERCENARY_FORCE_TAG);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        player = xr.findFreeColGameObject(getGame(), PLAYER_TAG,
                                          Player.class, (Player)null, true);

        name = xr.getAttribute(NAME_TAG, player.getNation().getRulerNameKey());

        supportSea = xr.getAttribute(SUPPORT_SEA_TAG, false);

        displeasure = xr.getAttribute(DISPLEASURE_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        // @compat 0.10.5
        // Intervention and mercenary forces introduced here.  Add
        // default definitions for the benefit of earlier versions.        
        final Specification spec = getSpecification();
        if (interventionForce.getUnits().isEmpty()) {
            interventionForce = new Force((UnitListOption)spec.getOption(GameOptions.INTERVENTION_FORCE), null);
        }
        if (mercenaryForce.getUnits().isEmpty()) {
            mercenaryForce = new Force((UnitListOption)spec.getOption(GameOptions.MERCENARY_FORCE), null);
        }
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (EXPEDITIONARY_FORCE_TAG.equals(tag)) {
            expeditionaryForce.readFromXML(xr);

        } else if (INTERVENTION_FORCE_TAG.equals(tag)) {
            interventionForce.readFromXML(xr);

        // @compat 0.10.5
        } else if (Force.LAND_UNITS_TAG.equals(tag)) {
            expeditionaryForce.getLandUnits().clear();
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                AbstractUnit newUnit = new AbstractUnit(xr);
                expeditionaryForce.getLandUnits().add(newUnit);
            }
        // end @compat

        } else if (MERCENARY_FORCE_TAG.equals(tag)) {
            mercenaryForce.readFromXML(xr);
            
        // @compat 0.10.5
        } else if (Force.NAVAL_UNITS_TAG.equals(tag)) {
            expeditionaryForce.getNavalUnits().clear();
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                AbstractUnit newUnit = new AbstractUnit(xr);
                expeditionaryForce.getNavalUnits().add(newUnit);
            }
        // end @compat

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "monarch".
     */
    public static String getXMLElementTagName() {
        return "monarch";
    }
}
