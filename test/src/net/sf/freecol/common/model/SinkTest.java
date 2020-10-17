package net.sf.freecol.common.model;


import java.util.List;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class SinkTest extends FreeColTestCase {
	private static final UnitType galleonType
    = spec().getUnitType("model.unit.galleon");
	
    private static final TileType ocean
    = spec().getTileType("model.tile.ocean");
    
    private static final UnitType privateerType
    = spec().getUnitType("model.unit.privateer");
	public void testAddSink() {
		final Game game =  ServerTestHelper.startServerGame(getTestMap(ocean));
        final Map map = game.getMap();
        final InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer french = getServerPlayer(game, "model.nation.french");
        igc.changeStance(french, Stance.WAR, dutch, true);
        assertEquals("Dutch should be at war with french",
                     dutch.getStance(french), Stance.WAR);
        assertEquals("French should be at war with dutch",
                     french.getStance(dutch), Stance.WAR);

        Tile tile1 = map.getTile(5, 8);
        tile1.setExplored(dutch, true);
        tile1.setExplored(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExplored(dutch, true);
        tile2.setExplored(french, true);


        Unit galleon = new ServerUnit(game, tile1, dutch, galleonType);
        Unit privateer = new ServerUnit(game, tile2, french, privateerType);
        
        dutch.getEurope();
        assertEquals("Galleon repair location is Europe",
                dutch.getEurope(), galleon.getRepairLocation());
        
        List<CombatResult> crs
        = fakeAttackResult(CombatResult.WIN, privateer, galleon);
        checkCombat("Privateer v galleon", crs,
                CombatResult.WIN,
                CombatResult.DAMAGE_SHIP_ATTACK);
        igc.combat(dutch, privateer, galleon, crs);
    
        assertTrue("Galleon should be in Europe repairing",
            galleon.isDamaged());
    
        assertEquals(3, galleon.RepairNum);
	}
}
