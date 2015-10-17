/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class GoodsTest extends FreeColTestCase {

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType clothType
        = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType crossesType
        = spec().getGoodsType("model.goods.crosses");
    private static final GoodsType fishType
        = spec().getGoodsType("model.goods.fish");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType hammersType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType oreType
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");
    private static final GoodsType tradeGoodsType
        = spec().getGoodsType("model.goods.tradeGoods");

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");
    private static final UnitType veteranSoldierType
        = spec().getUnitType("model.unit.veteranSoldier");
    private static final UnitType wagonTrainType
        = spec().getUnitType("model.unit.wagonTrain");


    public void testGoodsGameLocationIntInt() {

        Map map = getTestMap(plainsType);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(),
                                    wagonTrainType);

        Goods g = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(wagon, g.getLocation());
        assertEquals(cottonType, g.getType());
        assertEquals(75, g.getAmount());
    }

    public void testSetOwner() {

        try {
            Map map = getTestMap(plainsType);

            Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(),
                                        wagonTrainType);

            Goods g = new Goods(getGame(), wagon, cottonType, 75);

            g.setOwner(getGame().getCurrentPlayer());

            fail("Should not allow setOwner");
        } catch (UnsupportedOperationException e) {
            // Okay to throw exception.
        }
    }

    public void testToString() {

        Messages.loadMessageBundle(Locale.ENGLISH);

        Map map = getTestMap(plainsType);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType);

        Goods g = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals("75 cotton", g.toString());
    }

    public void testGetName() {

        Locale.setDefault(Locale.ENGLISH);

        Goods g = new Goods(getGame(), null, cottonType, 75);

        assertEquals("75 Cotton", Messages.message(g.getLabel(true)));
        assertEquals("75 Cotton (boycotted)", Messages.message(g.getLabel(false)));

        // Same as getName(int, boolean)
        //assertEquals(g.getName(), Goods.getName(cottonType));
        //assertEquals(g.getName(false), Goods.getName(cottonType, false));
        //assertEquals(g.getName(true), Goods.getName(cottonType, true));

    }

    public void testGetInputType() {
        assertEquals(null, cottonType.getInputType());
        assertEquals(cottonType, clothType.getInputType());
    }

    public void testGetOutputType() {
        assertEquals(null, clothType.getOutputType());
        assertEquals(clothType, cottonType.getOutputType());
    }

    public void testIsFarmedGoods() {
        assertFalse(bellsType.isFarmed());
        assertTrue(cottonType.isFarmed());
        assertFalse(clothType.isFarmed());
    }

    public void testMilitaryGoods() {
        assertFalse(bellsType.isMilitaryGoods());
        assertFalse(crossesType.isMilitaryGoods());
        assertFalse(clothType.isMilitaryGoods());
        assertFalse(cottonType.isMilitaryGoods());
        assertTrue(horsesType.isMilitaryGoods());
        assertTrue(musketsType.isMilitaryGoods());
    }

    public void testBuildingMaterials() {
        // for dragoon role
        assertTrue(horsesType.isBuildingMaterial());
        assertTrue(musketsType.isBuildingMaterial());
        // for buildings and units
        assertTrue(hammersType.isBuildingMaterial());
        assertTrue(toolsType.isBuildingMaterial());

        // other goods not involved in construction, or not directly involved
        assertFalse(bellsType.isBuildingMaterial());
        assertFalse(crossesType.isBuildingMaterial());
        assertFalse(clothType.isBuildingMaterial());
        assertFalse(cottonType.isBuildingMaterial());
        assertFalse(fishType.isBuildingMaterial());
        //assertFalse(foodType.isBuildingMaterial());
        assertFalse(oreType.isBuildingMaterial());
        assertFalse(lumberType.isBuildingMaterial());

        // checking raw materials
        assertTrue(oreType.isRawBuildingMaterial());
        assertTrue(lumberType.isRawBuildingMaterial());
        assertFalse(toolsType.isRawBuildingMaterial());
        assertFalse(foodType.isRawBuildingMaterial());// freecol-ruleset!
        assertTrue(grainType.isRawBuildingMaterial());// freecol-ruleset!
        assertFalse(fishType.isRawBuildingMaterial());
        assertFalse(horsesType.isRawBuildingMaterial());
        assertFalse(musketsType.isRawBuildingMaterial());
        assertFalse(hammersType.isRawBuildingMaterial());
        assertFalse(bellsType.isRawBuildingMaterial());
        assertFalse(crossesType.isRawBuildingMaterial());
        assertFalse(clothType.isRawBuildingMaterial());
        assertFalse(cottonType.isRawBuildingMaterial());
    }

    public void testTradeGoods() {
        assertTrue(tradeGoodsType.isTradeGoods());
    }

    public void testGetTakeSpace() {
        Map map = getTestMap(plainsType, true);
        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(),
                                    wagonTrainType);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);
        assertEquals(1, cotton.getSpaceTaken());
    }

    public void testSetGetAmount() {
        Map map = getTestMap(plainsType, true);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType);

        Goods cotton = new Goods(getGame(), wagon, cottonType, 75);

        assertEquals(75, cotton.getAmount());

        cotton.setAmount(-10);

        assertEquals(-10, cotton.getAmount());

        cotton.setAmount(100000);

        assertEquals(100000, cotton.getAmount());

    }

    public Player dutch(){
        return getGame().getPlayerByNationId("model.nation.dutch");
    }

    public void testAdjustAmount() {

        Map map = getTestMap(plainsType, true);

        Unit wagon = new ServerUnit(getGame(), map.getTile(9, 10), dutch(), wagonTrainType);

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
        Document document = DOMMessage.createNewDocument();
        Element element = goods1.toXMLElement(document);

        element.setAttribute(FreeColObject.ID_ATTRIBUTE_TAG, "newID");
        Goods goods2 = new Goods(colony.getGame(), element);

        assertEquals(goods1.getGame(), goods2.getGame());
        assertEquals(goods1.getLocation(), goods2.getLocation());
        assertEquals(goods1.getType(), goods2.getType());
        assertEquals(goods1.getAmount(), goods2.getAmount());

    }

    public void testProductionChain() {
        List<GoodsType> chain = hammersType.getProductionChain();
        assertEquals(lumberType, chain.get(0));
        assertEquals(hammersType, chain.get(1));

        chain = musketsType.getProductionChain();
        assertEquals(oreType, chain.get(0));
        assertEquals(toolsType, chain.get(1));
        assertEquals(musketsType, chain.get(2));
    }
}
