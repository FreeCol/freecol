/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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
import net.sf.freecol.util.test.FreeColTestCase;


public class GoodsContainerTest extends FreeColTestCase {

    private GoodsType sugar() { return spec().getGoodsType("model.goods.sugar"); }
    private GoodsType food() { return spec().getPrimaryFoodType(); }
    private GoodsType fish() { return spec().getGoodsType("model.goods.fish"); }
    private GoodsType lumber() { return spec().getGoodsType("model.goods.lumber"); }

    private GoodsContainer getPreparedContainer() {
        Game game = getGame();
        game.changeMap(getTestMap(true)); 
        Colony colony = createStandardColony();
        return new GoodsContainer(game, colony);
    }

    /**
     * Verifies that different goods types respond correctly to removeAbove().
     * Lumber should be capped, but Fish and Food should ignore the limit.
     */
    public void testLimitIgnoringGoods() {
        GoodsContainer container = getPreparedContainer();
        GoodsType lumber = lumber();
        GoodsType fish = fish();
        GoodsType food = food();

        // Add 200 of each
        container.addGoods(lumber, 200);
        container.addGoods(fish, 200);
        container.addGoods(food, 200);

        // Try to cap everything at 100
        container.removeAbove(100);

        assertEquals("Lumber should be capped at 100", 100, container.getGoodsCount(lumber));
        assertEquals("Fish should ignore the cap and stay at 200", 200, container.getGoodsCount(fish));
        assertEquals("Food should ignore the cap and stay at 200", 200, container.getGoodsCount(food));
    }

    public void testGetSpaceTakenWithMultipleTypes() {
        GoodsContainer container = getPreparedContainer();
        
        // 150 Sugar = 2 slots
        container.addGoods(sugar(), 150);
        // 50 Lumber = 1 slot
        container.addGoods(lumber(), 50);

        assertEquals("Should occupy 3 slots total (2 for sugar, 1 for lumber)", 3, container.getSpaceTaken());
    }

    public void testContainerBasicOps() {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        assertEquals(0, container.getGoodsCount(sugar));
        container.addGoods(sugar, 50);
        assertEquals(50, container.getGoodsCount(sugar));
        
        container.removeGoods(sugar, 20);
        assertEquals(30, container.getGoodsCount(sugar));
    }

    public void testStateManagement() {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        container.addGoods(sugar, 50);
        container.saveState(); 

        container.addGoods(sugar, 20); 
        assertTrue("Container should report change", container.hasChanged());
        
        container.restoreState();
        assertEquals("Should restore to 50", 50, container.getGoodsCount(sugar));
    }

    public void testGetGoodsListChunking() {
        GoodsContainer container = getPreparedContainer();
        container.addGoods(sugar(), 250);

        List<Goods> goodsList = container.getGoodsList();
        assertEquals("Should be split into 3 stacks", 3, goodsList.size());

        int total = goodsList.stream().mapToInt(AbstractGoods::getAmount).sum();
        assertEquals(250, total);
    }

    public void testNegativeGoodsException() {
        GoodsContainer container = getPreparedContainer();
        try {
            container.addGoods(sugar(), -10);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // Success
        }
    }

    public void testEdgeCases() {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        // Removing more than exists should clamp to zero
        container.addGoods(sugar, 30);
        container.removeGoods(sugar, 999);
        assertEquals("Removing too much should clamp to zero", 0, container.getGoodsCount(sugar));

        // Adding zero should not change state
        container.addGoods(sugar, 0);
        assertEquals(0, container.getGoodsCount(sugar));

        // Removing zero should not change state
        container.removeGoods(sugar, 0);
        assertEquals(0, container.getGoodsCount(sugar));

        // Adding extremely large amounts should not overflow
        container.addGoods(sugar, Integer.MAX_VALUE / 2);
        assertTrue("Should store large values safely", container.getGoodsCount(sugar) > 0);

        // Removing goods of a type not present should not crash
        container.removeGoods(food(), 10);
        assertEquals(0, container.getGoodsCount(food()));
    }

    public void testMoveGoods() {
        GoodsContainer src = getPreparedContainer();
        GoodsContainer dst = new GoodsContainer(getGame(), src.getParent());
        
        GoodsType sugar = sugar();
        src.addGoods(sugar, 100);
        GoodsContainer.moveGoods(src, sugar, 40, dst);

        assertEquals("Source should have 60 left", 60, src.getGoodsCount(sugar));
        assertEquals("Destination should have 40", 40, dst.getGoodsCount(sugar));
        assertEquals("Source old state should be 100", 100, src.getOldGoodsCount(sugar));
        assertTrue("Source should report changes", src.hasChanged());
    }

    public void testRemoveAll() {
        GoodsContainer container = getPreparedContainer();
        container.addGoods(sugar(), 50);
        container.addGoods(lumber(), 50);
        container.removeAll();
        
        assertEquals(0, container.getGoodsCount(sugar()));
        assertEquals(0, container.getGoodsCount(lumber()));
        assertEquals(0, container.getSpaceTaken());
    }
}
