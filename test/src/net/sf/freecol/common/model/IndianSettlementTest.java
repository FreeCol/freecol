/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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


public class IndianSettlementTest extends FreeColTestCase {

    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final Role armedBraveRole
        = spec().getRole("model.role.armedBrave");
    private static final Role nativeDragoonRole
        = spec().getRole("model.role.nativeDragoon");


    public void testAutomaticEquipBraves() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(1).build();

        Unit indianBrave = camp.getUnitList().get(0);

        assertNull("No auto-equip, no muskets",
                   indianBrave.getAutomaticRole());
        camp.addGoods(musketsType, 100);
        assertEquals("Auto-equip to armed brave, muskets present",
                     armedBraveRole, indianBrave.getAutomaticRole());
        camp.addGoods(horsesType, 100);
        assertEquals("Auto-equip to native dragoon, horses and muskets present",
                     nativeDragoonRole, indianBrave.getAutomaticRole());
    }

    /*
     * Test settlement adjacent tiles ownership
     * Per Col1 rules, Indian settlements do not own water tiles
     */
    public void testSettlementDoesNotOwnWaterTiles(){
        Game game = getStandardGame();
        Map map = getCoastTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);

        Tile campTile = map.getTile(9, 9);
        Tile landTile = map.getTile(8, 9);
        Tile waterTile = map.getTile(10, 9);

        assertTrue("Setup error, camp tile should be land", campTile.isLand());
        assertTrue("Setup error, tile should be land", landTile.isLand());
        assertFalse("Setup error, tile should be water", waterTile.isLand());
        assertTrue("Setup error, tiles should be adjacent", campTile.isAdjacent(waterTile));
        assertTrue("Setup error, tiles should be adjacent", campTile.isAdjacent(landTile));

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);

        IndianSettlement camp = builder.settlementTile(campTile).build();

        Player indianPlayer = camp.getOwner();
        assertTrue("Indian player should own camp tile", campTile.getOwner() == indianPlayer);
        assertTrue("Indian player should own land tile", landTile.getOwner() == indianPlayer);
        assertFalse("Indian player should not own water tile", waterTile.getOwner() == indianPlayer);
    }

    /*
     * Test settlement trade
     */
    public void testTradeGoodsWithSetlement(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Tile camp2Tile = map.getTile(3, 3);

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);

        IndianSettlement camp1 = builder.build();
        IndianSettlement camp2 = builder.reset().settlementTile(camp2Tile).build();

        final int notEnoughToShare = 50;
        final int enoughToShare = 100;
        final int none = 0;

        camp1.addGoods(horsesType, notEnoughToShare);
        camp1.addGoods(musketsType, enoughToShare);

        String wrongQtyMusketsMsg = "Wrong quantity of muskets";
        String wrongQtyHorsesMsg = "Wrong quantity of horses";

        assertEquals(wrongQtyMusketsMsg,enoughToShare,camp1.getGoodsCount(musketsType));
        assertEquals(wrongQtyHorsesMsg,notEnoughToShare,camp1.getGoodsCount(horsesType));
        assertEquals(wrongQtyMusketsMsg,none,camp2.getGoodsCount(musketsType));
        assertEquals(wrongQtyHorsesMsg,none,camp2.getGoodsCount(horsesType));

        camp1.tradeGoodsWithSettlement(camp2);

        assertEquals(wrongQtyMusketsMsg,enoughToShare / 2,camp1.getGoodsCount(musketsType));
        assertEquals(wrongQtyHorsesMsg,notEnoughToShare,camp1.getGoodsCount(horsesType));
        assertEquals(wrongQtyMusketsMsg,enoughToShare / 2,camp2.getGoodsCount(musketsType));
        assertEquals(wrongQtyHorsesMsg,none,camp2.getGoodsCount(horsesType));
    }
}
