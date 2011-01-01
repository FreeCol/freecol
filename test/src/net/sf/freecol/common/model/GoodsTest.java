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

import java.util.List;
import java.util.Locale;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class GoodsTest extends FreeColTestCase {

    public static GoodsType cottonType = spec().getGoodsType("model.goods.cotton");

    public static TileType plainsType = spec().getTileType("model.tile.plains");

    public static UnitType privateerType = spec().getUnitType("model.unit.privateer");
    public static UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    public static UnitType veteranSoldierType = spec().getUnitType("model.unit.veteranSoldier");


    public void testGoodsGameLocationIntInt() {

        Map map = getTestMap(plainsType);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(),
                                    spec().getUnitType("model.unit.wagonTrain"),
                                    UnitState.ACTIVE);

        Goods g = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(wagon, g.getLocation());
        assertEquals(cottonType, g.getType());
        assertEquals(75, g.getAmount());
    }

    public void testSetOwner() {

        try {
            Map map = getTestMap(plainsType);

            Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(),
                                        spec().getUnitType("model.unit.wagonTrain"),
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

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                                    UnitState.ACTIVE);

        Goods g = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals("75 model.goods.cotton", g.toString());
    }

    public void testGetName() {

        Locale.setDefault(Locale.ENGLISH);

        Goods g = new Goods(getGame(), null, cottonType, 75);

        System.out.println(Messages.message(g.getLabel(true)));
        assertEquals("75 Cotton", Messages.message(g.getLabel(true)));

        assertEquals("75 Cotton (boycotted)", Messages.message(g.getLabel(false)));

        // Same as getName(int, boolean)
        //assertEquals(g.getName(), Goods.getName(cottonType));
        //assertEquals(g.getName(false), Goods.getName(cottonType, false));
        //assertEquals(g.getName(true), Goods.getName(cottonType, true));

    }

    public void testGetRawMaterial() {
    	GoodsType cotton = spec().getGoodsType("model.goods.cotton");
    	GoodsType cloth = spec().getGoodsType("model.goods.cloth");

        assertEquals(null, cotton.getRawMaterial());
        assertEquals(cotton, cloth.getRawMaterial());
    }

    public void testGetManufactoredGoods() {
    	GoodsType cotton = spec().getGoodsType("model.goods.cotton");
    	GoodsType cloth = spec().getGoodsType("model.goods.cloth");

        assertEquals(null, cloth.getProducedMaterial());
        assertEquals(cloth, cotton.getProducedMaterial());
    }

    public void testIsFarmedGoods() {
    	GoodsType bells = spec().getGoodsType("model.goods.bells");
    	GoodsType cloth = spec().getGoodsType("model.goods.cloth");
    	GoodsType cotton = spec().getGoodsType("model.goods.cotton");

        assertFalse(bells.isFarmed());
        assertTrue(cotton.isFarmed());
        assertFalse(cloth.isFarmed());
    }

    public void testMilitaryGoods() {
        GoodsType bells = spec().getGoodsType("model.goods.bells");
        GoodsType crosses = spec().getGoodsType("model.goods.crosses");
        GoodsType cloth = spec().getGoodsType("model.goods.cloth");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        GoodsType muskets = spec().getGoodsType("model.goods.muskets");
        GoodsType horses = spec().getGoodsType("model.goods.horses");

        assertFalse(bells.isMilitaryGoods());
        assertFalse(crosses.isMilitaryGoods());
        assertFalse(cloth.isMilitaryGoods());
        assertFalse(cotton.isMilitaryGoods());
        assertTrue(horses.isMilitaryGoods());
        assertTrue(muskets.isMilitaryGoods());
    }

    public void testBuildingMaterials() {
        GoodsType bells = spec().getGoodsType("model.goods.bells");
        GoodsType crosses = spec().getGoodsType("model.goods.crosses");
        GoodsType cloth = spec().getGoodsType("model.goods.cloth");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        GoodsType muskets = spec().getGoodsType("model.goods.muskets");
        GoodsType horses = spec().getGoodsType("model.goods.horses");
        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        GoodsType tools = spec().getGoodsType("model.goods.tools");
        GoodsType fish = spec().getGoodsType("model.goods.fish");
        GoodsType food = spec().getPrimaryFoodType();
        GoodsType ore = spec().getGoodsType("model.goods.ore");
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");

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
        //assertFalse(food.isBuildingMaterial());
        assertFalse(ore.isBuildingMaterial());
        assertFalse(lumber.isBuildingMaterial());

        // checking raw materials
        assertTrue(ore.isRawBuildingMaterial());
        assertTrue(lumber.isRawBuildingMaterial());
        assertFalse(tools.isRawBuildingMaterial());
        assertTrue(food.isRawBuildingMaterial());
        assertFalse(fish.isRawBuildingMaterial());
        assertFalse(horses.isRawBuildingMaterial());
        assertFalse(muskets.isRawBuildingMaterial());
        assertFalse(hammers.isRawBuildingMaterial());
        assertFalse(bells.isRawBuildingMaterial());
        assertFalse(crosses.isRawBuildingMaterial());
        assertFalse(cloth.isRawBuildingMaterial());
        assertFalse(cotton.isRawBuildingMaterial());
   }

    public void testGetTakeSpace() {
        Map map = getTestMap(plainsType, true);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                                    UnitState.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(1, cotton.getSpaceTaken());
    }

    public void testSetGetAmount() {
        Map map = getTestMap(plainsType, true);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
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

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType,
                                    UnitState.ACTIVE);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(75, cotton.getAmount());

        cotton.adjustAmount();

        cotton.setAmount(-10);

        assertEquals(-10, cotton.getAmount());

        cotton.setAmount(100000);

        assertEquals(100000, cotton.getAmount());

    }

    public void testSerialize() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));

        Colony colony = getStandardColony();
        Goods goods1 = new Goods(game, colony, cottonType, 75);
        Document document = Message.createNewDocument();
        Element element = goods1.toXMLElement(null, document, true, true);

        element.setAttribute("ID", "newID");
        Goods goods2 = new Goods(colony.getGame(), element);

        assertEquals(goods1.getGame(), goods2.getGame());
        assertEquals(goods1.getLocation(), goods2.getLocation());
        assertEquals(goods1.getType(), goods2.getType());
        assertEquals(goods1.getAmount(), goods2.getAmount());

    }

    public void testProductionChain() {
        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");
        GoodsType muskets = spec().getGoodsType("model.goods.muskets");
        GoodsType tools = spec().getGoodsType("model.goods.tools");
        GoodsType ore = spec().getGoodsType("model.goods.ore");

        List<GoodsType> chain = hammers.getProductionChain();
        assertEquals(lumber, chain.get(0));
        assertEquals(hammers, chain.get(1));

        chain = muskets.getProductionChain();
        assertEquals(ore, chain.get(0));
        assertEquals(tools, chain.get(1));
        assertEquals(muskets, chain.get(2));

    }

}
