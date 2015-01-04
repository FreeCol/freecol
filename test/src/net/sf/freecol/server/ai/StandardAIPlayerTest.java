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

package net.sf.freecol.server.ai;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;


public class StandardAIPlayerTest extends FreeColTestCase {

    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final Role nativeDragoonRole
        = spec().getRole("model.role.nativeDragoon");

    private int horsesReqPerUnit = 0, musketsReqPerUnit = 0;

    private LogBuilder lb = new LogBuilder(0); // dummy


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }

    private void setupNativeDragoons() {
        for (AbstractGoods ag : nativeDragoonRole.getRequiredGoods()) {
            if (ag.getType() == horsesType) {
                horsesReqPerUnit = ag.getAmount();
            } else if (ag.getType() == musketsType) {
                musketsReqPerUnit = ag.getAmount();
            }
        }
        assertFalse(horsesReqPerUnit == 0);
        assertFalse(musketsReqPerUnit == 0);
    }

    public void testEquipBraves() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(3).build();
        NativeAIPlayer player = (NativeAIPlayer) aiMain.getAIPlayer(camp.getOwner());
        game.setCurrentPlayer(camp.getOwner());

        setupNativeDragoons();
        int bravesToEquip = camp.getUnitCount();
        int totalHorsesReq = bravesToEquip * horsesReqPerUnit;
        int totalMusketsReq = bravesToEquip * musketsReqPerUnit;
        int totalHorsesAvail = totalHorsesReq*2;
        int totalMusketsAvail = totalMusketsReq*2;

        // Verify initial conditions
        assertEquals("No horses should exist in camp", 0,
            camp.getGoodsCount(horsesType));
        assertEquals("No muskets should exist in camp", 0,
            camp.getGoodsCount(musketsType));

        for (Unit unit : camp.getUnitList()) {
            assertFalse("Indian should not have mounted braves",
                unit.isMounted());
            assertFalse("Indian should not have armed braves",
                unit.isArmed());
        }

        // Setup
        camp.addGoods(horsesType, totalHorsesAvail);
        camp.addGoods(musketsType, totalMusketsAvail);

        assertEquals("Wrong initial number of horses in Indian camp",
            totalHorsesAvail, camp.getGoodsCount(horsesType));
        assertEquals("Wrong initial number of muskets in Indian camp",
            totalMusketsAvail, camp.getGoodsCount(musketsType));

        player.equipBraves(camp, lb);

        // Verify results
        int mounted = 0;
        int armed = 0;
        for (Unit unit : camp.getUnitList()) {
            if (unit.isMounted()) mounted++;
            if (unit.isArmed()) armed++;
        }
        assertEquals("Wrong number of units armed", bravesToEquip, armed);
        assertEquals("Wrong number of units mounted", bravesToEquip, mounted);
        assertEquals("Wrong final number of muskets in Indian camp",
            totalMusketsReq, camp.getGoodsCount(musketsType));
        assertEquals("Wrong final number of horses in Indian camp",
            totalHorsesReq, camp.getGoodsCount(horsesType));
    }

    public void testEquipBravesNotEnoughReqGoods() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(3).build();
        NativeAIPlayer player = (NativeAIPlayer) aiMain.getAIPlayer(camp.getOwner());
        game.setCurrentPlayer(camp.getOwner());

        setupNativeDragoons();
        int bravesToEquip = camp.getUnitCount() - 1;
        int totalHorsesAvail = bravesToEquip * horsesReqPerUnit
            + horsesType.getBreedingNumber();
        int totalMusketsAvail = bravesToEquip * musketsReqPerUnit;

        // Verify initial conditions
        assertEquals("No horses should exist in camp", 0,
            camp.getGoodsCount(horsesType));
        assertEquals("No muskets should exist in camp", 0,
            camp.getGoodsCount(musketsType));

        for (Unit unit : camp.getUnitList()) {
            if (unit.isMounted()) {
                fail("Indian should not have mounted braves");
            }
            if (unit.isArmed()) {
                fail("Indian should not have armed braves");
            }
        }

        // Setup
        camp.addGoods(horsesType, totalHorsesAvail);
        camp.addGoods(musketsType, totalMusketsAvail);

        assertEquals("Wrong initial number of horses in Indian camp",
            totalHorsesAvail, camp.getGoodsCount(horsesType));
        assertEquals("Wrong initial number of muskets in Indian camp",
            totalMusketsAvail, camp.getGoodsCount(musketsType));

        player.equipBraves(camp, lb);

        // Verify results
        int mounted = 0;
        int armed = 0;
        for (Unit unit : camp.getUnitList()) {
            if (unit.isMounted()) mounted++;
            if (unit.isArmed()) armed++;
        }
        assertEquals("Wrong number of units armed", bravesToEquip, armed);
        assertEquals("Wrong number of units mounted", bravesToEquip, mounted);
        assertEquals("Wrong final number of muskets in Indian camp",
            0, camp.getGoodsCount(musketsType));
        assertEquals("Wrong final number of horses in Indian camp",
            horsesType.getBreedingNumber(), camp.getGoodsCount(horsesType));
    }
}
