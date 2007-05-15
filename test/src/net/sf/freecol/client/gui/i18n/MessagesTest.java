package net.sf.freecol.client.gui.i18n;

import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;

public class MessagesTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public void tearDown(){
        Messages.setMessageBundle(Locale.US);
    }

    public void testMessageString() {

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        // With parameters
        assertEquals("Gold: %gold%    |    Tax: %tax%%    |    Year: %year%", Messages.message("menuBar.statusLine"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.", Messages
                .message("model.goods.Food.description"));

        // Message not found
        assertEquals(null, Messages.message("should.not.exist.and.thus.return.null"));
    }

    public void testMessageStringStringArrayArray() {

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade", null));

        // With parameters for "Gold: %gold% | Tax: %tax%% | Year: %year%"
        assertEquals("Gold: silver    |    Tax: 13%    |    Year: %year%", Messages.message("menuBar.statusLine",
                new String[][] { new String[] { "%gold%", "silver" }, new String[] { "%tax%", "13" } }));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.", Messages.message(
                "model.goods.Food.description", new String[][] {}));

        // Invalid Inputs
        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade", new String[][] {}));
        assertEquals("Gold: %gold%    |    Tax: %tax%%    |    Year: %year%", Messages.message("menuBar.statusLine",
                new String[][] { new String[] { "%tax%" } }));

        
        // Message not found
        assertEquals(null, Messages.message("should.not.exist.and.thus.return.null", new String[][] {}));
        
        assertEquals(null, Messages.message("should.not.exist.and.thus.return.null", 
                new String[][] { new String[] { "%gold%", "silver" }, new String[] { "%tax%", "13" } }));
    }

    public void testChangeLocaleSettings() {
        Messages.setMessageBundle(Locale.US);

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        Messages.setMessageBundle(Locale.GERMANY);

        assertEquals("Handelsberater", Messages.message("menuBar.report.trade"));
    }
}
