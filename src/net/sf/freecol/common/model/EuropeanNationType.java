
package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

/**
 * Represents one of the European nations present in the game.
 */
public class EuropeanNationType extends NationType {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * Stores the starting units of this Nation.
     */
    private List<StartingUnit> startingUnits = new ArrayList<StartingUnit>();

    /**
     * Sole constructor.
     */
    public EuropeanNationType(int index) {
        super(index);
    }


    /**
     * Returns a list of this Nation's starting units.
     *
     * @return a list of this Nation's starting units.
     */
    public List<StartingUnit> getStartingUnits() {
        return startingUnits;
    }

    public void readFromXmlElement(Node xml, final Map<String, UnitType> unitTypeByRef) {

        setID(Xml.attribute(xml, "id"));
        setColor(new Color(Integer.parseInt(Xml.attribute(xml, "color"), 16)));

        Xml.Method method = new Xml.Method() {
                public void invokeOn(Node node) {
                    if ("ability".equals(node.getNodeName())) {
                        String abilityId = Xml.attribute(node, "id");
                        boolean value = Xml.booleanAttribute(node, "value");
                        setAbility(abilityId, value);
                    } else if (Modifier.getXMLElementTagName().equals(node.getNodeName())) {
                        Modifier modifier = new Modifier((Element) node);
                        setModifier(modifier.getId(), modifier);
                    } else if ("unit".equals(node.getNodeName())) {
                        StartingUnit unit = new StartingUnit();
                        unit.unitType = unitTypeByRef.get(Xml.attribute(node, "id"));
                        if (Xml.hasAttribute(node, "armed")) {
                            unit.armed = Xml.booleanAttribute(node, "armed");
                        }
                        if (Xml.hasAttribute(node, "mounted")) {
                            unit.mounted = Xml.booleanAttribute(node, "mounted");
                        }
                        if (Xml.hasAttribute(node, "missionary")) {
                            unit.missionary = Xml.booleanAttribute(node, "missionary");
                        }
                        if (Xml.hasAttribute(node, "tools")) {
                            unit.tools = Xml.intAttribute(node, "tools");
                        }
                        startingUnits.add(unit);
                    }
                }
            };
        Xml.forEachChild(xml, method);
    }

    /**
     * This class records the data necessary to construct a starting unit.
     */
    public class StartingUnit {
        public UnitType unitType;
        public boolean armed = false;
        public boolean mounted = false;
        public boolean missionary = false;
        public int tools = 0;
    }

}