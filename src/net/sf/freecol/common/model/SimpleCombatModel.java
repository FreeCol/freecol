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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;

public class SimpleCombatModel implements CombatModel {

    private static final Logger logger = Logger.getLogger(SimpleCombatModel.class.getName());

    /**
     * Return the offensive power of the attacker versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>float</code> value
     */
    public float getOffensePower(Unit attacker, Unit defender) {
        List<Modifier> modifiers = getOffensiveModifiers(attacker, defender);
        return modifiers.get(modifiers.size() - 1).getValue();
    }

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public List<Modifier> getOffensiveModifiers(Unit attacker, Unit defender) {
        ArrayList<Modifier> result = new ArrayList<Modifier>();

        float addend, percentage;
        float totalAddend = attacker.getType().getOffence();
        float totalPercentage = 100;

        result.add(new Modifier("modifiers.baseOffense", totalAddend, Modifier.ADDITIVE));

        if (attacker.isNaval()) {
            int goodsCount = attacker.getGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                percentage = -12.5f * goodsCount;
                result.add(new Modifier("modifiers.cargoPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }
            if (attacker.hasAbility("model.ability.piracy")) {
                Modifier piracyBonus = attacker.getModifier("model.modifier.piracyBonus");
                if (piracyBonus != null) {
                    // Drake grants 50% power bonus (in colonization gives for attack and defense)
                    result.add(piracyBonus);
                    totalPercentage += piracyBonus.getValue();
                }
            }
        } else {

            if (attacker.isArmed()) {
                if (totalAddend == 0) {
                    // civilian
                    addend = 2;
                } else {
                    // brave or REF
                    addend = 1;
                }
                result.add(new Modifier("modifiers.armed", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            }

            if (attacker.isMounted()) {
                addend = 1;
                result.add(new Modifier("modifiers.mounted", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            }

            // 50% veteran bonus
            Modifier veteranModifier = attacker.getModifier("model.modifier.veteranBonus");
            if (veteranModifier != null) {
                result.add(veteranModifier);
                totalPercentage += veteranModifier.getValue();
            }

            // 50% attack bonus
            percentage = 50;
            result.add(new Modifier("modifiers.attackBonus", percentage, Modifier.PERCENTAGE));
            totalPercentage += percentage;

            // movement penalty
            int movesLeft = attacker.getMovesLeft();
            if (movesLeft == 1) {
                percentage = -66;
                result.add(new Modifier("modifiers.movementPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            } else if (movesLeft == 2) {
                percentage = -33;
                result.add(new Modifier("modifiers.movementPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }

            // In the open
            if (defender != null && defender.getTile() != null && defender.getTile().getSettlement() == null) {

                /**
                 * Ambush bonus in the open = defender's defense bonus, if
                 * defender is REF, or attacker is indian.
                 */
                if (attacker.hasAbility("model.ability.ambushBonus") ||
                    defender.hasAbility("model.ability.ambushPenalty")) {
                    percentage = defender.getTile().defenseBonus();
                    result.add(new Modifier("modifiers.ambushBonus", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }

                // 75% Artillery in the open penalty
                // TODO: is it right? or should it be another ability?
                if (attacker.hasAbility("model.ability.bombard")) {
                    percentage = -75;
                    result.add(new Modifier("modifiers.artilleryPenalty", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            }

            // Attacking a settlement
            if (defender != null && defender.getTile() != null && defender.getTile().getSettlement() != null) {
                // REF bombardment bonus
                Modifier bombardModifier = attacker.getModifier("model.modifier.bombardBonus");
                if (bombardModifier != null) {
                    result.add(bombardModifier);
                    totalPercentage += bombardModifier.getValue();
                }
            }
        }

        float offensivePower = (totalAddend * totalPercentage) / 100;
        result.add(new Modifier("modifiers.finalResult", offensivePower, Modifier.ADDITIVE));
        return result;
    }

    /**
     * Return the defensive power of the defender versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>float</code> value
     */
    public float getDefensePower(Unit attacker, Unit defender) {
        List<Modifier> modifiers = getDefensiveModifiers(attacker, defender);
        return modifiers.get(modifiers.size() - 1).getValue();
    }

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public List<Modifier> getDefensiveModifiers(Unit attacker, Unit defender) {

        ArrayList<Modifier> result = new ArrayList<Modifier>();
        if (defender == null) {
            return result;
        }

        float addend, percentage;
        float totalAddend = defender.getType().getDefence();
        float totalPercentage = 100;

        result.add(new Modifier("modifiers.baseDefense", totalAddend, Modifier.ADDITIVE));

        if (defender.isNaval()) {
            int goodsCount = defender.getVisibleGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                percentage =  -12.5f * goodsCount;
                result.add(new Modifier("modifiers.cargoPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }
            if (defender.hasAbility("model.ability.piracy")) {
                Modifier piracyBonus = defender.getModifier("model.modifier.piracyBonus");
                if (piracyBonus != null) {
                    // Drake grants 50% power bonus (in colonization gives for attack and defense)
                    result.add(piracyBonus);
                    totalPercentage += piracyBonus.getValue();
                }
            }
        } else {
            // Paul Revere makes an unarmed colonist in a settlement pick up
            // a stock-piled musket if attacked, so the bonus should be applied
            // for unarmed colonists inside colonies where there are muskets
            // available.
            if (defender.isArmed()) {
                addend = 1;
                result.add(new Modifier("modifiers.armed", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            } else if (defender.getOwner().hasAbility("model.ability.automaticDefense") && defender.isColonist()
                       && defender.getLocation() instanceof WorkLocation) {
                Colony colony = ((WorkLocation) defender.getLocation()).getColony();
                if (colony.getGoodsCount(Goods.MUSKETS) >= 50) {
                    addend = 1;
                    result.add(new Modifier("modifiers.paulRevere", addend, Modifier.ADDITIVE));
                    totalAddend += addend;
                }
            }

            if (defender.isMounted()) {
                addend = 1;
                result.add(new Modifier("modifiers.mounted", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            }

            // 50% veteran bonus
            Modifier veteranModifier = defender.getModifier("model.modifier.veteranBonus");
            if (veteranModifier != null) {
                result.add(veteranModifier);
                totalPercentage += veteranModifier.getValue();
            }

            // 50% fortify bonus
            if (defender.getState() == UnitState.FORTIFIED) {
                percentage = 50;
                result.add(new Modifier("modifiers.fortified", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }

            if (defender.getTile() != null && defender.getTile().getSettlement() != null) {
                Modifier settlementModifier = getSettlementModifier(attacker, defender.getTile().getSettlement());
                result.add(settlementModifier);
                totalPercentage += settlementModifier.getValue();
                // TODO: is it right? or should it be another ability?
                if (defender.hasAbility("model.ability.bombard") && attacker.getOwner().isIndian()) {
                    // 100% defense bonus against an Indian raid
                    percentage = 100;
                    result.add(new Modifier("modifiers.artilleryAgainstRaid", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            } else if (defender.getTile() != null) {
                // In the open
                if (!(attacker.hasAbility("model.ability.ambushBonus") ||
                      defender.hasAbility("model.ability.ambushPenalty"))) {
                    // Terrain defensive bonus.
                    percentage = defender.getTile().defenseBonus();
                    result.add(new Modifier("modifiers.terrainBonus", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
                // TODO: is it right? or should it be another ability?
                if (defender.hasAbility("model.ability.bombard") && defender.getState() != UnitState.FORTIFIED) {
                    // -75% Artillery in the Open penalty
                    percentage = -75;
                    result.add(new Modifier("modifiers.artilleryPenalty", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            }

        }
        float defensivePower = (totalAddend * totalPercentage) / 100;
        result.add(new Modifier("modifiers.finalResult", defensivePower, Modifier.ADDITIVE));
        return result;
    }

    /**
     * Return the defensive modifier that applies to defenders in the given
     * settlement versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @return a <code>Modifier</code>
     */
    public Modifier getSettlementModifier(Unit attacker, Settlement settlement) {

        if (settlement instanceof Colony) {
            // Colony defensive bonus.
            Colony colony = (Colony) settlement;
            Building stockade = colony.getStockade();
            if (stockade == null) {
                // 50% colony bonus
                return new Modifier("modifiers.inColony", 50, Modifier.PERCENTAGE);
            } else {
                String modifier = stockade.getType().getId();
                modifier = "modifiers." + modifier.substring(modifier.lastIndexOf(".") + 1);
                return new Modifier(modifier, colony.getDefenseBonus(), Modifier.PERCENTAGE);
            }
        } else if (settlement instanceof IndianSettlement) {
            // Indian settlement defensive bonus.
            return new Modifier("modifiers.inSettlement", 50, Modifier.PERCENTAGE);
        } else {
            return new Modifier(null, 0, Modifier.PERCENTAGE);
        }
    }

    /**
     * Attack a unit with the given outcome. This method ignores the damage parameter.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender The <code>Unit</code> defending against attack.
     * @param result The result of the attack.
     * @param damage an <code>int</code> value
     * @param plunderGold an <code>int</code> value
     */
    public void attack(Unit attacker, Unit defender, CombatResult result, int damage, int plunderGold) {
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

        switch (result) {
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
                attacker.shipDamaged();
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
                attacker.demote(defender);
                if (defendingPlayer.hasAbility("model.ability.automaticPromotion")) {
                    defender.promote();
                }
            }
            break;
        case GREAT_LOSS:
            if (attacker.isNaval()) {
                attacker.shipSunk();
                attacker.addModelMessage(attacker, "model.unit.shipSunk",
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
                attacker.demote(defender);
                defender.promote();
            }
            break;
        case DONE_SETTLEMENT:
            if (settlement instanceof IndianSettlement) {
                defender.dispose();
                attacker.destroySettlement((IndianSettlement) settlement);
            } else if (settlement instanceof Colony) {
                attacker.captureColony((Colony) settlement, plunderGold);
            } else {
                throw new IllegalStateException("Unknown type of settlement.");
            }
            break;
        case WIN:
            if (attacker.isNaval()) {
                Location repairLocation = defendingPlayer.getRepairLocation(defender);
                attacker.captureGoods(defender);
                defender.shipDamaged();
                attacker.addModelMessage(attacker, "model.unit.enemyShipDamaged",
                                         new String[][] {
                                             { "%unit%", attacker.getName() },
                                             { "%enemyUnit%", defender.getName() },
                                             { "%enemyNation%", defendingPlayer.getNationAsString() }
                                         }, ModelMessage.COMBAT_RESULT);
                defender.addModelMessage(defender, "model.unit.shipDamaged",
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
                    attacker.promote();
                }
                if (!defender.isNaval()) {
                    defender.demote(attacker);
                    if (settlement instanceof IndianSettlement) {
                        getConvert(attacker, (IndianSettlement) settlement);
                    }
                }
            }
            break;
        case GREAT_WIN:
            if (attacker.isNaval()) {
                attacker.captureGoods(defender);
                defender.shipSunk();
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
                attacker.promote();
                if (!defender.isNaval()) {
                    defender.demote(attacker);
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
            ship.shipDamaged();
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
     * Check whether some indian converts due to the attack or they burn all missions
     *
     * @param indianSettlement The attacked indian settlement
     */
    private void getConvert(Unit attacker, IndianSettlement indianSettlement) {
        ModelController modelController = attacker.getGame().getModelController();
        int random = modelController.getRandom(attacker.getId() + "getConvert", 100);
        int convertProbability = (5 - attacker.getOwner().getDifficulty()) * 10; // 50% - 10%
        Modifier modifier = attacker.getModifier("model.ability.nativeConvertBonus");
        if (modifier != null) {
            convertProbability += modifier.getValue();
        }
        // TODO: it should be bigger when tension is high
        int burnProbability = (1 + attacker.getOwner().getDifficulty()) * 2; // 2% - 10%
        
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
       model.unit.shipEvaded=%unit% has evaded an attack by %enemyNation% %enemyUnit%.
       model.unit.enemyShipEvaded=%enemyNation% %enemyUnit% has evaded an attack by %unit%.

       model.unit.shipDamaged=%unit% has been damaged by %enemyNation% %enemyUnit% and must return to %repairLocation% for repairs. All goods and units on board have been lost! 
       model.unit.enemyShipDamaged=%unit% has damaged %enemyNation% %enemyUnit%. %enemyNation% %enemyUnit% must return for repairs.

       model.unit.shipSunk=%unit% has been sunk by %enemyNation% %enemyUnit%!
       model.unit.enemyShipSunk=%unit% has sunk %enemyNation% %enemyUnit%!
  */
}