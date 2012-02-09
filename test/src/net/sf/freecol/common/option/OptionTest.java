/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

package net.sf.freecol.common.option;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.util.test.FreeColTestCase;

public class OptionTest extends FreeColTestCase {

    public void testGameOptions() {
        assertNotNull(spec().getOptionGroup("gameOptions"));
        assertNotNull(spec().getOptionGroup("gameOptions.map"));
        assertNotNull(spec().getOptionGroup("gameOptions.colony"));
        assertNotNull(spec().getOption("model.option.fogOfWar"));
        assertNotNull(spec().getOption("model.option.allowStudentSelection"));
        OptionGroup gameOptions = spec().getOptionGroup("gameOptions");
        assertNotNull(gameOptions);
        assertTrue(gameOptions.iterator().hasNext());
        assertNotNull(gameOptions.getOption("model.option.fogOfWar"));
        assertFalse(((BooleanOption) gameOptions.getOption(GameOptions.CUSTOM_IGNORE_BOYCOTT))
                    .getValue());
        assertFalse(spec().getBooleanOption(GameOptions.CUSTOM_IGNORE_BOYCOTT).getValue());
        assertFalse(spec().getBooleanOption(GameOptions.EXPERTS_HAVE_CONNECTIONS).getValue());
        assertFalse(spec().getBooleanOption(GameOptions.SAVE_PRODUCTION_OVERFLOW).getValue());
        assertTrue(spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION).getValue());
    }

    public void testCloneIntegerOption() {
        IntegerOption money = spec().getIntegerOption("model.option.startingMoney");
        IntegerOption money2 = money.clone();

        assertFalse(money == money2);
        assertEquals(money.getId(), money2.getId());
        assertEquals(money.getValue(), money2.getValue());
        assertEquals(money.getMinimumValue(), money2.getMinimumValue());
        assertEquals(money.getMaximumValue(), money2.getMaximumValue());

        money2.setValue(money.getValue() + 23);
        assertEquals((int) (money.getValue() + 23), (int) money2.getValue());

    }

    public void testUnitListOption() {

        UnitListOption refOption = (UnitListOption) spec().getOption("model.option.refSize");

        for (AbstractUnit unit : refOption.getOptionValues()) {
            assertTrue(unit.getNumber() > 0);
            assertTrue(unit.getNumber() < Integer.MAX_VALUE);
        }

    }


}