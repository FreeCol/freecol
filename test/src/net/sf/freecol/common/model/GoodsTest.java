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

import java.util.Locale;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.util.test.FreeColTestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GoodsTest extends FreeColTestCase {

    public static GoodsType cottonType = FreeCol.getSpecification().getGoodsType("model.goods.cotton");

    public static TileType plainsType = FreeCol.getSpecification().getTileType("model.tile.plains");

    public static UnitType privateerType = FreeCol.getSpecification().getUnitType("model.unit.privateer");
    public static UnitType wagonTrainType = FreeCol.getSpecification().getUnitType("model.unit.wagonTrain");
    public static UnitType veteranSoldierType = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");


    public void testGoodsGameLocationIntInt() {

        Map map = getTestMap(plainsType);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch(),
                              FreeCol.getSpecification().getUnitType("model.unit.wagonTrain"),
                              UnitState.ACTIVE);

        Goods g = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(wagon, g.getLocation());
        assertEquals(cottonType, g.getType());
        assertEquals(75, g.getAmount());
    }

    public void testSetOwner() {

        try {
            Map map = getTestMap(plainsType);

            Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch(), 
                                  FreeCol.getSpecification().getUnitType("model.unit.wagonTrain"),
                                  UnitState.ACTIVE);

            Goods g = new Goods(getGame(), wagon, cottonType, 75);

            g.setOwner(getGame().getCurrentPlayer());

            fail("Should not allow setOwner");
        } catch (UnsupportedOperationException e) {
            // Okay to throw exception.
        }
    }

    public void testToString() {

        Messages.setMessageBundle(Locale.ENGLISH);

        Map map = getTestMap(plainsType);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                UnitState.ACTIVE);

        Goods g = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals("75 Cotton", g.toString());
    }

    /**
    public void testGetName() {

        Locale.setDefault(Locale.ENGLISH);

        Goods g = new Goods(getGame(), null, cottonType, 75);

        assertEquals("75 Cotton", g.getName());

        assertEquals("75 Cotton (boycotted)", g.getName(false));

        assertEquals("75 Cotton", g.getName(true));

        // Same as getName(int, boolean)
        assertEquals(g.getName(), Goods.getName(cottonType));
        assertEquals(g.getName(false), Goods.getName(cottonType, false));
        assertEquals(g.getName(true), Goods.getName(cottonType, true));

    }
    */

    public void testGetTile() {
        Game game = getStandardGame();
        Player dutch = dutch();
        Map map = getTestMap(plainsType);
        game.setMap(map);

        // Check in a colony
        //map.getTile(5, 8).setBonus(true);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);

        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, veteranSoldierType, UnitState.ACTIVE);

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        // Create goods
        Goods cotton = new Goods(getGame(), null, cottonType, 75);

        // Check if location null
        assertEquals(null, cotton.getTile());

        // Check in colony
        cotton.setLocation(colony);
        assertEquals(colony.getTile(), cotton.getTile());
        assertEquals(75, colony.getGoodsCount(cottonType));

        // Check in a wagon
        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch, wagonTrainType, UnitState.ACTIVE);
        cotton.setLocation(wagon);
        assertEquals(map.getTile(9, 10), cotton.getTile());
    }

    public void testGetRawMaterial() {
    	GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.cotton");
    	GoodsType cloth = FreeCol.getSpecification().getGoodsType("model.goods.cloth");
    	
        assertEquals(null, cotton.getRawMaterial());
        assertEquals(cotton, cloth.getRawMaterial());
    }

    public void testGetManufactoredGoods() {
    	GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.cotton");
    	GoodsType cloth = FreeCol.getSpecification().getGoodsType("model.goods.cloth");
    	
        assertEquals(null, cloth.getProducedMaterial());
        assertEquals(cloth, cotton.getProducedMaterial());
    }

    public void testIsFarmedGoods() {
    	GoodsType bells = FreeCol.getSpecification().getGoodsType("model.goods.bells");
    	GoodsType cloth = FreeCol.getSpecification().getGoodsType("model.goods.cloth");
    	GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.cotton");
    	
        assertFalse(bells.isFarmed());
        assertTrue(cotton.isFarmed());
        assertFalse(cloth.isFarmed());
    }
    
    public void testMilitaryGoods() {
        GoodsType bells = FreeCol.getSpecification().getGoodsType("model.goods.bells");
        GoodsType crosses = FreeCol.getSpecification().getGoodsType("model.goods.crosses");
        GoodsType cloth = FreeCol.getSpecification().getGoodsType("model.goods.cloth");
        GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.cotton");
        GoodsType muskets = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        GoodsType horses = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        
        assertFalse(bells.isMilitaryGoods());
        assertFalse(crosses.isMilitaryGoods());
        assertFalse(cloth.isMilitaryGoods());
        assertFalse(cotton.isMilitaryGoods());
        assertTrue(horses.isMilitaryGoods());
        assertTrue(muskets.isMilitaryGoods());
    }

    public void testBuildingMaterials() {
        GoodsType bells = FreeCol.getSpecification().getGoodsType("model.goods.bells");
        GoodsType crosses = FreeCol.getSpecification().getGoodsType("model.goods.crosses");
        GoodsType cloth = FreeCol.getSpecification().getGoodsType("model.goods.cloth");
        GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.cotton");
        GoodsType muskets = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        GoodsType horses = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType hammers = FreeCol.getSpecification().getGoodsType("model.goods.hammers");
        GoodsType tools = FreeCol.getSpecification().getGoodsType("model.goods.tools");
        GoodsType fish = FreeCol.getSpecification().getGoodsType("model.goods.fish");
        GoodsType food = FreeCol.getSpecification().getGoodsType("model.goods.food");
        GoodsType ore = FreeCol.getSpecification().getGoodsType("model.goods.ore");
        GoodsType lumber = FreeCol.getSpecification().getGoodsType("model.goods.lumber");
        
        // for EquipmentType horses
        assertTrue(horses.isBuildingMaterial());
        // for EquipmentType muskets
        assertTrue(muskets.isBuildingMaterial());
        // for buildings and units
        assertTrue(hammers.isBuildingMaterial());
        assertTrue(tools.isBuildingMaterial());
        
        // other goods not involved in construction, or not directly involved
        assertFalse(bells.isBuildingMaterial());
        assertFalse(crosses.isBuildingMaterial());
        assertFalse(cloth.isBuildingMaterial());
        assertFalse(cotton.isBuildingMaterial());
        assertFalse(fish.isBuildingMaterial());
        assertFalse(food.isBuildingMaterial());
        assertFalse(ore.isBuildingMaterial());
        assertFalse(lumber.isBuildingMaterial());
        
        // checking raw materials
        assertTrue(ore.isRawBuildingMaterial());
        assertTrue(lumber.isRawBuildingMaterial());
        assertTrue(tools.isRawBuildingMaterial());  // ? why ?
        assertTrue(food.isRawBuildingMaterial());   // ? why ?
        assertFalse(fish.isRawBuildingMaterial());  // ? why not the same as food ?
        assertFalse(horses.isRawBuildingMaterial());
        assertFalse(muskets.isRawBuildingMaterial());
        assertFalse(hammers.isRawBuildingMaterial());
        assertFalse(bells.isRawBuildingMaterial());
        assertFalse(crosses.isRawBuildingMaterial());
        assertFalse(cloth.isRawBuildingMaterial());
        assertFalse(cotton.isRawBuildingMaterial());
   }

    public void testSetGetLocation() {
        Game game = getStandardGame();
        Player dutch = dutch();
        Map map = getTestMap(plainsType, true);
        game.setMap(map);
        Colony colony = getStandardColony();

        // Check in Colony
        Goods cotton = new Goods(getGame(), null, cottonType, 75);
        cotton.setLocation(colony);
        assertEquals(colony, cotton.getLocation());
        assertEquals(75, colony.getGoodsCount(cottonType));

        // Check in a wagon
        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch, wagonTrainType, UnitState.ACTIVE);
        cotton.setLocation(wagon);
        assertEquals(wagon, cotton.getLocation());

        // Can only add to GoodsContainers
        try {
            cotton.setLocation(map.getTile(9, 10));
            fail();
        } catch (IllegalArgumentException e) {
            // Okay to throw exception.
        }
    }

    public void testGetTakeSpace() {
        Map map = getTestMap(plainsType, true);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                UnitState.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(1, cotton.getSpaceTaken());
    }

    public void testSetGetAmount() {
        Map map = getTestMap(plainsType, true);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                UnitState.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(75, cotton.getAmount());

        cotton.setAmount(-10);

        assertEquals(-10, cotton.getAmount());

        cotton.setAmount(100000);

        assertEquals(100000, cotton.getAmount());

    }
    
    public Player dutch(){
    	return getGame().getPlayer("model.nation.dutch");    	
    }

    public void testAdjustAmount() {

        Map map = getTestMap(plainsType, true);

        Unit wagon = new Unit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                UnitState.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(75, cotton.getAmount());

        cotton.adjustAmount();

        cotton.setAmount(-10);

        assertEquals(-10, cotton.getAmount());

        cotton.setAmount(100000);

        assertEquals(100000, cotton.getAmount());

    }

    public void testLoadOntoInColony() {
        Game game = getStandardGame();
        Map map = getTestMap(plainsType, true);
        game.setMap(map);
        Colony colony = getStandardColony();

        Unit wagonInColony = new Unit(getGame(), colony.getTile(), getGame().getPlayer("model.nation.dutch"), wagonTrainType,
                UnitState.ACTIVE);
        Unit wagonNotInColony = new Unit(getGame(), map.getTile(10, 10), getGame().getPlayer("model.nation.dutch"),
                wagonTrainType, UnitState.ACTIVE);

        // Check that it does not work if current Location == null
        Goods cotton = new Goods(getGame(), null, cottonType, 75);
        try {
            cotton.loadOnto(wagonInColony);
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            cotton.loadOnto(wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check from colony to wagon train
        cotton.setLocation(colony);
        cotton.loadOnto(wagonInColony);
        assertEquals(wagonInColony, cotton.getLocation());

        try {
            cotton.loadOnto(wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check from unit to unit
        wagonInColony.setLocation(wagonNotInColony.getTile());
        cotton.loadOnto(wagonNotInColony);
        assertEquals(wagonNotInColony, cotton.getLocation());

    }

    public void testLoadOntoInEurope() {
        Game game = getStandardGame();
        Map map = getTestMap(plainsType, true);
        game.setMap(map);
        Goods cotton = new Goods(getGame(), null, cottonType, 75);
        Europe europe = getGame().getPlayer("model.nation.dutch").getEurope();
        Unit privateer1 = new Unit(getGame(), europe, getGame().getPlayer("model.nation.dutch"), privateerType,
                                   UnitState.ACTIVE);
        Unit privateer2 = new Unit(getGame(), europe, getGame().getPlayer("model.nation.dutch"), privateerType,
                                   UnitState.ACTIVE);

        // While source in Europe, target in Europe
        cotton.setLocation(privateer1);
        cotton.loadOnto(privateer2);
        assertEquals(privateer2, cotton.getLocation());

        // While source moving from America, target in Europe
        cotton.setLocation(privateer1);
        assertEquals(europe, privateer1.getLocation());
        privateer1.moveToAmerica();
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target in Europe
        cotton.setLocation(privateer1);
        privateer1.moveToEurope();
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving to America
        privateer1.setLocation(europe);
        privateer2.moveToAmerica();

        cotton.setLocation(privateer1);
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target moving to America
        cotton.setLocation(privateer1);
        privateer1.moveToAmerica();
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target moving to America
        cotton.setLocation(privateer1);
        privateer1.moveToEurope();
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving from America
        privateer1.setLocation(europe);
        privateer2.moveToEurope();

        cotton.setLocation(privateer1);
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target moving from America
        cotton.setLocation(privateer1);
        privateer1.moveToAmerica();
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target moving from America
        cotton.setLocation(privateer1);
        privateer1.moveToEurope();
        try {
            cotton.loadOnto(privateer2);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testUnload() {
        Game game = getStandardGame();
        Map map = getTestMap(plainsType, true);
        game.setMap(map);
        Colony colony = getStandardColony();

        Unit wagonInColony = new Unit(getGame(), colony.getTile(), getGame().getPlayer("model.nation.dutch"), wagonTrainType,
                UnitState.ACTIVE);
        Unit wagonNotInColony = new Unit(getGame(), map.getTile(10, 10), getGame().getPlayer("model.nation.dutch"),
                wagonTrainType, UnitState.ACTIVE);

        Goods cotton = new Goods(getGame(), null, cottonType, 75);

        // Unload in Colony
        cotton.setLocation(wagonInColony);
        cotton.unload();
        assertEquals(colony, cotton.getLocation());

        // Unload outside of colony does not work
        cotton.setLocation(wagonNotInColony);
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }

        // Not allowed to unload in Europe
        Unit privateer = new Unit(getGame(), getGame().getPlayer("model.nation.dutch").getEurope(), getGame().getPlayer(
                "model.nation.dutch"), privateerType, UnitState.ACTIVE);
        cotton.setLocation(privateer);
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }

        // While moving from America
        cotton.setLocation(privateer);
        privateer.moveToAmerica();
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }

        // While moving to America
        cotton.setLocation(privateer);
        privateer.moveToEurope();
        try {
            cotton.unload();
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testSerialize() {

        Colony colony = getStandardColony();
        Goods goods1 = new Goods(getGame(), colony, cottonType, 75);
        Document document = Message.createNewDocument();
        Element element = goods1.toXMLElement(null, document, true, true);

        element.setAttribute("ID", "newID");
        Goods goods2 = new Goods(colony.getGame(), element);

        assertEquals(goods1.getGame(), goods2.getGame());
        assertEquals(goods1.getLocation(), goods2.getLocation());
        assertEquals(goods1.getType(), goods2.getType());
        assertEquals(goods1.getAmount(), goods2.getAmount());

    }

}
