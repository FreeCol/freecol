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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.io.StringWriter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.util.test.FreeColTestCase;

public class MonarchTest extends FreeColTestCase {


    public void testSerialize() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory xif = XMLOutputFactory.newInstance();
            XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
            dutch.getMonarch().toXML(xsw);
            xsw.close();
        } catch (XMLStreamException e) {
        }

    }

    public void testTaxActionChoices() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        // grace period has not yet expired
        List<RandomChoice<MonarchAction>> choices = dutch.getMonarch().getActionChoices();
        assertTrue(choices.isEmpty());

        game.setTurn(new Turn(100));
        dutch.setTax(Monarch.MINIMUM_TAX_RATE / 2);
        choices = dutch.getMonarch().getActionChoices();
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX));
        assertFalse(choicesContain(choices, MonarchAction.LOWER_TAX));

        int maximumTax = spec().getIntegerOption("model.option.maximumTax").getValue();
        dutch.setTax(maximumTax / 2);
        choices = dutch.getMonarch().getActionChoices();
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX));

        dutch.setTax(maximumTax + 2);
        choices = dutch.getMonarch().getActionChoices();
        assertFalse(choicesContain(choices, MonarchAction.RAISE_TAX));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX));

        dutch.setPlayerType(Player.PlayerType.REBEL);
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
