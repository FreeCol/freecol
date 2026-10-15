/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.CombatModel.CombatEffectType;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;


/**
 * Persistent, append-only record of every combat whose result was drawn
 * from the random number generator, so that the stated win probability
 * can be compared with what actually happened over many games.
 *
 * The file is {@code combat-history.csv} in the user data directory.
 * It is never truncated.  Write failures are logged and otherwise ignored.
 */
public final class CombatHistory {

    private static final Logger logger = Logger.getLogger(CombatHistory.class.getName());

    public static final String FILE_NAME = "combat-history.csv";

    private static final String HEADER
        = "time,turn,model,action,attacker,defender,odds,roll,result,effects\n";

    private static boolean failureLogged = false;


    private CombatHistory() {}

    /**
     * Append one combat to the history.
     *
     * @param modelName The name of the combat model used.
     * @param turn The turn number.
     * @param attacker The attacker.
     * @param defender The defender.
     * @param result The {@code CombatResult} (with odds and roll set).
     * @param effects The effects, the first being WIN/LOSE/NO_RESULT.
     */
    public static synchronized void record(String modelName, int turn,
                                           FreeColGameObject attacker,
                                           FreeColGameObject defender,
                                           CombatResult result,
                                           List<CombatEffectType> effects) {
        final File dir = FreeColDirectories.getUserDataDirectory();
        if (dir == null) return;
        write(new File(dir, FILE_NAME), modelName, turn, attacker, defender,
              result, effects);
    }

    /**
     * Append one combat to a given file.  Split out for testing.
     */
    static synchronized void write(File file, String modelName, int turn,
                                   FreeColGameObject attacker,
                                   FreeColGameObject defender,
                                   CombatResult result,
                                   List<CombatEffectType> effects) {
        if (Double.isNaN(result.getRoll()) || effects.isEmpty()) return;
        try {
            StringBuilder sb = new StringBuilder(256);
            if (!file.exists()) sb.append(HEADER);
            sb.append(Instant.now()).append(',')
              .append(turn).append(',')
              .append(modelName).append(',')
              .append((attacker instanceof Unit) ? "attack" : "bombard").append(',')
              .append(describe(attacker)).append(',')
              .append(describe(defender)).append(',')
              .append(result.getOdds()).append(',')
              .append(result.getRoll()).append(',')
              .append(effects.get(0)).append(',')
              .append(effects.stream().map(Enum::name)
                  .collect(Collectors.joining(";")))
              .append('\n');
            Files.write(file.toPath(),
                        sb.toString().getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException|RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                logger.log(Level.WARNING, "Could not write " + file, e);
            }
        }
    }

    /** Describe a combatant as nation/type/role, free of commas. */
    private static String describe(FreeColGameObject fcgo) {
        if (fcgo instanceof Unit) {
            Unit u = (Unit) fcgo;
            return u.getOwner().getNationId() + "/" + u.getType().getSuffix()
                + "/" + u.getRole().getSuffix();
        } else if (fcgo instanceof Settlement) {
            Settlement s = (Settlement) fcgo;
            return s.getOwner().getNationId() + "/settlement";
        }
        return String.valueOf(fcgo);
    }
}
