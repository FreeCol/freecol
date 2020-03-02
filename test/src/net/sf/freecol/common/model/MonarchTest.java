/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.util.test.FreeColTestCase;


public class MonarchTest extends FreeColTestCase {

    public void testSerialize() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        try (StringWriter sw = new StringWriter();
            FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            dutch.getMonarch().toXML(xw);
        } catch (IOException|XMLStreamException ex) {
            fail(ex.toString());
        }
    }

    public void testTaxActionChoices() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());

        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        // grace period has not yet expired
        List<RandomChoice<MonarchAction>> choices
            = dutch.getMonarch().getActionChoices();
        assertTrue(choices.isEmpty());

        Colony colony = getStandardColony();
        game.setTurn(new Turn(100));
        dutch.setTax(Monarch.MINIMUM_TAX_RATE / 2);
        choices = dutch.getMonarch().getActionChoices();
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX_WAR));
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX_ACT));
        assertFalse(choicesContain(choices, MonarchAction.LOWER_TAX_WAR));
        assertFalse(choicesContain(choices, MonarchAction.LOWER_TAX_OTHER));

        int maximumTax = spec().getInteger(GameOptions.MAXIMUM_TAX);
        dutch.setTax(maximumTax / 2);
        choices = dutch.getMonarch().getActionChoices();
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX_WAR));
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX_ACT));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX_WAR));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX_OTHER));

        dutch.setTax(maximumTax + 2);
        choices = dutch.getMonarch().getActionChoices();
        assertFalse(choicesContain(choices, MonarchAction.RAISE_TAX_WAR));
        assertFalse(choicesContain(choices, MonarchAction.RAISE_TAX_ACT));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX_WAR));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX_OTHER));

        dutch.changePlayerType(Player.PlayerType.REBEL);
        choices = dutch.getMonarch().getActionChoices();
        assertTrue(choices.isEmpty());

    }


    private boolean choicesContain(List<RandomChoice<MonarchAction>> choices, MonarchAction action) {
        for (RandomChoice<MonarchAction> choice : choices) {
            if (choice.getObject() == action) {
                return true;
            }
        }
        return false;
    }

}
