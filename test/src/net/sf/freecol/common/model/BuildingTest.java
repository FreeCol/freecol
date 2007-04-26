package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class BuildingTest extends FreeColTestCase {

    public void test_canBuildNext_FalseAboveMaxLevel() {
       
        Game game = getStandardGame();
        Player dutch = game.getPlayer(Player.DUTCH);

        Map map = getTestMap(Tile.PLAINS);
        game.setMap(map);
        map.getTile(5, 8).setExploredBy(dutch, true);
       
        Colony colony = new Colony(game, dutch, "New Amsterdam",
                                   map.getTile(5, 8));
       
        Building new_building = new Building(game, colony,
                                             Building.BLACKSMITH,
                                             Building.MAX_LEVEL +  1);
       
        assertFalse(new_building.canBuildNext());
    }

    public void test_canBuildNext_FalseAboveFactoryLevel() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer(Player.DUTCH);

        Map map = getTestMap(Tile.PLAINS);
        game.setMap(map);
        map.getTile(5, 8).setExploredBy(dutch, true);
       
        Colony colony = new Colony(game, dutch, "New Amsterdam",
                                   map.getTile(5, 8));
       
        int level = Building.FACTORY + 1;

        Building new_building = new Building(game, colony, Building.BLACKSMITH, level);
       
        assertFalse(new_building.canBuildNext());

    }
    
} 
