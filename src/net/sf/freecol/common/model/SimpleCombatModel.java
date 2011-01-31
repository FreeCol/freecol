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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.util.Utils;


/**
 * This class implements the original Colonization combat model.
 * Note that the damage part of any CombatResult is ignored throughout.
 */
public class SimpleCombatModel extends CombatModel {

    private static final Logger logger = Logger.getLogger(SimpleCombatModel.class.getName());

    // The maximum attack power of a Colony's fortifications against a
    // naval unit.
    public static final int MAXIMUM_BOMBARD_POWER = 48;

    public static final String SMALL_MOVEMENT_PENALTY
        = "model.modifier.smallMovementPenalty";
    public static final String BIG_MOVEMENT_PENALTY
        = "model.modifier.bigMovementPenalty";
    public static final String ARTILLERY_IN_THE_OPEN
        = "model.modifier.artilleryInTheOpen";
    public static final String ATTACK_BONUS
        = "model.modifier.attackBonus";
    public static final String FORTIFIED
        = "model.modifier.fortified";
    public static final String ARTILLERY_AGAINST_RAID
        = "model.modifier.artilleryAgainstRaid";
    public static final Modifier UNKNOWN_DEFENCE_MODIFIER
        = new Modifier("bogus", Modifier.UNKNOWN, Modifier.Type.ADDITIVE);


    public SimpleCombatModel() {}


    /**
     * Calculates the odds of success in combat.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The combat odds.
     */
    public CombatOdds calculateCombatOdds(FreeColGameObject attacker,
                                          FreeColGameObject defender) {
        if (attacker == null || defender == null) {
            return new CombatOdds(CombatOdds.UNKNOWN_ODDS);
        }

        float attackPower = getOffencePower(attacker, defender);
        float defencePower = getDefencePower(attacker, defender);
        if (attackPower == 0.0f && defencePower == 0.0f) {
            return new CombatOdds(CombatOdds.UNKNOWN_ODDS);
        }
        float victory = attackPower / (attackPower + defencePower);
        return new CombatOdds(victory);
    }

    /**
     * Get the offensive power of a unit attacking another.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The offensive power.
     */
    public float getOffencePower(FreeColGameObject attacker,
                                 FreeColGameObject defender) {
        float result = 0.0f;
        if (combatIsMeasurement(attacker, defender)
            || combatIsAttack(attacker, defender)
            || combatIsSettlementAttack(attacker, defender)) {
            result = FeatureContainer.applyModifierSet(0,
                    attacker.getGame().getTurn(),
                    getOffensiveModifiers(attacker, defender));

        } else if (combatIsBombard(attacker, defender)) {
            Settlement attackerSettlement = (Settlement) attacker;
            Unit defenderUnit = (Unit) defender;
            if (attackerSettlement.hasAbility("model.ability.bombardShips")) {
                for (Unit unit : attackerSettlement.getTile().getUnitList()) {
                    if (unit.hasAbility("model.ability.bombard")) {
                        result += unit.getType().getOffence();
                    }
                }
            }
            if (result > MAXIMUM_BOMBARD_POWER) result = MAXIMUM_BOMBARD_POWER;

        } else {
            throw new IllegalArgumentException("Bogus combat");
        }
        return result;
    }

    /**
     * Get the defensive power wrt an attacker.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The defensive power.
     */
    public float getDefencePower(FreeColGameObject attacker,
                                 FreeColGameObject defender) {
        float result;
        if (combatIsAttack(attacker, defender)
            || combatIsSettlementAttack(attacker, defender)) {
            result = FeatureContainer.applyModifierSet(0,
                    defender.getGame().getTurn(),
                    getDefensiveModifiers(attacker, defender));

        } else if (combatIsBombard(attacker, defender)) {
            result = ((Unit) defender).getType().getDefence();

        } else {
            throw new IllegalArgumentException("Bogus combat");
        }
        return result;
    }

    /**
     * Collect all the offensive modifiers that apply to an attack.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return All the applicable offensive modifiers.
     */
    public Set<Modifier> getOffensiveModifiers(FreeColGameObject attacker,
                                               FreeColGameObject defender) {
        Set<Modifier> result = new LinkedHashSet<Modifier>();
        if (combatIsMeasurement(attacker, defender)
            || combatIsAttack(attacker, defender)
            || combatIsSettlementAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            UnitType type = attackerUnit.getType();
            Specification spec = attackerUnit.getSpecification();
            result.add(new Modifier(Modifier.OFFENCE,
                                    Specification.BASE_OFFENCE_SOURCE,
                                    type.getOffence(),
                                    Modifier.Type.ADDITIVE));
            result.addAll(attackerUnit
                          .getModifierSet(Modifier.OFFENCE));
            if (defender instanceof Ownable) {
                result.addAll(attackerUnit
                              .getModifierSet(Modifier.OFFENCE_AGAINST,
                                              (Ownable) defender));
            }
            if (attackerUnit.isNaval()) {
                addNavalOffensiveModifiers(attackerUnit, result);
            } else {
                addLandOffensiveModifiers(attacker, defender, result);
            }

        } else if (combatIsBombard(attacker, defender)) {
            result.add(new Modifier("model.modifier.bombardModifier",
                                    getOffencePower(attacker, defender),
                                    Modifier.Type.ADDITIVE));

        } else {
            throw new IllegalArgumentException("Bogus combat");
        }
        return result;
    }

    /**
     * Add all the offensive modifiers that apply to a naval attack.
     *
     * @param attacker The attacker.
     * @param result The set of modifiers to add to.
     */
    private void addNavalOffensiveModifiers(Unit attacker,
                                            Set<Modifier> result) {
        int goodsCount = attacker.getGoodsCount();
        if (goodsCount > 0) {
            // Penalty for every unit of cargo.
            // TODO: shouldn't this be -cargo/capacity?
            // TODO: magic number to spec
            result.add(new Modifier(Modifier.OFFENCE,
                                    Specification.CARGO_PENALTY_SOURCE,
                                    -12.5f * goodsCount,
                                    Modifier.Type.PERCENTAGE));
        }
    }

    /**
     * Add all the offensive modifiers that apply to a land attack.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @param result The set of modifiers to add to.
     */
    private void addLandOffensiveModifiers(FreeColGameObject attacker,
                                           FreeColGameObject defender,
                                           Set<Modifier> result) {
        Unit attackerUnit = (Unit) attacker;
        Specification spec = attackerUnit.getSpecification();
        // Equipment bonuses
        TypeCountMap<EquipmentType> equip = attackerUnit.getEquipment();
        if (equip != null) {
            for (EquipmentType et : equip.keySet()) {
                result.addAll(et.getModifierSet(Modifier.OFFENCE));
            }
        }
        // Attack bonus
        result.addAll(spec.getModifiers(ATTACK_BONUS));
        // Movement penalty
        int movesLeft = attackerUnit.getMovesLeft();
        if (movesLeft == 1) {
            result.addAll(spec.getModifiers(BIG_MOVEMENT_PENALTY));
        } else if (movesLeft == 2) {
            result.addAll(spec.getModifiers(SMALL_MOVEMENT_PENALTY));
        }

        if (combatIsSettlementAttack(attacker, defender)) {
            // Settlement present, REF bombardment bonus
            result.addAll(attackerUnit
                          .getModifierSet("model.modifier.bombardBonus"));
        } else if (defender != null && ((Unit) defender).getTile() != null) {
            Unit defenderUnit = (Unit) defender;
            if (defenderUnit.getSettlement() != null) {
                result.addAll(attackerUnit
                              .getModifierSet("model.modifier.bombardBonus"));
            } else {
                // Ambush bonus in the open = defender's defence
                // bonus, if defender is REF, or attacker is indian.
                if (isAmbush(attacker, defender)) {
                    for (Modifier mod : defenderUnit.getTile().getType()
                             .getModifierSet(Modifier.DEFENCE)) {
                        Modifier modifier = new Modifier(mod);
                        modifier.setId(Modifier.OFFENCE);
                        modifier.setSource(Specification.AMBUSH_BONUS_SOURCE);
                        result.add(modifier);
                    }
                }
                // Artillery in the open penalty
                if (attackerUnit.hasAbility("model.ability.bombard")
                    && attackerUnit.getSettlement() == null) {
                    result.addAll(spec.getModifiers(ARTILLERY_IN_THE_OPEN));
                }
            }
        }
    }

    /**
     * Collect all defensive modifiers when defending against an attack.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return All the applicable defensive modifiers.
     */
    public Set<Modifier> getDefensiveModifiers(FreeColGameObject attacker,
                                               FreeColGameObject defender) {
        Set<Modifier> result = new LinkedHashSet<Modifier>();
        if (combatIsAttack(attacker, defender)) {
            Unit defenderUnit = (Unit) defender;
            result.add(new Modifier(Modifier.DEFENCE,
                                    Specification.BASE_DEFENCE_SOURCE,
                                    defenderUnit.getType().getDefence(),
                                    Modifier.Type.ADDITIVE));
            result.addAll(defenderUnit.getType()
                          .getModifierSet(Modifier.DEFENCE));
            if (defenderUnit.isNaval()) {
                addNavalDefensiveModifiers(defender, result);
            } else {
                addLandDefensiveModifiers(attacker, defender, result);
            }

        } else if (combatIsSettlementAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            UnitType type = attackerUnit.getType();
            Settlement settlement = (Settlement) defender;
            Player defenderPlayer = settlement.getOwner();
            if (settlement.getFeatureContainer() == null) {
                // Client can not see inside the settlement
                result.add(UNKNOWN_DEFENCE_MODIFIER);
            } else {
                result.addAll(settlement.getFeatureContainer()
                              .getModifierSet(Modifier.DEFENCE, type));
            }

        } else if (combatIsBombard(attacker, defender)) {
            Unit defenderUnit = (Unit) defender;
            result.add(new Modifier("model.modifier.defenceBonus",
                                    defenderUnit.getType().getDefence(),
                                    Modifier.Type.ADDITIVE));

        } else {
            throw new IllegalArgumentException("Bogus combat");
        }
        return result;
    }

    /**
     * Add all the defensive modifiers that apply to a naval attack.
     *
     * @param defender The defender.
     * @param result The set of modifiers to add to.
     */
    private void addNavalDefensiveModifiers(FreeColGameObject defender,
                                            Set<Modifier> result) {
        Unit defenderUnit = (Unit) defender;
        int goodsCount = defenderUnit.getVisibleGoodsCount();
        if (goodsCount > 0) {
            // Penalty for every unit of cargo.
            // TODO: should this be -cargo/capacity?
            result.add(new Modifier(Modifier.DEFENCE,
                                    Specification.CARGO_PENALTY_SOURCE,
                                    -12.5f * goodsCount,
                                    Modifier.Type.PERCENTAGE));
        }
    }

    /**
     * Add all the defensive modifiers that apply to a land attack.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @param result The set of modifiers to add to.
     */
    private void addLandDefensiveModifiers(FreeColGameObject attacker,
                                           FreeColGameObject defender,
                                           Set<Modifier> result) {
        Unit attackerUnit = (Unit) attacker;
        Unit defenderUnit = (Unit) defender;
        Specification spec = defender.getSpecification();
        // Auto-equip and equipment bonuses.
        TypeCountMap<EquipmentType> equip = defenderUnit.getEquipment();
        if (equip != null) {
            for (EquipmentType et : equip.keySet()) {
                result.addAll(et.getModifierSet(Modifier.DEFENCE));
            }
        }
        equip = defenderUnit.getAutomaticEquipment();
        if (equip != null) {
            for (EquipmentType et : equip.keySet()) {
                result.addAll(et.getModifierSet(Modifier.DEFENCE));
            }
        }
        // Fortify bonus
        if (defenderUnit.getState() == UnitState.FORTIFIED) {
            result.addAll(spec.getModifiers(FORTIFIED));
        }
        Tile tile = defenderUnit.getTile();
        if (tile != null) {
            if (tile.getSettlement() == null) { // In the open
                // Terrain defensive bonus.
                if (!isAmbush(attacker, defender)) {
                    result.addAll(tile.getType().getDefenceBonus());
                }
                // Artillery in the Open penalty
                if (defenderUnit.hasAbility("model.ability.bombard")
                    && defenderUnit.getState() != UnitState.FORTIFIED) {
                    result.addAll(spec.getModifiers(ARTILLERY_IN_THE_OPEN));
                }
            } else { // In settlement
                result.addAll(tile.getSettlement().getModifierSet(Modifier.DEFENCE));
                // Artillery defence bonus against an Indian raid
                if (defenderUnit.hasAbility("model.ability.bombard")
                    && attackerUnit.getOwner().isIndian()) {
                    result.addAll(spec.getModifiers(ARTILLERY_AGAINST_RAID));
                }
            }
        }
    }

    /**
     * Generates a result of a unit attacking.
     * Takes care to only call the pseudo-random source *once*.
     *
     * @param random A pseudo-random number source.
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The results of the combat.
     */
    public List<CombatResult> generateAttackResult(Random random,
                                                   FreeColGameObject attacker,
                                                   FreeColGameObject defender) {
        ArrayList<CombatResult> crs = new ArrayList<CombatResult>();
        CombatOdds odds = calculateCombatOdds(attacker, defender);
        float r = random.nextFloat();
        boolean great = false; // Great win or loss?
        String action;

        if (combatIsAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            Unit defenderUnit = (Unit) defender;
            action = "Attack";

            // For random float 0 <= r < 1.0:
            // Partition this range into wins < odds.win and losses above.
            // Within the 0 <= r < odds.win range, partition the first 10%
            // to be great wins and the rest to be ordinary wins.
            //   r < 0.1 * odds.win  => great win
            //   else r < odds.win   => win
            // Within the odds.win <= r < 1.0 range, partition the first
            // 20% to be evasions (if defender has the evadeAttack ability),
            // the next 70% to be ordinary losses, and the rest great losses.
            //   r < odds.win + 0.2 * (1.0 - odds.win) = 0.8 * odds.win + 0.2
            //     => evade
            //   else r < odds.win + (0.2 + 0.7) * (1.0 - odds.win)
            //     = 0.1 * odds.win + 0.9 => loss
            //   else => great loss
            // ...and beached ships always lose.
            if (r < odds.win || isBeached(defenderUnit)) {
                great = r < 0.1f * odds.win; // Great Win
                crs.add(CombatResult.WIN);
                r /= 0.1f * odds.win; // Rescale to 0 <= r < 1
                resolveAttack(attackerUnit, defenderUnit, great, r, crs);
            } else if (r < 0.8f * odds.win + 0.2f
                    && defenderUnit.hasAbility("model.ability.evadeAttack")) {
                crs.add(CombatResult.NO_RESULT);
                crs.add(CombatResult.EVADE_ATTACK);
            } else {
                great = r >= 0.1f * odds.win + 0.9f; // Great Loss
                crs.add(CombatResult.LOSE);
                // Rescale to 0 <= r < 1
                //   (by rearranging: 0.8 * odds.win + 0.2 <= r < 1.0)
                r = (1.25f * r - 0.25f - odds.win) / (1.0f - odds.win);
                resolveAttack(defenderUnit, attackerUnit, great, r, crs);
            }

        } else if (combatIsBombard(attacker, defender)) {
            Unit defenderUnit = (Unit) defender;
            if (!defenderUnit.isNaval()) {
                // One day we might want:
                //   crs.add(CombatResult.SLAUGHTER_UNIT_BOMBARD);
                throw new IllegalStateException("Bombard of non-naval");
            }
            action = "Bombard";

            // The bombard succeeds.
            if (r <= odds.win) {
                crs.add(CombatResult.WIN);

                // Great wins occur at most in 1 in 3 of successful bombards,
                // Good defences reduce this proportion.
                float offencePower = getOffencePower(attacker, defender);
                float defencePower = getDefencePower(attacker, defender);
                float diff = Math.max(3f, defencePower * 2f - offencePower);
                great = r < odds.win / diff;

                // Sink the defender on great wins or lack of repair
                // location, otherwise just damage.
                if (great || defenderUnit.getRepairLocation() == null) {
                    crs.add(CombatResult.SINK_SHIP_BOMBARD);
                } else {
                    crs.add(CombatResult.DAMAGE_SHIP_BOMBARD);
                }

            // The bombard fails but this is not a win for the
            // defender, just an evasion, as it is not currently given
            // an opportunity to return fire.
            } else {
                crs.add(CombatResult.NO_RESULT);
                crs.add(CombatResult.EVADE_BOMBARD);
            }

        } else {
            throw new IllegalStateException("Bogus combat");
        }

        // Log the results so that we have a solid record of combat
        // determinations for debugging and investigation of user
        // `I just lost N combats' complaints.
        List<String> results = new ArrayList<String>();
        for (CombatResult cr : crs) results.add(cr.toString());
        logger.info(attacker.toString() + " " + action
                    + " " + defender.toString()
                    + ": victory=" + Float.toString(odds.win)
                    + " random(1.0) = " + Float.toString(r)
                    + " great=" + Boolean.toString(great)
                    + " => " + Utils.join(" ", results));
        return crs;
    }

    /**
     * Resolve all the consequences of a normal attack.
     *
     * @param winner The winning <code>Unit</code>.
     * @param loser The losing <code>Unit</code>.
     * @param great True if this is a great win/loss.
     * @param r A "residual" random value (for convert/burn mission).
     * @param crs A list of <code>CombatResult</code>s to add to.
     */
    private void resolveAttack(Unit winner, Unit loser, boolean great,
                               float r, List<CombatResult> crs) {
        Player loserPlayer = loser.getOwner();
        Tile tile = loser.getTile();
        Player winnerPlayer = winner.getOwner();
        boolean attackerWon = crs.get(0) == CombatResult.WIN;

        if (loser.isNaval()) {
            // Naval victors get to loot the defenders hold.  Sink the
            // loser on great win/loss, lack of repair location, or
            // beached.
            if (winner.isNaval() && winner.canCaptureGoods()
                && !loser.getGoodsList().isEmpty()) {
                crs.add(CombatResult.LOOT_SHIP);
            }
            if (great || loser.getRepairLocation() == null
                || isBeached(loser)) {
                crs.add(CombatResult.SINK_SHIP_ATTACK);
            } else {
                crs.add(CombatResult.DAMAGE_SHIP_ATTACK);
            }

        } else { // loser is land unit
            Settlement settlement = tile.getSettlement();
            EquipmentType autoEquip = null;
            EquipmentType equip = null;
            boolean loserWasUnarmed = !loser.isDefensiveUnit();

            // Autoequip the defender due to Revere?
            if (settlement instanceof Colony) {
                autoEquip = loser.getBestCombatEquipmentType(loser
                        .getAutomaticEquipment());
                if (autoEquip != null) {
                    crs.add(CombatResult.AUTOEQUIP_UNIT);
                    loserWasUnarmed = false;
                }
            }

            // Failed to defend a colony and was not armed.
            if (settlement instanceof Colony && loserWasUnarmed) {
                // A Colony falls to Europeans when the last defender
                // is unarmed.  Natives will pillage if possible but
                // otherwise proceed to kill colonists incrementally
                // until the colony falls for lack of survivors.
                // Ships in a falling colony will be damaged or sunk
                // if they have no repair location.
                Colony colony = (Colony) settlement;
                CombatResult shipResult
                    = (colony.getShipList().isEmpty()) ? null
                    : (colony.getShipList().get(0).getRepairLocation() == null)
                    ? CombatResult.SINK_COLONY_SHIPS
                    : CombatResult.DAMAGE_COLONY_SHIPS;
                if (winnerPlayer.isEuropean()) {
                    if (shipResult != null) crs.add(shipResult);
                    crs.add(CombatResult.CAPTURE_COLONY);
                } else if (!great && colony.canBePillaged(winner)) {
                    crs.add(CombatResult.PILLAGE_COLONY);
                } else if (colony.getUnitCount() > 1
                           || loser.getLocation() == tile) {
                    crs.add(CombatResult.SLAUGHTER_UNIT);
                } else {
                    if (shipResult != null) crs.add(shipResult);
                    crs.add(CombatResult.DESTROY_COLONY);
                }

            // Failed to defend a native settlement.
            } else if (settlement instanceof IndianSettlement) {
                // Attacking and defeating the defender of a native
                // settlement with a mission may yield converts but
                // also may provoke the burning of all missions.
                // Native settlements fall when there are no units
                // present either in-settlement or on the settlement
                // tile.
                IndianSettlement is = (IndianSettlement) settlement;
                int lose = 1;
                if (attackerWon) {
                    if (r < winner.getConvertProbability()) {
                        if (is.getUnitCount() + tile.getUnitCount() > 1
                            && is.getMissionary(winnerPlayer) != null) {
                            crs.add(CombatResult.CAPTURE_CONVERT);
                            lose++;
                        }
                    } else if (r >= 1.0f - winner.getBurnProbability()) {
                        for (IndianSettlement s
                                 : loserPlayer.getIndianSettlements()) {
                            if (s.getMissionary(winnerPlayer) != null) {
                                crs.add(CombatResult.BURN_MISSIONS);
                                break;
                            }
                        }
                    }
                }
                if (settlement.getUnitCount() + tile.getUnitCount() > lose) {
                    crs.add(CombatResult.SLAUGHTER_UNIT);
                } else {
                    crs.add(CombatResult.DESTROY_SETTLEMENT);
                }

            // Immediately slaughter units with this property.
            } else if (loser.hasAbility("model.ability.disposeOnCombatLoss")) {
                crs.add(CombatResult.SLAUGHTER_UNIT);

            // Disarm losing defensive units.
            } else if ((equip = loser.getBestCombatEquipmentType(
                        loser.getEquipment())) != null) {
                crs.add((loser.losingEquipmentKillsUnit(equip))
                        ? CombatResult.SLAUGHTER_UNIT
                        : (winner.canCaptureEquipment(equip, loser) != null)
                        ? CombatResult.CAPTURE_EQUIP
                        : CombatResult.LOSE_EQUIP);

            // Consume autoequipment.
            } else if (settlement instanceof Colony && autoEquip != null) {
                crs.add((loser.losingEquipmentKillsUnit(autoEquip))
                        ? CombatResult.SLAUGHTER_UNIT
                        : (winner.canCaptureEquipment(autoEquip, loser) != null)
                        ? CombatResult.CAPTURE_AUTOEQUIP
                        : CombatResult.LOSE_AUTOEQUIP);

            // Demote units with a demotion available.
            } else if (loser.getTypeChange(ChangeType.DEMOTION, loserPlayer)
                       != null) {
                crs.add(CombatResult.DEMOTE_UNIT);

            // Capture suitable units if the winner is capable.
            } else if (loser.hasAbility("model.ability.canBeCaptured")
                       && winner.hasAbility("model.ability.captureUnits")) {
                crs.add(CombatResult.CAPTURE_UNIT);

            // Final catch all is just to slaughter.
            } else {
                crs.add(CombatResult.SLAUGHTER_UNIT);
            }
        }

        // Promote great winners or with automatic promotion, if possible.
        UnitTypeChange promotion = winner.getType().getUnitTypeChange(ChangeType.PROMOTION, winnerPlayer);
        if (promotion != null
            && (winner.hasAbility("model.ability.automaticPromotion")
                || (great && (100 * (r - Math.floor(r)) <= promotion.getProbability(ChangeType.PROMOTION))))) {
            crs.add(CombatResult.PROMOTE_UNIT);
        }
    }

    /**
     * Could this attack be an ambush?
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return True if the attack can be an ambush.
     */
    private boolean isAmbush(FreeColGameObject attacker,
                             FreeColGameObject defender) {
        return attacker.hasAbility("model.ability.ambushBonus")
            || defender.hasAbility("model.ability.ambushPenalty");
    }

    /**
     * Is a unit a beached ship?
     *
     * @param unit The <code>Unit</code> to test.
     * @return True if the unit is a beached ship.
     */
    private boolean isBeached(Unit unit) {
        return unit.isNaval() && unit.getTile().isLand()
            && unit.getSettlement() == null;
    }

}
