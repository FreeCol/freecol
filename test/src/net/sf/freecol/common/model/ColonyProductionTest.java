package net.sf.freecol.common.model;

import java.util.Vector;

import net.sf.freecol.FreeCol;
import net.sf.freecol.util.test.FreeColTestCase;

public class ColonyProductionTest extends FreeColTestCase {

    public void testProductionOne() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

        for (int x = 0; x < 10; x++) {
            tiles.add(new Vector<Tile>(15));
        }

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles.get(x).add(new Tile(game, FreeCol.getSpecification().getTileType("model.tile.plains"), x, y));
            }
        }

        Map map = new Map(game, tiles);

        map.getTile(5, 8).setResource(FreeCol.getSpecification().getResourceType("model.resource.grain"));
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
                
        game.setMap(map);
                
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, 
                                FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier"), 
                                Unit.ACTIVE, true, false, 0, false);
                
        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        { // Test the colony
            assertEquals(map.getTile(6, 8), colony.getTile());

            assertEquals("New Amsterdam", colony.getLocationName());

            assertEquals(colony, colony.getTile().getSettlement());

            assertEquals(dutch, colony.getTile().getOwner());

            // Should have 50 Muskets and nothing else
            GoodsType muskets = FreeCol.getSpecification().getGoodsType("model.goods.Muskets");
            assertNotNull(muskets);
            
            for (GoodsType type : FreeCol.getSpecification().getGoodsTypeList()){
                if (type == muskets)
                    assertEquals(50, colony.getGoodsCount(type));
                else
                    assertEquals(type.getName(), 0, colony.getGoodsCount(type));
            }
        }

        { // Test the state of the soldier
            // Soldier should be working on the field with the bonus
            assertEquals(Goods.FOOD, soldier.getWorkType());

            assertEquals(colony.getColonyTile(map.getTile(5,8)).getTile(), soldier.getLocation().getTile());

            assertEquals(0, soldier.getMovesLeft());

            assertEquals(false, soldier.isArmed());
        }
    }

}
