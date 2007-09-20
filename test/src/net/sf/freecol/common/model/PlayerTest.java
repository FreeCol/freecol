package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class PlayerTest extends FreeColTestCase {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public void testGetREF(){
    	
    	Game g = getStandardGame();
    	
    	// Every european non ref player should have a REF player. 
    	for (Player p : g.getPlayers()){
    		assertEquals(p.isEuropean() && !p.isREF(), p.getREFPlayer() != null);
    	}
    }
    
}
