
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents one of the European nations present in the game, i.e. both REFs
 * and possible human players.
 */
public class EuropeanNationType extends NationType {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * Whether this is an REF Nation.
     */
    private boolean ref;

    /**
     * Stores the starting units of this Nation.
     */
    private List<AbstractUnit> startingUnits = new ArrayList<AbstractUnit>();

    /**
     * Constructor.
     */
    public EuropeanNationType(int index) {
        super(index);
    }

    /**
     * Returns the name of this Nation's Home Port.
     *
     * @return a <code>String</code> value
     */
    public String getEuropeName() {
        return Messages.message(getID() + ".europe");
    }

    /**
     * Returns the name of this Nation's REF.
     *
     * @return a <code>String</code> value
     */
    public String getREFName() {
        return Messages.message(getID() + ".ref");
    }

    /**
     * Get the <code>REF</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isREF() {
        return ref;
    }

    /**
     * Set the <code>REF</code> value.
     *
     * @param newREF The new REF value.
     */
    public final void setREF(final boolean newREF) {
        this.ref = newREF;
    }

    /**
     * Returns true.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEuropean() {
        return true;
    }

    /**
     * Returns a list of this Nation's starting units.
     *
     * @return a list of this Nation's starting units.
     */
    public List<AbstractUnit> getStartingUnits() {
        return startingUnits;
    }

    public void readFromXML(XMLStreamReader in, final Map<String, UnitType> unitTypeByRef)
            throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        ref = getAttribute(in, "ref", false);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("ability".equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                setAbility(abilityId, value);
                in.nextTag(); // close this element
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in); // Modifier close the element
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getID());
                }
                setModifier(modifier.getId(), modifier);
            } else if ("unit".equals(childName)) {
                AbstractUnit unit = new AbstractUnit(in); // AbstractUnit closes element
                startingUnits.add(unit);
            }
        }
    }

}