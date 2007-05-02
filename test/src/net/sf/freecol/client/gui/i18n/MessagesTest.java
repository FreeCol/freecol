package net.sf.freecol.client.gui.i18n;

import java.util.Locale;
import java.util.MissingResourceException;

import junit.framework.TestCase;

public class MessagesTest extends TestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    public void setUp() {
        // Make sure that English is the default locale
        if (!Locale.getDefault().equals(Locale.US)) {
            Messages.setResources(Messages.getMessageBundle(Locale.US));
        }
    }
    
    public void tearDown(){
        Messages.setResources(Messages.getMessageBundle(Locale.US));
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
        try {
            Messages.message("should.not.exist.and.thus.throw.exception");
            fail();
        } catch (MissingResourceException e) {
            // Okay to fail.
        }
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
        try {
            Messages.message("should.not.exist.and.thus.throw.exception", new String[][] {});
            fail();
        } catch (MissingResourceException e) {
            // Okay to fail.
        }
    }

    public void testChangeLocaleSettings() {
        Messages.setResources(Messages.getMessageBundle(Locale.US));

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        Messages.setResources(Messages.getMessageBundle(Locale.GERMANY));

        assertEquals("Handelsberater", Messages.message("menuBar.report.trade"));
    }
}
