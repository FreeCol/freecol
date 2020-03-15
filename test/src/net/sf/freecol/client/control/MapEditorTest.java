package net.sf.freecol.client.control;


import net.sf.freecol.client.ClientTestHelper;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class MapEditorTest extends FreeColTestCase {

    public static final String TEST_MAP_LOC = "test/data/test_map.fsm";
    private static final TileType plains
            = spec().getTileType("model.tile.plains");

    public void testSaveMap() throws InterruptedException {

        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        FreeColClient client = ClientTestHelper.startClient(ServerTestHelper.getServer(), spec());
        client.setGame(game);

        File newMap = new File(TEST_MAP_LOC);
        client.getMapEditorController().saveMapEditorGame(newMap);
        //Note: Instead of this, would be best to refactor saveMapEditorGame at some point
        Thread.sleep(1000);
        assertTrue(newMap.exists());
        newMap.delete();
    }

}
