package net.sf.freecol.common.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.util.test.FreeColTestCase;

public class IndianSettlementTest extends FreeColTestCase {
    private static GoodsType musketsType = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
    private static GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
    
    /**
     * Changes the ownership of a number of tiles from and around camp1 to camp2.
     * The param 'nTiles' defines the number of tiles to change ownership of.
     * @param game
     * @param camp1
     * @param camp2
     * @param nTiles
     */
    private void setOverlapingCamps(Game game, IndianSettlement camp1, IndianSettlement camp2, int nTiles){
        Tile settlementTile = camp1.getTile();
        
        // Change tile ownership around camp1 to camp2
        for (Tile t: settlementTile.getSurroundingTiles(camp1.getRadius())){
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

    public void testAutomaticEquipBraves(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(1).build();

        Unit indianBrave = camp.getFirstUnit();
	    
        String errMsg = "Unit should not be able to automatically equip, no muskets available";
        assertTrue(errMsg, indianBrave.getAutomaticEquipment() == null);
	    
        camp.addGoods(musketsType, 100);
	    
        errMsg = "Unit should be able to automatically equip, camp has muskets available";
        assertFalse(errMsg, indianBrave.getAutomaticEquipment() == null);
    }
	   
    public void testWarDeclarationAffectsSettlementAlarm(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");
        Player.makeContact(inca, dutch);
        
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
        
        camp1.tradeGoodsWithSetlement(camp2);
        
        assertEquals(wrongQtyMusketsMsg,enoughToShare / 2,camp1.getGoodsCount(musketsType));
        assertEquals(wrongQtyHorsesMsg,notEnoughToShare,camp1.getGoodsCount(horsesType));
        assertEquals(wrongQtyMusketsMsg,enoughToShare / 2,camp2.getGoodsCount(musketsType));
        assertEquals(wrongQtyHorsesMsg,none,camp2.getGoodsCount(horsesType));
    }
}