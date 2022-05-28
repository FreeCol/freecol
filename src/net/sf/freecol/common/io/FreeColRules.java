/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.common.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FreeColRules {

    private static final Logger logger = Logger.getLogger(FreeColRules.class.getName());
    
    /** A cache of all the rules. */
    private static final Map<String, FreeColModFile> allRules = new HashMap<>();
    
    
    /**
     * Get all the standard rule sets.
     *
     * @return A list of {@code FreeColModFile}s holding the rule sets.
     */
    public static List<FreeColModFile> getRulesList() {
        List<FreeColModFile> ret = new ArrayList<>();
        for (File f : FreeColDirectories.getRulesFileList()) {
            try {
                ret.add(new FreeColModFile(f));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to load TC from: " + f, ioe);
            }
        }
        return ret;
    }


    // Cache manipulation

    /**
     * Require all rules to be loaded.
     */
    public static void loadRules() {
        if (allRules.isEmpty()) {
            for (FreeColModFile fctf : getRulesList()) {
                allRules.put(fctf.getId(), fctf);
            }
        }
    }
    
    /**
     * Get a rules file by ID.
     *
     * @param id The rules file identifier to look for.
     * @return The {@code FreeColModFile} found, or null if none present.
     */
    public static FreeColModFile getFreeColRulesFile(String id) {
        return allRules.get(id);
    }
}
