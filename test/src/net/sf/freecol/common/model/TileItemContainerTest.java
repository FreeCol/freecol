/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

    private static final TileImprovementType riverImprov
        = spec().getTileImprovementType("model.improvement.river");
    private static final TileImprovementType roadImprov
        = spec().getTileImprovementType("model.improvement.road");
    private static final ResourceType oreRsc = spec().getResourceType("model.resource.ore");


    private TileItemContainer getSample(Game game, Tile t, 
                                        boolean addImprovements, 
                                        boolean addResources,
                                        boolean addRumours) {
        if (addImprovements) {
            t.addRiver(1, "0101");
            TileImprovement road = t.addRoad();
            road.setTurnsToComplete(0);
        }
    	
        if (addResources) {
            t.addResource(new Resource(game, t, oreRsc));
        }
    	
        if (addRumours) {
            t.add(new LostCityRumour(game, t, RumourType.FOUNTAIN_OF_YOUTH, "fountain"));
        }
    	
        if (t.getTileItemContainer() == null) {
            t.setTileItemContainer(new TileItemContainer(game, t));
        }
        return t.getTileItemContainer();
    }


    public void testCopyFromWithEveryThing() {
        final TileType desert = spec().getTileType("model.tile.desert");
        final TileType plains = spec().getTileType("model.tile.plains");
    	
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Tile tOriginal = map.getTile(8, 8);
        tOriginal.setType(plains);
        Tile tCopy = map.getTile(8, 9);
        tCopy.setType(desert);
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
