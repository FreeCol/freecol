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

import java.util.Iterator;
import java.util.Set;

import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

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
        assertTrue(offenceModifiers.contains(SimpleCombatModel.BIG_MOVEMENT_PENALTY));
        offenceModifiers.remove(SimpleCombatModel.BIG_MOVEMENT_PENALTY);
        assertTrue(offenceModifiers.contains(veteranModifier));
        offenceModifiers.remove(veteranModifier);
        assertTrue(offenceModifiers.contains(musketModifier));
        offenceModifiers.remove(musketModifier);
        assertTrue(offenceModifiers.contains(horsesModifier));
        offenceModifiers.remove(horsesModifier);
        assertTrue(offenceModifiers.contains(SimpleCombatModel.ATTACK_BONUS));
        offenceModifiers.remove(SimpleCombatModel.ATTACK_BONUS);
        // this was also added by the combat model
        assertEquals("modifiers.baseOffence", offenceModifiers.iterator().next().getSource());

        Set<Modifier> hillsModifierSet = hills.getDefenceBonus();
        assertFalse(soldier.hasAbility("model.ability.ambushBonus"));
        assertFalse(colonist.hasAbility("model.ability.ambushPenalty"));                     
        assertEquals(1, hillsModifierSet.size());
        Modifier hillsModifier = hillsModifierSet.iterator().next();

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(soldier, colonist);
        assertEquals(3, defenceModifiers.size());
        assertTrue(defenceModifiers.contains(hillsModifier));
        defenceModifiers.remove(hillsModifier);
        assertTrue(defenceModifiers.contains(SimpleCombatModel.FORTIFICATION_BONUS));
        defenceModifiers.remove(SimpleCombatModel.FORTIFICATION_BONUS);
        // this was also added by the combat model
        assertEquals("modifiers.baseDefence", defenceModifiers.iterator().next().getSource());

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
        assertEquals("modifiers.baseOffence", offenceModifiers.iterator().next().getSource());

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        assertEquals(1, defenceModifiers.size());
        assertEquals("modifiers.baseDefence", defenceModifiers.iterator().next().getSource());

        /**
         * Fortification should have no effect.
         */
        galleon.setState(UnitState.FORTIFYING);
        galleon.setState(UnitState.FORTIFIED);
        defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        assertEquals(1, defenceModifiers.size());
        assertEquals("modifiers.baseDefence", defenceModifiers.iterator().next().getSource());

        /**
         * Penalties due to cargo.
         */
        Goods goods1 = new Goods(game, null, Goods.LUMBER, 50);
        privateer.add(goods1);
        offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        Iterator<Modifier> privIt = offenceModifiers.iterator();
        assertEquals(2, offenceModifiers.size());
        assertEquals("modifiers.baseOffence", privIt.next().getSource());
        Modifier goodsPenalty1 = privIt.next();
        assertEquals("modifiers.cargoPenalty", goodsPenalty1.getSource());
        assertEquals(-12.5f, goodsPenalty1.getValue());

        Goods goods2 = new Goods(game, null, Goods.LUMBER, 150);
        galleon.add(goods2);
        assertEquals(2, galleon.getVisibleGoodsCount());
        defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        Iterator<Modifier> gallIt = defenceModifiers.iterator();
        assertEquals(2, defenceModifiers.size());
        assertEquals("modifiers.baseDefence", gallIt.next().getSource());
        Modifier goodsPenalty2 = gallIt.next();
        assertEquals("modifiers.cargoPenalty", goodsPenalty2.getSource());
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
        assertEquals("modifiers.baseOffence", privIt.next().getSource());
        Modifier newDrakeModifier = privIt.next();
        assertEquals(drakeModifier, newDrakeModifier);
        goodsPenalty1 = privIt.next();
        assertEquals("modifiers.cargoPenalty", goodsPenalty1.getSource());
        assertEquals(-12.5f, goodsPenalty1.getValue());

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

        Unit galleon = new Unit(game, tile1, dutch, galleonType, UnitState.ACTIVE);
        Unit privateer = new Unit(game, tile2, french, privateerType, UnitState.ACTIVE);
        
        String errMsg = "Galleon should be empty";
        assertEquals(errMsg,galleon.getGoodsCount(),0);
        
        Goods cargo = new Goods(game,galleon,Goods.MUSKETS,100);
        galleon.add(cargo);
        
        errMsg = "Galleon should be loaded";
        assertEquals(errMsg,galleon.getGoodsCount(),1);
        
        errMsg = "Galeon should not be repairing";
        assertFalse(errMsg,galleon.isUnderRepair());
        
        int damage = galleon.getHitpoints() -1;
        CombatModel.CombatResultType combatResolution = CombatModel.CombatResultType.WIN;
        
        CombatModel.CombatResult combatResult = new  CombatModel.CombatResult(combatResolution,damage);
        
        combatModel.attack(privateer, galleon, combatResult, 0);
        
        errMsg = "Galleon should be in Europe repairing";
        assertTrue(errMsg,galleon.isUnderRepair());
        
        errMsg = "Galleon should be empty";
        assertEquals(errMsg,galleon.getGoodsCount(),0);
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
                     Unit.MoveType.ILLEGAL_MOVE, colonial.getMoveType(tile2));


        combatModel.attack(soldier, colonial, victory, 0);
        assertEquals(french, colonial.getOwner());
        assertEquals(tile2, colonial.getTile());
        assertEquals(colonistType, colonial.getType());

    }

    public void testAttackColonyWithVeteran() {

        Game game = getStandardGame();
        Map map = getTestMap();
        Colony colony = getStandardColony(1, 5, 8);

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
        combatModel.attack(attacker, defender, victory, 0);
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());

        // attacker should lose horses
        combatModel.attack(attacker, defender, defeat, 0);
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());

        // defender should lose muskets
        combatModel.attack(attacker, defender, victory, 0);
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertFalse(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        // this should force DONE_SETTLEMENT
        assertFalse(defender.isDefensiveUnit());

        combatModel.captureColony(attacker, colony, 0);
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

        Game game = getStandardGame();
        Map map = getTestMap();
        Colony colony = getStandardColony(1, 5, 8);

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit defender = new Unit(getGame(), colony.getTile(), dutch, veteranType, UnitState.ACTIVE, horses, muskets);
        Unit attacker = new Unit(getGame(), tile2, inca, braveType, UnitState.ACTIVE, horses, muskets);

        // defender should lose horses
        combatModel.attack(attacker, defender, victory, 0);
        assertEquals(1, colony.getUnitCount());
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        assertTrue(defender.isDefensiveUnit());

        // attacker should lose horses
        combatModel.attack(attacker, defender, defeat, 0);
        assertEquals(1, colony.getUnitCount());
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isMounted());
        assertTrue(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        assertTrue(defender.isDefensiveUnit());

        // defender should lose muskets
        combatModel.attack(attacker, defender, victory, 0);
        assertEquals(1, colony.getUnitCount());
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isMounted());
        assertFalse(defender.isArmed());
        assertEquals(veteranType, defender.getType());
        // this should force DONE_SETTLEMENT
        assertFalse(defender.isDefensiveUnit());

        // colony should be destroyed
        combatModel.captureColony(attacker, colony, 0);
        assertFalse(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(braveType, attacker.getType());
        assertFalse(defender.isDisposed());
        assertTrue(colonist.isDisposed());
        assertTrue(colony.isDisposed());
        assertEquals(colony.getTile(), attacker.getTile());

    }

    public void testDefendColonyWithScout() {

        Game game = getStandardGame();
        Map map = getTestMap();
        Colony colony = getStandardColony(1, 5, 8);

        SimpleCombatModel combatModel = new SimpleCombatModel(game.getModelController().getPseudoRandom());
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.frenchREF");

        assertTrue(french.isREF());

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit defender = new Unit(getGame(), colony.getTile(), dutch, colonistType, UnitState.ACTIVE, horses);
        Unit attacker = new Unit(getGame(), tile2, french, veteranType, UnitState.ACTIVE, horses, muskets);

        assertTrue(defender.isDefensiveUnit());

        // defender should lose horses
        combatModel.attack(attacker, defender, victory, 0);
        assertTrue(attacker.isMounted());
        assertTrue(attacker.isArmed());
        assertEquals(veteranType, attacker.getType());
        assertFalse(defender.isMounted());
        assertFalse(defender.isArmed());
        assertEquals(colonistType, defender.getType());
        // this should force DONE_SETTLEMENT
        assertFalse(defender.isDefensiveUnit());

    }
 
}