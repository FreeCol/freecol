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

package net.sf.freecol.common.option;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.util.test.FreeColTestCase;


public class OptionTest extends FreeColTestCase {

    public void testGameOptions() {
        OptionGroup gameOptions = spec().getGameOptions();
        assertNotNull(gameOptions);
        assertNotNull(spec().getOptionGroup("gameOptions.map"));
        assertNotNull(spec().getOptionGroup("gameOptions.colony"));
        assertNotNull(spec().getOption(GameOptions.FOG_OF_WAR));
        assertNotNull(spec().getOption(GameOptions.ALLOW_STUDENT_SELECTION));
        assertNotNull(gameOptions);
        assertTrue(gameOptions.iterator().hasNext());
        assertNotNull(gameOptions.getOption(GameOptions.FOG_OF_WAR));
        assertFalse(((BooleanOption) gameOptions.getOption(GameOptions.CUSTOM_IGNORE_BOYCOTT))
                    .getValue());
        assertFalse(spec().getBoolean(GameOptions.CUSTOM_IGNORE_BOYCOTT));
        assertFalse(spec().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS));
        assertFalse(spec().getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW));
        assertTrue(spec().getBoolean(GameOptions.ALLOW_STUDENT_SELECTION));
    }

    public void testCloneIntegerOption() {
        IntegerOption money = spec().getIntegerOption(GameOptions.STARTING_MONEY);
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

        UnitListOption refOption = (UnitListOption) spec().getOption(GameOptions.REF_FORCE);

        for (AbstractUnit unit : refOption.getOptionValues()) {
            assertTrue(unit.getNumber() > 0);
            assertTrue(unit.getNumber() < Integer.MAX_VALUE);
        }

    }


    /**
     * OptionGroups are editable by default. If an OptionGroup is not
     * editable, however, none of its subgroups are editable either.
     */
    public void testInheritsEditable() {

        OptionGroup difficulties = spec().getOptionGroup("difficultyLevels");
        assertNotNull(difficulties);
        assertTrue(difficulties.isEditable());

        String[] levels = new String[] { "veryEasy", "easy", "medium", "hard", "veryHard" };
        String[] names = new String[] { "immigration", "natives", "monarch", "government", "other" };
        for (String level : levels) {
            OptionGroup group = (OptionGroup) difficulties.getOption("model.difficulty." + level);
            assertNotNull("Failed to find difficulty level '" + level + "'", group);
            assertFalse("Difficulty level '" + level + "' should not be editable", group.isEditable());
            for (String name : names) {
                OptionGroup subGroup = (OptionGroup) group.getOption("model.difficulty." + name);
                assertNotNull("Failed to find option group '" + name + "' (" + level + ")", subGroup);
                assertFalse("Option group '" + name + "' in '" + level + "' should not be editable", subGroup.isEditable());
            }
        }

        OptionGroup group = (OptionGroup) difficulties.getOption("model.difficulty.custom");
        assertNotNull("Failed to find difficulty level 'custom'", group);
        assertTrue("Difficulty level 'custom' should be editable", group.isEditable());
        for (String name : names) {
            OptionGroup subGroup = (OptionGroup) group.getOption("model.difficulty." + name);
            assertNotNull("Failed to find option group '" + name + "' (custom)", subGroup);
            assertTrue("Option group '" + name + "' should be editable", subGroup.isEditable());
        }

    }


}
