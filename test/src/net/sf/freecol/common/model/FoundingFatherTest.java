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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Test cases that apply to all Founding Fathers, or to the
 * FoundingFather class in general.
 */
public class FoundingFatherTest extends FreeColTestCase {

    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final UnitType servantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType statesmanType
        = spec().getUnitType("model.unit.elderStatesman");


    public void testFeatures() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        FoundingFather father1 = new FoundingFather("father1", spec());
        father1.setType(FoundingFatherType.TRADE);
        Ability ability = new Ability("some.new.ability");
        spec().addAbility(ability);
        father1.addAbility(ability);
        dutch.addFather(father1);

        assertTrue(dutch.hasAbility("some.new.ability"));

        FoundingFather father2 = new FoundingFather("father2", spec());
        father2.setType(FoundingFatherType.TRADE);
        Modifier modifier = new Modifier("some.new.modifier", 2f,
                                         ModifierType.ADDITIVE, father2);
        father2.addModifier(modifier);
        spec().addModifier(modifier);
        dutch.addFather(father2);

        Set<Modifier> modifierSet = dutch.getModifiers("some.new.modifier");
        assertEquals(1, modifierSet.size());
        assertEquals(2f, modifierSet.iterator().next().getValue());
        assertEquals(4f, FeatureContainer.applyModifiers(2, null,
                modifierSet));

        FoundingFather father3 = new FoundingFather("father3", spec());
        father3.setType(FoundingFatherType.TRADE);
        father3.addModifier(new Modifier("some.new.modifier", 2f,
                                         ModifierType.ADDITIVE, father3));
        dutch.addFather(father3);

        assertFalse(dutch.getModifiers("some.new.modifier").isEmpty());
        assertEquals(6f, dutch.applyModifiers(2, null, "some.new.modifier"));

        FoundingFather father4 = new FoundingFather("father4", spec());
        father4.setType(FoundingFatherType.TRADE);
        Ability ability2 = new Ability("some.new.ability", false);
        assertFalse(ability.equals(ability2));
        assertFalse(ability.hashCode() == ability2.hashCode());
        father4.addAbility(ability2);
        dutch.addFather(father4);
        assertFalse(dutch.hasAbility("some.new.ability"));
    }

    public void testAddAllFathers() {
        // check that all fathers can be added
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();

        for (FoundingFather father : spec().getFoundingFathers()) {
            player.addFather(father);
        }
    }

    public void testBellsRequired() {
        int[] expectedValues = new int[] { 40, 161, 241, 321, 401 };

        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        //assertEquals(2, getGame().getGameOptions().getInteger(GameOptions.DIFFICULTY));
        assertEquals(40, spec().getInteger(GameOptions.FOUNDING_FATHER_FACTOR));

        for (int index = 0; index < expectedValues.length; index++) {
            assertEquals(index, dutch.getFatherCount());
            assertEquals(expectedValues[index], dutch.getTotalFoundingFatherCost());
            FoundingFather father = new FoundingFather("father" + index, spec());
            dutch.addFather(father);
        }

    }

    public void testAvailableTo() {
        // this feature is not used at the moment
        Game game = getGame();
        for (FoundingFather father : spec().getFoundingFathers()) {
            for (Player player : game.getPlayers()) {
                assertEquals(player.getNationId(), player.isEuropean(), 
                             father.isAvailableTo(player));
            }
        }

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        FoundingFather newFather = new FoundingFather("father", spec());
        newFather.setType(FoundingFatherType.TRADE);

        Scope dutchScope = new Scope();
        dutchScope.setMethodName("getNationId");
        dutchScope.setMethodValue("model.nation.dutch");
        assertTrue(dutchScope.appliesTo(dutch));

        Scope frenchScope = new Scope();
        frenchScope.setMethodName("getNationType");
        frenchScope.setMethodValue("model.nationType.cooperation");
        assertTrue(frenchScope.appliesTo(french));

        List<Scope> scopes = new ArrayList<>();
        scopes.add(dutchScope);
        scopes.add(frenchScope);
        newFather.setScopes(scopes);
        for (Player player : game.getPlayers()) {
            assertEquals(player.getNationId(),
                         (player == french || player == dutch),
                         newFather.isAvailableTo(player));
        }
    }
}
