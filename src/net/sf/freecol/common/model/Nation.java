package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColGameObjectType {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The default color to use for this nation. Can be changed by the
     * Player.
     */
    private Color color;

    /**
     * Describe type here.
     */
    private NationType type;

    /**
     * Describe selectable here.
     */
    private boolean selectable;

    /**
     * Describe classic here.
     */
    private boolean classic;

    /**
     * Describe refID here.
     */
    private String refID;


    /**
     * Sole constructor.
     */
    public Nation(int index) {
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
     * Get the <code>Type</code> value.
     *
     * @return a <code>NationType</code> value
     */
    public final NationType getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final NationType newType) {
        this.type = newType;
    }

    /**
     * Get the <code>RulerName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getRulerName() {
        return Messages.message(getID() + ".ruler");
    }

    /**
     * Get the <code>Selectable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isSelectable() {
        return selectable;
    }

    /**
     * Get the <code>RefID</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getRefID() {
        return refID;
    }

    /**
     * Set the <code>RefID</code> value.
     *
     * @param newRefID The new RefID value.
     */
    public final void setRefID(final String newRefID) {
        this.refID = newRefID;
    }

    /**
     * Set the <code>Selectable</code> value.
     *
     * @param newSelectable The new Selectable value.
     */
    public final void setSelectable(final boolean newSelectable) {
        this.selectable = newSelectable;
    }

    /**
     * Get the <code>Classic</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isClassic() {
        return classic;
    }

    /**
     * Set the <code>Classic</code> value.
     *
     * @param newClassic The new Classic value.
     */
    public final void setClassic(final boolean newClassic) {
        this.classic = newClassic;
    }

    public String toString() {
        return getName();
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, NationType> nationTypeByRef)
        throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        setColor(new Color(Integer.decode(in.getAttributeValue(null, "color"))));
        type = nationTypeByRef.get(in.getAttributeValue(null, "nation-type"));
        selectable = getAttribute(in, "selectable", false);
        classic = getAttribute(in, "classic", false);
        refID = getAttribute(in, "ref-of", null);
        in.nextTag();
   }


}