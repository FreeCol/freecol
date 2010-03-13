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

import net.sf.freecol.util.test.FreeColTestCase;

public class GoodsContainerTest extends FreeColTestCase {

    GoodsType sugar = spec().getGoodsType("model.goods.sugar");
    GoodsType food = spec().getGoodsType("model.goods.food");
    GoodsType fish = spec().getGoodsType("model.goods.fish");
    GoodsType lumber = spec().getGoodsType("model.goods.lumber");

    public void testContainer() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony();
        GoodsContainer container = new GoodsContainer(game, colony);

        assertEquals(0, container.getGoodsCount(sugar));
        assertEquals(0, container.getGoodsCount(food));
        assertEquals(0, container.getGoodsCount(fish));
        assertEquals(0, container.getGoodsCount(lumber));

        container.addGoods(sugar, 36);
        assertEquals(36, container.getGoodsCount(sugar));
        container.addGoods(lumber, 37);
        assertEquals(37, container.getGoodsCount(lumber));
        container.addGoods(food, 38);
        assertEquals(38, container.getGoodsCount(food));
        container.addGoods(fish, 39);
        assertEquals(39, container.getGoodsCount(fish));

        int difference = 20;
        int totalDifference = difference;
        container.addGoods(sugar, difference);
        assertEquals(36 + totalDifference, container.getGoodsCount(sugar));
        container.addGoods(lumber, difference);
        assertEquals(37 + totalDifference, container.getGoodsCount(lumber));
        container.addGoods(food, difference);
        assertEquals(38 + totalDifference, container.getGoodsCount(food));
        container.addGoods(fish, difference);
        assertEquals(39 + totalDifference, container.getGoodsCount(fish));

        difference = 10;
        totalDifference -= difference;
        container.removeGoods(sugar, difference);
        assertEquals(36 + totalDifference, container.getGoodsCount(sugar));
        container.removeGoods(lumber, difference);
        assertEquals(37 + totalDifference, container.getGoodsCount(lumber));
        container.removeGoods(food, difference);
        assertEquals(38 + totalDifference, container.getGoodsCount(food));
        container.removeGoods(fish, difference);
        assertEquals(39 + totalDifference, container.getGoodsCount(fish));

        difference = -20;
        totalDifference += difference;
        container.addGoods(sugar, difference);
        assertEquals(36 + totalDifference, container.getGoodsCount(sugar));
        container.addGoods(lumber, difference);
        assertEquals(37 + totalDifference, container.getGoodsCount(lumber));
        container.addGoods(food, difference);
        assertEquals(38 + totalDifference, container.getGoodsCount(food));
        container.addGoods(fish, difference);
        assertEquals(39 + totalDifference, container.getGoodsCount(fish));

        container.removeAbove(10);
        assertEquals(10, container.getGoodsCount(sugar));
        assertEquals(10, container.getGoodsCount(lumber));
        // food and fish ignore limit
        assertEquals(38 + totalDifference, container.getGoodsCount(food));
        assertEquals(39 + totalDifference, container.getGoodsCount(fish));

        difference = 20;
        totalDifference -= difference;
        container.removeGoods(sugar, difference);
        assertEquals(0, container.getGoodsCount(sugar));
        container.removeGoods(lumber, difference);
        assertEquals(0, container.getGoodsCount(lumber));
        container.removeGoods(food, difference);
        assertEquals(38 + totalDifference, container.getGoodsCount(food));
        container.removeGoods(fish, difference);
        assertEquals(39 + totalDifference, container.getGoodsCount(fish));
    }
    
    public void testLimitsMessageDelivery(){
    	final int lowLevel = 10;
    	final int highLevel = 90;
    	final int exportLevel = 50;
    	final int overfullLevel = 201;
    	final boolean canExport = true;
    	int foodStart= lowLevel;
    	
        Game game = getStandardGame();
    	game.setMap(getTestMap());
    	
    	Colony colony = getStandardColony(1);
    	
    	//Setup
    	colony.addGoods(new AbstractGoods(food,foodStart));
    	assertEquals("Setup error, wrong food count", foodStart, colony.getGoodsCount(food));
    	colony.setExportData(new ExportData(food,canExport,lowLevel,highLevel,exportLevel));
    	assertEquals("Setup error, wrong food low level",lowLevel,colony.getExportData(food).getLowLevel()); 
    	colony.getGoodsContainer().saveState();
    	
    	// Test current condition, no warnings
    	assertTrue("Setup error, no messages should have bee received yet", colony.getOwner().getModelMessages().isEmpty());
    	colony.getGoodsContainer().cleanAndReport();
    	assertTrue("Player should not have received any messages", colony.getOwner().getModelMessages().isEmpty());
    	
    	// Simulate consumption of food
    	colony.getGoodsContainer().removeGoods(food, 1);
    	
    	// Test new condition food below limits
    	colony.getGoodsContainer().cleanAndReport();
    	assertTrue("Player should have received one message", colony.getOwner().getModelMessages().size() == 1);

      // Stuff the colony overfull with food.  This should *not* trigger
      // a warning because upper limits are ignored for food.
      colony.getGoodsContainer().addGoods(food, overfullLevel);
      colony.getOwner().clearModelMessages();
    	colony.getGoodsContainer().cleanAndReport();
      assertTrue("Food does not have a storage limit", food.limitIgnored());
      assertTrue("Player should not receive a message", colony.getOwner().getModelMessages().size() == 0);
    }

}
