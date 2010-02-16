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
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.Specification;
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

    public static final String SMALL_MOVEMENT_PENALTY =
        "model.modifier.smallMovementPenalty";
    public static final String BIG_MOVEMENT_PENALTY =
        "model.modifier.bigMovementPenalty";
    public static final String ARTILLERY_IN_THE_OPEN =
        "model.modifier.artilleryInTheOpen";
    public static final String ATTACK_BONUS =
        "model.modifier.attackBonus";
    public static final String FORTIFIED =
        "model.modifier.fortified";
    public static final String ARTILLERY_AGAINST_RAID =
        "model.modifier.artilleryAgainstRaid";


    private PseudoRandom random;

    /**
     * Calculates the chance of the outcomes of combat between the units.
     * Currently only calculates the chance of winning combat. 
     * 
     * @param attacker The attacking <code>Unit</code>. 
     * @param defender The defending unit.
     * @return A <code>CombatOdds</code> value.
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

    public SimpleCombatModel(PseudoRandom pseudoRandom) {
        this.random = pseudoRandom;
    }

    /**
     * Generates a result of an attack.
     * 
     * @param attacker The <code>Unit</code> attacking.
     * @param defender The defending unit.
     * @return a <code>CombatResult</code> value
     */
    public CombatResult generateAttackResult(Unit attacker, Unit defender) {

        float attackPower = getOffencePower(attacker, defender);
        float defencePower = getDefencePower(attacker, defender);
        float victory = attackPower / (attackPower + defencePower);
        int damage = 0;
        int r = random.nextInt(100);
        
        CombatResultType result = CombatResultType.EVADES;
        if (r <= victory * 20) {
            // 10% of the times winning:
            result = CombatResultType.GREAT_WIN;
            damage = defender.getHitpoints();
        } else if (r <= 100 * victory) {
            // 90% of the times winning:
            result = CombatResultType.WIN;
            damage = defender.getHitpoints() - 1;
        } else if (defender.isNaval()
                && r <= (80 * victory) + 20) {
            // 20% of the times loosing:
            result = CombatResultType.EVADES;
        } else if (r <= (10 * victory) + 90) {
            // 70% of the times loosing:
            result = CombatResultType.LOSS;
            damage = attacker.getHitpoints() - 1;
        } else {
            // 10% of the times loosing:
            result = CombatResultType.GREAT_LOSS;
            damage = attacker.getHitpoints();
        }

        // If naval battle, needs to check for repair location
        // naval defender loses, does not have any place to repair, is sunk
        if( defender.isNaval() &&
         	result == CombatResultType.WIN &&
            defender.getOwner().getRepairLocation(defender) == null){
             	result = CombatResultType.GREAT_WIN;
             	damage = defender.getHitpoints();
        }
        
        // naval attacker does not have any place to repair, is sunk
        if(attacker.isNaval() &&
           result == CombatResultType.LOSS &&
           attacker.getOwner().getRepairLocation(attacker) == null){
             result = CombatResultType.GREAT_LOSS;
             damage = defender.getHitpoints();
        }
        
        
        if (result.compareTo(CombatResultType.WIN) >= 0 &&
            defender.getTile().getSettlement() != null) {
            if (defender.getTile().getSettlement() instanceof Colony) {
                if (!defender.isDefensiveUnit() &&
                    defender.getAutomaticEquipment() == null) {
                    result = CombatResultType.DONE_SETTLEMENT;
                }
            } else if (defender.getTile().getSettlement() instanceof IndianSettlement) {
                if (defender.getTile().getUnitCount() + defender.getTile().getSettlement().getUnitCount() <= 1) {
                    result = CombatResultType.DONE_SETTLEMENT;
                }
            } else {
                throw new IllegalStateException("Unknown Settlement.");
            }
        }
        return new CombatResult(result, damage);
    }

    /**
     * Generates the result of a colony bombarding a Unit.
     *
     * @param colony the bombarding <code>Colony</code>
     * @param defender the defending <code>Unit</code>
     * @return a <code>CombatResult</code> value
     */
    public CombatResult generateAttackResult(Colony colony, Unit defender) {

        float attackPower = getOffencePower(colony, defender);
        float defencePower = getDefencePower(colony, defender);
        float totalProbability = attackPower + defencePower;
        int damage = 0;
        CombatResultType result = CombatResultType.EVADES;
        int r = random.nextInt(Math.round(totalProbability) + 1);
        if (r < attackPower) {
            int diff = Math.round(defencePower * 2 - attackPower);
            int r2 = random.nextInt((diff < 3) ? 3 : diff);
            if (r2 == 0) {
                result = CombatResultType.GREAT_WIN;
                damage = defender.getHitpoints();
            } else {
                result = CombatResultType.WIN;
                damage = defender.getHitpoints() - 1;
                
                // defender does not have any place to repair, is sunk
                if(defender.isNaval() && defender.getOwner().getRepairLocation(defender) == null){
                    result = CombatResultType.GREAT_WIN;
                    damage = defender.getHitpoints();
                }
            }
        }
        return new CombatResult(result, damage);
    }

    /**
     * Returns the power for bombarding
     * 
     * @param colony a <code>Colony</code> value
     * @param defender an <code>Unit</code> value
     * @return the power for bombarding
     */
    public float getOffencePower(Colony colony, Unit defender) {
        float attackPower = 0;
        if (defender.isNaval() &&
            colony.hasAbility("model.ability.bombardShips")) {
            for (Unit unit : colony.getTile().getUnitList()) {
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
     * Return the offensive power of the attacker versus the defender.
     * 
     * Null can be passed as the defender when only the attacker unit stats
     *  are required
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>float</code> value
     */
    public float getOffencePower(Unit attacker, Unit defender) {
        return FeatureContainer.applyModifierSet(0, attacker.getGame().getTurn(),
                                                 getOffensiveModifiers(attacker, defender));
    }

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * Null can be passed as the defender when only the attacker unit stats
     *  are required
     * 
     * @param colony an <code>Colony</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getOffensiveModifiers(Colony colony, Unit defender) {
        Set<Modifier> result = new HashSet<Modifier>();
        result.add(new Modifier("model.modifier.bombardModifier", 
                                getOffencePower(colony, defender),
                                Modifier.Type.ADDITIVE));
        return result;
    }

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * Null can be passed as the defender when only the attacker unit stats
     *  are required
     *  
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getOffensiveModifiers(Unit attacker, Unit defender) {
        Specification spec = Specification.getSpecification();
        Set<Modifier> result = new LinkedHashSet<Modifier>();

        result.add(new Modifier(Modifier.OFFENCE, spec.BASE_OFFENCE_SOURCE,
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
                result.add(new Modifier(Modifier.OFFENCE, spec.CARGO_PENALTY_SOURCE,
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
                    // In the open

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
                            ambushModifier.setSource(spec.AMBUSH_BONUS_SOURCE);
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
     * Return the defensive power of the defender versus the
     * bombarding colony.
     * 
     * @param colony a <code>Colony</code> value
     * @param defender a <code>Unit</code> value
     * @return an <code>float</code> value
     */
    public float getDefencePower(Colony colony, Unit defender) {
        return defender.getType().getDefence();
    }

    /**
     * Return the defensive power of the defender versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>float</code> value
     */
    public float getDefencePower(Unit attacker, Unit defender) {
        return FeatureContainer.applyModifierSet(0, attacker.getGame().getTurn(),
                                                 getDefensiveModifiers(attacker, defender));
    }

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the bombarding colony.
     * 
     * @param colony a <code>Colony</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getDefensiveModifiers(Colony colony, Unit defender) {
        Set<Modifier> result = new LinkedHashSet<Modifier>();
        result.add(new Modifier("model.modifier.defenceBonus",
                                defender.getType().getDefence(),
                                Modifier.Type.ADDITIVE));
        return result;
    }

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getDefensiveModifiers(Unit attacker, Unit defender) {
        Specification spec = Specification.getSpecification();
        Set<Modifier> result = new LinkedHashSet<Modifier>();
        if (defender == null) {
            return result;
        }

        result.add(new Modifier(Modifier.DEFENCE, spec.BASE_DEFENCE_SOURCE,
                                defender.getType().getDefence(),
                                Modifier.Type.ADDITIVE));
        result.addAll(defender.getType().getFeatureContainer()
                      .getModifierSet(Modifier.DEFENCE));


        if (defender.isNaval()) {
            int goodsCount = defender.getVisibleGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                result.add(new Modifier(Modifier.DEFENCE, spec.CARGO_PENALTY_SOURCE,
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
     * Attack a unit with the given outcome. This method ignores the damage parameter.
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
            defendingPlayer.setAttackedByPrivateers();
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
            defender.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                      "model.unit.automaticDefence", defender)
                                     .addName("%unit%", defender.getName())
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
    public void captureColony(Unit attacker, Colony colony, int plunderGold,
                              Location repairLocation) {
        logger.finest("Entering captureColony()");
        Player defendingPlayer = colony.getOwner();
        Player attackingPlayer = attacker.getOwner();

        defendingPlayer.modifyTension(attacker.getOwner(), Tension.TENSION_ADD_MAJOR);
        if (attackingPlayer.isEuropean()) {
            attackingPlayer.getHistory().add(new HistoryEvent(attackingPlayer.getGame().getTurn().getNumber(),
                                                       HistoryEvent.Type.CONQUER_COLONY,
                                                       "%nation%", defendingPlayer.getNationAsString(),
                                                       "%colony%", colony.getName()));
            defendingPlayer.getHistory().add(new HistoryEvent(defendingPlayer.getGame().getTurn().getNumber(),
                                                    HistoryEvent.Type.COLONY_CONQUERED,
                                                    "%colony%", colony.getName(),
                                                    "%nation%", attackingPlayer.getNationAsString()));
            defendingPlayer.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                             "model.unit.colonyCapturedBy", defendingPlayer)
                                            .addName("%colony%", colony.getName())
                                            .addAmount("%amount%", plunderGold)
                                            .addName("%player%", attackingPlayer.getNationAsString()));
            damageAllShips(colony, attacker, repairLocation);

            attackingPlayer.modifyGold(plunderGold);
            defendingPlayer.modifyGold(-plunderGold);
            defendingPlayer.divertModelMessages(colony, defendingPlayer);

            // This also changes over all of the units...
            colony.setOwner(attackingPlayer);
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
                                          HistoryEvent.Type.COLONY_DESTROYED,
                                          "%nation%", attackingPlayer.getNationAsString(),
                                          "%colony%", colony.getName()));
                defendingPlayer
                    .addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                      "model.unit.colonyBurning", defendingPlayer)
                                     .addName("%colony%", colony.getName())
                                     .addAmount("%amount%", plunderGold)
                                     .addName("%nation%", attackingPlayer.getNationAsString())
                                     .addName("%unit%", attacker.getName()));
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
                                                    .addName("%unit%", victim.getName())
                                                    .addName("%nation%", attackingPlayer.getNationAsString())
                                                    .addName("%enemyUnit%", attacker.getName()));
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
        
        String nation = attacker.getOwner().getNationAsString();
        String unitName = attacker.getName();
        String colonyName = colony.getName();
        
        int limit = buildingList.size() + goodsList.size() + shipList.size() + 1;
        int random = attacker.getGame().getModelController().getRandom(attacker.getId() + "pillageColony", limit);
                                                                       
        if (random < buildingList.size()) {
            Building building = buildingList.get(random);
            colony.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.buildingDamaged", colony)
                                   .add("%building%", building.getNameKey())
                                   .addName("%colony%", colonyName)
                                   .addName("%enemyNation%", nation)
                                   .addName("%enemyUnit%", unitName));
            building.damage();
        } else if (random < buildingList.size() + goodsList.size()) {
            Goods goods = goodsList.get(random - buildingList.size());
            goods.setAmount(Math.min(goods.getAmount() / 2, 50));
            colony.removeGoods(goods);
            if (attacker.getSpaceLeft() > 0) {
                attacker.add(goods);
            }
            colony.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.goodsStolen", colony, goods)
                                   .addAmount("%amount%", goods.getAmount())
                                   .add("%goods%", goods.getNameKey())
                                   .addName("%colony%", colonyName)
                                   .addName("%enemyNation%", nation)
                                   .addName("%enemyUnit%", unitName));
        } else if (random < buildingList.size() + goodsList.size() + shipList.size()) {
            Unit ship = shipList.get(random - buildingList.size() - goodsList.size());
            damageShip(ship, null, attacker, repairLocation);
        } else { // steal gold
            int gold = colony.getOwner().getGold() / 10;
            colony.getOwner().modifyGold(-gold);
            attacker.getOwner().modifyGold(gold);
            colony.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                    "model.unit.indianPlunder", colony)
                                   .addAmount("%amount%", gold)
                                   .addName("%colony%", colonyName)
                                   .addName("%enemyNation%", nation)
                                   .addName("%enemyUnit%", unitName));
        }
    }

    /**
     * Destroys an Indian settlement.
     * 
     * @param attacker an <code>Unit</code> value
     * @param settlement an <code>IndianSettlement</code> value
     */
    private void destroySettlement(Unit attacker, IndianSettlement settlement) {
        Player enemy = settlement.getOwner();
        boolean wasCapital = settlement.isCapital();
        Tile newTile = settlement.getTile();
        ModelController modelController = attacker.getGame().getModelController();
        SettlementType settlementType = ((IndianNationType) enemy.getNationType()).getTypeOfSettlement();
        String settlementName = settlement.getName();
        settlement.dispose();

        enemy.modifyTension(attacker.getOwner(), Tension.TENSION_ADD_MAJOR);

        List<UnitType> treasureUnitTypes = FreeCol.getSpecification()
            .getUnitTypesWithAbility("model.ability.carryTreasure");
        if (treasureUnitTypes.size() > 0) {
            int randomTreasure = modelController.getRandom(attacker.getId() + "indianTreasureRandom" + 
                                                           attacker.getId(), 11);
            int random = modelController.getRandom(attacker.getId() + "newUnitForTreasure" +
                                                   attacker.getId(), treasureUnitTypes.size());
            Unit tTrain = modelController.createUnit(attacker.getId() + "indianTreasure" +
                                                     attacker.getId(), newTile, attacker.getOwner(),
                                                     treasureUnitTypes.get(random));

            // Larger treasure if Hernan Cortes is present in the congress:
            Set<Modifier> modifierSet = attacker.getModifierSet("model.modifier.nativeTreasureModifier");
            randomTreasure = (int) FeatureContainer.applyModifierSet(randomTreasure, attacker.getGame().getTurn(),
                                                                     modifierSet);
            if (settlementType == SettlementType.INCA_CITY ||
                settlementType == SettlementType.AZTEC_CITY) {
                tTrain.setTreasureAmount(randomTreasure * 500 + 1000);
            } else {
                tTrain.setTreasureAmount(randomTreasure * 50  + 300);
            }

            // capitals give more gold
            if (wasCapital) {
                tTrain.setTreasureAmount((tTrain.getTreasureAmount() * 3) / 2);
            }

            attacker.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                      "model.unit.indianTreasure", attacker)
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
                                  HistoryEvent.Type.DESTROY_SETTLEMENT,
                                  "%nation%", enemy.getNationAsString(),
                                  "%settlement%", settlementName));
        if (enemy.getSettlements().isEmpty()) {
            attacker.getOwner().getHistory()
                .add(new HistoryEvent(attacker.getGame().getTurn().getNumber(),
                                      HistoryEvent.Type.DESTROY_NATION,
                                      "%nation%", enemy.getNationAsString()));
        }
    }

    /**
     * Check whether some indian converts due to the attack or they burn all missions
     *
     * @param indianSettlement The attacked indian settlement
     */
    private void getConvert(Unit attacker, IndianSettlement indianSettlement) {
        ModelController modelController = attacker.getGame().getModelController();
        int random = modelController.getRandom(attacker.getId() + "getConvert", 100);
        int convertProbability = (int) FeatureContainer.applyModifierSet(Specification.getSpecification()
                .getIntegerOption("model.option.nativeConvertProbability").getValue(), attacker.getGame().getTurn(),
                attacker.getModifierSet("model.modifier.nativeConvertBonus"));
        // TODO: it should be bigger when tension is high
        int burnProbability = Specification.getSpecification().getIntegerOption("model.option.burnProbability")
                .getValue();
        
        if (random < convertProbability) {
            Unit missionary = indianSettlement.getMissionary();
            if (missionary != null && missionary.getOwner() == attacker.getOwner() &&
                attacker.getGame().getViewOwner() == null && indianSettlement.getUnitCount() > 1) {
                List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                if (converts.size() > 0) {
                    indianSettlement.getFirstUnit().dispose();
                    random = modelController.getRandom(attacker.getId() + "getConvertType", converts.size());
                    modelController.createUnit(attacker.getId() + "indianConvert", attacker.getLocation(),
                                               attacker.getOwner(), converts.get(random));
                }
            }
        } else if (random >= 100 - burnProbability) {
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
                attacker.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                          "model.unit.burnMissions",
                                                          attacker, indianSettlement)
                                         .addName("%nation%", attacker.getOwner().getNationAsString())
                                         .addName("%enemyNation%", indianSettlement.getOwner().getNationAsString()));
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
        String nation = defender.getApparentOwnerName();

        if (attackerColony != null) {
            attackerColony.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                            "model.unit.shipEvadedBombardment", attackerColony)
                                           .addName("%colony%", attackerColony.getName())
                                           .addName("%unit%", defender.getName())
                                           .addName("%nation%", nation));
            defender.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT, 
                                                      "model.unit.shipEvadedBombardment", defender)
                                     .addName("%colony%", attackerColony.getName())
                                     .addName("%unit%", defender.getName())
                                     .addName("%nation%", nation));
        } else if (attackerUnit != null) {
            String attackerNation = attackerUnit.getApparentOwnerName();

            attackerUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                          "model.unit.enemyShipEvaded", attackerUnit)
                                         .addName("%unit%", attackerUnit.getName())
                                         .addName("%enemyUnit%", defender.getName())
                                         .addName("%enemyNation%", nation));
            defender.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                      "model.unit.shipEvaded", defender)
                                     .addName("%unit%", defender.getName())
                                     .addName("%enemyUnit%", attackerUnit.getName())
                                     .addName("%enemyNation%", attackerNation));
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
        String nation = damagedShip.getApparentOwnerName();
        StringTemplate repairLocationName = (repairLocation == null)
            ? StringTemplate.name("")
            : repairLocation.getLocationName();

        if (attackerColony != null) {
            attackerColony.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                            "model.unit.enemyShipDamagedByBombardment",
                                                            attackerColony)
                                           .addName("%colony%", attackerColony.getName())
                                           .addName("%nation%", nation)
                                           .addName("%unit%", damagedShip.getName()));
        		
            damagedShip.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                         "model.unit.shipDamagedByBombardment", damagedShip)
                                        .addName("%colony%", attackerColony.getName())
                                        .addName("%unit%", damagedShip.getName())
                                        .addStringTemplate("%repairLocation%", repairLocationName));
        } else if (attackerUnit != null) {
            String attackerNation = attackerUnit.getApparentOwnerName();
            
            attackerUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                          "model.unit.enemyShipDamaged",
                                                          attackerUnit)
                                         .addName("%unit%", attackerUnit.getName())
                                         .addName("%enemyNation%", nation)
                                         .addName("%enemyUnit%", damagedShip.getName()));
            
            damagedShip.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                         "model.unit.shipDamaged",
                                                         damagedShip)
                                        .addName("%unit%", damagedShip.getName())
                                        .addName("%enemyUnit%", attackerUnit.getName())
                                        .addName("%enemyNation%", attackerNation)
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
        String nation = sinkingShip.getApparentOwnerName();

        if (attackerColony != null) {
            attackerColony.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                            "model.unit.shipSunkByBombardment",
                                                            attackerColony)
                                           .addName("%colony%", attackerColony.getName())
                                           .addName("%unit%", sinkingShip.getName())
                                           .addName("%nation%", nation));
            sinkingShip.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                         "model.unit.shipSunkByBombardment",
                                                         sinkingShip)
                                        .addName("%colony%", attackerColony.getName())
                                        .addName("%unit%", sinkingShip.getName()));
        } else if (attackerUnit != null) {
            String attackerNation = attackerUnit.getApparentOwnerName();

            attackerUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                          "model.unit.enemyShipSunk",
                                                          attackerUnit)
                                         .addName("%unit%", attackerUnit.getName())
                                         .addName("%enemyUnit%", sinkingShip.getName())
                                         .addName("%enemyNation%", nation));
            sinkingShip.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                         "model.unit.shipSunk",
                                                         sinkingShip)
                                        .addName("%unit%", sinkingShip.getName())
                                        .addName("%enemyUnit%", attackerUnit.getName())
                                        .addName("%enemyNation%", attackerNation));
        }
        sinkingShip.getOwner().divertModelMessages(sinkingShip,
                                                   sinkingShip.getTile());
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
        String nation = loser.getNationAsString();
        String oldName = unit.getName();
        String enemyNation = enemyUnit.getOwner().getNationAsString();
        String messageID = Messages.getKey(unit.getType().getId() + ".captured",
                                           "model.unit.unitCaptured");

        // unit is about to change sides, launch message now!
        unit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                              messageID, unit)
                             .addName("%nation%", nation)
                             .addName("%unit%", oldName)
                             .addName("%enemyNation%", enemyNation)
                             .addName("%enemyUnit%", enemyUnit.getName())
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
        enemyUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   messageID, enemyUnit)
                                  .addName("%nation%", nation)
                                  .addName("%unit%", oldName)
                                  .addName("%enemyNation%", enemyNation)
                                  .addName("%enemyUnit%", enemyUnit.getName())
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
        String nation = unit.getOwner().getNationAsString();
        String oldName = unit.getName();
        String enemyNation = enemyUnit.getOwner().getNationAsString();
        String messageID = Messages.getKey(unit.getType().getId() + ".demoted",
                                           "model.unit.unitDemoted");

        unit.setType(downgrade);
        enemyUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   messageID, enemyUnit)
                                  .addName("%nation%", nation)
                                  .addName("%oldName%", oldName)
                                  .addName("%unit%", unit.getName())
                                  .addName("%enemyNation%", enemyNation)
                                  .addName("%enemyUnit%", enemyUnit.getName())
                                  .addStringTemplate("%location%", locationName));
        unit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                              messageID, unit)
                             .addName("%nation%", nation)
                             .addName("%oldName%", oldName)
                             .addName("%unit%", unit.getName())
                             .addName("%enemyNation%", enemyNation)
                             .addName("%enemyUnit%", enemyUnit.getName())
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
        String nation = unit.getOwner().getNationAsString();
        String oldName = unit.getName();
        String enemyNation = enemyUnit.getOwner().getNationAsString();
        String messageID = Messages.getKey(unit.getType().getId() + ".demoted",
                                           "model.unit.unitDemoted");

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
        enemyUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   messageID, enemyUnit)
                                  .addName("%nation%", nation)
                                  .addName("%oldName%", oldName)
                                  .addName("%unit%", unit.getName())
                                  .addName("%enemyNation%", enemyNation)
                                  .addName("%enemyUnit%", enemyUnit.getName())
                                  .addStringTemplate("%location%", locationName));
        unit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                              messageID, unit)
                             .addName("%nation%", nation)
                             .addName("%oldName%", oldName)
                             .addName("%unit%", unit.getName())
                             .addName("%enemyNation%", enemyNation)
                             .addName("%enemyUnit%", enemyUnit.getName())
                             .addStringTemplate("%location%", locationName));
               
        enemyCapturesEquipment(unit,enemyUnit,typeToLose);
    }

    private void enemyCapturesEquipment(Unit unit, Unit enemyUnit, EquipmentType typeToLose) {
        if (!enemyUnit.hasAbility("model.ability.captureEquipment")){
            return;
        }
        
        // we may need to change the equipment type if:
        //    - the attacker is an indian and the defender is not, or vice-versa
        //  and
        //    - the equipment is muskets or horses
        EquipmentType newEquipType = typeToLose;
        boolean defenderIsIndian = unit.getOwner().isIndian();
        boolean attackerIsIndian = enemyUnit.getOwner().isIndian();
        if(!defenderIsIndian && attackerIsIndian){
            if(typeToLose == FreeCol.getSpecification().getEquipmentType("model.equipment.horses")){
                newEquipType = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.horses");
            }
            if(typeToLose == FreeCol.getSpecification().getEquipmentType("model.equipment.muskets")){
                newEquipType = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.muskets");
            }
        }
        if(defenderIsIndian && !attackerIsIndian){
            if(typeToLose == FreeCol.getSpecification().getEquipmentType("model.equipment.indian.horses")){
                newEquipType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
            }
            if(typeToLose == FreeCol.getSpecification().getEquipmentType("model.equipment.indian.muskets")){
                newEquipType = FreeCol.getSpecification().getEquipmentType("model.equipment.muskets");
            }
        }
        
        if(!enemyUnit.canBeEquippedWith(newEquipType)){
            return;
        }
        
        enemyUnit.equipWith(newEquipType, 1, true);
        unit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                              "model.unit.equipmentCaptured", unit)
                             .addName("%nation%", enemyUnit.getOwner().getNationAsString())
                             .add("%equipment%", typeToLose.getNameKey()));
        IndianSettlement settlement = enemyUnit.getIndianSettlement();
        if (settlement != null) {
            for (AbstractGoods goods : typeToLose.getGoodsRequired()) {
                settlement.addGoods(goods);
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
        String nation = loser.getNationAsString();
        String enemyNation = enemyUnit.getOwner().getNationAsString();
        String messageID = Messages.getKey(unit.getType().getId() + ".destroyed",
                                           "model.unit.unitSlaughtered");

        enemyUnit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                   messageID, enemyUnit)
                                  .addName("%nation%", nation)
                                  .addName("%unit%", unit.getName())
                                  .addName("%enemyNation%", enemyNation)
                                  .addName("%enemyUnit%", enemyUnit.getName())
                                  .addStringTemplate("%location%", locationName));
        unit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                              messageID, unit)
                             .addName("%nation%", nation)
                             .addName("%unit%", unit.getName())
                             .addName("%enemyNation%", enemyNation)
                             .addName("%enemyUnit%", enemyUnit.getName())
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
        String oldName = unit.getName();
        String nation = unit.getOwner().getNationAsString();
        UnitType newType = unit.getType().getUnitTypeChange(ChangeType.PROMOTION, unit.getOwner());
        
        if (newType != null && newType.isAvailableTo(unit.getOwner())) {
            unit.setType(newType);
            if (unit.getType().equals(newType)) {
                // the new unit type was successfully applied
                unit.addModelMessage(new ModelMessage(ModelMessage.MessageType.COMBAT_RESULT,
                                                      "model.unit.unitPromoted", unit)
                                     .addName("%oldName%", oldName)
                                     .addName("%unit%", unit.getName())
                                     .addName("%nation%", nation));
            }
        }
    }
}
