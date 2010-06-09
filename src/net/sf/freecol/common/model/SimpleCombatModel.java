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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;


/**
 * This class implements the original Colonization combat model.
 */
public class SimpleCombatModel implements CombatModel {

    private static final Logger logger = Logger.getLogger(SimpleCombatModel.class.getName());

    /**
     * The maximum attack power of a Colony's fortifications against a
     * naval unit.
     */
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


    public SimpleCombatModel() {}

    /**
     * Check some special case attack results.
     * Naval losers are sunk if they can not find a repair location.
     * Some wins become settlement defeats.
     *
     * @param type The result of the attack.
     * @param attacker The attacking <code>Unit</code> (may be null).
     * @param defender The defending <code>Unit</code>.
     * @return The updated result.
     */
    private CombatResultType checkResult(CombatResultType type,
                                         Unit attacker, Unit defender) {
        if (defender.isNaval()
            && type == CombatResultType.WIN
            && defender.getOwner().getRepairLocation(defender) == null) {
            type = CombatResultType.GREAT_WIN;
        } else if (attacker != null
                   && attacker.isNaval()
                   && type == CombatResultType.LOSS
                   && attacker.getOwner().getRepairLocation(attacker) == null) {
            type = CombatResultType.GREAT_LOSS;
        } else if (type.compareTo(CombatResultType.WIN) >= 0
                   && defender.getTile().getSettlement() != null) {
            Settlement settlement = defender.getTile().getSettlement();
            if (settlement instanceof Colony) {
                if (!defender.isDefensiveUnit()
                    && defender.getAutomaticEquipment() == null) {
                    type = CombatResultType.DONE_SETTLEMENT;
                }
            } else if (settlement instanceof IndianSettlement) {
                if (defender.getTile().getUnitCount()
                    + defender.getTile().getSettlement().getUnitCount() <= 1) {
                    type = CombatResultType.DONE_SETTLEMENT;
                }
            } else {
                throw new IllegalStateException("Bogus Settlement.");
            }
        }
        return type;
    }


    // Implementation of CombatModel follows.
    // Note that the damage part of any CombatResult is ignored throughout.

    /**
     * Calculates the chance of the outcomes of combat between the units.
     * Currently only calculates the chance of winning combat. 
     * 
     * @param attacker The attacking <code>Unit</code>. 
     * @param defender The defending <code>Unit</code>.
     * @return The combat odds.
     */
    public CombatOdds calculateCombatOdds(Unit attacker, Unit defender) {
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
     * Calculates the chance of the outcomes of bombarding a unit.
     * Currently only calculates the chance of winning combat.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The combat odds.
     */
    public CombatOdds calculateCombatOdds(Colony attacker, Unit defender) {
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
     * Generates a result of an attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The result of the combat.
     */
    public CombatResult generateAttackResult(Unit attacker, Unit defender) {
        ModelController mc = attacker.getGame().getModelController();
        CombatOdds odds = calculateCombatOdds(attacker, defender);
        int magic = 1000000;
        float r = (1.0f/magic) * mc.getRandom(attacker.getId() + ".attack." + defender.getId(), magic);

        // Generate a random float 0 <= r < 1.0.
        // Partition this range into wins < odds.win and losses above.
        // Within the 0 <= r < odds.win range, partition the first 10%
        // to be great wins and the rest to be ordinary wins.
        //   r < 0.1 * odds.win  => great win
        //   else r < odds.win   => win
        // Within the odds.win <= r < 1.0 range, partition the first
        // 20% to be evasions (only if naval defender of course), the
        // next 70% to be ordinary losses, and the rest great losses.
        //   r < odds.win + 0.2 * (1.0 - odds.win) = 0.8 * odds.win + 0.2
        //     => evade
        //   else r < odds.win + (0.2 + 0.7) * (1.0 - odds.win)
        //     = 0.1 * odds.win + 0.9 => loss
        //   else => great loss
        CombatResultType type
            = (r < 0.1 * odds.win) ? CombatResultType.GREAT_WIN
            : (r < odds.win) ? CombatResultType.WIN
            : (r < 0.8 * odds.win + 0.2 && defender.isNaval())
                ? CombatResultType.EVADES
            : (r < 0.1 * odds.win + 0.9) ? CombatResultType.LOSS
            : CombatResultType.GREAT_LOSS;
        type = checkResult(type, attacker, defender);
        logger.info("Attack " + attacker.toString()
                    + " v " + defender.toString()
                    + ": victory=" + Float.toString(odds.win)
                    + " random=" + Float.toString(r)
                    + " => " + type.toString());
        return new CombatResult(type, 0);
    }

    /**
     * Generates the result of a colony bombarding a unit.
     *
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The result of the combat.
     */
    public CombatResult generateAttackResult(Colony attacker, Unit defender) {
        ModelController mc = attacker.getGame().getModelController();
        CombatOdds odds = calculateCombatOdds(attacker, defender);
        int magic = 1000000;
        float r = (1.0f/magic) * mc.getRandom(attacker.getId() + ".bombard." + defender.getId(), magic);
        CombatResultType type;
        if (r <= odds.win) {
            float offencePower = getOffencePower(attacker, defender);
            float defencePower = getDefencePower(attacker, defender);
            int diff = Math.round(defencePower * 2 - offencePower);
            int r2 = mc.getRandom(attacker.getId() + ".damage." + defender.getId(), Math.max(diff, 3));
            type = (r2 == 0) ? CombatResultType.GREAT_WIN
                : CombatResultType.WIN;
            type = checkResult(type, null, defender);
        } else {
            type = CombatResultType.EVADES;
        }
        logger.info("Bombard " + attacker.toString()
                    + " v " + defender.toString()
                    + ": victory=" + Float.toString(odds.win)
                    + " random=" + Float.toString(r)
                    + " => " + type.toString());
        return new CombatResult(type, 0);
    }


    /**
     * Get the offensive power of a unit attacking another.
     * 
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The offensive power.
     */
    public float getOffencePower(Unit attacker, Unit defender) {
        return FeatureContainer.applyModifierSet(0,
                attacker.getGame().getTurn(),
                getOffensiveModifiers(attacker, defender));
    }

    /**
     * Get the offensive power of a colony bombarding a unit.
     *
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The offensive power.
     */
    public float getOffencePower(Colony attacker, Unit defender) {
        float attackPower = 0;
        if (defender.isNaval() &&
            attacker.hasAbility("model.ability.bombardShips")) {
            for (Unit unit : attacker.getTile().getUnitList()) {
                if (unit.hasAbility("model.ability.bombard")) {
                    attackPower += unit.getType().getOffence();
                    if (attackPower >= MAXIMUM_BOMBARD_POWER) {
                        return MAXIMUM_BOMBARD_POWER;
                    }
                }
            }
        }
        return attackPower;
    }
    
    /**
     * Get the defensive power of a unit defending against another.
     * 
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The defensive power.
     */
    public float getDefencePower(Unit attacker, Unit defender) {
        return FeatureContainer.applyModifierSet(0,
                attacker.getGame().getTurn(),
                getDefensiveModifiers(attacker, defender));
    }

    /**
     * Get the defensive power of a unit defending against a colony.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The defensive power.
     */
    public float getDefencePower(Colony attacker, Unit defender) {
        return defender.getType().getDefence();
    }


    /**
     * Collect all the offensive modifiers that apply to a unit
     * attacking another.
     * 
     * Null can be passed as the defender when only the attacker unit
     * stats are required.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable offensive modifiers.
     */
    public Set<Modifier> getOffensiveModifiers(Unit attacker, Unit defender) {
        Specification spec = Specification.getSpecification();
        Set<Modifier> result = new LinkedHashSet<Modifier>();

        result.add(new Modifier(Modifier.OFFENCE, Specification.BASE_OFFENCE_SOURCE,
                                attacker.getType().getOffence(),
                                Modifier.Type.ADDITIVE));

        result.addAll(attacker.getType().getFeatureContainer()
                      .getModifierSet(Modifier.OFFENCE));

        result.addAll(attacker.getOwner().getFeatureContainer()
                      .getModifierSet(Modifier.OFFENCE, attacker.getType()));

        if (attacker.isNaval()) {
            int goodsCount = attacker.getGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                result.add(new Modifier(Modifier.OFFENCE, Specification.CARGO_PENALTY_SOURCE,
                                        -12.5f * goodsCount,
                                        Modifier.Type.PERCENTAGE));
            }
        } else {
            for (EquipmentType equipment : attacker.getEquipment().keySet()) {
                result.addAll(equipment.getFeatureContainer().getModifierSet(Modifier.OFFENCE));
            }
            // 50% attack bonus
            result.addAll(spec.getModifiers(ATTACK_BONUS));
            // movement penalty
            int movesLeft = attacker.getMovesLeft();
            if (movesLeft == 1) {
                result.addAll(spec.getModifiers(BIG_MOVEMENT_PENALTY));
            } else if (movesLeft == 2) {
                result.addAll(spec.getModifiers(SMALL_MOVEMENT_PENALTY));
            }

            if (defender != null && defender.getTile() != null) {

                if (defender.getTile().getSettlement() == null) {
                    /**
                     * Ambush bonus in the open = defender's defence bonus, if
                     * defender is REF, or attacker is indian.
                     */
                    if (attacker.hasAbility("model.ability.ambushBonus") ||
                        defender.hasAbility("model.ability.ambushPenalty")) {
                        Set<Modifier> ambushModifiers = defender.getTile().getType()
                            .getModifierSet(Modifier.DEFENCE);
                        for (Modifier modifier : ambushModifiers) {
                            Modifier ambushModifier = new Modifier(modifier);
                            ambushModifier.setId(Modifier.OFFENCE);
                            ambushModifier.setSource(Specification.AMBUSH_BONUS_SOURCE);
                            result.add(ambushModifier);
                        }
                    }

                    // 75% Artillery in the open penalty
                    if (attacker.hasAbility("model.ability.bombard") &&
                        attacker.getTile().getSettlement() == null) {
                        result.addAll(spec.getModifiers(ARTILLERY_IN_THE_OPEN));
                    }
                } else {
                    // Attacking a settlement
                    // REF bombardment bonus
                    result.addAll(attacker.getModifierSet("model.modifier.bombardBonus"));
                }
            }
        }

        return result;
    }

    /**
     * Collect all the offensive modifiers that apply to a colony
     * bombarding a unit.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable offensive modifiers.
     */
    public Set<Modifier> getOffensiveModifiers(Colony attacker, Unit defender) {
        Set<Modifier> result = new HashSet<Modifier>();
        result.add(new Modifier("model.modifier.bombardModifier",
                                getOffencePower(attacker, defender),
                                Modifier.Type.ADDITIVE));
        return result;
    }

    /**
     * Collect all defensive modifiers that apply to a unit defending
     * against another.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable defensive modifiers.
     */
    public Set<Modifier> getDefensiveModifiers(Unit attacker, Unit defender) {
        Specification spec = Specification.getSpecification();
        Set<Modifier> result = new LinkedHashSet<Modifier>();
        if (defender == null) {
            return result;
        }

        result.add(new Modifier(Modifier.DEFENCE, Specification.BASE_DEFENCE_SOURCE,
                                defender.getType().getDefence(),
                                Modifier.Type.ADDITIVE));
        result.addAll(defender.getType().getFeatureContainer()
                      .getModifierSet(Modifier.DEFENCE));


        if (defender.isNaval()) {
            int goodsCount = defender.getVisibleGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                result.add(new Modifier(Modifier.DEFENCE, Specification.CARGO_PENALTY_SOURCE,
                                        -12.5f * goodsCount,
                                        Modifier.Type.PERCENTAGE));
            }
        } else {
            // Paul Revere makes an unarmed colonist in a settlement pick up
            // a stock-piled musket if attacked, so the bonus should be applied
            // for unarmed colonists inside colonies where there are muskets
            // available. Indians can also pick up equipment.
            TypeCountMap<EquipmentType> autoEquipList = defender.getAutomaticEquipment();
            if (autoEquipList != null) {
                // add modifiers given by equipment of defense
                for(EquipmentType equipment : autoEquipList.keySet()){
                    result.addAll(equipment.getModifierSet("model.modifier.defence"));
                }
            }

            for (EquipmentType equipment : defender.getEquipment().keySet()) {
                result.addAll(equipment.getFeatureContainer().getModifierSet(Modifier.DEFENCE));
            }
            // 50% fortify bonus
            if (defender.getState() == UnitState.FORTIFIED) {
                result.addAll(spec.getModifiers(FORTIFIED));
            }

            if (defender.getTile() != null) {
                Tile tile = defender.getTile();
                if (tile.getSettlement() == null) {
                    // In the open
                    if (!(attacker.hasAbility("model.ability.ambushBonus") ||
                          defender.hasAbility("model.ability.ambushPenalty"))) {
                        // Terrain defensive bonus.
                        result.addAll(tile.getType().getDefenceBonus());
                    }
                    if (defender.hasAbility("model.ability.bombard") &&
                        defender.getState() != UnitState.FORTIFIED) {
                        // -75% Artillery in the Open penalty
                        result.addAll(spec.getModifiers(ARTILLERY_IN_THE_OPEN));
                    }
                } else {
                    result.addAll(tile.getSettlement().getOwner().getFeatureContainer()
                                  .getModifierSet(Modifier.SETTLEMENT_DEFENCE));
                    if (tile.getSettlement().isCapital()) {
                        result.addAll(tile.getSettlement().getOwner().getFeatureContainer()
                                      .getModifierSet(Modifier.CAPITAL_DEFENCE));
                    }
                    if (defender.hasAbility("model.ability.bombard") &&
                        attacker.getOwner().isIndian()) {
                        // 100% defence bonus against an Indian raid
                        result.addAll(spec.getModifiers(ARTILLERY_AGAINST_RAID));
                    }
                }
            }

        }
        return result;
    }

    /**
     * Collect all defensive modifiers that apply to a unit defending
     * against a colony.
     *
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable defensive modifiers.
     */
    public Set<Modifier> getDefensiveModifiers(Colony attacker, Unit defender) {
        Set<Modifier> result = new LinkedHashSet<Modifier>();
        result.add(new Modifier("model.modifier.defenceBonus",
                                defender.getType().getDefence(),
                                Modifier.Type.ADDITIVE));
        return result;
    }


    /**
     * Attack a unit with the given outcome.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender The <code>Unit</code> defending against attack.
     * @param result The result of the attack.
     * @param plunderGold an <code>int</code> value
     * @param repairLocation a <code>Location</code> value
     */
    public void attack(Unit attacker, Unit defender, CombatResult result, int plunderGold, Location repairLocation) {
        Player attackingPlayer = attacker.getOwner();
        Player defendingPlayer = defender.getOwner();

        // make sure we are at war, unless one of both units is a privateer
        if (attacker.hasAbility("model.ability.piracy")) {
            defendingPlayer.setAttackedByPrivateers(true);
        } else if (!defender.hasAbility("model.ability.piracy")) {
            attackingPlayer.changeRelationWithPlayer(defendingPlayer, Stance.WAR);
        }

        // Wake up if you're attacking something.
        // Before, a unit could stay fortified during execution of an
        // attack. - sjm
        attacker.setState(UnitState.ACTIVE);
        
        // The Revenger unit can attack multiple times
        // Other units can only attack once
        if (!attacker.hasAbility("model.ability.multipleAttacks")) {
            attacker.setMovesLeft(0);
        } else {
            //spend at least the eventual cost of moving to the tile
            int movecost = attacker.getMoveCost(defender.getTile());
            attacker.setMovesLeft(attacker.getMovesLeft()-movecost);
        }

        Tile newTile = defender.getTile();
        //attacker.adjustTension(defender);
        Settlement settlement = newTile.getSettlement();

        // Warn of automatic arming
        TypeCountMap<EquipmentType> autoEquipList = defender.getAutomaticEquipment();
        if (autoEquipList != null) {
            defendingPlayer.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                             "model.unit.automaticDefence",
                                                             defender)
                                     .addStringTemplate("%unit%", defender.getLabel())
                                     .addName("%colony%", settlement.getName()));
        }

        switch (result.type) {
        case EVADES:
            if (attacker.isNaval()) {
                evade(defender, null, attacker);
            } else {
                logger.warning("Non-naval unit evades!");
            }
            break;
        case LOSS:
            if (attacker.isNaval()) {
                damageShip(attacker, null, defender, repairLocation);
            } else {
                loseCombat(attacker, defender);
                if (defendingPlayer.hasAbility("model.ability.automaticPromotion")) {
                    promote(defender);
                }
            }
            break;
        case GREAT_LOSS:
            if (attacker.isNaval()) {
                sinkShip(attacker, null, defender);
            } else {
                loseCombat(attacker, defender);
                promote(defender);
            }
            break;
        case DONE_SETTLEMENT:
            if (settlement instanceof IndianSettlement) {
                defender.dispose();
                destroySettlement(attacker, (IndianSettlement) settlement);
            } else if (settlement instanceof Colony) {
                captureColony(attacker, (Colony) settlement, plunderGold, repairLocation);
            } else {
                throw new IllegalStateException("Unknown type of settlement.");
            }
            promote(attacker);
            break;
        case WIN:
            if (attacker.isNaval()) {
                attacker.captureGoods(defender);
                damageShip(defender, null, attacker, repairLocation);
            } else if (attacker.hasAbility("model.ability.pillageUnprotectedColony") && 
                       !defender.isDefensiveUnit() &&
                       defender.getColony() != null &&
                       !defender.getColony().hasStockade() &&
                       defender.getAutomaticEquipment() == null) {
                pillageColony(attacker, defender.getColony(), repairLocation);
            } else {
                if (!defender.isNaval()) {
                    loseCombat(defender, attacker);
                    if (settlement instanceof IndianSettlement) {
                        getConvert(attacker, (IndianSettlement) settlement);
                    }
                }
                if (attacker.hasAbility("model.ability.automaticPromotion")) {
                    promote(attacker);
                }
            }
            break;
        case GREAT_WIN:
            if (attacker.isNaval()) {
                attacker.captureGoods(defender);
                sinkShip(defender, null, attacker);
            } else {
                if (!defender.isNaval()) {
                    loseCombat(defender, attacker);
                    if (settlement instanceof IndianSettlement) {
                        getConvert(attacker, (IndianSettlement) settlement);
                    }
                }
                promote(attacker);
            }
            break;
        default:
            logger.warning("Illegal result of attack!");
            throw new IllegalArgumentException("Illegal result of attack!");
        }
    }

    /**
     * Bombard a unit with the given outcome.
     * 
     * @param colony a <code>Colony</code> value
     * @param defender The <code>Unit</code> defending against bombardment.
     * @param result The result of the bombardment.
     */
    public void bombard(Colony colony, Unit defender, CombatResult result, Location repairLocation) {
        switch (result.type) {
        case EVADES:
            evade(defender, colony, null);
            break;
        case WIN:
            damageShip(defender, colony, null, repairLocation);
            break;
        case GREAT_WIN:
            sinkShip(defender, colony, null);
            break;
        case DONE_SETTLEMENT:
            // cannot happen when we are bombarding an enemy ship.
            assert false;
            break;
        case GREAT_LOSS:
        case LOSS:
            // nothing to do here...
            break;
        }
    }

    /**
     * Captures an enemy colony and plunders gold.
     * 
     * @param attacker an <code>Unit</code> value
     * @param colony a <code>Colony</code> value
     * @param plunderGold The amount of gold to plunder.
     */
    private void captureColony(Unit attacker, Colony colony, int plunderGold,
                               Location repairLocation) {
        logger.finest("Entering captureColony()");
        Player defendingPlayer = colony.getOwner();
        Player attackingPlayer = attacker.getOwner();

        defendingPlayer.modifyTension(attacker.getOwner(), Tension.TENSION_ADD_MAJOR);
        if (attackingPlayer.isEuropean()) {
            attackingPlayer.getHistory()
                .add(new HistoryEvent(attackingPlayer.getGame().getTurn().getNumber(),
                                      HistoryEvent.EventType.CONQUER_COLONY)
                     .addStringTemplate("%nation%", defendingPlayer.getNationName())
                     .addName("%colony%", colony.getName()));
            defendingPlayer.getHistory()
                .add(new HistoryEvent(defendingPlayer.getGame().getTurn().getNumber(),
                                      HistoryEvent.EventType.COLONY_CONQUERED)
                     .addName("%colony%", colony.getName())
                     .addStringTemplate("%nation%", attackingPlayer.getNationName()));
            defendingPlayer.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                             "model.unit.colonyCapturedBy", defendingPlayer)
                                            .addName("%colony%", colony.getName())
                                            .addAmount("%amount%", plunderGold)
                                            .addStringTemplate("%player%", attackingPlayer.getNationName()));
            damageAllShips(colony, attacker, repairLocation);

            attackingPlayer.modifyGold(plunderGold);
            defendingPlayer.modifyGold(-plunderGold);
            defendingPlayer.divertModelMessages(colony, defendingPlayer);

            // This also changes over all of the units...
            colony.changeOwner(attackingPlayer);
            // However, not all units might be available
            for (Unit capturedUnit : colony.getUnitList()) {
                defendingPlayer.divertModelMessages(capturedUnit, defendingPlayer);
                if (!capturedUnit.getType().isAvailableTo(attackingPlayer)) {
                    UnitType downgrade = capturedUnit.getType().getUnitTypeChange(ChangeType.CAPTURE, attackingPlayer);
                    if (downgrade != null) {
                        capturedUnit.setType(downgrade);
                    } else {
                        capturedUnit.dispose();
                    }
                }
            }                    

            attackingPlayer.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                             "model.unit.colonyCaptured", colony)
                                            .addName("%colony%", colony.getName())
                                            .addAmount("%amount%", plunderGold));

            // Demote all soldiers and clear all orders:
            for (Unit capturedUnit : colony.getTile().getUnitList()) {
                defendingPlayer.divertModelMessages(capturedUnit, defendingPlayer);
                if (attacker.isUndead()) {
                    capturedUnit.setType(attacker.getType());
                } else {
                    UnitType downgrade = capturedUnit.getType().getUnitTypeChange(ChangeType.CAPTURE, attackingPlayer);
                    if (downgrade != null) {
                        capturedUnit.setType(downgrade);
                    }
                }
                capturedUnit.setState(UnitState.ACTIVE);
            }

            for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
                colony.getExportData(goodsType).setExported(false);
            }                                 

            if (attacker.isUndead()) {
                for (Unit capturedUnit : colony.getUnitList()) {
                    capturedUnit.setType(attacker.getType());
                }
            }
            attacker.setLocation(colony.getTile());
        } else {
            // the attacker is Indian, which cannot be a player
            // only messages directed to the losing player need to be sent
            if (colony.getUnitCount() <= 1) {
                defendingPlayer.getHistory()
                    .add(new HistoryEvent(defendingPlayer.getGame().getTurn().getNumber(),
                                          HistoryEvent.EventType.COLONY_DESTROYED)
                         .addStringTemplate("%nation%", attackingPlayer.getNationName())
                         .addName("%colony%", colony.getName()));
                defendingPlayer
                    .addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                      "model.unit.colonyBurning", defendingPlayer)
                                     .addName("%colony%", colony.getName())
                                     .addAmount("%amount%", plunderGold)
                                     .addStringTemplate("%nation%", attackingPlayer.getNationName())
                                     .addStringTemplate("%unit%", attacker.getLabel()));
                attackingPlayer.modifyGold(plunderGold);
                defendingPlayer.modifyGold(-plunderGold);
                damageAllShips(colony, attacker, repairLocation);
                defendingPlayer.divertModelMessages(colony, defendingPlayer);
                for (Unit victim : colony.getUnitList()) {
                    defendingPlayer.divertModelMessages(victim, defendingPlayer);
                    victim.dispose();
                }
                colony.dispose();
                attacker.setLocation(colony.getTile());
            } else {
                Unit victim = colony.getRandomUnit();
                if (victim == null) {
                    logger.warning("could not find colonist to slaughter");
                } else {
                    defendingPlayer.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                                     "model.unit.colonistSlaughtered", colony)
                                                    .addName("%colony%", colony.getName())
                                                    .addStringTemplate("%unit%", victim.getLabel())
                                                    .addStringTemplate("%nation%", attackingPlayer.getNationName())
                                                    .addStringTemplate("%enemyUnit%", attacker.getLabel()));
                    defendingPlayer.divertModelMessages(victim, defendingPlayer);
                    victim.dispose();
                }
            }
        }

    }


    /**
     * Damages all ship located on this <code>Colony</code>'s
     * <code>Tile</code>. That is: they are sent to the closest location for
     * repair.
     * 
     * @see #damageShip
     */
    private void damageAllShips(Colony colony, Unit attacker, Location repairLocation) {
        // a new list must be created as the first one may be changed
        //elsewhere in between loop calls
        List<Unit> navalUnitsOutsideColony = new ArrayList<Unit>();
        for (Unit unit : colony.getTile().getUnitList()) {
            if (unit.isNaval()) {
                navalUnitsOutsideColony.add(unit);
            }
        }
        
        for (Unit unit : navalUnitsOutsideColony)
                damageShip(unit, null, attacker, repairLocation);
    }

    /**
     * Damage a building or a ship or steal some goods or gold. It's called
     * from attack when an indian attacks a colony and lose the combat with
     * LOSS as result
     *
     * @param colony The attacked colony
     */
    private void pillageColony(Unit attacker, Colony colony, Location repairLocation) {
        ArrayList<Building> buildingList = new ArrayList<Building>();
        ArrayList<Unit> shipList = new ArrayList<Unit>();
        List<Goods> goodsList = colony.getGoodsContainer().getCompactGoods();
        
        for (Building building : colony.getBuildings()) {
            if (building.canBeDamaged()) {
                buildingList.add(building);
            }
        }
        
        List<Unit> unitList = colony.getTile().getUnitList();
        for (Unit unit : unitList) {
            if (unit.isNaval()) {
                shipList.add(unit);
            }
        }
        
        StringTemplate nation = attacker.getOwner().getNationName();
        StringTemplate unitName = attacker.getLabel();
        String colonyName = colony.getName();
        Player owner = colony.getOwner();
        int limit = buildingList.size() + goodsList.size() + shipList.size() + 1;
        int pillage = attacker.getGame().getModelController().getRandom(attacker.getId() + "pillageColony", limit);
                                                                       
        if (pillage < buildingList.size()) {
            Building building = buildingList.get(pillage);
            owner.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.buildingDamaged",
                                                   colony)
                                   .add("%building%", building.getNameKey())
                                   .addName("%colony%", colonyName)
                                   .addStringTemplate("%enemyNation%", nation)
                                   .addStringTemplate("%enemyUnit%", unitName));
            building.damage();
        } else if (pillage < buildingList.size() + goodsList.size()) {
            Goods goods = goodsList.get(pillage - buildingList.size());
            goods.setAmount(Math.min(goods.getAmount() / 2, 50));
            colony.removeGoods(goods);
            if (attacker.getSpaceLeft() > 0) {
                attacker.add(goods);
            }
            owner.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.goodsStolen",
                                                   colony, goods)
                                   .addAmount("%amount%", goods.getAmount())
                                   .add("%goods%", goods.getNameKey())
                                   .addName("%colony%", colonyName)
                                   .addStringTemplate("%enemyNation%", nation)
                                   .addStringTemplate("%enemyUnit%", unitName));
        } else if (pillage < buildingList.size() + goodsList.size() + shipList.size()) {
            Unit ship = shipList.get(pillage - buildingList.size() - goodsList.size());
            damageShip(ship, null, attacker, repairLocation);
        } else { // steal gold
            int gold = colony.getOwner().getGold() / 10;
            colony.getOwner().modifyGold(-gold);
            attacker.getOwner().modifyGold(gold);
            owner.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.indianPlunder",
                                                   colony)
                                   .addAmount("%amount%", gold)
                                   .addName("%colony%", colonyName)
                                   .addStringTemplate("%enemyNation%", nation)
                                   .addStringTemplate("%enemyUnit%", unitName));
        }
    }

    /**
     * Destroys an Indian settlement.
     * 
     * @param attacker an <code>Unit</code> value
     * @param settlement an <code>IndianSettlement</code> value
     */
    private void destroySettlement(Unit attacker, IndianSettlement settlement) {
        Player player = attacker.getOwner();
        Player enemy = settlement.getOwner();
        boolean wasCapital = settlement.isCapital();
        Tile newTile = settlement.getTile();
        ModelController mc = attacker.getGame().getModelController();
        SettlementType settlementType = ((IndianNationType) enemy.getNationType()).getTypeOfSettlement();
        String settlementName = settlement.getName();
        settlement.dispose();

        enemy.modifyTension(attacker.getOwner(), Tension.TENSION_ADD_MAJOR);

        List<UnitType> treasureUnitTypes = FreeCol.getSpecification()
            .getUnitTypesWithAbility("model.ability.carryTreasure");
        if (treasureUnitTypes.size() > 0) {
            int treasure = mc.getRandom(attacker.getId() + "indianTreasureRandom" + attacker.getId(), 11);
            UnitType type = treasureUnitTypes.get(mc.getRandom(attacker.getId() + "newUnitForTreasure" + attacker.getId(),
                                                               treasureUnitTypes.size()));
            Unit tTrain = mc.createUnit(attacker.getId() + "indianTreasure" + attacker.getId(),
                                        newTile, attacker.getOwner(), type);

            // Larger treasure if Hernan Cortes is present in the congress:
            Set<Modifier> modifierSet = attacker.getModifierSet("model.modifier.nativeTreasureModifier");
            treasure = (int) FeatureContainer.applyModifierSet(treasure, attacker.getGame().getTurn(),
                                                               modifierSet);
            if (settlementType == SettlementType.INCA_CITY ||
                settlementType == SettlementType.AZTEC_CITY) {
                tTrain.setTreasureAmount(treasure * 500 + 1000);
            } else {
                tTrain.setTreasureAmount(treasure * 50  + 300);
            }

            // capitals give more gold
            if (wasCapital) {
                tTrain.setTreasureAmount((tTrain.getTreasureAmount() * 3) / 2);
            }

            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.indianTreasure",
                                                    attacker)
                                     .addName("%settlement%", settlementName)
                                     .addAmount("%amount%", tTrain.getTreasureAmount()));
        }
        int atrocities = Player.SCORE_SETTLEMENT_DESTROYED;
        if (settlementType == SettlementType.INCA_CITY ||
            settlementType == SettlementType.AZTEC_CITY) {
            atrocities *= 2;
        }
        if (wasCapital) {
            atrocities = (atrocities * 3) / 2;
        }
        attacker.getOwner().modifyScore(atrocities);
        attacker.setLocation(newTile);
        attacker.getOwner().getHistory()
            .add(new HistoryEvent(attacker.getGame().getTurn().getNumber(),
                                  HistoryEvent.EventType.DESTROY_SETTLEMENT)
                 .addStringTemplate("%nation%", enemy.getNationName())
                 .addName("%settlement%", settlementName));
        if (enemy.getSettlements().isEmpty()) {
            attacker.getOwner().getHistory()
                .add(new HistoryEvent(attacker.getGame().getTurn().getNumber(),
                                      HistoryEvent.EventType.DESTROY_NATION)
                     .addStringTemplate("%nation%", enemy.getNationName()));
        }
    }

    /**
     * Check whether some indian converts due to the attack or they burn all missions
     *
     * @param indianSettlement The attacked indian settlement
     */
    private void getConvert(Unit attacker, IndianSettlement indianSettlement) {
        ModelController mc = attacker.getGame().getModelController();
        int convert = mc.getRandom(attacker.getId() + "getConvert", 100);
        int convertProbability = (int) FeatureContainer.applyModifierSet(Specification.getSpecification()
                .getIntegerOption("model.option.nativeConvertProbability").getValue(), attacker.getGame().getTurn(),
                attacker.getModifierSet("model.modifier.nativeConvertBonus"));
        // TODO: it should be bigger when tension is high
        int burnProbability = Specification.getSpecification().getIntegerOption("model.option.burnProbability")
                .getValue();
        
        if (convert < convertProbability) {
            Unit missionary = indianSettlement.getMissionary();
            if (missionary != null && missionary.getOwner() == attacker.getOwner() &&
                attacker.getGame().getViewOwner() == null && indianSettlement.getUnitCount() > 1) {
                List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                if (converts.size() > 0) {
                    indianSettlement.getFirstUnit().dispose();
                    convert = mc.getRandom(attacker.getId() + "getConvertType", converts.size());
                    mc.createUnit(attacker.getId() + "indianConvert", attacker.getLocation(),
                                               attacker.getOwner(), converts.get(convert));
                }
            }
        } else if (convert >= 100 - burnProbability) {
            boolean burn = false;
            List<Settlement> settlements = indianSettlement.getOwner().getSettlements();
            for (Settlement settlement : settlements) {
                IndianSettlement indian = (IndianSettlement) settlement;
                Unit missionary = indian.getMissionary();
                if (missionary != null && missionary.getOwner() == attacker.getOwner()) {
                    burn = true;
                    indian.setMissionary(null);
                }
            }
            if (burn) {
                Player player = attacker.getOwner();
                player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                        "model.unit.burnMissions",
                                                        attacker, indianSettlement)
                                         .addStringTemplate("%nation%", attacker.getOwner().getNationName())
                                         .addStringTemplate("%enemyNation%", indianSettlement.getOwner().getNationName()));
            }
        }
    }

    /**
     * Evade a naval engagement.
     *
     * @param defender A naval unit that evades the attacker
     * @param attackerColony A colony that may have bombarded the defender
     * @param attackerUnit A unit that may have attacked the defender
     **/
    private void evade(Unit defender, Colony attackerColony, Unit attackerUnit) {
        Player player = defender.getOwner();
        StringTemplate nation = defender.getApparentOwnerName();
        if (attackerColony != null) {
            Player enemy = attackerColony.getOwner();
            enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.shipEvadedBombardment",
                                                   attackerColony)
                                           .addName("%colony%", attackerColony.getName())
                                           .addStringTemplate("%unit%", defender.getLabel())
                                           .addStringTemplate("%nation%", nation));
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.shipEvadedBombardment",
                                                    defender)
                                     .addName("%colony%", attackerColony.getName())
                                     .addStringTemplate("%unit%", defender.getLabel())
                                     .addStringTemplate("%nation%", nation));
        } else if (attackerUnit != null) {
            Player enemy = attackerUnit.getOwner();
            StringTemplate attackerNation = attackerUnit.getApparentOwnerName();
            enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.enemyShipEvaded",
                                                   attackerUnit)
                                         .addStringTemplate("%unit%", attackerUnit.getLabel())
                                         .addStringTemplate("%enemyUnit%", defender.getLabel())
                                         .addStringTemplate("%enemyNation%", nation));
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.shipEvaded",
                                                    defender)
                                     .addStringTemplate("%unit%", defender.getLabel())
                                     .addStringTemplate("%enemyUnit%", attackerUnit.getLabel())
                                     .addStringTemplate("%enemyNation%", attackerNation));
        }
    }

    /**
     * Sets the damage to this ship and sends it to its repair location.
     * 
     * @param damagedShip A ship that is loosing a battle and getting damaged
     * @param attackerColony The colony that may have opened fire on this unit
     * @param attackerUnit A unit which may have damaged the ship
     */
    private void damageShip(Unit damagedShip, Colony attackerColony, Unit attackerUnit, Location repairLocation) {
        Player player = damagedShip.getOwner();
        StringTemplate nation = damagedShip.getApparentOwnerName();
        StringTemplate repairLocationName = (repairLocation == null)
            ? StringTemplate.name("")
            : repairLocation.getLocationName();

        if (attackerColony != null) {
            Player enemy = attackerColony.getOwner();
            enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.enemyShipDamagedByBombardment",
                                                   attackerColony)
                                           .addName("%colony%", attackerColony.getName())
                                           .addStringTemplate("%nation%", nation)
                                           .addStringTemplate("%unit%", damagedShip.getLabel()));
        		
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.shipDamagedByBombardment",
                                                    damagedShip)
                                        .addName("%colony%", attackerColony.getName())
                                        .addStringTemplate("%unit%", damagedShip.getLabel())
                                        .addStringTemplate("%repairLocation%", repairLocationName));
        } else if (attackerUnit != null) {
            Player enemy = attackerUnit.getOwner();
            StringTemplate attackerNation = attackerUnit.getApparentOwnerName();
            enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.enemyShipDamaged",
                                                   attackerUnit)
                                         .addStringTemplate("%unit%", attackerUnit.getLabel())
                                         .addStringTemplate("%enemyNation%", nation)
                                         .addStringTemplate("%enemyUnit%", damagedShip.getLabel()));
            
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.shipDamaged",
                                                    damagedShip)
                                        .addStringTemplate("%unit%", damagedShip.getLabel())
                                        .addStringTemplate("%enemyUnit%", attackerUnit.getLabel())
                                        .addStringTemplate("%enemyNation%", attackerNation)
                                        .addStringTemplate("%repairLocation%", repairLocationName));
        }
        damagedShip.setHitpoints(1);
        damagedShip.disposeAllUnits();
        damagedShip.getGoodsContainer().removeAll();
        damagedShip.setDestination(null);
        damagedShip.sendToRepairLocation(repairLocation);
    }

    /**
     * Sinks this ship.
     * 
     * @param sinkingShip the Unit that is going to sink
     * @param attackerColony The colony that may have opened fire on this unit
     * @param attackerUnit The unit which may have attacked the ship
     */
    private void sinkShip(Unit sinkingShip, Colony attackerColony, Unit attackerUnit) {
        Player player = sinkingShip.getOwner();
        StringTemplate nation = sinkingShip.getApparentOwnerName();

        if (attackerColony != null) {
            Player enemy = attackerColony.getOwner();
            enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.shipSunkByBombardment",
                                                   attackerColony)
                                           .addName("%colony%", attackerColony.getName())
                                           .addStringTemplate("%unit%", sinkingShip.getLabel())
                                           .addStringTemplate("%nation%", nation));
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                         "model.unit.shipSunkByBombardment",
                                                         sinkingShip)
                                        .addName("%colony%", attackerColony.getName())
                                        .addStringTemplate("%unit%", sinkingShip.getLabel()));
        } else if (attackerUnit != null) {
            Player enemy = attackerUnit.getOwner();
            StringTemplate attackerNation = attackerUnit.getApparentOwnerName();
            enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   "model.unit.enemyShipSunk",
                                                   attackerUnit)
                                         .addStringTemplate("%unit%", attackerUnit.getLabel())
                                         .addStringTemplate("%enemyUnit%", sinkingShip.getLabel())
                                         .addStringTemplate("%enemyNation%", nation));
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                         "model.unit.shipSunk",
                                                         sinkingShip)
                                        .addStringTemplate("%unit%", sinkingShip.getLabel())
                                        .addStringTemplate("%enemyUnit%", attackerUnit.getLabel())
                                        .addStringTemplate("%enemyNation%", attackerNation));
        }
        player.divertModelMessages(sinkingShip, sinkingShip.getTile());
        sinkingShip.dispose();
    }

    /**
     * Find the highest priority equipment carried by a unit, if any
     *
     * @param victim a <code>Unit</code> that is about to lose equipment
     * @return The highest priority <code>EquipmentType</code> carried by a unit,
     * or null if none.
     **/
    private EquipmentType findEquipmentTypeToLose(Unit victim) {
        EquipmentType toLose = null;
        int combatLossPriority = 0;
        
        TypeCountMap<EquipmentType> equipmentList = victim.getEquipment();
        if(equipmentList.isEmpty()){
            TypeCountMap<EquipmentType> autoEquipment = victim.getAutomaticEquipment(); 
            if(autoEquipment != null){
                equipmentList = autoEquipment;
            }
        }

        for (EquipmentType equipmentType : equipmentList.keySet()) {
            if (equipmentType.getCombatLossPriority() > combatLossPriority) {
                toLose = equipmentType;
                combatLossPriority = equipmentType.getCombatLossPriority();
            }
        }
        return toLose;
    }

    /**
     * Lose a combat.  Units can be disarmed, demoted, captured or slaughtered.
     * The enemy may plunder equipment.
     *
     * @param unit a <code>Unit</code> that has lost a combat
     * @param enemyUnit a <code>Unit</code> that has won a combat
     */
    private void loseCombat(Unit unit, Unit enemyUnit) {
        if (unit.hasAbility("model.ability.disposeOnCombatLoss")) {
            slaughterUnit(unit, enemyUnit);
            return;
        }
        
        EquipmentType typeToLose = findEquipmentTypeToLose(unit);
        boolean hasEquipToLose = typeToLose != null;
        if(hasEquipToLose){
            if(losingEquipDiscardsUnit(unit,typeToLose)){
                slaughterUnit(unit, enemyUnit);
            }
            else{
                disarmUnit(unit, typeToLose, enemyUnit);
            }
            return;
        }
        
        UnitType downgrade = unit.getType().getUnitTypeChange(ChangeType.DEMOTION, unit.getOwner()); 
        if (downgrade != null) {
            demoteUnit(unit, downgrade, enemyUnit);
            return;
        }
        
        boolean unitCanBeCaptured = unit.hasAbility("model.ability.canBeCaptured")
                                    && enemyUnit.hasAbility("model.ability.captureUnits");
        if(unitCanBeCaptured){
            captureUnit(unit, enemyUnit);
        }
        else{
            slaughterUnit(unit, enemyUnit);
        }        
    }

    private boolean losingEquipDiscardsUnit(Unit unit, EquipmentType typeToLose) {
        if(!unit.hasAbility("model.ability.disposeOnAllEquipLost")){
            return false;
        }
        // verifies if unit has other equipment
        for(EquipmentType equip : unit.getEquipment().keySet()){
            if(equip != typeToLose){
                return false;
            }
        }
        return true;
    }

    /**
     * Capture a unit.
     *
     * @param unit a <code>Unit</code> to be captured
     * @param enemyUnit a <code>Unit</code> that is capturing
     */
    private void captureUnit(Unit unit, Unit enemyUnit) {
        StringTemplate locationName = unit.getLocation().getLocationName();
        Player loser = unit.getOwner();
        StringTemplate nation = loser.getNationName();
        StringTemplate oldName = unit.getLabel();
        Player enemy = enemyUnit.getOwner();
        StringTemplate enemyNation = enemy.getNationName();
        String messageID = unit.getType().getId() + ".captured";

        // unit is about to change sides, launch message now!
        loser.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, unit)
                             .setDefaultId("model.unit.unitCaptured")
                             .addStringTemplate("%nation%", nation)
                             .addStringTemplate("%unit%", oldName)
                             .addStringTemplate("%enemyNation%", enemyNation)
                             .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                             .addStringTemplate("%location%", locationName));
        loser.divertModelMessages(unit, unit.getTile());
        unit.setLocation(enemyUnit.getTile());
        unit.setOwner(enemyUnit.getOwner());
        if (enemyUnit.isUndead()) {
            unit.setType(enemyUnit.getType());
        } else {
            UnitType downgrade = unit.getType().getUnitTypeChange(ChangeType.CAPTURE, unit.getOwner());
            if (downgrade != null) unit.setType(downgrade);
        }
        enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, enemyUnit)
                                  .setDefaultId("model.unit.unitCaptured")
                                  .addStringTemplate("%nation%", nation)
                                  .addStringTemplate("%unit%", oldName)
                                  .addStringTemplate("%enemyNation%", enemyNation)
                                  .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                                  .addStringTemplate("%location%", locationName));
    }

    /**
     * Demotes a unit.
     *
     * @param unit a <code>Unit</code> to demote
     * @param downgrade the <code>UnitType</code> to downgrade the init to
     * @param enemyUnit a <code>Unit</code> that caused the demotion
     */
    private void demoteUnit(Unit unit, UnitType downgrade, Unit enemyUnit) {
        StringTemplate locationName = unit.getLocation().getLocationName();
        Player loser = unit.getOwner();
        StringTemplate nation = loser.getNationName();
        StringTemplate oldName = unit.getLabel();
        Player enemy = enemyUnit.getOwner();
        StringTemplate enemyNation = enemy.getNationName();
        String messageID = unit.getType().getId() + ".demoted";

        unit.setType(downgrade);
        enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, enemyUnit)
                                  .setDefaultId("model.unit.unitDemoted")
                                  .addStringTemplate("%nation%", nation)
                                  .addStringTemplate("%oldName%", oldName)
                                  .addStringTemplate("%unit%", unit.getLabel())
                                  .addStringTemplate("%enemyNation%", enemyNation)
                                  .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                                  .addStringTemplate("%location%", locationName));
        loser.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, unit)
                             .setDefaultId("model.unit.unitDemoted")
                             .addStringTemplate("%nation%", nation)
                             .addStringTemplate("%oldName%", oldName)
                             .addStringTemplate("%unit%", unit.getLabel())
                             .addStringTemplate("%enemyNation%", enemyNation)
                             .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                             .addStringTemplate("%location%", locationName));
    }

    /**
     * Disarm a unit.
     *
     * @param unit a <code>Unit</code> to be disarmed
     * @param typeToLose the <code>EquipmentType</code> to lose
     * @param enemyUnit a <code>Unit</code> that is disarming
     */
    private void disarmUnit(Unit unit, EquipmentType typeToLose, Unit enemyUnit) {
        StringTemplate locationName = unit.getLocation().getLocationName();
        Player loser = unit.getOwner();
        StringTemplate nation = loser.getNationName();
        StringTemplate oldName = unit.getLabel();
        Player enemy = enemyUnit.getOwner();
        StringTemplate enemyNation = enemy.getNationName();
        String messageID = unit.getType().getId() + ".demoted";

        boolean hasAutoEquipment = unit.getEquipment().isEmpty() && unit.getAutomaticEquipment() != null;

        if(hasAutoEquipment){
            // auto equipment isnt actually equiped by the unit (cannot be)
            // its kept stored in the settlement of the unit
            // we need to remove it from there if its lost
            Settlement settlement = null;
            if(unit.getLocation() instanceof IndianSettlement){
                settlement = unit.getIndianSettlement();
            }
            else{
                settlement = unit.getColony();
            }
            for(AbstractGoods goods : typeToLose.getGoodsRequired()){
                settlement.removeGoods(goods);
            }
        }
        else{
            unit.removeEquipment(typeToLose, 1, true);
            if (unit.getEquipment().isEmpty()) {
                messageID = "model.unit.unitDemotedToUnarmed";
            }
        }
        enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, enemyUnit)
                                  .setDefaultId("model.unit.unitDemoted")
                                  .addStringTemplate("%nation%", nation)
                                  .addStringTemplate("%oldName%", oldName)
                                  .addStringTemplate("%unit%", unit.getLabel())
                                  .addStringTemplate("%enemyNation%", enemyNation)
                                  .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                                  .addStringTemplate("%location%", locationName));
        loser.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, unit)
                             .setDefaultId("model.unit.unitDemoted")
                             .addStringTemplate("%nation%", nation)
                             .addStringTemplate("%oldName%", oldName)
                             .addStringTemplate("%unit%", unit.getLabel())
                             .addStringTemplate("%enemyNation%", enemyNation)
                             .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                             .addStringTemplate("%location%", locationName));
               
        enemyCapturesEquipment(unit,enemyUnit,typeToLose);
    }

    /*
     * Capture equipment.
     *
     * @param unit The <code>Unit</code> that is losing equipment.
     * @param enemyUnit The <code>Unit</code> that is capturing equipment.
     * @param equipType The type of equipment to be captured.
     */
    private void enemyCapturesEquipment(Unit unit, Unit enemyUnit,
                                        EquipmentType equipType) {
        if (enemyUnit.hasAbility("model.ability.captureEquipment")) {
            Player enemy = enemyUnit.getOwner();
            EquipmentType newEquip = equipType;
            if (unit.getOwner().isIndian() != enemy.isIndian()) {
                // May need to change the equipment type if the attacker is
                // native and the defender is not, or vice-versa.
                newEquip = equipType.getCaptureEquipment(enemy.isIndian());
            }

            if (enemyUnit.canBeEquippedWith(newEquip)) {
                enemyUnit.equipWith(newEquip, true);
                ModelMessage m
                    = new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                       "model.unit.equipmentCaptured", unit)
                    .addStringTemplate("%nation%", enemyUnit.getOwner().getNationName())
                    .add("%equipment%", newEquip.getNameKey());
                unit.getOwner().addModelMessage(m);

                // TODO: Immediately transferring the captured goods back
                // to a potentially remote settlement is pretty dubious.
                // Apparently Col1 did it, but its cheating nonetheless.
                // Better would be to give the capturing unit a return-home-
                // -with-plunder mission.
                IndianSettlement settlement = enemyUnit.getIndianSettlement();
                if (settlement != null) {
                    for (AbstractGoods goods : equipType.getGoodsRequired()) {
                        settlement.addGoods(goods);
                    }
                }
            }
        }
    }

    /**
     * Slaughter a unit.
     *
     * @param unit a <code>Unit</code> to slaughter
     * @param enemyUnit a <code>Unit</code> that is slaughtering
     */
    private void slaughterUnit(Unit unit, Unit enemyUnit) {
        StringTemplate locationName = enemyUnit.getLocation().getLocationName();
        Player loser = unit.getOwner();
        StringTemplate nation = loser.getNationName();
        Player enemy = enemyUnit.getOwner();
        StringTemplate enemyNation = enemy.getNationName();
        String messageID = unit.getType().getId() + ".destroyed";

        enemy.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, enemyUnit)
                                  .setDefaultId("model.unit.unitSlaughtered")
                                  .addStringTemplate("%nation%", nation)
                                  .addStringTemplate("%unit%", unit.getLabel())
                                  .addStringTemplate("%enemyNation%", enemyNation)
                                  .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                                  .addStringTemplate("%location%", locationName));
        loser.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                               messageID, unit)
                             .setDefaultId("model.unit.unitSlaughtered")
                             .addStringTemplate("%nation%", nation)
                             .addStringTemplate("%unit%", unit.getLabel())
                             .addStringTemplate("%enemyNation%", enemyNation)
                             .addStringTemplate("%enemyUnit%", enemyUnit.getLabel())
                             .addStringTemplate("%location%", locationName));
        
        for(EquipmentType equip : unit.getEquipment().keySet()){
            enemyCapturesEquipment(unit, enemyUnit, equip);
        }
        
        loser.divertModelMessages(unit, unit.getTile());
        unit.dispose();
    }

    /**
     * Promotes this unit.
     *
     * @param unit an <code>Unit</code> value
     */
    private void promote(Unit unit) {
        StringTemplate oldName = unit.getLabel();
        Player player = unit.getOwner();
        StringTemplate nation = player.getNationName();
        UnitType newType = unit.getType().getUnitTypeChange(ChangeType.PROMOTION, unit.getOwner());
        
        if (newType != null && newType.isAvailableTo(unit.getOwner())) {
            unit.setType(newType);
            if (unit.getType().equals(newType)) {
                // the new unit type was successfully applied
                player.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                        "model.unit.unitPromoted",
                                                        unit)
                                     .addStringTemplate("%oldName%", oldName)
                                     .addStringTemplate("%unit%", unit.getLabel())
                                     .addStringTemplate("%nation%", nation));
            }
        }
    }
}
