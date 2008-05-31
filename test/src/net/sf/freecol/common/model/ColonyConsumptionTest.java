package net.sf.freecol.common.model;

import java.util.List;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class ColonyConsumptionTest extends FreeColTestCase {

	public void testFoodConsumption(){
		Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, FreeCol.getSpecification().getTileType("model.tile.plains"), x, y);
            }
        }
        
        Map map = new Map(game, tiles);
        game.setMap(map);
        
        //////////////////////
        // Setting test colony and colonist
        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(5, 8));
        UnitType colonistType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
        new Unit(game, colony.getBuildingForProducing(Goods.BELLS), dutch, colonistType, UnitState.ACTIVE,
                colonistType.getDefaultEquipment());
        assertEquals(0, colony.getFoodCount());
        
        int quantity = colony.getFoodConsumption() * 2;
        colony.addGoods(Goods.FOOD, quantity);
        int foodStored = colony.getFoodCount();
        assertEquals(quantity, foodStored);
        
        colony.updateFood();
        int foodRemaining = foodStored - colony.getFoodConsumption();
        
        assertEquals("Unexpected value for remaining food, ", foodRemaining,colony.getFoodCount());
        
	}
	
	public void testEqualFoodProductionConsumptionCase() {
		
        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, FreeCol.getSpecification().getTileType("model.tile.desert"), x, y);
            }
        }
        
        Map map = new Map(game, tiles);
        game.setMap(map);
        
        //////////////////////
        // Setting test colony
        
        Tile colonyTile = map.getTile(5, 8);
        
        colonyTile.setExploredBy(dutch, true);
        
        map.getTile(5, 8).setExploredBy(dutch, true);
        game.setMap(map);
           
        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(5, 8));
        
        // Set the food production of the center tile of the colony to 2
        // This will be the only food production of the colony
        List<AbstractGoods> colonyTileProduction = colonyTile.getType().getProduction();
        for(int i=0; i< colonyTileProduction.size(); i++ ){
        	AbstractGoods production = colonyTileProduction.get(i);
        	
        	if(production.getType() == Goods.FOOD){
        		colonyTile.getType().getProduction().get(i).setAmount(2);
        		break;
        	}
        }

        UnitType colonistType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
        
        new Unit(game, colony.getBuildingForProducing(Goods.BELLS), dutch, colonistType, UnitState.ACTIVE,
                colonistType.getDefaultEquipment());

        
        // Verify that there is enough food stored
        colony.addGoods(Goods.FOOD, colony.getFoodConsumption()*2);
        
        int colonists = colony.getUnitCount();
        
        String errMsg = "Production not equal to consumption, required to setup test";
        assertEquals(errMsg,colony.getFoodConsumption(),colony.getFoodProduction());
        
        assertEquals("Unexpected change of colonists in colony",colonists,colony.getUnitCount());
        
        assertEquals("Unexpected change of production/consumption ratio",colony.getFoodProduction(),colony.getFoodConsumption());
	}
	
	public void testDeathByStarvation() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, FreeCol.getSpecification().getTileType("model.tile.marsh"), x, y);
            }
        }

        Map map = new Map(game, tiles);

        map.getTile(5, 8).setExploredBy(dutch, true);
        game.setMap(map);
           
        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(5, 8));
        
        UnitType pioneerType = FreeCol.getSpecification().getUnitType("model.unit.hardyPioneer");
        
        int unitsBeforeNewTurn = 3;
        
        for(int i=0; i<unitsBeforeNewTurn;i++){
        	new Unit(game, colony.getBuildingForProducing(Goods.BELLS), dutch, pioneerType, UnitState.ACTIVE,
                                pioneerType.getDefaultEquipment());
        };
        
        int consumption = colony.getFoodConsumption();
        int production = colony.getFoodProduction();
        String errMsg = "Food consumption (" + String.valueOf(consumption) 
                        + ") should be higher than food production ("
                        + String.valueOf(production) + ")"; 
        assertTrue( errMsg, consumption  > production);
         
        int foodStored = colony.getFoodCount();
        colony.removeGoods(Goods.FOOD);
        errMsg = "No food should be stored, colony has (" + String.valueOf(foodStored) + ")"; 
        
        assertTrue(errMsg,foodStored == 0);
        
        assertEquals("Wrong number of units in colony",unitsBeforeNewTurn,colony.getUnitCount());
        
        colony.updateFood();
        
        assertEquals("Wrong number of units in colony",unitsBeforeNewTurn-1,colony.getUnitCount());
        
        consumption = colony.getFoodConsumption();
        production = colony.getFoodProduction();
        errMsg = "Food consumption (" + String.valueOf(consumption) 
                        + ") should be higher than food production ("
                        + String.valueOf(production) + ")"; 
        assertTrue( errMsg, consumption  > production);
         
        
        foodStored = colony.getFoodCount();
        errMsg = "No food should be stored, colony has (" + String.valueOf(foodStored) + ")";
        assertTrue(errMsg,foodStored == 0);
	}
}
