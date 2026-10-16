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

package net.sf.freecol.common.logging;


/**
 * The registry of field and category names used with {@link WideEvent}.
 *
 * A wide event's keys are plain strings with no compiler-enforced
 * schema, so as more call sites adopt the pattern it becomes easy for
 * the same concept to drift into several spellings (e.g. {@code unit}
 * vs {@code unitId}).  Declaring every key once here, and having call
 * sites reference the constant rather than a literal, keeps the keys
 * consistent and gives one place to see the full vocabulary in use.
 *
 * Add to this list when introducing a new wide event boundary or note
 * category; do not invent a new literal at the call site.
 */
public final class WideEventFields {

    // Fields common to more than one event.
    public static final String UNIT = "unit";
    public static final String UNIT_TYPE = "unitType";
    public static final String RESULT = "result";
    public static final String DETAIL = "detail";

    // ai.european.turn
    public static final String PLAYER = "player";
    public static final String TURN = "turn";
    public static final String UNITS = "units";
    public static final String COLONIES = "colonies";
    public static final String DECLARE_INDEPENDENCE = "declareIndependence";
    public static final String LAND_REF_RATIO = "landRefRatio";
    public static final String NAVAL_REF_RATIO = "navalRefRatio";
    public static final String BADLY_DEFENDED_COLONIES = "badlyDefendedColonies";

    // ai.european.turn note categories
    public static final String NOTE_MISSION_COLLAPSE = "missionCollapse";
    public static final String NOTE_NO_TIP_TARGET = "noTipTarget";
    public static final String NOTE_SHOULD_WORK_INSIDE_COLONY = "shouldWorkInsideColony";
    public static final String NOTE_FALLBACK_MISSION_SKIPPED = "fallbackMissionSkipped";

    // client.moveUnit
    public static final String DIRECTION = "direction";
    public static final String MOVE_TYPE = "moveType";
    public static final String INTERACTIVE = "interactive";

    // client.moveUnit note categories
    public static final String NOTE_PRODUCTION_TYPE = "productionType";
    public static final String NOTE_INFO_PANEL = "infoPanel";


    private WideEventFields() {
        // Not to be instantiated.
    }
}
