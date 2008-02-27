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
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType.DowngradeType;

/**
 * This class implements the original Colonization combat model.
 */
public class SimpleCombatModel implements CombatModel {

    private static final Logger logger = Logger.getLogger(SimpleCombatModel.class.getName());

    private static PseudoRandom random;

    public static final Modifier SMALL_MOVEMENT_PENALTY =
        new Modifier("model.modifier.offence", "modifiers.movementPenalty",
                     -33, Modifier.Type.PERCENTAGE);
    public static final Modifier BIG_MOVEMENT_PENALTY =
        new Modifier("model.modifier.offence", "modifiers.movementPenalty",
                     -66, Modifier.Type.PERCENTAGE);
    public static final Modifier ARTILLERY_PENALTY =
        new Modifier("model.modifier.offence", "modifiers.artilleryPenalty",
                     -75, Modifier.Type.PERCENTAGE);
    public static final Modifier ATTACK_BONUS =
        new Modifier("model.modifier.offence", "modifiers.attackBonus",
                     50, Modifier.Type.PERCENTAGE);
    public static final Modifier FORTIFICATION_BONUS =
        new Modifier("model.modifier.defence", "modifiers.fortified",
                     50, Modifier.Type.PERCENTAGE);
    public static final Modifier INDIAN_RAID_BONUS =
        new Modifier("model.modifier.defence", "modifiers.artilleryAgainstRaid",
                     100, Modifier.Type.PERCENTAGE);

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

        int r = random.nextInt(100);
        
        CombatResultType result = CombatResultType.EVADES;
        if (r <= victory * 20) {
            // 10% of the times winning:
            result = CombatResultType.GREAT_WIN;
        } else if (r <= 100 * victory) {
            // 90% of the times winning:
            result = CombatResultType.WIN;
        } else if (defender.isNaval()
                && r <= (80 * victory) + 20) {
            // 20% of the times loosing:
            result = CombatResultType.EVADES;
        } else if (r <= (10 * victory) + 90) {
            // 70% of the times loosing:
            result = CombatResultType.LOSS;
        } else {
            // 10% of the times loosing:
            result = CombatResultType.GREAT_LOSS;
        }
        
        if (result.compareTo(CombatResultType.WIN) >= 0 &&
            defender.getTile().getSettlement() != null) {
            final boolean lastDefender;
            if (defender.getTile().getSettlement() instanceof Colony) {
                lastDefender = !defender.isDefensiveUnit();
            } else if (defender.getTile().getSettlement() instanceof IndianSettlement) {
                final int defenders = defender.getTile().getUnitCount()
                        + defender.getTile().getSettlement().getUnitCount();
                lastDefender = (defenders <= 1);
            } else {
                throw new IllegalStateException("Unknown Settlement.");
            }
            if (lastDefender) {
                result = CombatResultType.DONE_SETTLEMENT;
            }
        }
        return new CombatResult(result, 0);
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
        CombatResultType result = CombatResultType.EVADES;
        int r = random.nextInt(Math.round(totalProbability) + 1);
        if (r < attackPower) {
            int diff = Math.round(defencePower * 2 - attackPower);
            int r2 = random.nextInt((diff < 3) ? 3 : diff);
            if (r2 == 0) {
                result = CombatResultType.GREAT_WIN;
            } else {
                result = CombatResultType.WIN;
            }
        }
        return new CombatResult(result, 0);
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
                }
            }
            if (attackPower > 48) {
                attackPower = 48;
            }
        }
        return attackPower;
    }
    
    /**
     * Return the offensive power of the attacker versus the defender.
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
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getOffensiveModifiers(Unit attacker, Unit defender) {
        Set<Modifier> result = new LinkedHashSet<Modifier>();

        result.add(new Modifier("model.modifier.offence",
                                "modifiers.baseOffence",
                                attacker.getType().getOffence(),
                                Modifier.Type.ADDITIVE));

        if (attacker.isNaval()) {
            int goodsCount = attacker.getGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                result.add(new Modifier("model.modifier.offence",
                                        "modifiers.cargoPenalty",
                                        -12.5f * goodsCount,
                                        Modifier.Type.PERCENTAGE));
            }
            /* this should no longer be necessary, as Drake adds an
             * offence bonus directly
            if (attacker.hasAbility("model.ability.piracy")) {
                result.addAll(attacker.getModifierSet("model.modifier.piracyBonus"));
            }
            */
        } else {
            for (EquipmentType equipment : attacker.getEquipment()) {
                result.addAll(equipment.getFeatureContainer().getModifierSet("model.modifier.offence"));
            }
            result.addAll(attacker.getType().getFeatureContainer()
                          .getModifierSet("model.modifier.offence"));

            // 50% attack bonus
            result.add(ATTACK_BONUS);
            // movement penalty
            int movesLeft = attacker.getMovesLeft();
            if (movesLeft == 1) {
                result.add(BIG_MOVEMENT_PENALTY);
            } else if (movesLeft == 2) {
                result.add(SMALL_MOVEMENT_PENALTY);
            }

            // In the open
            if (defender != null && defender.getTile() != null && defender.getTile().getSettlement() == null) {

                /**
                 * Ambush bonus in the open = defender's defence bonus, if
                 * defender is REF, or attacker is indian.
                 */
                if (attacker.hasAbility("model.ability.ambushBonus") ||
                    defender.hasAbility("model.ability.ambushPenalty")) {
                    result.add(new Modifier("model.modifier.offence",
                                            "modifiers.ambushBonus", 
                                            defender.getTile().defenceBonus(),
                                            Modifier.Type.PERCENTAGE));
                }

                // 75% Artillery in the open penalty
                // TODO: is it right? or should it be another ability?
                if (attacker.hasAbility("model.ability.bombard")) {
                    result.add(ARTILLERY_PENALTY);
                }
            }

            // Attacking a settlement
            if (defender != null && defender.getTile() != null && defender.getTile().getSettlement() != null) {
                // REF bombardment bonus
                result.addAll(attacker.getModifierSet("model.modifier.bombardBonus"));
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

        Set<Modifier> result = new LinkedHashSet<Modifier>();
        if (defender == null) {
            return result;
        }

        result.add(new Modifier("model.modifier.defence",
                                "modifiers.baseDefence",
                                defender.getType().getDefence(),
                                Modifier.Type.ADDITIVE));

        if (defender.isNaval()) {
            int goodsCount = defender.getVisibleGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                result.add(new Modifier("model.modifier.defence", 
                                        "modifiers.cargoPenalty",
                                        -12.5f * goodsCount,
                                        Modifier.Type.PERCENTAGE));
            }
            /* this should no longer be necessary, as Drake adds an
             * defence bonus directly
            if (defender.hasAbility("model.ability.piracy")) {
                result.addAll(defender.getModifierSet("model.modifier.piracyBonus"));
            }
            */
        } else {
            // Paul Revere makes an unarmed colonist in a settlement pick up
            // a stock-piled musket if attacked, so the bonus should be applied
            // for unarmed colonists inside colonies where there are muskets
            // available.
            if (!defender.isArmed() &&
                defender.isColonist() &&
                defender.getLocation() instanceof WorkLocation &&
                defender.getOwner().hasAbility("model.ability.automaticDefence") &&
                defender.getColony().getGoodsCount(Goods.MUSKETS) >= 50) {
                Set<Ability> autoDefence = defender.getOwner().getFeatureContainer()
                    .getAbilitySet("model.ability.automaticDefence");
                result.add(new Modifier("model.modifier.defence", 
                                        autoDefence.iterator().next().getSource(),
                                        1, Modifier.Type.ADDITIVE));
            }

            for (EquipmentType equipment : attacker.getEquipment()) {
                result.addAll(equipment.getFeatureContainer().getModifierSet("model.modifier.defence"));
            }
            // 50% veteran bonus
            result.addAll(attacker.getType().getFeatureContainer()
                          .getModifierSet("model.modifier.defence"));

            // 50% fortify bonus
            if (defender.getState() == UnitState.FORTIFIED) {
                result.add(FORTIFICATION_BONUS);
            }

            if (defender.getTile() != null && defender.getTile().getSettlement() != null) {
                result.addAll(defender.getTile().getSettlement().getFeatureContainer()
                              .getModifierSet("model.modifier.defence"));
                // TODO: is it right? or should it be another ability?
                if (defender.hasAbility("model.ability.bombard") &&
                    attacker.getOwner().isIndian()) {
                    // 100% defence bonus against an Indian raid
                    result.add(INDIAN_RAID_BONUS);
                }
            } else if (defender.getTile() != null) {
                // In the open
                if (!(attacker.hasAbility("model.ability.ambushBonus") ||
                      defender.hasAbility("model.ability.ambushPenalty"))) {
                    // Terrain defensive bonus.
                    result.add(new Modifier("model.modifier.defence", 
                                            "modifiers.terrainBonus",
                                            defender.getTile().defenceBonus(),
                                            Modifier.Type.PERCENTAGE));
                }
                // TODO: is it right? or should it be another ability?
                if (defender.hasAbility("model.ability.bombard") &&
                    defender.getState() != UnitState.FORTIFIED) {
                    // -75% Artillery in the Open penalty
                    result.add(ARTILLERY_PENALTY);
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
     */
    public void attack(Unit attacker, Unit defender, CombatResult result, int plunderGold) {
        Player attackingPlayer = attacker.getOwner();
        Player defendingPlayer = defender.getOwner();

        if (attackingPlayer.getStance(defendingPlayer) == Stance.ALLIANCE) {
            throw new IllegalStateException("Cannot attack allied players.");
        }

        // make sure we are at war, unless one of both units is a privateer
        //getOwner().isEuropean() && defendingPlayer.isEuropean() &&
        if (attacker.hasAbility("model.ability.piracy")) {
            defendingPlayer.setAttackedByPrivateers();
        } else if (!defender.hasAbility("model.ability.piracy")) {
            attackingPlayer.setStance(defendingPlayer, Stance.WAR);
            defendingPlayer.setStance(attackingPlayer, Stance.WAR);
        }

        // Wake up if you're attacking something.
        // Before, a unit could stay fortified during execution of an
        // attack. - sjm
        attacker.setState(UnitState.ACTIVE);
        
        // The Revenger unit can attack multiple times
        // Other units can only attack once
        if (!attacker.hasAbility("model.ability.multipleAttacks")) {
            attacker.setMovesLeft(0);
        }

        Tile newTile = defender.getTile();
        //attacker.adjustTension(defender);
        Settlement settlement = newTile.getSettlement();

        switch (result.type) {
        case EVADES:
            if (attacker.isNaval()) {
                // send message to both parties
                attacker.addModelMessage(attacker, "model.unit.enemyShipEvaded",
                                         new String[][] {
                                             { "%unit%", attacker.getName() },
                                             { "%enemyUnit%", defender.getName() },
                                             { "%enemyNation%", defendingPlayer.getNationAsString() }
                                         }, ModelMessage.COMBAT_RESULT, attacker);
                defender.addModelMessage(defender, "model.unit.shipEvaded",
                                         new String[][] {
                                             { "%unit%", defender.getName() },
                                             { "%enemyUnit%", attacker.getName() },
                                             { "%enemyNation%", attackingPlayer.getNationAsString() }
                                         }, ModelMessage.COMBAT_RESULT, defender);
            } else {
                logger.warning("Non-naval unit evades!");
            }
            break;
        case LOSS:
            if (attacker.isNaval()) {
                Location repairLocation = attackingPlayer.getRepairLocation(attacker);
                damageShip(attacker, null);
                attacker.addModelMessage(attacker, "model.unit.shipDamaged",
                                         new String[][] {
                                             { "%unit%", attacker.getName() },
                                             { "%repairLocation%", repairLocation.getLocationName() },
                                             { "%enemyUnit%", defender.getName() },
                                             { "%enemyNation%", defendingPlayer.getNationAsString() }
                                         }, ModelMessage.UNIT_DEMOTED);
                defender.addModelMessage(defender, "model.unit.enemyShipDamaged",
                                         new String[][] {
                                             { "%unit%", defender.getName() },
                                             { "%enemyUnit%", attacker.getName() },
                                             { "%enemyNation%", attackingPlayer.getNationAsString() },
                                         }, ModelMessage.COMBAT_RESULT);
            } else {
                demote(attacker, defender);
                if (defendingPlayer.hasAbility("model.ability.automaticPromotion")) {
                    promote(defender);
                }
            }
            break;
        case GREAT_LOSS:
            if (attacker.isNaval()) {
                sinkShip(attacker, null);
                attacker.addModelMessage(attacker, "model.unit.sinkShip",
                                         new String[][] {
                                             { "%unit%", attacker.getName() },
                                             { "%enemyUnit%", defender.getName() },
                                             { "%enemyNation%", defendingPlayer.getNationAsString() }
                                         }, ModelMessage.UNIT_LOST);
                defender.addModelMessage(defender, "model.unit.enemyShipSunk",
                                         new String[][] {
                                             { "%unit%", defender.getName() },
                                             { "%enemyUnit%", attacker.getName() },
                                             { "%enemyNation%", attackingPlayer.getNationAsString() }
                                         }, ModelMessage.COMBAT_RESULT);
            } else {
                demote(attacker, defender);
                promote(defender);
            }
            break;
        case DONE_SETTLEMENT:
            if (settlement instanceof IndianSettlement) {
                defender.dispose();
                destroySettlement(attacker, (IndianSettlement) settlement);
            } else if (settlement instanceof Colony) {
                captureColony(attacker, (Colony) settlement, plunderGold);
            } else {
                throw new IllegalStateException("Unknown type of settlement.");
            }
            break;
        case WIN:
            if (attacker.isNaval()) {
                Location repairLocation = defendingPlayer.getRepairLocation(defender);
                attacker.captureGoods(defender);
                damageShip(defender, null);
                attacker.addModelMessage(attacker, "model.unit.enemyShipDamaged",
                                         new String[][] {
                                             { "%unit%", attacker.getName() },
                                             { "%enemyUnit%", defender.getName() },
                                             { "%enemyNation%", defendingPlayer.getNationAsString() }
                                         }, ModelMessage.COMBAT_RESULT);
                defender.addModelMessage(defender, "model.unit.damageShip",
                                         new String[][] {
                                             { "%unit%", defender.getName() },
                                             { "%repairLocation%", repairLocation.getLocationName() },
                                             { "%enemyUnit%", attacker.getName() },
                                             { "%enemyNation%", attackingPlayer.getNationAsString() },
                                         }, ModelMessage.UNIT_DEMOTED);
            } else if (attacker.hasAbility("model.ability.pillageUnprotectedColony") && 
                       !defender.isDefensiveUnit() &&
                       defender.getColony() != null &&
                       !defender.getColony().hasStockade()) {
                pillageColony(attacker, defender.getColony());
            } else {
                if (attacker.hasAbility("model.ability.automaticPromotion")) {
                    promote(attacker);
                }
                if (!defender.isNaval()) {
                    demote(defender, attacker);
                    if (settlement instanceof IndianSettlement) {
                        getConvert(attacker, (IndianSettlement) settlement);
                    }
                }
            }
            break;
        case GREAT_WIN:
            if (attacker.isNaval()) {
                attacker.captureGoods(defender);
                sinkShip(defender, null);
                attacker.addModelMessage(attacker, "model.unit.enemyShipSunk",
                                         new String[][] {
                                             { "%unit%", attacker.getName() },
                                             { "%enemyUnit%", defender.getName() },
                                             { "%enemyNation%", defendingPlayer.getNationAsString() }
                                         }, ModelMessage.COMBAT_RESULT);
                defender.addModelMessage(defender, "model.unit.shipSunk",
                                         new String[][] {
                                             { "%unit%", defender.getName() },
                                             { "%enemyUnit%", attacker.getName() },
                                             { "%enemyNation%", attackingPlayer.getNationAsString() }
                                         }, ModelMessage.UNIT_LOST);
            } else {
                promote(attacker);
                if (!defender.isNaval()) {
                    demote(defender, attacker);
                    if (settlement instanceof IndianSettlement) {
                        getConvert(attacker, (IndianSettlement) settlement);
                    }
                }
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
    public void bombard(Colony colony, Unit defender, CombatResult result) {

        Player attackingPlayer = colony.getOwner();
        Player defendingPlayer = defender.getOwner();
        
        switch (result.type) {
        case EVADES:
            // send message to both parties
            attackingPlayer.addModelMessage(colony, "model.unit.shipEvadedBombardment",
                                            new String[][] {
                                                { "%colony%", colony.getName() },
                                                { "%unit%", defender.getName() },
                                                { "%nation%", defender.getOwner().getNationAsString() } }, 
                                            ModelMessage.DEFAULT, colony);
            defendingPlayer.addModelMessage(defender, "model.unit.shipEvadedBombardment",
                                            new String[][] { { "%colony%", colony.getName() },
                                                             { "%unit%", defender.getName() },
                                                             { "%nation%", defender.getOwner().getNationAsString() } }, 
                                            ModelMessage.DEFAULT, colony);
            break;
        case WIN:
            damageShip(defender, colony);
            attackingPlayer.addModelMessage(colony, "model.unit.enemyShipDamagedByBombardment",
                                            new String[][] {
                                                { "%colony%", colony.getName() },
                                                { "%unit%", defender.getName() },
                                                { "%nation%", defender.getOwner().getNationAsString() }
                                            }, ModelMessage.UNIT_DEMOTED);
            break;
        case GREAT_WIN:
            sinkShip(defender, colony);
            defendingPlayer.addModelMessage(colony, "model.unit.shipSunkByBombardment",
                                            new String[][] {
                                                { "%colony%", colony.getName() },
                                                { "%unit%", defender.getName() },
                                                { "%nation%", defender.getOwner().getNationAsString() } },
                                            ModelMessage.UNIT_DEMOTED);
            break;
        }
    }

    /**
     * Captures an enemy colony and plunders gold.
     * 
     * @param unit an <code>Unit</code> value
     * @param colony a <code>Colony</code> value
     * @param plunderGold The amount of gold to plunder.
     */
    public void captureColony(Unit unit, Colony colony, int plunderGold) {
        Player enemy = colony.getOwner();
        Player myPlayer = unit.getOwner();
        enemy.modifyTension(unit.getOwner(), Tension.TENSION_ADD_MAJOR);

        if (myPlayer.isEuropean()) {
            enemy.addModelMessage(enemy, "model.unit.colonyCapturedBy",
                                  new String[][] {
                                      { "%colony%", colony.getName() },
                                      { "%amount%", Integer.toString(plunderGold) },
                                      { "%player%", myPlayer.getNationAsString() }
                                  }, ModelMessage.DEFAULT);
            damageAllShips(colony);

            myPlayer.modifyGold(plunderGold);
            enemy.modifyGold(-plunderGold);

            colony.setOwner(myPlayer); // This also changes over all of the
            // units...
            myPlayer.addModelMessage(colony, "model.unit.colonyCaptured", 
                                     new String[][] {
                                         { "%colony%", colony.getName() },
                                         { "%amount%", Integer.toString(plunderGold) }
                                     }, ModelMessage.DEFAULT);

            // Demote all soldiers and clear all orders:
            for (Unit capturedUnit : colony.getTile().getUnitList()) {
                if (unit.isUndead()) {
                    capturedUnit.setType(unit.getType());
                } else {
                    UnitType downgrade = capturedUnit.getType().getDowngrade(DowngradeType.CAPTURE);
                    if (downgrade != null) {
                        capturedUnit.setType(downgrade);
                    }
                }
                capturedUnit.setState(UnitState.ACTIVE);
            }

            if (unit.isUndead()) {
                for (Unit capturedUnit : colony.getUnitList()) {
                    capturedUnit.setType(unit.getType());
                }

                unit.setLocation(colony.getTile());
            } else { // Indian:
                if (colony.getUnitCount() <= 1) {
                    myPlayer.modifyGold(plunderGold);
                    enemy.modifyGold(-plunderGold);
                    myPlayer.addModelMessage(enemy, "model.unit.colonyBurning",
                                             new String[][] { { "%colony%", colony.getName() },
                                                              { "%amount%", Integer.toString(plunderGold) },
                                                              { "%nation%", myPlayer.getNationAsString() },
                                                              { "%unit%", unit.getName() }
                                             }, ModelMessage.DEFAULT);
                    damageAllShips(colony);
                    colony.dispose();
                } else {
                    Unit victim = colony.getRandomUnit();
                    if (victim == null) {
                        return;
                    }
                    myPlayer.addModelMessage(colony, "model.unit.colonistSlaughtered",
                                             new String[][] {
                                                 { "%colony%", colony.getName() },
                                                 { "%unit%", victim.getName() },
                                                 { "%nation%", myPlayer.getNationAsString() },
                                                 { "%enemyUnit%", unit.getName() }
                                             }, ModelMessage.UNIT_LOST);
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
    private void damageAllShips(Colony colony) {
        for (Unit unit : colony.getTile().getUnitList()) {
            if (unit.isNaval()) {
                damageShip(unit, null);
            }
        }
    }

    /**
     * Damage a building or a ship or steal some goods or gold. It's called
     * from attack when an indian attacks a colony and lose the combat with
     * LOSS as result
     *
     * @param colony The attacked colony
     */
    private void pillageColony(Unit attacker, Colony colony) {
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
            colony.addModelMessage(colony, "model.unit.buildingDamaged",
                                   new String[][] {
                                       {"%building%", building.getName()}, {"%colony%", colonyName},
                                       {"%enemyNation%", nation}, {"%enemyUnit%", unitName}},
                                   ModelMessage.DEFAULT, colony);
            building.damage();
        } else if (random < buildingList.size() + goodsList.size()) {
            Goods goods = goodsList.get(random - buildingList.size());
            goods.setAmount(Math.min(goods.getAmount() / 2, 50));
            colony.removeGoods(goods);
            if (attacker.getSpaceLeft() > 0) {
                attacker.add(goods);
            }
            colony.addModelMessage(colony, "model.unit.goodsStolen",
                                   new String[][] {
                                       {"%amount%", String.valueOf(goods.getAmount())},
                                       {"%goods%", goods.getName()}, {"%colony%", colonyName},
                                       {"%enemyNation%", nation}, {"%enemyUnit%", unitName}},
                                   ModelMessage.DEFAULT, goods);
        } else if (random < buildingList.size() + goodsList.size() + shipList.size()) {
            Unit ship = shipList.get(random - buildingList.size() - goodsList.size());
            damageShip(ship, null);
        } else { // steal gold
            int gold = colony.getOwner().getGold() / 10;
            colony.getOwner().modifyGold(-gold);
            attacker.getOwner().modifyGold(gold);
            colony.addModelMessage(colony, "model.unit.indianPlunder",
                                   new String[][] {
                                       {"%amount%", String.valueOf(gold)}, {"%colony%", colonyName},
                                       {"%enemyNation%", nation}, {"%enemyUnit%", unitName}},
                                   ModelMessage.DEFAULT, colony);
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
            SettlementType settlementType = ((IndianNationType) enemy.getNationType()).getTypeOfSettlement();
            if (settlementType == SettlementType.INCA_CITY ||
                settlementType == SettlementType.AZTEC_CITY) {
                tTrain.setTreasureAmount(randomTreasure * 500 + 10000);
            } else {
                tTrain.setTreasureAmount(randomTreasure * 50  + 300);
            }

            // capitals give more gold
            if (wasCapital) {
                tTrain.setTreasureAmount((tTrain.getTreasureAmount() * 3) / 2);
            }

            attacker.addModelMessage(attacker, "model.unit.indianTreasure", new String[][] {
                    { "%indian%", enemy.getNationAsString() },
                    { "%amount%", Integer.toString(tTrain.getTreasureAmount()) }
                }, ModelMessage.DEFAULT);
        }
        attacker.setLocation(newTile);
    }

    /**
     * Check whether some indian converts due to the attack or they burn all missions
     *
     * @param indianSettlement The attacked indian settlement
     */
    private void getConvert(Unit attacker, IndianSettlement indianSettlement) {
        ModelController modelController = attacker.getGame().getModelController();
        int random = modelController.getRandom(attacker.getId() + "getConvert", 100);
        int convertProbability = (int) FeatureContainer
            .applyModifierSet(attacker.getOwner().getDifficulty().getNativeConvertProbability(),
                              attacker.getGame().getTurn(),
                              attacker.getModifierSet("model.ability.nativeConvertBonus"));
        // TODO: it should be bigger when tension is high
        int burnProbability = attacker.getOwner().getDifficulty().getBurnProbability();
        
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
                attacker.addModelMessage(attacker, "model.unit.burnMissions", new String[][] {
                        {"%nation%", attacker.getOwner().getNationAsString()},
                        {"%enemyNation%", indianSettlement.getOwner().getNationAsString()}},
                    ModelMessage.DEFAULT, indianSettlement);
            }
        }
    }

    /**
     * Sets the damage to this ship and sends it to its repair location.
     * 
     * @param colony The colony that opened fire on this unit.
     */
    private void damageShip(Unit damagedUnit, Colony colony) {
        String nation = damagedUnit.getOwner().getNationAsString();
        Location repairLocation = damagedUnit.getOwner().getRepairLocation(damagedUnit);
        if (repairLocation == null) {
            // This fixes a problem with enemy ships without a known repair
            // location.
            damagedUnit.dispose();
            return;
        }
        String repairLocationName = repairLocation.getLocationName();
        if (colony != null) {
            damagedUnit.addModelMessage(damagedUnit, "model.unit.damageShipByBombardment", 
                                        new String[][] {
                                            { "%colony%", colony.getName() },
                                            { "%unit%", damagedUnit.getName() },
                                            { "%repairLocation%", repairLocationName },
                                            { "%nation%", nation } },
                                        ModelMessage.UNIT_DEMOTED);
        } else {
            damagedUnit.addModelMessage(damagedUnit, "model.unit.damageShip",
                                        new String[][] {
                                            { "%unit%", damagedUnit.getName() },
                                            { "%repairLocation%", repairLocationName },
                                            { "%nation%", nation } },
                                        ModelMessage.UNIT_DEMOTED);
        }
        damagedUnit.setHitpoints(1);
        damagedUnit.getUnitContainer().disposeAllUnits();
        damagedUnit.getGoodsContainer().removeAll();
        damagedUnit.sendToRepairLocation();
    }

    /**
     * Sinks this ship.
     * 
     * @param colony The colony that opened fire on this unit.
     */
    private void sinkShip(Unit damagedUnit, Colony colony) {
        String nation = damagedUnit.getOwner().getNationAsString();
        if (colony != null) {
            damagedUnit.addModelMessage(damagedUnit, "model.unit.sinkShipByBombardment",
                                        new String[][] {
                                            { "%colony%", colony.getName() },
                                            { "%unit%", damagedUnit.getName() },
                                            { "%nation%", nation }
                                        }, ModelMessage.UNIT_LOST);
        } else {
            damagedUnit.addModelMessage(damagedUnit, "model.unit.sinkShip",
                                        new String[][] {
                                            { "%unit%", damagedUnit.getName() },
                                            { "%nation%", nation }
                                        }, ModelMessage.UNIT_LOST);
        }
        damagedUnit.dispose();
    }

    /**
     * Demotes a unit. A unit that can not be further demoted is
     * captured or destroyed. The enemy may plunder equipment.
     *
     * @param unit a <code>Unit</code> value
     * @param enemyUnit a <code>Unit</code> value
     */
    private void demote(Unit unit, Unit enemyUnit) {
        UnitType oldType = unit.getType();
        String messageID = "model.unit.unitDemoted";
        String nation = unit.getOwner().getNationAsString();
        int messageType = ModelMessage.UNIT_LOST;

        if (unit.hasAbility("model.ability.canBeCaptured")) {
            if (enemyUnit.hasAbility("model.ability.captureUnits")) {
                unit.setLocation(enemyUnit.getTile());
                unit.setOwner(enemyUnit.getOwner());
                if (enemyUnit.isUndead()) {
                    unit.setType(enemyUnit.getType());
                } else {
                    UnitType downgrade = unit.getType().getDowngrade(DowngradeType.CAPTURE);
                    if (downgrade != null) {
                        unit.setType(downgrade);
                    }
                }
                String tempID = oldType.getId() + ".captured";
                if (Messages.containsKey(tempID)) {
                    messageID = tempID;
                } else {
                    messageID = "model.unit.unitCaptured";
                }
            } else {
                String tempID = oldType.getId() + ".destroyed";
                if (Messages.containsKey(tempID)) {
                    messageID = tempID;
                } else {
                    messageID = "model.unit.unitSlaughtered";
                }
                unit.dispose();
            }
        } else {
            // unit has equipment that protects from capture, or will
            // be downgraded
            EquipmentType typeToLose = null;
            int combatLossPriority = 0;
            for (EquipmentType equipmentType : unit.getEquipment()) {
                if (equipmentType.getCombatLossPriority() > combatLossPriority) {
                    typeToLose = equipmentType;
                    combatLossPriority = equipmentType.getCombatLossPriority();
                }
            }
            if (typeToLose != null) {
                // lose equipment as a result of combat
                unit.removeEquipment(typeToLose, true);
                if (unit.getEquipment().isEmpty()) {
                    messageID = "model.unit.unitDemotedToUnarmed";
                }
                if (enemyUnit.hasAbility("model.ability.captureEquipment") &&
                    enemyUnit.canBeEquippedWith(typeToLose)) {
                    enemyUnit.equipWith(typeToLose, true);
                    unit.addModelMessage(unit, "model.unit.equipmentCaptured",
                                         new String[][] {
                                             {"%nation%", enemyUnit.getOwner().getNationAsString()},
                                             {"%equipment%", typeToLose.getName()}},
                                         ModelMessage.FOREIGN_DIPLOMACY);
                }
            } else {
                // be downgraded as a result of combat
                UnitType downgrade = unit.getType().getDowngrade(DowngradeType.DEMOTION);
                if (downgrade != null) {
                    unit.setType(downgrade);
                    messageType = ModelMessage.UNIT_DEMOTED;
                    String tempID = oldType.getId() + ".demoted";
                    if (Messages.containsKey(tempID)) {
                        messageID = tempID;
                    } else {
                        messageID = "model.unit.unitDemoted";
                    }
                } else {                
                    String tempID = oldType.getId() + ".destroyed";
                    if (Messages.containsKey(tempID)) {
                        messageID = tempID;
                    } else {
                        messageID = "model.unit.unitSlaughtered";
                    }
                    unit.dispose();
                }
            }
        }
        String newName = unit.getName();
        FreeColGameObject source = unit;
        if (unit.getColony() != null) {
            source = unit.getColony();
        }

        // TODO: this still doesn't work as intended
        unit.addModelMessage(source, messageID, new String[][] {
                { "%oldName%", oldType.getName() },
                { "%unit%", newName },
                { "%nation%", nation },
                { "%enemyUnit%", enemyUnit.getName() },
                { "%enemyNation%", enemyUnit.getOwner().getNationAsString() }
            }, messageType, unit);

        if (unit.getOwner() != enemyUnit.getOwner()) {
            // unit unit hasn't been captured by enemyUnit, show message to
            // enemyUnit's owner
            source = enemyUnit;
            if (enemyUnit.getColony() != null) {
                source = enemyUnit.getColony();
            }
            unit.addModelMessage(source, messageID, new String[][] {
                    { "%oldName%", oldType.getName() },
                    { "%unit%", newName },
                    { "%enemyUnit%", enemyUnit.getName() },
                    { "%nation%", nation },
                    { "%enemyNation%", enemyUnit.getOwner().getNationAsString() }
                }, messageType, unit);
        }
    }

    /**
     * Promotes this unit.
     *
     * @param unit an <code>Unit</code> value
     */
    private void promote(Unit unit) {
        String oldName = unit.getName();
        String nation = unit.getOwner().getNationAsString();
        UnitType newType = unit.getType().getPromotion();
        
        if (newType != null) {
            unit.setType(newType);
            if (unit.getType().equals(newType)) {
                // the new unit type was successfully applied
                unit.addModelMessage(unit, "model.unit.unitPromoted",
                                 new String[][] {
                                     { "%oldName%", oldName },
                                     { "%unit%", unit.getName() },
                                     { "%nation%", nation }
                                 }, ModelMessage.UNIT_IMPROVED);
            }
        }
    }
}
