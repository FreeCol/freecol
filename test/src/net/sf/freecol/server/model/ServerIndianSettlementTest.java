/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.server.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ServerIndianSettlementTest extends FreeColTestCase {

    private static final TileType desertType
        = spec().getTileType("model.tile.desert");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");

    private static final UnitType brave
        = spec().getUnitType("model.unit.brave");


    public void testFoodConsumption() {
        Map map = getTestMap();
        Game game = ServerTestHelper.startServerGame(map);

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();

        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getFoodCount());

        int foodProduced = camp.getProductionOf(grainType);
        int foodConsumed = camp.getFoodConsumption();
        assertTrue("Food Produced should be more the food consumed",foodProduced > foodConsumed);

        ServerTestHelper.newTurn();

        int foodRemaining = Math.max(foodProduced - foodConsumed, 0);
        assertEquals("Unexpected value for remaining food, ", foodRemaining,camp.getFoodCount());
    }

    public void testHorseBreeding() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();

        //verify initial conditions
        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getFoodCount());

        //add horses
        int initialHorses = horsesType.getBreedingNumber();
        camp.addGoods(horsesType, initialHorses);

        // verify that there is food production for the horses
        assertEquals("Horses need food", foodType, horsesType.getRawMaterial());
        int foodProduced = camp.getProductionOf(grainType);
        int foodConsumed = camp.getFoodConsumption();
        int foodAvail = foodProduced - foodConsumed;
        assertTrue("Food Produced should be more the food consumed",
                   foodProduced > foodConsumed);

        int expectedHorseProd = Math.min(IndianSettlement.MAX_HORSES_PER_TURN,
                                         foodAvail);
        assertTrue("Horses should breed", expectedHorseProd > 0);

        ServerTestHelper.newTurn();

        int horsesBred = camp.getGoodsCount(horsesType) - initialHorses;
        assertEquals("Wrong number of horses bred",
                     expectedHorseProd, horsesBred);
    }

    public void testHorseBreedingNoFoodAvail() {
        Map map = getTestMap(desertType);
        Game game = ServerTestHelper.startServerGame(map);

        int initialBravesInCamp = 3;
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp1 = builder.initialBravesInCamp(initialBravesInCamp).build();
        IndianSettlement camp2 = builder.reset()
            .settlementTile(camp1.getTile().getNeighbourOrNull(Direction.N)
                            .getNeighbourOrNull(Direction.N)).build();

        //////////////////////
        // Simulate that only the center tile is owned by camp 1
        // Does not matter where camp 2 is, so we put it in the same tile as camp1
        int overlappingTiles = 8; // all the tiles around the camp
        for (Tile t: camp1.getTile().getSurroundingTiles(camp1.getRadius())) {
            t.setOwningSettlement(camp2);
        }


        //verify initial conditions
        assertEquals(initialBravesInCamp, camp1.getUnitCount());
        assertEquals(0, camp1.getFoodCount());

        int foodProduced = camp1.getProductionOf(grainType);
        int foodConsumed = camp1.getFoodConsumption();
        assertEquals(2, brave.getConsumptionOf(foodType));
        assertEquals(2 * camp1.getUnitCount(), foodConsumed);
        assertTrue("Food Produced should be less the food consumed",foodProduced < foodConsumed);

        //add horses
        int initialHorses = 2;
        camp1.addGoods(horsesType, initialHorses);

        ServerTestHelper.newTurn();

        int expectedHorsesBreeded = 0;
        int horsesBreeded = camp1.getGoodsCount(horsesType) - initialHorses;
        assertEquals("No horses should be bred",expectedHorsesBreeded,horsesBreeded);
    }
}
