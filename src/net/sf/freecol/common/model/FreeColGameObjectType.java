/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.option.OptionGroup;


/**
 * The base class for all types defined by the specification. It can
 * be instantiated in order to provide a source for modifiers and
 * abilities that are provided by the code rather than defined in the
 * specification, such as the "artillery in the open" penalty.
 */
public class FreeColGameObjectType extends FreeColObject {

    private int index = -1;

    /**
     * The default index of Modifiers provided by this type.
     */
    private int modifierIndex = 100;

    /**
     * Whether the type is abstract or can be instantiated.
     */
    private boolean abstractType;

    /** The features of this game object type. */
    private final FeatureContainer featureContainer = new FeatureContainer();


    protected FreeColGameObjectType() {
        // empty constructor
    }

    public FreeColGameObjectType(String id) {
        this(id, null);
    }

    public FreeColGameObjectType(Specification specification) {
        this(null, specification);
    }

    public FreeColGameObjectType(String id, Specification specification) {
        setId(id);
        setSpecification(specification);
    }

    /**
     * Gets the feature container.
     *
     * @return The <code>FeatureContainer</code>.
     */
    @Override
    public final FeatureContainer getFeatureContainer() {
        return featureContainer;
    }
    
    /**
     * Describe <code>setIndex</code> method here.
     *
     * @param index an <code>int</code> value
     */
    protected final void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Returns the index of this FreeColGameObjectType. The index
     * imposes a total ordering consistent with equals on each class
     * extending FreeColGameObjectType, but this ordering is nothing
     * but the order in which the objects of the respective class were
     * defined. It is guaranteed to remain stable only for a
     * particular revision of a particular specification.
     *
     * @return an <code>int</code> value
     */
    protected int getIndex() {
        return index;
    }

    /**
     * Returns a string suitable for looking up the name of this
     * object in {@link net.sf.freecol.client.gui.i18n.Messages}.
     *
     * @return a <code>String</code> value
     */
    public final String getNameKey() {
        return getId() + ".name";
    }

    /**
     * Returns a string suitable for looking up the description of
     * this object in {@link net.sf.freecol.client.gui.i18n.Messages}.
     *
     * @return a <code>String</code> value
     */
    public final String getDescriptionKey() {
        return getId() + ".description";
    }

    /**
     * Returns the ID of this object with the given prefix removed if
     * the ID of the object starts with the prefix, and the entire ID
     * otherwise.
     *
     * @param prefix a <code>String</code> value
     * @return a <code>String</code> value
     */
    public final String getSuffix(String prefix) {
        if (getId().startsWith(prefix)) {
            return getId().substring(prefix.length());
        } else {
            return getId();
        }
    }

    /**
     * Applies the difficulty level with the given ID to this
     * FreeColGameObjectType. This method does nothing. If the
     * behaviour of a FreeColGameObjectType depends on difficulty, it
     * must override this method.
     *
     * @param difficulty difficulty level to apply
     */
    public void applyDifficultyLevel(OptionGroup difficulty) {
        // do nothing
    }

    /**
     * Get the <code>ModifierIndex</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getModifierIndex() {
        return modifierIndex;
    }

    /**
     * Get the index for the given Modifier.
     *
     * @param modifier a <code>Modifier</code> value
     * @return an <code>int</code> value
     */
    public int getModifierIndex(Modifier modifier) {
        return modifierIndex;
    }

    /**
     * Set the <code>ModifierIndex</code> value.
     *
     * @param newModifierIndex The new ModifierIndex value.
     */
    public final void setModifierIndex(final int newModifierIndex) {
        this.modifierIndex = newModifierIndex;
    }

    /**
     * Get the <code>Abstract</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isAbstractType() {
        return abstractType;
    }

    /**
     * Set the <code>Abstract</code> value.
     *
     * @param newAbstract The new Abstract value.
     */
    public final void setAbstractType(final boolean newAbstract) {
        this.abstractType = newAbstract;
    }


    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // don't use this
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, String tag)
        throws XMLStreamException {
        super.toXML(out, tag);
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        for (Ability ability : getAbilities()) {
            ability.toXMLImpl(out);
        }
        for (Modifier modifier : getModifiers()) {
            modifier.toXMLImpl(out);
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        setAbstractType(getAttribute(in, "abstract", false));
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * Reads a common child object, i.e. an Ability or Modifier.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if an error occurs
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if (Ability.getXMLElementTagName().equals(childName)) {
            if (getAttribute(in, "delete", false)) {
                String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                removeAbilities(id);
                in.nextTag();
            } else {
                Ability ability = new Ability(in, getSpecification());
                if (ability.getSource() == null) {
                    ability.setSource(this);
                }
                addAbility(ability); // Ability close the element
                getSpecification().addAbility(ability);
            }
        } else if (Modifier.getXMLElementTagName().equals(childName)) {
            if (getAttribute(in, "delete", false)) {
                String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                removeModifiers(id);
                in.nextTag();
            } else {
                Modifier modifier = new Modifier(in, getSpecification());
                if (modifier.getSource() == null) {
                    modifier.setSource(this);
                }
                if (modifier.getIndex() < 0) {
                    modifier.setIndex(getModifierIndex(modifier));
                }
                addModifier(modifier); // Modifier close the element
                getSpecification().addModifier(modifier);
            }
        } else {
            logger.warning("Parsing of " + childName
                + " is not implemented yet");
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT
                   || !in.getLocalName().equals(childName)) {
                in.nextTag();
            }
        }
    }

    /**
     * Use only for debugging purposes! A human-readable and localized name is
     * returned by getName().
     */
    @Override
    public String toString() {
        return getId();
    }
}
