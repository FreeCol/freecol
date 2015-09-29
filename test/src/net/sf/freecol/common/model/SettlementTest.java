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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.util.test.FreeColTestCase;


public class SettlementTest extends FreeColTestCase {

    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final Role mountedBraveRole
        = spec().getRole("model.role.mountedBrave");
    private static final Role nativeDragoonRole
        = spec().getRole("model.role.nativeDragoon");
    private static final Role soldierRole
        = spec().getRole("model.role.soldier");


    public void testSettlementRadius() throws FreeColException {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        FreeColTestCase.IndianSettlementBuilder builder =
            new FreeColTestCase.IndianSettlementBuilder(game);

        Player arawak = game.getPlayerByNationId("model.nation.arawak");
        builder.player(arawak);
        IndianSettlement settlement = builder.build();
        assertEquals(1, arawak.getNationType().getSettlementType(false).getClaimableRadius());
        assertEquals(1, settlement.getType().getClaimableRadius());
        settlement.dispose();

        builder.capital(true);
        settlement = builder.build();
        assertEquals(2, arawak.getNationType().getCapitalType().getClaimableRadius());
        assertEquals(2, settlement.getType().getClaimableRadius());
        settlement.dispose();

        Player inca = game.getPlayerByNationId("model.nation.inca");
        builder.player(inca);
        builder.capital(false);
        settlement = builder.build();
        assertEquals(2, inca.getNationType().getSettlementType(false).getClaimableRadius());
        assertEquals(2, settlement.getType().getClaimableRadius());
        settlement.dispose();

        builder.capital(true);
        settlement = builder.build();
        assertEquals(3, inca.getNationType().getCapitalType().getClaimableRadius());
        assertEquals(3, settlement.getType().getClaimableRadius());
        settlement.dispose();
    }

    public void testColonyRadius() {

        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony();
        assertEquals(1, colony.getOwner().getNationType().getSettlementType(false).getClaimableRadius());
        //assertEquals(1, colony.getOwner().getNationType().getCapitalType().getClaimableRadius());
        assertEquals(1, colony.getRadius());
        assertFalse(colony.isCapital());

    }

    public void testSettlementDoesNotClaimWater() {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        map.getTile(4, 8).setType(spec().getTileType("model.tile.ocean"));
        assertFalse(map.getTile(4, 8).isLand());

        FreeColTestCase.IndianSettlementBuilder builder =
            new FreeColTestCase.IndianSettlementBuilder(game);

        IndianSettlement settlement = builder.build();
        for (Tile tile : settlement.getTile().getSurroundingTiles(1)) {
            assertEquals(tile.isLand(), tile.getOwner() == settlement.getOwner());
            assertEquals(tile.isLand(), tile.getOwningSettlement() == settlement);
        }
    }

    public void testColonyClaimsWater() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);
        map.getTile(4, 8).setType(spec().getTileType("model.tile.ocean"));
        assertFalse(map.getTile(4, 8).isLand());

        Colony colony = getStandardColony();
        for (Tile tile : colony.getTile().getSurroundingTiles(1)) {
            assertEquals("Tile " + tile.toString()
                         + " should be owned by " + colony.getOwner().getId(),
                         tile.getOwner(), colony.getOwner());
            assertEquals("Tile " + tile.toString()
                         + " should be owned by " + colony.getId(),
                         tile.getOwningSettlement(), colony);
        }
    }

    public void testLineOfSight() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);
        Colony colony = getStandardColony();

        assertEquals(2, colony.getLineOfSight());

        BuildingType towerType = new BuildingType("tower", spec());
        Modifier modifier = new Modifier(Modifier.LINE_OF_SIGHT_BONUS, 2,
                                         ModifierType.ADDITIVE);
        towerType.addModifier(modifier);
        Building tower = new ServerBuilding(getGame(), colony, towerType);
        colony.addBuilding(tower);

        assertEquals(4, colony.getLineOfSight());
    }

    public void testCanImproveUnitMilitaryRole() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);
        Colony colony = getStandardColony(4);

        // Colony has no equipment for the unit
        Unit colonist = colony.getUnitList().get(0);
        assertNull(colony.canImproveUnitMilitaryRole(colonist));
        
        // Colony has some equipment, but not enough
        colony.addGoods(musketsType, 40);
        assertNull(colony.canImproveUnitMilitaryRole(colonist));

        // Colony now has enough equipment
        colony.addGoods(musketsType, 10);
        assertTrue(colony.canProvideGoods(soldierRole.getRequiredGoods()));
        assertEquals(soldierRole,
                     colony.canImproveUnitMilitaryRole(colonist));

        // Equipping succeeds, colony can no longer improve
        assertTrue(colony.equipForRole(colonist, soldierRole, 1));
        assertNull(colony.canImproveUnitMilitaryRole(colonist));

        // Adding more muskets does not help
        colony.addGoods(musketsType, 100);
        assertNull(colony.canImproveUnitMilitaryRole(colonist));

        // But adding horses does
        colony.addGoods(horsesType, 100);
        assertEquals(dragoonRole,
                     colony.canImproveUnitMilitaryRole(colonist));

        // Unless now a dragoon
        assertTrue(colony.equipForRole(colonist, dragoonRole, 1));
        assertNull(colony.canImproveUnitMilitaryRole(colonist));

        // Repeat previous tests for natives
        Player arawak = game.getPlayerByNationId("model.nation.arawak");
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game)
            .player(arawak).initialBravesInCamp(4);
        IndianSettlement settlement = builder.build();

        Unit brave = settlement.getUnitList().get(0);
        assertNull(settlement.canImproveUnitMilitaryRole(brave));
        
        settlement.addGoods(horsesType, 20);
        assertNull(settlement.canImproveUnitMilitaryRole(brave));

        settlement.addGoods(horsesType, 10); // avoid breeding number
        assertEquals(mountedBraveRole,
                     settlement.canImproveUnitMilitaryRole(brave));

        assertTrue(settlement.equipForRole(brave, mountedBraveRole, 1));
        assertNull(settlement.canImproveUnitMilitaryRole(brave));

        settlement.addGoods(horsesType, 100);
        assertNull(settlement.canImproveUnitMilitaryRole(brave));

        settlement.addGoods(musketsType, 100);
        assertEquals(nativeDragoonRole,
                     settlement.canImproveUnitMilitaryRole(brave));

        assertTrue(settlement.equipForRole(brave, nativeDragoonRole, 1));
        assertNull(settlement.canImproveUnitMilitaryRole(brave));
    }
}
