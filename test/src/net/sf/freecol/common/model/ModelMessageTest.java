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

import java.util.HashSet;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.util.test.FreeColTestCase;


public class ModelMessageTest extends FreeColTestCase {

    public void testHashCode() {
        Game game = getGame();
        game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony(1);
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        
        ModelMessage mm1 = new ModelMessage(MessageType.MISSING_GOODS,
                                            "buildColony.landLocked",
                                            colony, cotton);
        ModelMessage mm2 = new ModelMessage(MessageType.MISSING_GOODS,
                                            "buildColony.landLocked",
                                            colony, cotton);
        assertEquals(mm1, mm2);
        assertEquals(mm1.hashCode(), mm2.hashCode());
        
        ModelMessage mm3 = new ModelMessage(MessageType.MISSING_GOODS,
                                            "buildColony.landLocked",
                                            colony, cotton);
        ModelMessage mm4 = new ModelMessage(MessageType.MISSING_GOODS,
                                            "buildColony.landLocked",
                                            colony, cotton);
        assertNotSame(mm3, mm4);
        assertNotSame(mm3.hashCode(), mm4.hashCode());
    }
    
    public void testModelMapSet2() {
        Game game = getGame();
        game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony(1);
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        
        ModelMessage mm1 = new ModelMessage(MessageType.WAREHOUSE_CAPACITY,
                                            "model.building.warehouseSoonFull",
                                            colony, cotton)
            .addNamed("%goods%", cotton)
            .addName("%colony%", colony.getName())
            .addAmount("%amount%", 10);
                                    
 
        ModelMessage mm2 = new ModelMessage(MessageType.WAREHOUSE_CAPACITY,
                                            "model.building.warehouseSoonFull",
                                            colony, cotton)
            .addNamed("%goods%", cotton)
            .addName("%colony%", colony.getName())
            .addAmount("%amount%", 10);

        HashSet<ModelMessage> set = new HashSet<ModelMessage>();
        assertEquals(mm1, mm2);
        assertEquals(mm1.hashCode(), mm2.hashCode());
        set.add(mm1);
        assertTrue(set.remove(mm2));
    }

    public void testDefaultId() {
        Game game = getGame();
        game.setMap(getTestMap(true));
    	
        Player player = game.getPlayerByNationId("model.nation.dutch");
        String realMessageId = "player"; // Must exist
        String fakeMessageId = "no.such.messageId"; // Must no exist
        ModelMessage mm1 = new ModelMessage(MessageType.WAREHOUSE_CAPACITY,
                                            fakeMessageId, realMessageId,
                                            player, null);
        assertEquals(Messages.message(realMessageId), Messages.message(mm1));
    }
}
