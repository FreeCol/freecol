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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
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

    public void testLimitIgnoringGoods() {
        GoodsContainer container = getPreparedContainer();
        GoodsType lumber = lumber();
        GoodsType fish = fish();
        GoodsType food = food();

        container.addGoods(lumber, 200);
        container.addGoods(fish, 200);
        container.addGoods(food, 200);

        container.removeAbove(100);

        assertEquals("Lumber should be capped at 100", 100, container.getGoodsCount(lumber));
        assertEquals("Fish should ignore the cap and stay at 200", 200, container.getGoodsCount(fish));
        assertEquals("Food should ignore the cap and stay at 200", 200, container.getGoodsCount(food));
    }

    public void testGetSpaceTakenWithMultipleTypes() {
        GoodsContainer container = getPreparedContainer();
        
        container.addGoods(sugar(), 150);
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

        container.addGoods(sugar, 30);
        container.removeGoods(sugar, 999);
        assertEquals("Removing too much should clamp to zero", 0, container.getGoodsCount(sugar));

        container.addGoods(sugar, 0);
        assertEquals(0, container.getGoodsCount(sugar));

        container.removeGoods(sugar, 0);
        assertEquals(0, container.getGoodsCount(sugar));

        container.addGoods(sugar, Integer.MAX_VALUE / 2);
        assertTrue("Should store large values safely", container.getGoodsCount(sugar) > 0);

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

    public void testConcurrentAccess() throws Exception {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalAdded = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    container.addGoods(sugar, 1);
                    totalAdded.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        assertEquals("Concurrent adds must be consistent",
            totalAdded.get(), container.getGoodsCount(sugar));
    }

    public void testNegativeRemovalActsAsAddition() {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        container.addGoods(sugar, 50);
        container.removeGoods(sugar, -20);

        assertEquals("Negative removal should increase goods",
            70, container.getGoodsCount(sugar));
    }

    public void testRemoveAboveZeroClearsAll() {
        GoodsContainer container = getPreparedContainer();
        container.addGoods(sugar(), 100);
        container.addGoods(lumber(), 100);

        container.removeAbove(0);

        assertEquals(0, container.getGoodsCount(sugar()));
        assertEquals(0, container.getGoodsCount(lumber()));
    }

    public void testRemoveAboveNegativeClearsAll() {
        GoodsContainer container = getPreparedContainer();
        container.addGoods(sugar(), 100);

        container.removeAbove(-1);

        assertEquals(0, container.getGoodsCount(sugar()));
    }

    public void testSpaceTakenExactBoundaries() {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        container.addGoods(sugar, 100);
        assertEquals(1, container.getSpaceTaken());

        container.removeAll();
        container.addGoods(sugar, 200);
        assertEquals(2, container.getSpaceTaken());

        container.removeAll();
        container.addGoods(sugar, 99);
        assertEquals(1, container.getSpaceTaken());

        container.removeAll();
        container.addGoods(sugar, 101);
        assertEquals(2, container.getSpaceTaken());

        container.removeAll();
        assertEquals(0, container.getSpaceTaken());
    }

    public void testXMLRoundTrip() throws Exception {
        GoodsContainer container = getPreparedContainer();
        container.addGoods(sugar(), 150);
        container.addGoods(lumber(), 75);
        container.saveState();

        String xml = writeToXML(container);
        GoodsContainer restored = readFromXML(xml, GoodsContainer.class);

        assertEquals(container.getGoodsCount(sugar()), restored.getGoodsCount(sugar()));
        assertEquals(container.getGoodsCount(lumber()), restored.getGoodsCount(lumber()));
        assertEquals(container.getOldGoodsCount(sugar()), restored.getOldGoodsCount(sugar()));
        assertEquals(container.getOldGoodsCount(lumber()), restored.getOldGoodsCount(lumber()));
    }

    public void testCopyInDeepCopy() {
        GoodsContainer src = getPreparedContainer();
        GoodsContainer dst = getPreparedContainer();

        src.addGoods(sugar(), 100);
        src.saveState();

        dst.copyIn(src);

        assertEquals(100, dst.getGoodsCount(sugar()));
        assertEquals(100, dst.getOldGoodsCount(sugar()));

        src.addGoods(sugar(), 50);

        assertEquals("dst must not share storedGoods reference",
            100, dst.getGoodsCount(sugar()));
    }

    public void testMoveGoodsAtomicity() {
        GoodsContainer src = getPreparedContainer();

        GoodsType sugar = sugar();
        src.addGoods(sugar, 100);

        GoodsContainer faultyDst = new GoodsContainer(getGame(), src.getParent()) {
            @Override
            public boolean addGoods(GoodsType type, int amount) {
                throw new RuntimeException("Simulated failure");
            }
        };

        try {
            GoodsContainer.moveGoods(src, sugar, 40, faultyDst);
            fail("Expected failure");
        } catch (RuntimeException e) {
            // expected
        }

        assertEquals("Source should remain consistent after failed move",
            60, src.getGoodsCount(sugar()));
    }

    public void testRemoveGoodsReturnValue() {
        GoodsContainer container = getPreparedContainer();
        GoodsType sugar = sugar();

        container.addGoods(sugar, 50);
        Goods g1 = container.removeGoods(sugar, 20);
        assertEquals(20, g1.getAmount());

        container.addGoods(sugar, 50);
        Goods g2 = container.removeGoods(sugar, 999);
        assertEquals(80, g2.getAmount());
    }

    public void testToStringFormatting() {
        GoodsContainer container = getPreparedContainer();
        String s1 = container.toString();
        assertTrue("Empty container should not end with comma", !s1.contains(", ]"));

        container.addGoods(sugar(), 50);
        String s2 = container.toString();
        assertTrue("Stored goods must appear", s2.contains("sugar"));
    }

    public void testMultiTypeStateRestore() {
        GoodsContainer container = getPreparedContainer();

        container.addGoods(sugar(), 50);
        container.addGoods(lumber(), 30);
        container.addGoods(fish(), 10);

        container.saveState();

        container.addGoods(sugar(), 20);
        container.addGoods(lumber(), 20);
        container.addGoods(fish(), 20);

        container.restoreState();

        assertEquals(50, container.getGoodsCount(sugar()));
        assertEquals(30, container.getGoodsCount(lumber()));
        assertEquals(10, container.getGoodsCount(fish()));
    }

    private String writeToXML(FreeColObject obj) throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        FreeColXMLWriter xw = new FreeColXMLWriter(sw, FreeColXMLWriter.WriteScope.toSave());
        obj.toXML(xw);
        xw.close();
        return sw.toString();
    }

    private <T extends FreeColObject> T readFromXML(String xml, Class<T> cls) throws Exception {
        java.io.StringReader sr = new java.io.StringReader(xml);
        FreeColXMLReader xr = new FreeColXMLReader(sr);
        xr.nextTag(); // <--- FIX: Advances the cursor to the START_ELEMENT node
        T obj = xr.readFreeColObject(getGame(), cls);
        xr.close();
        return obj;
    }
}
