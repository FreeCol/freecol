package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.*;
import java.util.*;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;

public class HighScoreTest extends FreeColTestCase {

	public void testAddHighScore() throws NoSuchFieldException, SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
		Game game = getStandardGame();
		assertNotNull(game.getUUID());
		Nation dutchNation = spec().getNation("model.nation.dutch");
		Player player = new ServerPlayer(game, false, dutchNation);
		
		HighScore hs = new HighScore();
		Class hsclass = hs.getClass();
        Field field = hsclass.getDeclaredField("gameID");
        field.setAccessible(true);
        field.set(hs, UUID.randomUUID());

        List<HighScore> list = new ArrayList<>();
        assertEquals(true, HighScore.checkHighScore(hs, list));
        list.add(hs);
        assertEquals(false, HighScore.checkHighScore(hs, list));
		
	}

}
