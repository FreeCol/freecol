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

package net.sf.freecol.util.test;

import java.util.ArrayList;
import java.util.Random;

import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;

public class MockModelController implements ModelController {
    private PseudoRandom setPseudoRandom = null;
    
    public Unit createUnit(String taskID, Location location, Player owner, int type) {
        // TODO Auto-generated method stub
        return null;
    }

    public void exploreTiles(Player player, ArrayList<Tile> tiles) {
        // TODO Auto-generated method stub
		
    }

    public TradeRoute getNewTradeRoute(Player player) {
        // TODO Auto-generated method stub
        return null;
    }

    public PseudoRandom getPseudoRandom() {
        if(setPseudoRandom != null){
            return setPseudoRandom;
        }
        
        return new PseudoRandom(){

            Random r = new Random(0);
			
            public int nextInt(int n) {
                return r.nextInt(n);
            }
			
        };
    }
    
    public void setPseudoRandom(PseudoRandom newPseudoRandom){
        setPseudoRandom = newPseudoRandom;
    }

    public int getRandom(String taskID, int n) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setStance(Player first, Player second, Stance stance) {
        // TODO Auto-generated method stub
		
    }

    public Location setToVacantEntryLocation(Unit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * All objects receive a new turn. 
     */
    public boolean shouldCallNewTurn(FreeColGameObject freeColGameObject) {
        return true;
    }

    public Unit createUnit(String taskID, Location location, Player owner,
                           UnitType type) {
        // TODO Auto-generated method stub
        return null;
    }

    public Building createBuilding(String taskID, Colony colony, BuildingType type) {
        // TODO Auto-generated method stub
        return new Building(colony.getGame(), colony, type);
    }

    public void tileImprovementFinished(Unit unit, TileImprovement improvement) {
        // TODO Auto-generated method stub
    }

}
