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

import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.util.test.FreeColTestCase;

public class TileItemContainerTest extends FreeColTestCase {

    private TileItemContainer getSample(Game game, Tile t, 
    				boolean addImprovements, 
    				boolean addResources,
    				boolean addRumours){
    	final TileImprovementType riverImprov =
            spec().getTileImprovementType("model.improvement.river");
    	final TileImprovementType roadImprov =
            spec().getTileImprovementType("model.improvement.road");
    	final ResourceType oreRsc = spec().getResourceType("model.resource.ore"); 
    	
    	TileItemContainer cont = new TileItemContainer(game,t);
    	
    	if(addImprovements){
    		TileImprovement river = new TileImprovement(game, t, riverImprov);
    		TileImprovement road = new TileImprovement(game, t, roadImprov);
    		cont.addTileItem(river);
    		cont.addTileItem(road);
    	}
    	
    	if(addResources){
    		Resource ore = new Resource(game,t,oreRsc);
    		cont.addTileItem(ore);
    	}
    	
    	if(addRumours){
    		LostCityRumour rumour = new LostCityRumour(game, t, RumourType.FOUNTAIN_OF_YOUTH, "fountain"); 
    		cont.addTileItem(rumour);
    	}
    	t.setTileItemContainer(cont);
    	
    	return cont;
    }


    public void testCopyFromWithEveryThing() {
    	final TileType desert = spec().getTileType("model.tile.desert");
    	final TileType plains = spec().getTileType("model.tile.plains");
    	
        Game game = getStandardGame();

    	Tile tOriginal = new Tile(game,plains,8,8);
    	Tile tCopy = new Tile(game,desert,8,9);
        TileItemContainer original = getSample(game,tOriginal,true,true,true);
        TileItemContainer copy = getSample(game,tCopy,false,false,false);
        
        assertTrue("Setup error, original must have road",original.getRoad()!=null);
        assertFalse("Setup error, copy cannot have road",copy.getRoad()!=null);
        assertTrue("Setup error, original must have river",original.getRiver()!=null);
        assertFalse("Setup error, copy cannot have river",copy.getRiver()!=null);
        assertTrue("Setup error, original must have resource",original.getResource()!=null);
        assertFalse("Setup error, copy cannot have resource",copy.getResource()!=null);
        assertTrue("Setup error, original must have rumour",original.getLostCityRumour()!=null);
        assertFalse("Setup error, copy cannot have rumour",copy.getLostCityRumour()!=null);
        
        copy.copyFrom(original, true, false);
        
        assertTrue("Copy should have road",copy.getRoad()!=null);
        assertTrue("Copy should have river",copy.getRiver()!=null);
        assertTrue("Copy should have resource",copy.getResource()!=null);
        assertTrue("Copy should have rumour",copy.getLostCityRumour()!=null);
        
        // Copy only natural
        copy.copyFrom(original, true, true);
        
        assertFalse("Copy should not have road",copy.getRoad()!=null);
        assertTrue("Copy should have river",copy.getRiver()!=null);
        assertTrue("Copy should have resource",copy.getResource()!=null);
        assertFalse("Copy should not have rumour",copy.getLostCityRumour()!=null);
    }
}
