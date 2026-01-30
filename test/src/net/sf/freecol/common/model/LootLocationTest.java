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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Tests for the LootLocation null-object implementation.
 */
public class LootLocationTest extends FreeColTestCase {

    public void testSingletonBasics() {
        LootLocation ll = LootLocation.INSTANCE;
        assertNotNull(ll);
        assertEquals(Location.LOCATION_RANK_NOWHERE, ll.getRank());
        assertNull(ll.getTile());
        assertNull(ll.getGoodsContainer());
        assertEquals(0, ll.getUnitCount());
        assertTrue(ll.getUnits().count() == 0);
    }

    public void testGoodsAssignedToLootLocation() {
        Game game = getStandardGame();
        game.setMap(getTestMap());
        GoodsType type = spec().getGoodsType("model.goods.food");
        Colony colony = createStandardColony();
        Goods goods = new Goods(game, colony, type, 10);
        goods.setLocation(LootLocation.INSTANCE);
        assertTrue(goods.isLoot());
        assertEquals(LootLocation.INSTANCE, goods.getLocation());
    }

    public void testAddAndRemoveLocatableDoesNotCrash() {
        LootLocation ll = LootLocation.INSTANCE;
        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ServerUnit unit = new ServerUnit(game, LootLocation.INSTANCE, dutch, colonist);
        assertTrue(ll.add(unit));
        assertTrue(ll.remove(unit));
        assertFalse(ll.contains(unit));
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getStandardGame();
        game.setMap(getTestMap());
        GoodsType type = spec().getGoodsType("model.goods.food");
        Colony colony = createStandardColony();
        Goods goods = new Goods(game, colony, type, 10);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw =
            new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);
        goods.toXML(xw);
        xw.close();
        String xml = out.toString("UTF-8");
        assertTrue(xml.contains("goods"));
        assertTrue(xml.contains("location"));
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();
        Goods loaded = new Goods(game, xr);
        xr.close();
        assertTrue(loaded.getLocation() instanceof Location);
    }

    public void testUpReturnsSelf() {
        LootLocation ll = LootLocation.INSTANCE;
        assertSame(ll, ll.up());
    }

    public void testToShortString() {
        assertEquals("Looting...", LootLocation.INSTANCE.toShortString());
    }

    public void testXMLTagNameIsStable() {
        assertEquals("lootLocation", LootLocation.INSTANCE.getXMLTagName());
    }

    public void testCopyInThrows() {
        LootLocation ll = LootLocation.INSTANCE;
        try {
            ll.copyIn(LootLocation.INSTANCE);
            fail("copyIn should not be supported for LootLocation");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testLootLocationSerializedAndRestored() throws Exception {
        Game game = getStandardGame();
        game.setMap(getTestMap());
        GoodsType type = spec().getGoodsType("model.goods.food");
        Colony colony = createStandardColony();
        Goods goods = new Goods(game, colony, type, 10);
        goods.setLocation(LootLocation.INSTANCE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw =
            new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);
        goods.toXML(xw);
        xw.close();
        String xml = out.toString("UTF-8");
        assertTrue(xml.contains("location"));
        assertTrue(xml.contains("model.location.loot"));
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();
        Goods loaded = new Goods(game, xr);
        xr.close();
        assertTrue(loaded.isLoot());
        assertEquals(LootLocation.INSTANCE, loaded.getLocation());
    }
}

