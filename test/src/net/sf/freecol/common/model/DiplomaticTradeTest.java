/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.util.test.FreeColTestCase;

public class DiplomaticTradeTest extends FreeColTestCase {

	private void setPlayersAt(Stance stance,Tension tension){
		Game game = getGame();
		
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        
        // Setup
        dutch.setStance(french, stance);
        dutch.setTension(french, new Tension(tension.getValue()));
        french.setStance(dutch, stance);
        french.setTension(dutch, new Tension(tension.getValue()));
        
        // Verify initial conditions
        Tension.Level expectedTension = tension.getLevel();

        assertEquals("Wrong Dutch player stance with french player",dutch.getStance(french),stance);
        assertEquals("Wrong French player stance with dutch player",french.getStance(dutch),stance);
        assertEquals("Tension of dutch player towards french player wrong",expectedTension,dutch.getTension(french).getLevel());
        assertEquals("Tension of french player towards dutch player wrong",expectedTension,french.getTension(dutch).getLevel());
	}
	
	/**
	 * Verifies conditions of treaty regarding stance and tension of player1 toward player2
	 */
	private void verifyTreatyResults(Player player1, Player player2, Stance expectedStance, int expectedTension){
		        
        assertFalse(player1 + " player should not be at war",player1.isAtWar());
        assertEquals(player1 + " player should be at peace with " + player2 + " player",
                     player1.getStance(player2),expectedStance);
        
        int player1CurrTension = player1.getTension(player2).getValue();
                
        assertEquals(player1 + " player tension values wrong",expectedTension,player1CurrTension);
	}
	
	/**
	 * Tests the implementation of an accepted peace treaty while at war
	 */
    public void testPeaceTreatyFromWarStance() {
    	Game game = getStandardGame();
    	
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Tension hateful = new Tension(Tension.Level.HATEFUL.getLimit());
        Stance initialStance = Stance.WAR; 
        Stance newStance =  Stance.PEACE;
        
    	//setup
    	setPlayersAt(initialStance,hateful);
    	
        int dutchInitialTension = dutch.getTension(french).getValue();
        int frenchInitialTension = french.getTension(dutch).getValue();
        StanceTradeItem peaceTreaty = new StanceTradeItem(game, dutch, french, newStance);
                
        // Execute peace treaty
        peaceTreaty.makeTrade();
        
        // Verify results        
        int dutchExpectedTension = Math.max(0,dutchInitialTension + Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER);
        int frenchExpectedTension = Math.max(0,frenchInitialTension + Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER);
        
        verifyTreatyResults(dutch, french, newStance, dutchExpectedTension);
        verifyTreatyResults(french, dutch, newStance, frenchExpectedTension);
    }
    
	/**
	 * Tests the implementation of an accepted peace treaty while at cease-fire
	 */
    public void testPeaceTreatyFromCeaseFireStance() {
    	Game game = getStandardGame();
    	
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Tension hateful = new Tension(Tension.Level.HATEFUL.getLimit());
        Stance initialStance = Stance.CEASE_FIRE; 
        Stance newStance =  Stance.PEACE;
        
    	//setup
        //Note: the game only allows setting cease fire stance from war stance
        setPlayersAt(Stance.WAR,hateful);
    	setPlayersAt(initialStance,hateful);
    	
        int dutchInitialTension = dutch.getTension(french).getValue();
        int frenchInitialTension = french.getTension(dutch).getValue();
        StanceTradeItem peaceTreaty = new StanceTradeItem(game, dutch, french, newStance);
                
        // Execute peace treaty
        peaceTreaty.makeTrade();
        
        // Verify results
        int dutchExpectedTension = Math.max(0,dutchInitialTension + Tension.PEACE_TREATY_MODIFIER);
        int frenchExpectedTension = Math.max(0,frenchInitialTension + Tension.PEACE_TREATY_MODIFIER);
        
        verifyTreatyResults(dutch, french, newStance, dutchExpectedTension);
        verifyTreatyResults(french, dutch, newStance, frenchExpectedTension);
    }
    
	/**
	 * Tests the implementation of an accepted cease fire treaty
	 */
    public void testCeaseFireTreaty() {
    	Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Tension hateful = new Tension(Tension.Level.HATEFUL.getLimit());
        Stance initialStance = Stance.WAR; 
        Stance newStance =  Stance.CEASE_FIRE;
        
        //setup
    	setPlayersAt(initialStance,hateful);
    	
        int dutchInitialTension = dutch.getTension(french).getValue();
        int frenchInitialTension = french.getTension(dutch).getValue();
        StanceTradeItem ceaseFire = new StanceTradeItem(game, dutch, french, newStance);
        
        // Execute cease-fire treaty
        ceaseFire.makeTrade();
        
        // Verify results
        int dutchExpectedTension = Math.max(0,dutchInitialTension + Tension.CEASE_FIRE_MODIFIER);
        int frenchExpectedTension = Math.max(0,frenchInitialTension + Tension.CEASE_FIRE_MODIFIER);
        
        verifyTreatyResults(dutch, french, newStance, dutchExpectedTension);
        verifyTreatyResults(french, dutch, newStance, frenchExpectedTension);
    }
}
