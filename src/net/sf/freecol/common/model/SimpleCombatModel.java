/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Unit;
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
    public static final String AMPHIBIOUS_ATTACK
        = "model.modifier.amphibiousAttack";
    public static final String BOMBARD_BONUS
        = "model.modifier.bombardBonus";
    public static final Modifier UNKNOWN_DEFENCE_MODIFIER
        = new Modifier("bogus", Modifier.UNKNOWN, Modifier.Type.ADDITIVE);


    /**
     * Deliberately empty constructor.
     */
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
        if (attacker == null) {
            throw new IllegalStateException("Null attacker");
        } else if (combatIsAttackMeasurement(attacker, defender)
            || combatIsAttack(attacker, defender)
            || combatIsSettlementAttack(attacker, defender)) {
            result = FeatureContainer.applyModifierSet(0,
                    attacker.getGame().getTurn(),
                    getOffensiveModifiers(attacker, defender));

        } else if (combatIsBombard(attacker, defender)) {
            Settlement attackerSettlement = (Settlement) attacker;
            if (attackerSettlement.hasAbility("model.ability.bombardShips")) {
                for (Unit unit : attackerSettlement.getTile().getUnitList()) {
                    if (unit.hasAbility(Ability.BOMBARD)) {
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
        if (combatIsDefenceMeasurement(attacker, defender)
            || combatIsAttack(attacker, defender)
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
        if (attacker == null) {
            throw new IllegalStateException("Null attacker");
        } else if (combatIsAttackMeasurement(attacker, defender)
            || combatIsAttack(attacker, defender)
            || combatIsSettlementAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            UnitType type = attackerUnit.getType();
            result.add(new Modifier(Modifier.OFFENCE,
                                    Specification.BASE_OFFENCE_SOURCE,
                                    type.getOffence(),
                                    Modifier.Type.ADDITIVE));
            result.addAll(attackerUnit.getModifierSet(Modifier.OFFENCE));
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
        int count = attacker.getGoodsSpaceTaken();
        if (count > 0) {
            // Penalty for every unit of cargo.
            // TODO: shouldn't this be -cargo/capacity?
            // TODO: magic number to spec
            result.add(new Modifier(Modifier.OFFENCE,
                                    Specification.CARGO_PENALTY_SOURCE,
                                    -12.5f * count,
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

        // Amphibious attack?
        if (combatIsAmphibious(attacker, defender)) {
            result.addAll(spec.getModifiers(AMPHIBIOUS_ATTACK));
        }

        if (combatIsAttackMeasurement(attacker, defender)) {
            ; // No defender information available
        } else if (combatIsSettlementAttack(attacker, defender)) {
            // Settlement present, REF bombardment bonus
            result.addAll(attackerUnit
                          .getModifierSet(BOMBARD_BONUS));
        } else if (combatIsAttack(attacker, defender)) {
            Unit defenderUnit = (Unit) defender;
            Tile tile = defenderUnit.getTile();
            if (tile != null) {
                if (tile.getSettlement() != null) {
                    result.addAll(attackerUnit
                                  .getModifierSet(BOMBARD_BONUS));
                } else {
                    // Ambush bonus in the open = defender's defence
                    // bonus, if defender is REF, or attacker is indian.
                    if (isAmbush(attacker, defender)) {
                        for (Modifier mod : tile.getType()
                                 .getModifierSet(Modifier.DEFENCE)) {
                            Modifier modifier = new Modifier(mod);
                            modifier.setId(Modifier.OFFENCE);
                            modifier.setSource(Specification.AMBUSH_BONUS_SOURCE);
                            result.add(modifier);
                        }
                    }
                    // Artillery in the open penalty, must be on a
                    // tile and not in a settlement.
                    if (attackerUnit.hasAbility(Ability.BOMBARD)
                        && attackerUnit.getLocation() instanceof Tile
                        && attackerUnit.getSettlement() == null) {
                        result.addAll(spec.getModifiers(ARTILLERY_IN_THE_OPEN));
                    }
                }
            }
        } else {
            throw new IllegalStateException("Bogus combat");
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
        if (combatIsDefenceMeasurement(attacker, defender)
            || combatIsAttack(attacker, defender)) {
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
            // Not allowed to see inside the settlement.  This only applies 
            // to the pre-combat dialog--- the actual attack is on the
            // unit chosen to defend.
            result.add(UNKNOWN_DEFENCE_MODIFIER);

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
        if (defenderUnit.getState() == Unit.UnitState.FORTIFIED) {
            result.addAll(spec.getModifiers(FORTIFIED));
        }
        Tile tile = defenderUnit.getTile();
        if (tile != null) {
            Settlement settlement = tile.getSettlement();
            if (settlement == null) { // In the open
                // Terrain defensive bonus.
                if (!isAmbush(attacker, defender)) {
                    result.addAll(tile.getType().getDefenceBonus());
                }
                // Artillery in the Open penalty
                if (defenderUnit.hasAbility(Ability.BOMBARD)
                    && defenderUnit.getState() != Unit.UnitState.FORTIFIED) {
                    result.addAll(spec.getModifiers(ARTILLERY_IN_THE_OPEN));
                }
            } else { // In settlement
                result.addAll(tile.getType().getDefenceBonus());
                result.addAll(settlement.getModifierSet(Modifier.DEFENCE));
                result.addAll(settlement.getOwner()
                    .getModifierSet(Modifier.DEFENCE, settlement.getType()));
                // Artillery defence bonus against an Indian raid
                if (defenderUnit.hasAbility(Ability.BOMBARD)
                    && attacker != null
                    && ((Unit)attacker).getOwner().isIndian()) {
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
        FreeColGameObject attacker, FreeColGameObject defender) {
        ArrayList<CombatResult> crs = new ArrayList<CombatResult>();
        CombatOdds odds = calculateCombatOdds(attacker, defender);
        float r = Utils.randomFloat(logger, "AttackResult", random);
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
            if (r < odds.win || defenderUnit.isBeached()) {
                great = r < 0.1f * odds.win; // Great Win
                crs.add(CombatResult.WIN);
                resolveAttack(attackerUnit, defenderUnit, great,
                    // Rescale to 0 <= r < 1
                    r / (0.1f * odds.win), crs);
            } else if (r < 0.8f * odds.win + 0.2f
                    && defenderUnit.hasAbility("model.ability.evadeAttack")) {
                crs.add(CombatResult.NO_RESULT);
                crs.add(CombatResult.EVADE_ATTACK);
            } else {
                great = r >= 0.1f * odds.win + 0.9f; // Great Loss
                crs.add(CombatResult.LOSE);
                resolveAttack(defenderUnit, attackerUnit, great,
                    // Rescaling to 0 <= r < 1
                    // (rearrange: 0.8 * odds.win + 0.2 <= r < 1.0)
                    (1.25f * r - 0.25f - odds.win)/(1.0f - odds.win), crs);
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
                    + " " + defender.toString() + ": victory=" + odds.win
                    + " random(1.0)=" + r + " great=" + great
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
            if (great
                || loser.getRepairLocation() == null
                || loser.isBeached()) {
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
                CombatResult colonyResult = (winnerPlayer.isEuropean())
                    ? CombatResult.CAPTURE_COLONY
                    : (!great && colony.canBePillaged(winner))
                    ? CombatResult.PILLAGE_COLONY
                    : (colony.getUnitCount() > 1
                        || loser.getLocation() == tile)
                    ? CombatResult.SLAUGHTER_UNIT
                    : CombatResult.DESTROY_COLONY;
                if (colonyResult == CombatResult.DESTROY_COLONY) {
                    crs.add(CombatResult.SLAUGHTER_UNIT);
                }
                if (colonyResult == CombatResult.CAPTURE_COLONY
                    || colonyResult == CombatResult.DESTROY_COLONY) {
                    CombatResult shipResult = null;
                    shipResult = (colony.getShipList().isEmpty()) ? null
                        : (colony.getShipList().get(0).getRepairLocation() == null)
                        ? CombatResult.SINK_COLONY_SHIPS
                        : CombatResult.DAMAGE_COLONY_SHIPS;
                    if (shipResult != null) crs.add(shipResult);
                }
                crs.add(colonyResult);

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
                            && is.getMissionary(winnerPlayer) != null
                            && winner.getTile() != null
                            && winner.getTile().isLand()) {
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
                crs.add(CombatResult.SLAUGHTER_UNIT);
                if (settlement.getUnitCount() + tile.getUnitCount() <= lose) {
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
                if (loser.losingEquipmentDemotesUnit(equip)) {
                    crs.add(CombatResult.DEMOTE_UNIT);
                }

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
            } else if (loser.hasAbility(Ability.CAN_BE_CAPTURED)
                       && winner.hasAbility(Ability.CAPTURE_UNITS)
                       && !combatIsAmphibious(winner, loser)) {
                crs.add(CombatResult.CAPTURE_UNIT);

            // Final catch all is just to slaughter.
            } else {
                crs.add(CombatResult.SLAUGHTER_UNIT);
            }
        }

        // Promote great winners or with automatic promotion, if possible.
        UnitTypeChange promotion = winner.getType()
            .getUnitTypeChange(ChangeType.PROMOTION, winnerPlayer);
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
        return (attacker != null
            && attacker.hasAbility("model.ability.ambushBonus"))
            || (defender != null
                && defender.hasAbility("model.ability.ambushPenalty"));
    }
}
