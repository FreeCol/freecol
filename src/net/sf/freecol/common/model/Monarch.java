/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.option.UnitListOption;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * This class implements the player's monarch, whose functions prior
 * to the revolution include raising taxes, declaring war on other
 * European countries, and occasionally providing military support.
 */
public final class Monarch extends FreeColGameObject implements Named {

    private static final Logger logger = Logger.getLogger(Monarch.class.getName());

    /**
     * A group of units with a common origin and purpose.
     */
    public class Force {

        /** The number of land units in the REF. */
        private final List<AbstractUnit> landUnits = new ArrayList<>();

        /** The number of naval units in the REF. */
        private final List<AbstractUnit> navalUnits = new ArrayList<>();

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
                        + ability + "\": " + unit);
                }
            }
            updateSpaceAndCapacity();
        }


        public final int getSpaceRequired() {
            return spaceRequired;
        }

        public final int getCapacity() {
            return capacity;
        }

        /**
         * Update the space and capacity variables.
         */
        public final void updateSpaceAndCapacity() {
            final Specification spec = getSpecification();
            capacity = navalUnits.stream()
                .filter(nu -> nu.getType(spec).canCarryUnits())
                .mapToInt(nu -> nu.getType(spec).getSpace()
                    * nu.getNumber()).sum();
            spaceRequired = landUnits.stream()
                .mapToInt(lu -> lu.getType(spec).getSpaceTaken()
                    * lu.getNumber()).sum();
        }

        /**
         * Gets all units.
         *
         * @return A copy of the list of all units.
         */
        public final List<AbstractUnit> getUnits() {
            List<AbstractUnit> result = getLandUnits();
            result.addAll(getNavalUnits());
            return result;
        }

        /**
         * Gets the naval units.
         *
         * @return A copy of the list of the naval units.
         */
        public final List<AbstractUnit> getNavalUnits() {
            return AbstractUnit.deepCopy(navalUnits);
        }

        /**
         * Gets the land units.
         *
         * @return A list of the  land units.
         */
        public final List<AbstractUnit> getLandUnits() {
            return AbstractUnit.deepCopy(landUnits);
        }

        /**
         * Is this Force empty?
         *
         * @return True if there are no land or naval units.
         */
        public final boolean isEmpty() {
            return landUnits.isEmpty() && navalUnits.isEmpty();
        }

        /**
         * Adds units to this Force.
         *
         * @param au The addition to this Force.
         */
        public void add(AbstractUnit au) {
            final Specification spec = getSpecification();
            final UnitType unitType = au.getType(spec);
            final int n = au.getNumber();
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

        /**
         * Calculate the approximate offence power of this force.
         *
         * @param naval If true, consider only naval units, otherwise
         *     consider the land units.
         * @return The approximate offence power.
         */
        public double calculateStrength(boolean naval) {
            return AbstractUnit.calculateStrength(getSpecification(),
                (naval) ? navalUnits : landUnits);
        }

        // @compat 0.10.x
        public void fixOldREFRoles() {
            Iterator<AbstractUnit> aui = landUnits.iterator();
            List<AbstractUnit> todo = new ArrayList<>();
            while (aui.hasNext()) {
                AbstractUnit au = aui.next();
                if ("SOLDIER".equals(au.getRoleId())
                    || "model.role.soldier".equals(au.getRoleId())) {
                    au.setRoleId("model.role.infantry");
                    aui.remove();
                    todo.add(au);
                } else if ("DRAGOON".equals(au.getRoleId())
                    || "model.role.dragoon".equals(au.getRoleId())) {
                    au.setRoleId("model.role.cavalry");
                    aui.remove();
                    todo.add(au);
                }
            }
            while (!todo.isEmpty()) add(todo.remove(0));
        }
        // end @compat 0.10.x

                    
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
                        add(new AbstractUnit(xr));
                    }
                } else if (NAVAL_UNITS_TAG.equals(tag)) {
                    while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        add(new AbstractUnit(xr));
                    }
                } else {
                    logger.warning("Bogus Force tag: " + tag);
                }
            }
        }
    }

    /** The minimum price for a monarch offer of mercenaries. */
    public static final int MONARCH_MINIMUM_PRICE = 200;

    /** The minimum price for a Hessian offer of mercenaries. */
    public static final int HESSIAN_MINIMUM_PRICE = 5000;

    /**
     * The minimum tax rate (given in percentage) from where it
     * can be lowered.
     */
    public static final int MINIMUM_TAX_RATE = 20;

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
        MONARCH_MERCENARIES, 
        HESSIAN_MERCENARIES,
        DISPLEASURE;

        /**
         * Get a key for this action.
         *
         * @return A message key.
         */
        private String getKey() {
            return "monarch.action." + getEnumKey(this);
        }

        public String getTextKey() {
            return "model." + getKey() + ".text";
        }
        
        public String getYesKey() {
            return "model." + getKey() + ".yes";
        }

        public String getNoKey() {
            return "model." + getKey() + ".no";
        }

        public String getHeaderKey() {
            return "model." + getKey() + ".header";
        }
    }

    /**
     * The Royal Expeditionary Force, which the Monarch will send to
     * crush the player's rebellion.
     */
    private Force expeditionaryForce;

    /**
     * The Foreign Intervention Force, which some random country will
     * send to support the player's rebellion.
     */
    private Force interventionForce;

    
    // Caches.  Do not serialize.
    /** The naval unit types suitable for support actions. */
    private List<UnitType> navalTypes = null;
    /** The bombard unit types suitable for support actions. */
    private List<UnitType> bombardTypes = null;
    /** The land unit types suitable for support actions. */
    private List<UnitType> landTypes = null;
    /** The roles identifiers suitable for land units with support actions. */
    private Role mountedRole = null, armedRole = null,
        refMountedRole, refArmedRole;
    /** The land unit types suitable for mercenary support. */
    private List<UnitType> mercenaryTypes = null;
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
     */
    public Monarch(Game game, Player player) {
        super(game);

        if (player == null) {
            throw new IllegalStateException("player == null");
        }
        this.player = player;

        // The Forces rely on the spec, but in the client we have to
        // create a player(with monarch) before the spec arrives from
        // the server, so the Forces *must* be instantiated lazily.
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

        // The Forces rely on the spec, but in the client we have to
        // create a player(with monarch) before the spec arrives from
        // the server, so the Forces *must* be instantiated lazily.
    }


    /**
     * Get the force describing the REF.
     *
     * @return The REF <code>Force</code>.
     */
    public Force getExpeditionaryForce() {
        if (expeditionaryForce == null) {
            final Specification spec = getSpecification();
            expeditionaryForce = new Force((UnitListOption)spec.getOption(GameOptions.REF_FORCE), null);
        }
        return expeditionaryForce;
    }

    /**
     * Get the force describing the Intervention Force.
     *
     * @return The intervention <code>Force</code>.
     */
    public Force getInterventionForce() {
        if (interventionForce == null) {
            final Specification spec = getSpecification();
            interventionForce = new Force((UnitListOption)spec.getOption(GameOptions.INTERVENTION_FORCE), null);
        }
        return interventionForce;
    }

    /**
     * Gets the force describing the Mercenary Force.
     *
     * This is never updated, and directly derived from the spec.
     *
     * @return The mercenary <code>Force</code>.
     */
    public Force getMercenaryForce() {
        final Specification spec = getSpecification();
        return new Force((UnitListOption)spec.getOption(GameOptions.MERCENARY_FORCE), null);
    }

    /**
     * Get the war support force.
     *
     * This is never updated, and directly derived from the spec.
     *
     * @return The war support <code>Force</code>.
     */
    public Force getWarSupportForce() {
        final Specification spec = getSpecification();
        return new Force((UnitListOption)spec
            .getOption(GameOptions.WAR_SUPPORT_FORCE), null);
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
     * Cache the unit types and roles for support and mercenary offers.
     */
    private void initializeCaches() {
        if (navalTypes != null) return;
        final Specification spec = getSpecification();
        navalTypes = new ArrayList<>();
        bombardTypes = new ArrayList<>();
        landTypes = new ArrayList<>();
        mercenaryTypes = new ArrayList<>();
        navalREFUnitTypes = spec.getREFUnitTypes(true);
        landREFUnitTypes = spec.getREFUnitTypes(false);

        for (UnitType unitType : spec.getUnitTypeList()) {
            if (unitType.hasAbility(Ability.SUPPORT_UNIT)) {
                if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                    navalTypes.add(unitType);
                } else if (unitType.hasAbility(Ability.BOMBARD)) {
                    bombardTypes.add(unitType);
                } else if (unitType.hasAbility(Ability.CAN_BE_EQUIPPED)) {
                    landTypes.add(unitType);
                }
            }
            if (unitType.hasAbility(Ability.MERCENARY_UNIT)) {
                mercenaryTypes.add(unitType);
            }
        }
        for (Role r : spec.getMilitaryRoles()) {
            boolean ok = r.isAvailableTo(player, landTypes.get(0));
            boolean armed = r.hasAbility(Ability.ARMED);
            boolean mounted = r.hasAbility(Ability.MOUNTED);
            boolean ref = r.requiresAbility(Ability.REF_UNIT);
            if (armed && mounted) {
                if (ok && !ref && mountedRole == null) {
                    mountedRole = r;
                } else if (!ok && ref && refMountedRole == null) {
                    refMountedRole = r;
                }
            } else if (armed && !mounted) {
                if (ok && !ref && armedRole == null) {
                    armedRole = r;
                } else if (!ok && ref && refArmedRole == null) {
                    refArmedRole = r;
                }
            }
        }
        
    }

    /**
     * Collects a list of potential enemies for this player.
     *
     * @return A list of potential enemy <code>Player</code>s.
     */
    public List<Player> collectPotentialEnemies() {
        // Benjamin Franklin puts an end to the monarch's interference
        return (player.hasAbility(Ability.IGNORE_EUROPEAN_WARS))
            ? Collections.<Player>emptyList()
            : getGame().getLiveEuropeanPlayers(player).stream()
                .filter(p -> p.isPotentialEnemy(player))
                .collect(Collectors.toList());
    }

    /**
     * Collects a list of potential friends for this player.
     *
     * Do not apply Franklin, he stops wars, not peace.
     *
     * @return A list of potential friendly <code>Player</code>s.
     */
    public List<Player> collectPotentialFriends() {
        return getGame().getLiveEuropeanPlayers(player).stream()
            .filter(p -> p.isPotentialFriend(player))
            .collect(Collectors.toList());
    }

    /**
     * Checks if a specified action is valid at present.
     *
     * @param action The <code>MonarchAction</code> to check.
     * @return True if the action is valid.
     */
    public boolean actionIsValid(MonarchAction action) {
        initializeCaches();

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
            return !(navalREFUnitTypes.isEmpty() || landREFUnitTypes.isEmpty());
        case DECLARE_PEACE:
            return !collectPotentialFriends().isEmpty();
        case DECLARE_WAR:
            return !collectPotentialEnemies().isEmpty();
        case SUPPORT_SEA:
            return player.getAttackedByPrivateers() && !getSupportSea()
                && !getDispleasure();
        case SUPPORT_LAND: case MONARCH_MERCENARIES:
            return player.isAtWar() && !getDispleasure();
        case HESSIAN_MERCENARIES:
            return player.checkGold(HESSIAN_MINIMUM_PRICE);
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
        List<RandomChoice<MonarchAction>> choices = new ArrayList<>();
        int dx = 1 + spec.getInteger(GameOptions.MONARCH_MEDDLING);
        int turn = getGame().getTurn().getNumber();
        int grace = (6 - dx) * 10; // 10-50

        // Nothing happens during the first few turns, if there are no
        // colonies, or after the revolution begins.
        if (turn < grace
            || player.getSettlements().isEmpty()
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
        if (player.checkGold(MONARCH_MINIMUM_PRICE)) {
            addIfValid(choices, MonarchAction.MONARCH_MERCENARIES, 6-dx);
        } else if (dx < 3) {
            addIfValid(choices, MonarchAction.SUPPORT_LAND, 3 - dx);
        }
        addIfValid(choices, MonarchAction.SUPPORT_SEA, 6 - dx);
        addIfValid(choices, MonarchAction.HESSIAN_MERCENARIES, 6-dx);

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
            choices.add(new RandomChoice<>(action, weight));
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
        adjust = 1 + randomInt(logger, "Tax rise", random, 5 + turn/adjust);
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
        adjust = 1 + randomInt(logger, "Tax reduction", random, adjust);
        return Math.max(oldTax - adjust, Monarch.MINIMUM_TAX_RATE);
    }

    // @compat 0.10.5
    /**
     * Get a unit type for the REF navy.  Bugfix workaround.
     *
     * @return A naval REF unit type.
     */
    public UnitType getNavalREFUnitType() {
        initializeCaches();
        return navalREFUnitTypes.get(0);
    }
    // end @compat 0.10.5

    /**
     * Gets units to be added to the Royal Expeditionary Force.
     *
     * @param random The <code>Random</code> number source to use.
     * @return An addition to the Royal Expeditionary Force.
     */
    public AbstractUnit chooseForREF(Random random) {
        initializeCaches();

        final Specification spec = getSpecification();
        // Preserve some extra naval capacity so that not all the REF
        // navy is completely loaded
        // FIXME: magic number 2.5 * Manowar-capacity = 15
        Force ref = getExpeditionaryForce();
        boolean needNaval = ref.getCapacity()
            < ref.getSpaceRequired() + 15;
        List<UnitType> types = (needNaval) ? navalREFUnitTypes
            : landREFUnitTypes;
        if (types.isEmpty()) return null;
        UnitType unitType = getRandomMember(logger, "Choose REF unit",
                                            types, random);
        Role role = (needNaval
            || !unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
            ? spec.getDefaultRole()
            : (randomInt(logger, "Choose land role", random, 3) == 0)
            ? refMountedRole
            : refArmedRole;
        int number = (needNaval) ? 1
            : randomInt(logger, "Choose land#", random, 3) + 1;
        AbstractUnit result = new AbstractUnit(unitType, role.getId(), number);
        logger.info("Add to " + player.getDebugName()
            + " REF: capacity=" + ref.getCapacity()
            + " spaceRequired=" + ref.getSpaceRequired()
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
            Force ivf = getInterventionForce();
            int updates = getGame().getTurn().getNumber() / interventionTurns;
            for (AbstractUnit unit : ivf.getLandUnits()) {
                // add units depending on current turn
                int value = unit.getNumber() + updates;
                unit.setNumber(value);
            }
            ivf.updateSpaceAndCapacity();
            while (ivf.getCapacity() < ivf.getSpaceRequired()) {
                boolean progress = false;
                for (AbstractUnit ship : ivf.getNavalUnits()) {
                    // add ships until all units can be transported at once
                    if (ship.getType(spec).canCarryUnits()
                        && ship.getType(spec).getSpace() > 0) {
                        int value = ship.getNumber() + 1;
                        ship.setNumber(value);
                        progress = true;
                    }
                }
                if (!progress) break;
                ivf.updateSpaceAndCapacity();
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
        initializeCaches();

        final Specification spec = getSpecification();
        List<AbstractUnit> support = new ArrayList<>();
        
        if (naval) {
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose naval support", navalTypes, random),
                        Specification.DEFAULT_ROLE_ID, 1));
            setSupportSea(true);
            return support;
        }

        /**
         * FIXME: we should use a total strength value, and randomly
         * add individual units until that limit has been
         * reached. E.g. given a value of 10, we might select two
         * units with strength 5, or three units with strength 3 (plus
         * one with strength 1, if there is one).
         */
        int difficulty = spec.getInteger(GameOptions.MONARCH_SUPPORT);
        switch (difficulty) {
        case 4:
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose bombard", bombardTypes, random),
                        Specification.DEFAULT_ROLE_ID, 1));
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose mounted", landTypes, random),
                        mountedRole.getId(), 2));
            break;
        case 3:
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose mounted", landTypes, random),
                        mountedRole.getId(), 2));
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose soldier", landTypes, random),
                        armedRole.getId(), 1));
            break;
        case 2:
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose mounted", landTypes, random),
                        mountedRole.getId(), 2));
            break;
        case 1:
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose mounted", landTypes, random),
                        mountedRole.getId(), 1));
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose soldier", landTypes, random),
                        armedRole.getId(), 1));
            break;
        case 0:
            support.add(new AbstractUnit(getRandomMember(logger,
                        "Choose soldier", landTypes, random),
                        armedRole.getId(), 1));
            break;
        default:
            break;
        }
        return support;
    }

    /**
     * Check if the monarch provides support for a war.
     *
     * @param enemy The enemy <code>Player</code>.
     * @param random A pseudo-random number source.
     * @return A list of <code>AbstractUnit</code>s provided as support.
     */
    public List<AbstractUnit> getWarSupport(Player enemy, Random random) {
        final Specification spec = getSpecification();
        final double baseStrength = player.calculateStrength(false);
        final double enemyStrength = enemy.calculateStrength(false);
        final double strengthRatio
            = Player.strengthRatio(baseStrength, enemyStrength);
        List<AbstractUnit> result = new ArrayList<AbstractUnit>();
        // We do not really know what Col1 did to decide whether to
        // provide war support, so we have made something up.
        //
        // Strength ratios are in [0, 1].  Support is granted in negative
        // proportion to the base strength ratio, such that we always
        // support if the enemy is stronger (ratio < 0.5), and never
        // support if the player is more than 50% stronger (ratio >
        // 0.6).  However if war support force sufficiently large with
        // respect to the current player and enemy forces it will be
        // reduced.
        // The principle at work is that the Crown is cautious/stingy.
        final double NOSUPPORT = 0.6;
        double p = 10.0 * (NOSUPPORT - strengthRatio);
        if (p >= 1.0 // Avoid calling randomDouble if unnecessary
            || (p > 0.0 && p > randomDouble(logger, "War support?", random))) {
            Force wsf = getWarSupportForce();
            result.addAll(wsf.getUnits());
            double supportStrength, fullRatio, strength, ratio;
            supportStrength = wsf.calculateStrength(false);
            fullRatio = Player.strengthRatio(baseStrength + supportStrength,
                                             enemyStrength);
            if (fullRatio < NOSUPPORT) { // Full support, some randomization
                for (AbstractUnit au : result) {
                    int amount = au.getNumber();
                    amount += randomInt(logger, "Vary war force " + au.getId(),
                                        random, 3) - 1;
                    au.setNumber(amount);
                }
            } else if (enemyStrength <= 0.0) { // Enemy is defenceless: 1 unit
                while (result.size() > 1) result.remove(0);
                result.get(0).setNumber(1);
            } else { // Reduce force until below NOSUPPORT or single unit/s
                outer: for (AbstractUnit au : result) {
                    for (int n = au.getNumber() - 1; n >= 1; n--) {
                        au.setNumber(n);
                        strength = AbstractUnit.calculateStrength(spec, result);
                        ratio = Player.strengthRatio(baseStrength + strength,
                                                     enemyStrength);
                        if (ratio < NOSUPPORT) break outer;
                    }
                }
            }
            strength = AbstractUnit.calculateStrength(spec, result);
            ratio = Player.strengthRatio(baseStrength+strength, enemyStrength);
            logger.finest("War support:"
                + " initially=" + supportStrength + "/" + fullRatio
                + " finally=" + strength + "/" + ratio);
        }
        return result;
    }
        
    /**
     * Gets some units available as mercenaries.
     *
     * @param random The <code>Random</code> number source to use.
     * @return A troop of mercenaries.
     */
    public List<AbstractUnit> getMercenaries(Random random) {
        initializeCaches();

        final Specification spec = getSpecification();
        final Role defaultRole = spec.getDefaultRole();
        final int mercPrice = spec.getInteger(GameOptions.MERCENARY_PRICE);
        List<Role> landRoles = new ArrayList<>();
        landRoles.add(armedRole);
        landRoles.add(mountedRole);

        // FIXME: magic numbers for 2-4 mercs
        List<AbstractUnit> mercs = new ArrayList<>();
        int count = randomInt(logger, "Mercenary count", random, 2) + 2;
        int price = 0;
        UnitType unitType = null;
        List<UnitType> unitTypes = new ArrayList<>(mercenaryTypes);
        while (!unitTypes.isEmpty() && count > 0) {
            unitType = getRandomMember(logger, "Merc unit",
                                       unitTypes, random);
            unitTypes.remove(unitType);
            Role role = (unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? getRandomMember(logger, "Merc role",
                                  landRoles, random)
                : defaultRole;
            int n = randomInt(logger, "Merc count " + unitType,
                              random, Math.min(count, 2)) + 1;
            AbstractUnit au = new AbstractUnit(unitType, role.getId(), n);
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

        /* Always return something, even if it is not affordable */
        if (mercs.isEmpty() && unitType != null) {
            Role r = (unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? armedRole : defaultRole;
            mercs.add(new AbstractUnit(unitType, r.getId(), 1));
        }
        return mercs;
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return this.player.getNation().getRulerNameKey();
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        // @compat 0.10.x
        // Detects/fixes bogus expeditionary force roles
        List<AbstractUnit> todo = new ArrayList<>();
        Force ref = getExpeditionaryForce();
        Iterator<AbstractUnit> it = ref.getLandUnits()
            .iterator();
        while (it.hasNext()) {
            AbstractUnit au = it.next();
            if ("model.role.soldier".equals(au.getRoleId())) {
                if (fix) {
                    au.setRoleId("model.role.infantry");
                    result = 0;
                    it.remove();
                    todo.add(au);
                } else {
                    return -1;
                }
            }
            if ("model.role.dragoon".equals(au.getRoleId())) {
                if (fix) {
                    au.setRoleId("model.role.cavalry");
                    result = 0;
                    it.remove();
                    todo.add(au);
                } else {
                    return -1;
                }
            }
        }
        for (AbstractUnit au : todo) ref.add(au);
        // end @compat 0.10.x
        return result;
    }


    // Serialization

    private static final String DISPLEASURE_TAG = "displeasure";
    private static final String EXPEDITIONARY_FORCE_TAG = "expeditionaryForce";
    private static final String INTERVENTION_FORCE_TAG = "interventionForce";
    private static final String PLAYER_TAG = "player";
    private static final String SUPPORT_SEA_TAG = "supportSea";
    // @compat 0.11.1
    private static final String NAME_TAG = "name";
    // end @compat 0.11.1
    // @compat 0.11.5
    private static final String MERCENARY_FORCE_TAG = "mercenaryForce";
    // end @compat 0.11.5


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(PLAYER_TAG, this.player);

        if (xw.validFor(this.player)) {

            xw.writeAttribute(SUPPORT_SEA_TAG, supportSea);

            xw.writeAttribute(DISPLEASURE_TAG, displeasure);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(this.player)) {

            getExpeditionaryForce().toXML(xw, EXPEDITIONARY_FORCE_TAG);

            getInterventionForce().toXML(xw, INTERVENTION_FORCE_TAG);
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

        supportSea = xr.getAttribute(SUPPORT_SEA_TAG, false);

        displeasure = xr.getAttribute(DISPLEASURE_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Provide dummy forces to read into.
        if (expeditionaryForce == null) expeditionaryForce = new Force();
        if (interventionForce == null) interventionForce = new Force();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (EXPEDITIONARY_FORCE_TAG.equals(tag)) {
            expeditionaryForce.readFromXML(xr);
            // @compat 0.11.3
            expeditionaryForce.fixOldREFRoles();
            // end @compat 0.11.3

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

        // @compat 0.11.5
        // Mercenary force is never updated, and lives in the spec now, so
        // just read and discard it.
        } else if (MERCENARY_FORCE_TAG.equals(tag)) {
            new Force().readFromXML(xr);
        // end @compat 0.11.5
            
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
    @Override
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
