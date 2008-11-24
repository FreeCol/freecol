package net.sf.freecol.common.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Map.CircleIterator;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class IndianSettlementTest extends FreeColTestCase {
	public void testFoodConsumption(){
		Game game = getStandardGame();

        Player indianPlayer = game.getPlayer("model.nation.tupi");

        Map map = getEmptyMap();
        game.setMap(map);
        
        //////////////////////
        // Setting test settlement and brave
        Tile settlementTile = map.getTile(5, 8);
        UnitType skillToTeach = FreeCol.getSpecification().getUnitType("model.unit.masterCottonPlanter");
        boolean isCapital = true;
        boolean isVisited = false;
        Unit residentMissionary = null;
        IndianSettlement camp = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        Unit brave = new Unit(game, camp, indianPlayer, indianBraveType, UnitState.ACTIVE,
                indianBraveType.getDefaultEquipment());
        camp.addOwnedUnit(brave);
        
        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getFoodCount());

        GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
        int foodProduced = camp.getProductionOf(foodType);
        int foodConsumed = camp.getFoodConsumption();
        assertTrue("Food Produced should be more the food consumed",foodProduced > foodConsumed);
        
        camp.newTurn();
        int foodRemaining = Math.max(foodProduced - foodConsumed, 0);
        
        assertEquals("Unexpected value for remaining food, ", foodRemaining,camp.getFoodCount());      
	}
	
	public void testDeathByStarvation(){
		Game game = getStandardGame();

        Player indianPlayer = game.getPlayer("model.nation.tupi");

        TileType desertType = FreeCol.getSpecification().getTileType("model.tile.desert");
        Map map = getTestMap(desertType);
        game.setMap(map);
        
        //////////////////////
        // Setting test settlement and braves
        Tile settlementTile = map.getTile(5, 8);
        UnitType skillToTeach = FreeCol.getSpecification().getUnitType("model.unit.masterCottonPlanter");
        boolean isCapital = false;
        boolean isVisited = false;
        Unit residentMissionary = null;
        IndianSettlement camp1 = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        int initialBravesInCamp = 3;
        
        for(int i=0; i < initialBravesInCamp; i++){
        	UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        	Unit brave = new Unit(game, camp1, indianPlayer, indianBraveType, UnitState.ACTIVE,
                indianBraveType.getDefaultEquipment());
        	camp1.addOwnedUnit(brave);
        }

        assertEquals(initialBravesInCamp, camp1.getUnitCount());
        assertEquals(0, camp1.getFoodCount());
        
        //////////////////////
        // Simulate that only the center tile is owned by camp 1
        IndianSettlement camp2 = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        CircleIterator tilesAroundCamp = map.getCircleIterator(settlementTile.getPosition(), true, camp1.getRadius());
        
        while(tilesAroundCamp.hasNext()){
        	Position p = tilesAroundCamp.next();
        	Tile t = map.getTile(p);
        	t.setOwningSettlement(camp2);
        	
        }
           
        GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
        int foodProduced = camp1.getProductionOf(foodType);
        int foodConsumed = camp1.getFoodConsumption();
        assertTrue("Food Produced should be less the food consumed",foodProduced < foodConsumed);
        
        camp1.newTurn();
        int foodRemaining = 0;
        
        assertEquals("Unexpected value for remaining food, ", foodRemaining,camp1.getFoodCount());
        assertFalse("Some braves should have died of starvation",camp1.getUnitCount() < initialBravesInCamp);
	}
	
	public void testHorseBreeding(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
        
		Game game = getStandardGame();

        Player indianPlayer = game.getPlayer("model.nation.tupi");

        Map map = getEmptyMap();
        game.setMap(map);
        
        //////////////////////
        // Setting test settlement and brave
        Tile settlementTile = map.getTile(5, 8);
        UnitType skillToTeach = FreeCol.getSpecification().getUnitType("model.unit.masterCottonPlanter");
        boolean isCapital = true;
        boolean isVisited = false;
        Unit residentMissionary = null;
        IndianSettlement camp = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        Unit brave = new Unit(game, camp, indianPlayer, indianBraveType, UnitState.ACTIVE,
                indianBraveType.getDefaultEquipment());
        camp.addOwnedUnit(brave);
        
        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getFoodCount());

        //add horses
        int initialHorses = 2;
        camp.addGoods(horsesType, initialHorses);
        
        // verify that there is food production for the horses
        int foodProduced = camp.getProductionOf(foodType);
        int foodConsumed = camp.getFoodConsumption();
        int foodAvail = foodProduced - foodConsumed;
        assertTrue("Food Produced should be more the food consumed",foodProduced > foodConsumed);
        
        camp.newTurn();
        
        int expectedHorseProd = Math.min(IndianSettlement.MAX_HORSES_PER_TURN, foodAvail);
        assertTrue("Horses should breed", expectedHorseProd > 0);
        
        int horsesBreeded = camp.getGoodsCount(horsesType) - initialHorses;
        assertEquals("Wrong number of horses breeded",expectedHorseProd,horsesBreeded);
	}
	
	public void testHorseBreedingNoHorsesAvail(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
        
		Game game = getStandardGame();

        Player indianPlayer = game.getPlayer("model.nation.tupi");

        Map map = getEmptyMap();
        game.setMap(map);
        
        //////////////////////
        // Setting test settlement and brave
        Tile settlementTile = map.getTile(5, 8);
        UnitType skillToTeach = FreeCol.getSpecification().getUnitType("model.unit.masterCottonPlanter");
        boolean isCapital = true;
        boolean isVisited = false;
        Unit residentMissionary = null;
        IndianSettlement camp = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        Unit brave = new Unit(game, camp, indianPlayer, indianBraveType, UnitState.ACTIVE,
                indianBraveType.getDefaultEquipment());
        camp.addOwnedUnit(brave);
        
        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getFoodCount());
        
        // verify that there is food production for the horses
        int foodProduced = camp.getProductionOf(foodType);
        int foodConsumed = camp.getFoodConsumption();
        assertTrue("Food Produced should be more the food consumed",foodProduced > foodConsumed);
        
        camp.newTurn();
        
        int expectedHorses = 0;
        int horsesAvail = camp.getGoodsCount(horsesType);
        assertEquals("No horses should be in settlement",expectedHorses,horsesAvail);
	}
	
	public void testHorseBreedingFoodAvail(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
		GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
		
		Game game = getStandardGame();

        Player indianPlayer = game.getPlayer("model.nation.tupi");

        TileType desertType = FreeCol.getSpecification().getTileType("model.tile.desert");
        Map map = getTestMap(desertType);
        game.setMap(map);
        
        //////////////////////
        // Setting test settlement and braves
        Tile settlementTile = map.getTile(5, 8);
        UnitType skillToTeach = FreeCol.getSpecification().getUnitType("model.unit.masterCottonPlanter");
        boolean isCapital = false;
        boolean isVisited = false;
        Unit residentMissionary = null;
        IndianSettlement camp1 = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        int initialBravesInCamp = 3;
        
        for(int i=0; i < initialBravesInCamp; i++){
        	UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        	Unit brave = new Unit(game, camp1, indianPlayer, indianBraveType, UnitState.ACTIVE,
                indianBraveType.getDefaultEquipment());
        	camp1.addOwnedUnit(brave);
        }

        assertEquals(initialBravesInCamp, camp1.getUnitCount());
        assertEquals(0, camp1.getFoodCount());
        
        //////////////////////
        // Simulate that only the center tile is owned by camp 1
        IndianSettlement camp2 = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
        
        CircleIterator tilesAroundCamp = map.getCircleIterator(settlementTile.getPosition(), true, camp1.getRadius());
        
        while(tilesAroundCamp.hasNext()){
        	Position p = tilesAroundCamp.next();
        	Tile t = map.getTile(p);
        	t.setOwningSettlement(camp2);
        	
        }
           
        int foodProduced = camp1.getProductionOf(foodType);
        int foodConsumed = camp1.getFoodConsumption();
        assertTrue("Food Produced should be less the food consumed",foodProduced < foodConsumed);
        
        //add horses
        int initialHorses = 2;
        camp1.addGoods(horsesType, initialHorses);
        
        camp1.newTurn();
        
        int expectedHorsesBreeded = 0;
        int horsesBreeded = camp1.getGoodsCount(horsesType) - initialHorses;
        assertEquals("No horses should be bred",expectedHorsesBreeded,horsesBreeded);
	}
}
