/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model.mission;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * The MissionManager keeps track of all missions defined.
 *
 * @see net.sf.freecol.client.gui.action.ActionManager
 */
public class MissionManager {

    private static final Logger logger = Logger.getLogger(MissionManager.class.getName());

    private static Map<String, Class<? extends AbstractMission>> missionMap =
        new HashMap<String, Class<? extends AbstractMission>>();

    static {
        missionMap.put(CompoundMission.getXMLElementTagName(),
                       CompoundMission.class);
        missionMap.put(GoToMission.getXMLElementTagName(),
                       GoToMission.class);
        missionMap.put(ImprovementMission.getXMLElementTagName(),
                       ImprovementMission.class);
    }


    /**
     * Returns true if the given String is a known mission tag.
     *
     * @param tag a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isMissionTag(String tag) {
        return missionMap.containsKey(tag);
    }

    /**
     * Returns the class associated with the given string, or null if
     * none is.
     *
     * @param tag the tag name of an AbstractMission
     * @return a <code>Class</code> value
     */
    public static Class<? extends AbstractMission> getMission(String tag) {
        return missionMap.get(tag);
    }


}