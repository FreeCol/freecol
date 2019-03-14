/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Force;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.UnitListOption;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * This class implements the player's monarch, whose functions prior
 * to the revolution include raising taxes, declaring war on other
 * European countries, and occasionally providing military support.
 */
public final class Monarch extends FreeColGameObject implements Named {

    private static final Logger logger = Logger.getLogger(Monarch.class.getName());

    public static final String TAG = "monarch";

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
     * @param game The enclosing {@code Game}.
     * @param player The {@code Player} to create the
     *     {@code Monarch} for.
     */
    public Monarch(Game game, Player player) {
        super(game);

        if (player == null) {
            throw new RuntimeException("player == null: " + this);
        }
        this.player = player;

        // The Forces rely on the spec, but in the client we have to
        // create a player(with monarch) before the spec arrives from
        // the server, so the Forces *must* be instantiated lazily.
    }

    /**
     * Initiates a new {@code Monarch} with the given identifier.
     * The object should later be initialized by calling
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public Monarch(Game game, String id) {
        super(game, id);

        // The Forces rely on the spec, but in the client we have to
        // create a player(with monarch) before the spec arrives from
        // the server, so the Forces *must* be instantiated lazily.
    }


    /**
     * Get the owning player.
     *
     * Note: Monarchs are not and should not be Ownable.
     *
     * @return The {@code Player} associated with this monarch.
     */
    protected Player getPlayer() {
        return this.player;
    }

    /**
     * Get the force describing the REF.
     *
     * @return The REF {@code Force}.
     */
    public Force getExpeditionaryForce() {
        if (this.expeditionaryForce == null) {
            final Specification spec = getSpecification();
            this.expeditionaryForce = new Force(spec,
                spec.getUnitList(GameOptions.REF_FORCE), null);
        }
        return this.expeditionaryForce;
    }

    /**
     * Get the force describing the Intervention Force.
     *
     * @return The intervention {@code Force}.
     */
    public Force getInterventionForce() {
        if (this.interventionForce == null) {
            final Specification spec = getSpecification();
            this.interventionForce = new Force(spec,
                spec.getUnitList(GameOptions.INTERVENTION_FORCE), null);
        }
        return this.interventionForce;
    }

    /**
     * Gets the force describing the Mercenary Force.
     *
     * This is never updated, and directly derived from the spec.
     *
     * @return The mercenary {@code Force}.
     */
    public Force getMercenaryForce() {
        final Specification spec = getSpecification();
        return new Force(spec,
            spec.getUnitList(GameOptions.MERCENARY_FORCE), null);
    }

    /**
     * Get the war support force.
     *
     * This is never updated, and directly derived from the spec.
     *
     * @return The war support {@code Force}.
     */
    public Force getWarSupportForce() {
        final Specification spec = getSpecification();
        return new Force(spec,
            spec.getUnitList(GameOptions.WAR_SUPPORT_FORCE), null);
    }

    /**
     * Gets the sea support status.
     *
     * @return Gets the sea support status.
     */
    public boolean getSupportSea() {
        return this.supportSea;
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
        return this.displeasure;
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
        for (Role r : spec.getMilitaryRolesList()) {
            boolean ok = r.isAvailableTo(player, first(landTypes));
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
     * @return A list of potential enemy {@code Player}s.
     */
    public List<Player> collectPotentialEnemies() {
        // Benjamin Franklin puts an end to the monarch's interference
        return (player.hasAbility(Ability.IGNORE_EUROPEAN_WARS))
            ? Collections.<Player>emptyList()
            : transform(getGame().getLiveEuropeanPlayers(player),
                        p -> p.isPotentialEnemy(player));
    }

    /**
     * Collects a list of potential friends for this player.
     *
     * Do not apply Franklin, he stops wars, not peace.
     *
     * @return A list of potential friendly {@code Player}s.
     */
    public List<Player> collectPotentialFriends() {
        return transform(getGame().getLiveEuropeanPlayers(player),
                         p -> p.isPotentialFriend(player));
    }

    /**
     * Checks if a specified action is valid at present.
     *
     * @param action The {@code MonarchAction} to check.
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
            return !navalREFUnitTypes.isEmpty() && !landREFUnitTypes.isEmpty();
        case DECLARE_PEACE:
            return !collectPotentialFriends().isEmpty();
        case DECLARE_WAR:
            return !collectPotentialEnemies().isEmpty();
        case SUPPORT_SEA:
            return player.getAttackedByPrivateers() && !getSupportSea()
                && !getDispleasure();
        case SUPPORT_LAND: case MONARCH_MERCENARIES:
            return player.isAtWar() && !getDispleasure()
                && player.hasSettlements();
        case HESSIAN_MERCENARIES:
            return player.checkGold(HESSIAN_MINIMUM_PRICE)
                && player.hasSettlements();
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
            || !player.hasSettlements()
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
            addIfValid(choices, MonarchAction.MONARCH_MERCENARIES, 6 - dx);
        } else if (dx < 3) {
            addIfValid(choices, MonarchAction.SUPPORT_LAND, 3 - dx);
        }
        addIfValid(choices, MonarchAction.SUPPORT_SEA, 6 - dx);
        addIfValid(choices, MonarchAction.HESSIAN_MERCENARIES, 6 - dx);

        return choices;
    }

    /**
     * Convenience hack to check if an action is valid, and if so add
     * it to a choice list with a given weight.
     *
     * @param choices The list of choices.
     * @param action The {@code MonarchAction} to check.
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
     * @param random The {@code Random} number source to use.
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
     * @param random The {@code Random} number source to use.
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

    /**
     * Get a unit type for the REF navy.
     *
     * @return A naval REF unit type.
     */
    public UnitType getNavalREFUnitType() {
        initializeCaches();
        return first(navalREFUnitTypes);
    }

    /**
     * Add units to the Royal Expeditionary Force.
     *
     * @param random The {@code Random} number source to use.
     * @return An addition to the Royal Expeditionary Force.
     */
    public AbstractUnit addToREF(Random random) {
        initializeCaches();

        final Specification spec = getSpecification();
        Force ref = getExpeditionaryForce();
        AbstractUnit result;
        if ((double)ref.getCapacity() < ref.getSpaceRequired() * 1.1) {
            // Expand navy to +10% of required size to transport the land units
            // FIXME: Magic number
            List<UnitType> types = navalREFUnitTypes;
            if (types.isEmpty()) return null;
            result = new AbstractUnit(getRandomMember(logger, "Naval REF unit",
                                                      types, random),
                                      Specification.DEFAULT_ROLE_ID, 1);
        } else {
            List<UnitType> types = landREFUnitTypes;
            if (types.isEmpty()) return null;
            UnitType unitType = getRandomMember(logger, "Land REF unit",
                                                types, random);
            Role role = (!unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? spec.getDefaultRole()
                : (randomInt(logger, "Choose land role", random, 3) == 0)
                ? refMountedRole
                : refArmedRole;
            int number = randomInt(logger, "Choose land#", random, 3) + 1;
            result = new AbstractUnit(unitType, role.getId(), number);
        }
        ref.add(result);
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
     *
     * Called when the IVF is created.
     */
    public void updateInterventionForce() {
        final Specification spec = getSpecification();
        final int interventionTurns = spec.getInteger(GameOptions.INTERVENTION_TURNS);
        final int updates = getGame().getTurn().getNumber() / interventionTurns;
        Force ivf = getInterventionForce();
        if (interventionTurns > 0 && updates > 0) {
            for (AbstractUnit au : ivf.getLandUnitsList()) {
                // add units depending on current turn
                ivf.add(new AbstractUnit(au.getType(spec), au.getRoleId(),
                                         updates));
            }
            ivf.prepareToBoard();
        }
    }

    /**
     * Gets a additions to the colonial forces.
     *
     * @param random The {@code Random} number source to use.
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
     * @param enemy The enemy {@code Player}.
     * @param random A pseudo-random number source.
     * @return A list of {@code AbstractUnit}s provided as support.
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
            result.addAll(wsf.getUnitList());
            double supportStrength, fullRatio, strength, ratio;
            supportStrength = wsf.calculateStrength(false);
            fullRatio = Player.strengthRatio(baseStrength + supportStrength,
                                             enemyStrength);
            if (fullRatio < NOSUPPORT) { // Full support, some randomization
                for (AbstractUnit au : result) {
                    au.addToNumber(randomInt(logger, "Vary war force " + au.getId(),
                                             random, 3) - 1);
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
     * @param random The {@code Random} number source to use.
     * @param mercs A list to load with mercenary {@code AbstractUnit}s.
     * @return The price the monarch will ask or negative if nothing to offer.
     */
    public int loadMercenaries(Random random, List<AbstractUnit> mercs) {
        initializeCaches();
        mercs.clear();

        final Specification spec = getSpecification();
        final Role defaultRole = spec.getDefaultRole();
        final int mercPrice = spec.getInteger(GameOptions.MERCENARY_PRICE);
        List<Role> landRoles = new ArrayList<>();
        landRoles.add(armedRole);
        landRoles.add(mountedRole);

        // FIXME: magic numbers for 2-4 mercs
        int count = randomInt(logger, "Mercenary count", random, 2) + 2;
        int price = 0;
        UnitType unitType = null;
        List<UnitType> unitTypes = new ArrayList<>(mercenaryTypes);
        while (count > 0 && !unitTypes.isEmpty()) {
            unitType = getRandomMember(logger, "Merc unit",
                                       unitTypes, random);
            unitTypes.remove(unitType);
            Role role = (unitType.hasAbility(Ability.CAN_BE_EQUIPPED))
                ? getRandomMember(logger, "Merc role",
                                  landRoles, random)
                : defaultRole;
            int n = randomInt(logger, "Merc count " + unitType,
                              random, Math.min(count, 2)) + 1;
            AbstractUnit au = new AbstractUnit(unitType, role.getId(), 1);
            int newPrice = player.getEuropeanPurchasePrice(au);
            if (newPrice <= 0 || newPrice == INFINITY) break;
            newPrice *= mercPrice / 100;
            while (n > 0 && !player.checkGold(price + newPrice * n)) n--;
            if (n > 0) {
                au.setNumber(n);
                mercs.add(au);
                price += newPrice * n;
                count -= n;
            } else {
                unitTypes.remove(unitType);
            }
        }
        return price;
    }

    /**
     * Load the mercenary force, reduced to the point of being
     * affordable if possible.
     *
     * @param random The {@code Random} number source to use.
     * @param mercs A list to load with mercenary {@code AbstractUnit}s.
     * @return The price the monarch will ask, or negative if nothing to offer.
     */
    public int loadMercenaryForce(Random random, List<AbstractUnit> mercs) {
        initializeCaches();
        mercs.clear();

        mercs.addAll(getMercenaryForce().getUnitList());
        List<Integer> prices = new ArrayList<>(mercs.size());
        for (AbstractUnit au : mercs) {
            int price = player.getMercenaryHirePrice(au) / au.getNumber();
            prices.add(price);
        }
        int i = 0, mercPrice = 0;
        while (i < mercs.size()) {
            int price = prices.get(i);
            if (price <= 0 || price == INFINITY) {
                prices.remove(i);
                mercs.remove(i);
            } else {
                mercPrice += price * mercs.get(i).getNumber();
                i++;
            }
        }
        while (!mercs.isEmpty()) {
            if (player.checkGold(mercPrice)) return mercPrice;
            int r = randomInt(logger, "merc downsize", random, mercs.size());
            mercPrice -= prices.get(r);
            AbstractUnit au = mercs.get(r);
            if (au.getNumber() > 1) {
                au.addToNumber(-1);
            } else {
                prices.remove(r);
                mercs.remove(r);
            }
        }
        return -1;
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return this.player.getNation().getRulerNameKey();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Monarch o = copyInCast(other, Monarch.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.player = game.updateRef(o.getPlayer());
        this.supportSea = o.getSupportSea();
        this.displeasure = o.getDispleasure();
        this.expeditionaryForce = o.getExpeditionaryForce();
        this.interventionForce = o.getInterventionForce();
        return true;
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

            xw.writeAttribute(SUPPORT_SEA_TAG, this.supportSea);

            xw.writeAttribute(DISPLEASURE_TAG, this.displeasure);
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

        this.supportSea = xr.getAttribute(SUPPORT_SEA_TAG, false);

        this.displeasure = xr.getAttribute(DISPLEASURE_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        // Provide dummy forces to read into.
        if (this.expeditionaryForce == null) this.expeditionaryForce = new Force(spec);
        if (this.interventionForce == null) this.interventionForce = new Force(spec);

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (EXPEDITIONARY_FORCE_TAG.equals(tag)) {
            this.expeditionaryForce.readFromXML(xr);

        } else if (INTERVENTION_FORCE_TAG.equals(tag)) {
            this.interventionForce.readFromXML(xr);

        // @compat 0.11.5
        // Mercenary force is never updated, and lives in the spec now, so
        // just read and discard it.
        } else if (MERCENARY_FORCE_TAG.equals(tag)) {
            new Force(getSpecification()).readFromXML(xr);
        // end @compat 0.11.5

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
