
package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

/**
 * Represents one of the European nations present in the game.
 */
public class NationType extends FreeColGameObjectType implements Abilities, Modifiers {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The default color to use for this nation type. Can be
     * overridden by the Player object.
     */
    private Color color;

    /**
     * Stores the abilities of this Nation.
     */
    private HashMap<String, Boolean> abilities = new HashMap<String, Boolean>();

    /**
     * Stores the Modifiers of this Nation.
     */
    private HashMap<String, Modifier> modifiers = new HashMap<String, Modifier>();

    /**
     * Stores the starting units of this Nation.
     */
    private List<StartingUnit> startingUnits = new ArrayList<StartingUnit>();

    /**
     * Sole constructor.
     */
    public NationType(int index) {
        setIndex(index);
    }


    /**
     * Get the <code>Color</code> value.
     *
     * @return a <code>Color</code> value
     */
    public final Color getColor() {
        return color;
    }

    /**
     * Set the <code>Color</code> value.
     *
     * @param newColor The new Color value.
     */
    public final void setColor(final Color newColor) {
        this.color = newColor;
    }

    /**
     * Returns true if this Nation has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return abilities.containsKey(id) && abilities.get(id);
    }

    /**
     * Returns a copy of this Nation's abilities.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Boolean> getAbilities() {
        return new HashMap<String, Boolean>(abilities);
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        abilities.put(id, newValue);
    }

    /**
     * Get the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public final Modifier getModifier(String id) {
        return modifiers.get(id);
    }

    /**
     * Set the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @param newModifier a <code>Modifier</code> value
     */
    public final void setModifier(String id, final Modifier newModifier) {
        modifiers.put(id, newModifier);
    }

    /**
     * Returns a copy of this Nation's modifiers.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Modifier> getModifiers() {
        return new HashMap<String, Modifier>(modifiers);
    }

    public void readFromXmlElement(Node xml, Map<String, GoodsType> goodsTypeByRef) {

        setID(Xml.attribute(xml, "id"));
        color = new Color(Integer.parseInt(Xml.attribute(xml, "color"), 16));

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
                        unit.unitType = Xml.attribute(node, "id");
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
        public String unitType;
        public boolean armed = false;
        public boolean mounted = false;
        public boolean missionary = false;
        public int tools = 0;
    }

}