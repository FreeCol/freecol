package net.sf.freecol.common.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Map.CircleIterator;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.util.test.FreeColTestCase;

public class IndianSettlementTest extends FreeColTestCase {
	
	/**
	 * Changes the ownership of a number of tiles from and around camp1 to camp2.
	 * The param 'nTiles' defines the number of tiles to change ownership of.
	 * @param game
	 * @param camp1
	 * @param camp2
	 * @param nTiles
	 */
	private void setOverlapingCamps(Game game, IndianSettlement camp1, IndianSettlement camp2, int nTiles){
        Map map = game.getMap();
		Tile settlementTile = camp1.getTile();
        
		// Change tile ownership around camp1 to camp2
        CircleIterator tilesAroundCamp = map.getCircleIterator(settlementTile.getPosition(), true, camp1.getRadius());    
        while(tilesAroundCamp.hasNext()){
        	Position p = tilesAroundCamp.next();
        	Tile t = map.getTile(p);
        	t.setOwningSettlement(camp2);
        }
	}
	
	public void testFoodConsumption(){
		Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        
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
	
	/*
	public void testDeathByStarvation(){
		Game game = getStandardGame();

        TileType desertType = FreeCol.getSpecification().getTileType("model.tile.desert");
        Map map = getTestMap(desertType);
        game.setMap(map);
        
        int initialBravesInCamp = 3;
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp1 = builder.initialBravesInCamp(initialBravesInCamp).build();
        IndianSettlement camp2 = builder.reset().build();
        
        //////////////////////
        // Simulate that only the center tile is owned by camp 1
        // Does not matter where camp 2 is, so we put it in the same tile as camp1

        int overlappingTiles = 8; // all the tiles around the camp
        
        setOverlapingCamps(game, camp1, camp2, overlappingTiles);
        
        //verify initial conditions
        assertEquals(initialBravesInCamp, camp1.getUnitCount());
        assertEquals(0, camp1.getFoodCount());
        
        GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
        int foodProduced = camp1.getProductionOf(foodType);
        int foodConsumed = camp1.getFoodConsumption();
        assertTrue("Food Produced should be less than food consumed",foodProduced < foodConsumed);
        
        // Execute
        camp1.newTurn();
        
        // Verify conditions
        int foodRemaining = 0;
        assertEquals("Unexpected value for remaining food, ", foodRemaining,camp1.getFoodCount());
        assertTrue("Some braves should have died of starvation",camp1.getUnitCount() < initialBravesInCamp);
        
	}
	*/
	
	public void testHorseBreeding(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
        
		Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        
        //verify initial conditions
        
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
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        
        // verify that there is food production for the horses
        int foodProduced = camp.getProductionOf(foodType);
        int foodConsumed = camp.getFoodConsumption();
        assertTrue("Food Produced should be more the food consumed",foodProduced > foodConsumed);
        
        camp.newTurn();
        
        int expectedHorses = 0;
        int horsesAvail = camp.getGoodsCount(horsesType);
        assertEquals("No horses should be in settlement",expectedHorses,horsesAvail);
	}
	
	public void testHorseBreedingNoFoodAvail(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
		GoodsType foodType = FreeCol.getSpecification().getGoodsType("model.goods.food");
		
		Game game = getStandardGame();

        TileType desertType = FreeCol.getSpecification().getTileType("model.tile.desert");
        Map map = getTestMap(desertType);
        game.setMap(map);
        
        int initialBravesInCamp = 3;
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp1 = builder.initialBravesInCamp(initialBravesInCamp).build();
        IndianSettlement camp2 = builder.reset().build();
        
        //////////////////////
        // Simulate that only the center tile is owned by camp 1
        // Does not matter where camp 2 is, so we put it in the same tile as camp1
        int overlappingTiles = 8; // all the tiles around the camp
        setOverlapingCamps(game, camp1, camp2, overlappingTiles);
        
        //verify initial conditions
        assertEquals(initialBravesInCamp, camp1.getUnitCount());
        assertEquals(0, camp1.getFoodCount());
        
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
	
	public void testEquipBraves(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType musketsType = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        
		Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(3).build();
        
        int bravesToEquip = camp.getUnitCount();
        int horsesReqPerUnit = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.horses").getAmountRequiredOf(horsesType);
        int musketsReqPerUnit = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.muskets").getAmountRequiredOf(musketsType);        
        int totalHorsesReq = bravesToEquip * horsesReqPerUnit;
        int totalMusketsReq = bravesToEquip * musketsReqPerUnit;        
        int totalHorsesAvail = totalHorsesReq*2;
        int totalMusketsAvail = totalMusketsReq*2;
        
        // Verify initial conditions
        assertEquals("No horses should exist in camp",0,camp.getGoodsCount(horsesType));
        assertEquals("No muskets should exist in camp",0,camp.getGoodsCount(musketsType));

        for(Unit unit : camp.getUnitList()){
        	if(unit.isMounted()){
        		fail("Indian should not have mounted braves");
        	}
        	if(unit.isArmed()){
        		fail("Indian should not have armed braves");
        	}
        }
        
        // Setup
        camp.addGoods(horsesType,totalHorsesAvail);
        camp.addGoods(musketsType,totalMusketsAvail);
     
        assertEquals("Wrong initial number of horses in Indian camp",totalHorsesAvail,camp.getGoodsCount(horsesType));
        assertEquals("Wrong initial number of muskets in Indian camp",totalMusketsAvail,camp.getGoodsCount(musketsType));
        
        // Exercise SUT
        camp.equipBraves();
        
        // Verify results
        assertEquals("Wrong final number of horses in Indian camp",totalHorsesReq,camp.getGoodsCount(horsesType));
        assertEquals("Wrong final number of muskets in Indian camp",totalMusketsReq,camp.getGoodsCount(musketsType));
        
        int mounted = 0;
        int armed = 0;
        for(Unit unit : camp.getUnitList()){
        	if(unit.isMounted()){ 
        		mounted++;
        	}
        	if(unit.isArmed()){
        		armed++;
        	}
        }
        assertEquals("Wrong number of units armed",camp.getUnitCount(),armed);
        assertEquals("Wrong number of units mounted",camp.getUnitCount(),mounted);
	}
	
	public void testEquipBravesNotEnoughReqGoods(){
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType musketsType = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        
		Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(3).build();
        
        int bravesToEquip = camp.getUnitCount() - 1;
        int horsesReqPerUnit = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.horses").getAmountRequiredOf(horsesType);
        int musketsReqPerUnit = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.muskets").getAmountRequiredOf(musketsType);        
        int totalHorsesAvail = bravesToEquip * horsesReqPerUnit;
        int totalMusketsAvail = bravesToEquip * musketsReqPerUnit;        
        
        // Verify initial conditions
        assertEquals("No horses should exist in camp",0,camp.getGoodsCount(horsesType));
        assertEquals("No muskets should exist in camp",0,camp.getGoodsCount(musketsType));

        for(Unit unit : camp.getUnitList()){
        	if(unit.isMounted()){
        		fail("Indian should not have mounted braves");
        	}
        	if(unit.isArmed()){
        		fail("Indian should not have armed braves");
        	}
        }
        
        // Setup
        camp.addGoods(horsesType,totalHorsesAvail);
        camp.addGoods(musketsType,totalMusketsAvail);
     
        assertEquals("Wrong initial number of horses in Indian camp",totalHorsesAvail,camp.getGoodsCount(horsesType));
        assertEquals("Wrong initial number of muskets in Indian camp",totalMusketsAvail,camp.getGoodsCount(musketsType));
        
        // Exercise SUT
        camp.equipBraves();
        
        // Verify results
        assertEquals("Wrong final number of horses in Indian camp",0,camp.getGoodsCount(horsesType));
        assertEquals("Wrong final number of muskets in Indian camp",0,camp.getGoodsCount(musketsType));
        
        int mounted = 0;
        int armed = 0;
        for(Unit unit : camp.getUnitList()){
        	if(unit.isMounted()){ 
        		mounted++;
        	}
        	if(unit.isArmed()){
        		armed++;
        	}
        }
        assertEquals("Wrong number of units armed",bravesToEquip,armed);
        assertEquals("Wrong number of units mounted",bravesToEquip,mounted);
	}
	
	public void testWarDeclarationAffectsSettlementAlarm(){
	    Game game = getStandardGame();
	    Map map = getTestMap();
	    game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");
	    inca.setContacted(dutch, true);
        
	    FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
	    IndianSettlement camp = builder.player(inca).build();
	    camp.setVisited(dutch);
	    
	    assertEquals("Inca should be at peace with dutch", Stance.PEACE, inca.getStance(dutch));
	    Tension campAlarm = camp.getAlarm(dutch);
	    assertNotNull("Camp should have had contact with dutch",campAlarm);
	    assertEquals("Camp should be hateful", Tension.Level.HAPPY, campAlarm.getLevel());

	    dutch.changeRelationWithPlayer(inca, Stance.WAR);
	    
	    assertEquals("Inca should be at war with dutch", Stance.WAR, inca.getStance(dutch));
	    campAlarm = camp.getAlarm(dutch);
	    assertEquals("Camp should be hateful", Tension.Level.HATEFUL, campAlarm.getLevel());
	}
}