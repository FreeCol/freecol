/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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
        IndianSettlement is = builder.build();
        assertEquals(1, arawak.getNationType().getSettlementType(false).getClaimableRadius());
        assertEquals(1, is.getType().getClaimableRadius());
        is.dispose();

        builder.capital(true);
        is = builder.build();
        assertEquals(2, arawak.getNationType().getCapitalType().getClaimableRadius());
        assertEquals(2, is.getType().getClaimableRadius());
        is.dispose();

        Player inca = game.getPlayerByNationId("model.nation.inca");
        builder.player(inca);
        builder.capital(false);
        is = builder.build();
        assertEquals(2, inca.getNationType().getSettlementType(false).getClaimableRadius());
        assertEquals(2, is.getType().getClaimableRadius());
        is.dispose();

        builder.capital(true);
        is = builder.build();
        assertEquals(3, inca.getNationType().getCapitalType().getClaimableRadius());
        assertEquals(3, is.getType().getClaimableRadius());
        is.dispose();
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

        IndianSettlement is = builder.build();
        for (Tile tile : is.getTile().getSurroundingTiles(1)) {
            assertEquals(tile.isLand(), tile.getOwner() == is.getOwner());
            assertEquals(tile.isLand(), tile.getOwningSettlement() == is);
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
        assertTrue(colony.canProvideGoods(soldierRole.getRequiredGoodsList()));
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
        IndianSettlement is = builder.build();

        Unit brave = is.getUnitList().get(0);
        assertNull(is.canImproveUnitMilitaryRole(brave));
        
        is.addGoods(horsesType, 20);
        assertNull(is.canImproveUnitMilitaryRole(brave));

        is.addGoods(horsesType, 10); // avoid breeding number
        assertEquals(mountedBraveRole,
                     is.canImproveUnitMilitaryRole(brave));

        assertTrue(is.equipForRole(brave, mountedBraveRole, 1));
        assertNull(is.canImproveUnitMilitaryRole(brave));

        is.addGoods(horsesType, 100);
        assertNull(is.canImproveUnitMilitaryRole(brave));

        is.addGoods(musketsType, 100);
        assertEquals(nativeDragoonRole,
                     is.canImproveUnitMilitaryRole(brave));

        assertTrue(is.equipForRole(brave, nativeDragoonRole, 1));
        assertNull(is.canImproveUnitMilitaryRole(brave));
    }
}
