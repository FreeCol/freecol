package net.sf.freecol.common.model;
import java.util.*;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;

public class RevealMapTest extends FreeColTestCase 
{
	public void testRevealMap()
	{
		Game game = getStandardGame();
		Nation dutchNation = spec().getNation("model.nation.dutch");
		Player player = new ServerPlayer(game, false, dutchNation);
		Map map = getTestMap();
		player.revealMap(player, map);
		for (int i = 0; i < map.getWidth(); i++ )
		{
			for (int j = 0; j < map.getHeight(); j++)
			{
				assertTrue(map.getTile(i,j).isExplored());
			}
		}
	}
}
