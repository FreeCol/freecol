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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;

public class CombatTest extends FreeColTestCase {

    CombatResult victory = new CombatResult(CombatModel.CombatResultType.WIN, 0);
    CombatResult defeat = new CombatResult(CombatModel.CombatResultType.LOSS, 0);

    TileType plains = spec().getTileType("model.tile.plains");
    TileType hills = spec().getTileType("model.tile.hills");
    TileType ocean = spec().getTileType("model.tile.ocean");

    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType privateerType = spec().getUnitType("model.unit.privateer");
    UnitType braveType = spec().getUnitType("model.unit.brave");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
    UnitType colonialType = spec().getUnitType("model.unit.colonialRegular");
    UnitType artilleryType = spec().getUnitType("model.unit.artillery");
    UnitType damagedArtilleryType = spec().getUnitType("model.unit.damagedArtillery");
    UnitType colonialRegularType = spec().getUnitType("model.unit.colonialRegular");
    UnitType kingsRegularType = spec().getUnitType("model.unit.kingsRegular");
    UnitType indianConvertType = spec().getUnitType("model.unit.indianConvert");
    UnitType pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
    UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");

    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
    EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
    EquipmentType tools = spec().getEquipmentType("model.equipment.tools");
    EquipmentType indianMuskets = spec().getEquipmentType("model.equipment.indian.muskets");
    EquipmentType indianHorses = spec().getEquipmentType("model.equipment.indian.horses");
    EquipmentType[] dragoonEquipment = new EquipmentType[] { horses, muskets };

    public void testColonistAttackedByVeteran() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setType(hills);
        assertEquals(hills, tile1.getType());
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        Unit soldier = new Unit(game, tile2, french, veteranType, UnitState.ACTIVE);

        soldier.equipWith(muskets, true);
        soldier.equipWith(horses, true);
        soldier.setMovesLeft(1);

        Modifier bigMovementPenalty = spec().getModifiers(SimpleCombatModel.BIG_MOVEMENT_PENALTY)
            .get(0);
        Modifier attackBonus = spec().getModifiers(SimpleCombatModel.ATTACK_BONUS).get(0);
        Modifier fortified = spec().getModifiers(SimpleCombatModel.FORTIFIED).get(0);

        Set<Modifier> veteranModifierSet = veteranType.getModifierSet("model.modifier.offence");
        assertEquals(1, veteranModifierSet.size());
        Modifier veteranModifier = veteranModifierSet.iterator().next();

        Set<Modifier> musketModifierSet = muskets.getModifierSet("model.modifier.offence");
        assertEquals(1, musketModifierSet.size());
        Modifier musketModifier = musketModifierSet.iterator().next();

        Set<Modifier> horsesModifierSet = horses.getModifierSet("model.modifier.offence");
        assertEquals(1, horsesModifierSet.size());
        Modifier horsesModifier = horsesModifierSet.iterator().next();

        Set<Modifier> offenceModifiers = combatModel.getOffensiveModifiers(soldier, colonist);
        assertEquals(6, offenceModifiers.size());
        assertTrue(offenceModifiers.contains(bigMovementPenalty));
        offenceModifiers.remove(bigMovementPenalty);
        assertTrue(offenceModifiers.contains(veteranModifier));
        offenceModifiers.remove(veteranModifier);
        assertTrue(offenceModifiers.contains(musketModifier));
        offenceModifiers.remove(musketModifier);
        assertTrue(offenceModifiers.contains(horsesModifier));
        offenceModifiers.remove(horsesModifier);
        assertTrue(offenceModifiers.contains(attackBonus));
        offenceModifiers.remove(attackBonus);
        // this was also added by the combat model
        assertEquals(spec().BASE_OFFENCE_SOURCE, offenceModifiers.iterator().next().getSource());

        Set<Modifier> hillsModifierSet = hills.getDefenceBonus();
        assertFalse(soldier.hasAbility("model.ability.ambushBonus"));
        assertFalse(colonist.hasAbility("model.ability.ambushPenalty"));                     
        assertEquals(1, hillsModifierSet.size());
        Modifier hillsModifier = hillsModifierSet.iterator().next();

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(soldier, colonist);
        assertEquals(3, defenceModifiers.size());
        assertTrue(defenceModifiers.contains(hillsModifier));
        defenceModifiers.remove(hillsModifier);
        assertTrue(defenceModifiers.contains(fortified));
        defenceModifiers.remove(fortified);
        // this was also added by the combat model
        assertEquals(spec().BASE_DEFENCE_SOURCE, defenceModifiers.iterator().next().getSource());

    }

    public void testGalleonAttackedByPrivateer() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(ocean);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit galleon = new Unit(game, tile1, dutch, galleonType, UnitState.ACTIVE);
        Unit privateer = new Unit(game, tile2, french, privateerType, UnitState.ACTIVE);

        /**
         * Only base modifiers should apply.
         */
        Set<Modifier> offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        assertEquals(1, offenceModifiers.size());
        assertEquals(spec().BASE_OFFENCE_SOURCE, offenceModifiers.iterator().next().getSource());

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        assertEquals(1, defenceModifiers.size());
        assertEquals(spec().BASE_DEFENCE_SOURCE, defenceModifiers.iterator().next().getSource());

        /**
         * Fortification should have no effect.
         */
        galleon.setState(UnitState.FORTIFYING);
        galleon.setState(UnitState.FORTIFIED);
        defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        assertEquals(1, defenceModifiers.size());
        assertEquals(spec().BASE_DEFENCE_SOURCE, defenceModifiers.iterator().next().getSource());

        /**
         * Penalties due to cargo.
         */
        GoodsType lumberType = spec().getGoodsType("model.goods.lumber");
        Goods goods1 = new Goods(game, null, lumberType, 50);
        privateer.add(goods1);
        offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        Iterator<Modifier> privIt = offenceModifiers.iterator();
        assertEquals(2, offenceModifiers.size());
        assertEquals(spec().BASE_OFFENCE_SOURCE, privIt.next().getSource());
        Modifier goodsPenalty1 = privIt.next();
        assertEquals(spec().CARGO_PENALTY_SOURCE, goodsPenalty1.getSource());
        assertEquals(-12.5f, goodsPenalty1.getValue());

        Goods goods2 = new Goods(game, null, lumberType, 150);
        galleon.add(goods2);
        assertEquals(2, galleon.getVisibleGoodsCount());
        defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        Iterator<Modifier> gallIt = defenceModifiers.iterator();
        assertEquals(2, defenceModifiers.size());
        assertEquals(spec().BASE_DEFENCE_SOURCE, gallIt.next().getSource());
        Modifier goodsPenalty2 = gallIt.next();
        assertEquals(spec().CARGO_PENALTY_SOURCE, goodsPenalty2.getSource());
        assertEquals(-25f, goodsPenalty2.getValue());

        /**
         * Francis Drake
         */
        FoundingFather drake = spec().getFoundingFather("model.foundingFather.francisDrake");
        Set<Modifier> drakeModifiers = drake.getFeatureContainer()
            .getModifierSet("model.modifier.offence", privateerType);
        assertEquals(1, drakeModifiers.size());
        Modifier drakeModifier = drakeModifiers.iterator().next();

        french.addFather(drake);
        drakeModifiers = french.getFeatureContainer().getModifierSet("model.modifier.offence",
                                                                     privateerType);
        assertEquals(1, drakeModifiers.size());
        assertEquals(drakeModifier, drakeModifiers.iterator().next());

        offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        privIt = offenceModifiers.iterator();
        assertEquals(3, offenceModifiers.size());
        assertEquals(spec().BASE_OFFENCE_SOURCE, privIt.next().getSource());
        Modifier newDrakeModifier = privIt.next();
        assertEquals(drakeModifier, newDrakeModifier);
        goodsPenalty1 = privIt.next();
        assertEquals(spec().CARGO_PENALTY_SOURCE, goodsPenalty1.getSource());
        assertEquals(-12.5f, goodsPenalty1.getValue());

        // Verify that the move is correctly interpreted
        assertEquals("Wrong move type",MoveType.ATTACK, privateer.getMoveType(tile1));
    }

    public void testAtackedNavalUnitIsDamaged(){
    	Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);
        
        assertEquals("Dutch should be at war with french",dutch.getStance(french),Stance.WAR);
        assertEquals("French should be at war with dutch",french.getStance(dutch),Stance.WAR);
        
        Map map = getTestMap(ocean);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Tile tile3 = map.getTile(6, 8);
        tile3.setExploredBy(dutch, true);
        tile3.setExploredBy(french, true);

        Unit galleon = new Unit(game, tile1, dutch, galleonType, UnitState.ACTIVE);
        Unit privateer = new Unit(game, tile2, french, privateerType, UnitState.ACTIVE);
        
        String errMsg = "Galleon should be empty";
        assertEquals(errMsg,galleon.getGoodsCount(),0);

        GoodsType musketType = spec().getGoodsType("model.goods.muskets");
        Goods cargo = new Goods(game,galleon,musketType,100);
        galleon.add(cargo);
        
        errMsg = "Galleon should be loaded";
        assertEquals(errMsg,galleon.getGoodsCount(),1);
        
        errMsg = "Galeon should not be repairing";
        assertFalse(errMsg,galleon.isUnderRepair());
        
        galleon.setDestination(tile3);
        errMsg = "Wrong destination for Galeon";
        assertEquals(errMsg, tile3, galleon.getDestination());
        
        int damage = galleon.getHitpoints() -1;
        CombatModel.CombatResultType combatResolution = CombatModel.CombatResultType.WIN;
        
        CombatModel.CombatResult combatResult = new  CombatModel.CombatResult(combatResolution,damage);
        
        combatModel.attack(privateer, galleon, combatResult, 0, dutch.getEurope());
        
        errMsg = "Galleon should be in Europe repairing";
        assertTrue(errMsg,galleon.isUnderRepair());
        
        errMsg = "Galleon should be empty";
        assertEquals(errMsg,galleon.getGoodsCount(),0);
        
        errMsg = "Galeon should no longer have a destination";
        assertTrue(errMsg, galleon.getDestination() == null);
    }

    public void testUnarmedAttack() {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        dutch.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));

        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        // no default equipment
        Unit colonial = new Unit(game, tile1, dutch, colonialType, UnitState.ACTIVE, 
                                 EquipmentType.NO_EQUIPMENT);
        assertEquals(colonialType, colonial.getType());
        assertEquals(UnitType.DEFAULT_OFFENCE, colonial.getType().getOffence());
        assertEquals(UnitType.DEFAULT_DEFENCE, colonial.getType().getDefence());
        assertFalse(colonial.isOffensiveUnit());

        // has default equipment
        Unit soldier = new Unit(game, tile2, french, veteranType, UnitState.ACTIVE);
        assertTrue(soldier.isArmed());
        assertTrue(soldier.isOffensiveUnit());

        assertEquals("Unarmed colonial regular can not attack!",
                     Unit.MoveType.MOVE_NO_ATTACK_CIVILIAN, colonial.getMoveType(tile2));


        combatModel.attack(soldier, colonial, victory, 0, null);
        assertEquals(french, colonial.getOwner());
        assertEquals(tile2, colonial.getTile());
        assertEquals(colonistType, colonial.getType());

    }

    public void testAttackColonyWithVeteran() {
    	Game game = getGame();
    	Map map = getTestMap(plainsType,true);
    	game.setMap(map);
    	
        Colony colony = getStandardColony();

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.frenchREF");

        dutch.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        assertTrue(french.isREF());

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = colony.getUnitIterator().next();
        colonist.setType(colonialType);
        assertEquals(colonialType, colonist.getType());

        Unit defender = new Unit(getGame(), colony.getTile(), dutch, veteranType, UnitState.ACTIVE, horses, muskets);
        Unit attacker = new Unit(getGame(), tile2, french, veteranType, UnitState.ACTIVE, horses, muskets);

        // defender should lose horses
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));
        combatModel.attack(attacker, defender, victory, 0, null);
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());

        // attacker should lose horses
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));
        combatModel.attack(attacker, defender, defeat, 0, null);
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());

        // defender should lose muskets
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));
        combatModel.attack(attacker, defender, victory, 0, null);
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertFalse(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        // this should force DONE_SETTLEMENT
        assertFalse(defender.isDefensiveUnit());

        combatModel.captureColony(attacker, colony, 0, dutch.getEurope());
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertFalse(defender.isArmed());
        assertEquals(colonistType, defender.getType());
        assertEquals(colony.getTile(), attacker.getTile());
        assertEquals(colony.getTile(), defender.getTile());

        assertEquals(attacker.getOwner(), colony.getOwner());
        assertEquals(colonist.getType(), colonistType);

    }

    public void testAttackColonyWithBrave() {
    	Game game = getGame();
    	Map map = getTestMap(plainsType,true);
    	game.setMap(map);
    	
        Colony colony = getStandardColony(1, 5, 8);

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit defender = new Unit(getGame(), colony.getTile(), dutch, veteranType, UnitState.ACTIVE, horses, muskets);
        Unit attacker = new Unit(getGame(), tile2, inca, braveType, UnitState.ACTIVE, indianHorses, indianMuskets);

        assertTrue(attacker.isArmed());
        assertTrue(attacker.isMounted());
        assertTrue(inca.isIndian());
        
        // defender should lose horses
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));
        combatModel.attack(attacker, defender, victory, 0, null);
        assertEquals(1, colony.getUnitCount());
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        assertTrue(defender.isDefensiveUnit());
        
        // defender should lose muskets
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));
        combatModel.attack(attacker, defender, victory, 0, null);
        assertEquals(1, colony.getUnitCount());
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isMounted());
        assertFalse(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        // this should force DONE_SETTLEMENT
        assertFalse(defender.isDefensiveUnit());

        // colony should be destroyed
        combatModel.captureColony(attacker, colony, 0, dutch.getEurope());
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isDisposed());
        assertTrue(colonist.isDisposed());
        assertTrue(colony.isDisposed());
        assertEquals(colony.getTile(), attacker.getTile());

    }

    public void testDefendColonyWithUnarmedColonist() {
    	Game game = getGame();
    	Map map = getTestMap(plainsType,true);
    	game.setMap(map);
    	
        Colony colony = getStandardColony();

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit attacker = new Unit(getGame(), tile2, inca, braveType, UnitState.ACTIVE, indianHorses, indianMuskets);

        assertEquals(colonist, colony.getDefendingUnit(attacker));
        assertEquals(colonist, colony.getTile().getDefendingUnit(attacker));

        Unit defender = new Unit(getGame(), colony.getTile(), dutch, colonistType, UnitState.ACTIVE);
        assertFalse("Colonist should not be defensive unit",defender.isDefensiveUnit());
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));

    }

    public void testDefendColonyWithRevere() {
    	Game game = getGame();
    	Map map = getTestMap(plainsType,true);
    	game.setMap(map);
    	
        Colony colony = getStandardColony();

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit attacker = new Unit(getGame(), tile2, inca, braveType, UnitState.ACTIVE, indianHorses, indianMuskets);

        assertEquals(colonist, colony.getDefendingUnit(attacker));

        dutch.addFather(spec().getFoundingFather("model.foundingFather.paulRevere"));
        for (EquipmentType equipment : dragoonEquipment) {
            for (AbstractGoods goods : equipment.getGoodsRequired()) {
                colony.addGoods(goods);
            }
        }

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(attacker, colonist);
        for (Modifier defenceModifier : muskets.getModifierSet("model.modifier.defence")) {
            assertTrue(defenceModifiers.contains(defenceModifier));
        }
        for (Modifier defenceModifier : horses.getModifierSet("model.modifier.defence")) {
            assertFalse(defenceModifiers.contains(defenceModifier));
        }
    }
    
    public void testLoseColonyDefenceWithRevere() {
        Game game = getGame();
        Map map = getTestMap(plainsType,true);
        game.setMap(map);
        
        Colony colony = getStandardColony();
     
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");
        dutch.changeRelationWithPlayer(inca, Stance.WAR);
        
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit attacker = new Unit(getGame(), tile2, inca, braveType, UnitState.ACTIVE, indianHorses, indianMuskets);

        assertEquals(colonist, colony.getDefendingUnit(attacker));

        dutch.addFather(spec().getFoundingFather("model.foundingFather.paulRevere"));
        
        java.util.Map<GoodsType,Integer> goodsAdded = new HashMap<GoodsType,Integer>();
        for (AbstractGoods goods : muskets.getGoodsRequired()) {
            colony.addGoods(goods);
            goodsAdded.put(goods.getType(), goods.getAmount());
        }

        // We need a deterministic random
        List<Integer> setValues = new ArrayList<Integer>();
        setValues.add(1);
        PseudoRandom mockRandom = new MockPseudoRandom(setValues,false);
        SimpleCombatModel combatModel = new SimpleCombatModel(mockRandom);

        CombatResult result = combatModel.generateAttackResult(attacker, colonist);
        
        String errMsg = "Wrong combat result, cannot be DONE_SETTLEMENT";
        assertFalse(errMsg,result.type == CombatResultType.DONE_SETTLEMENT);
        
        combatModel.attack(attacker, colonist, victory, 0, null);
        
        assertFalse("Colonist should not be disposed",colonist.isDisposed());
        assertFalse("Colonist should not be captured",colonist.getOwner() == attacker.getOwner());
        
        for (AbstractGoods goods : muskets.getGoodsRequired()) {
            boolean goodsLost = colony.getGoodsCount(goods.getType()) < goodsAdded.get(goods.getType());
            errMsg = "Colony should have lose " + goods.getType().toString();
            assertTrue(errMsg,goodsLost);
        }
    }

    public void testDefendSettlement() {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(inca, true);

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(inca).settlementTile(tile1).skillToTeach(null).capital(true).build();
        
        //IndianSettlement settlement = new IndianSettlement(game, inca, tile1, true, null, false, null);
        Unit defender = new Unit(game, settlement, inca, braveType, UnitState.ACTIVE);
        Unit attacker = new Unit(game, tile2, dutch, colonistType, UnitState.ACTIVE, horses, muskets);

        for (EquipmentType equipment : dragoonEquipment) {
            for (AbstractGoods goods : equipment.getGoodsRequired()) {
                settlement.addGoods(goods);
            }
        }

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(attacker, defender);
        for (Modifier defenceModifier : indianMuskets.getModifierSet("model.modifier.defence")) {
            assertTrue(defenceModifiers.contains(defenceModifier));
        }
        for (Modifier defenceModifier : indianHorses.getModifierSet("model.modifier.defence")) {
            assertTrue(defenceModifiers.contains(defenceModifier));
        }
    }
    
    public void testPioneerDiesNotLosesEquipment() {
    	Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap();
        game.setMap(map);
        
        Tile tile1 = map.getTile(5, 8);       
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit pioneer = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, veteranType, UnitState.ACTIVE);

        soldier.equipWith(muskets, true);
        soldier.equipWith(horses, true);
        soldier.setMovesLeft(1);
        pioneer.equipWith(tools, true);

        combatModel.attack(soldier, pioneer, victory, 0, null);
        assertTrue("Pioneer should be dead", pioneer.isDisposed());
    }

    public void testScoutDiesNotLosesEquipment() {
        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap();
        game.setMap(map);
        
        Tile tile1 = map.getTile(5, 8);       
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit scout = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, veteranType, UnitState.ACTIVE);

        soldier.equipWith(muskets, true);
        soldier.equipWith(horses, true);
        scout.setMovesLeft(1);
        scout.equipWith(horses, true);

        combatModel.attack(soldier, scout, victory, 0, null);
        assertTrue("Scout should be dead", scout.isDisposed());
    }


    public void testAttackIgnoresMovementPoints() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setType(hills);
        assertEquals(hills, tile1.getType());

        dutch.setStance(french, Player.Stance.WAR);
        french.setStance(dutch, Player.Stance.WAR);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        Unit soldier = new Unit(game, tile2, french, veteranType, UnitState.ACTIVE);

        soldier.equipWith(muskets, true);
        soldier.equipWith(horses, true);

        assertEquals(tile1, colonist.getLocation());
        assertEquals(tile2, soldier.getLocation());

        assertEquals(Unit.MoveType.ATTACK, soldier.getMoveType(tile2, tile1, 9, false));
        assertEquals(Unit.MoveType.ATTACK, soldier.getMoveType(tile2, tile1, 1, false));
        assertEquals(Unit.MoveType.MOVE_NO_MOVES, soldier.getMoveType(tile2, tile1, 0, false));

    }

}