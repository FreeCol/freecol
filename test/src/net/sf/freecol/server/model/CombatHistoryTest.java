/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

package net.sf.freecol.server.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.CombatModel.CombatEffectType;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.util.test.FreeColTestCase;


public class CombatHistoryTest extends FreeColTestCase {

    public void testRecordsOddsAndRoll() throws Exception {
        Game game = getStandardGame();
        net.sf.freecol.common.model.Map map
            = getTestMap(spec().getTileType("model.tile.plains"));
        game.changeMap(map);
        CombatModel cm = game.getCombatModel();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        Tile t1 = map.getTile(5, 8);
        Tile t2 = map.getTile(4, 8);
        Role soldier = spec().getRole("model.role.soldier");
        UnitType vet = spec().getUnitType("model.unit.veteranSoldier");
        Unit defender = new ServerUnit(game, t1, dutch, vet, soldier);
        Unit attacker = new ServerUnit(game, t2, french, vet, soldier);

        File file = File.createTempFile("combat-history", ".csv");
        file.delete();
        try {
            for (int i = 0; i < 3; i++) {
                CombatResult cr = cm.generateAttackResult(new Random(i),
                                                          attacker, defender);
                CombatHistory.write(file, "TestModel", 7, attacker, defender,
                                    cr, cr.getEffects());
            }
            List<String> lines = Files.readAllLines(file.toPath(),
                                                    StandardCharsets.UTF_8);
            assertEquals("header + 3 rows", 4, lines.size());
            assertTrue(lines.get(0).startsWith("time,turn,model"));
            for (String line : lines.subList(1, 4)) {
                String[] f = line.split(",");
                assertEquals(10, f.length);
                assertEquals("7", f[1]);
                assertEquals("attack", f[3]);
                double odds = Double.parseDouble(f[6]);
                double roll = Double.parseDouble(f[7]);
                assertEquals("WIN iff roll < odds",
                             roll < odds, "WIN".equals(f[8]));
            }
            // Appending must not duplicate the header.
            CombatResult cr = cm.generateAttackResult(new Random(9),
                                                      attacker, defender);
            CombatHistory.write(file, "TestModel", 8, attacker, defender,
                                cr, cr.getEffects());
            assertEquals(5, Files.readAllLines(file.toPath()).size());
        } finally {
            file.delete();
        }
    }

    public void testIgnoresResultWithoutRoll() throws Exception {
        File file = File.createTempFile("combat-history", ".csv");
        file.delete();
        CombatResult forced = new CombatResult(
            java.util.Arrays.asList(CombatEffectType.WIN));
        CombatHistory.write(file, "TestModel", 1, null, null, forced,
                            forced.getEffects());
        assertFalse("forced results are not recorded", file.exists());
    }
}
