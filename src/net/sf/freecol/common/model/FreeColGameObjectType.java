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
 *
 * In general, a FreeColGameObjectType does not need a reference to
 * the specification.  However, if it has attributes or children that
 * are themselves FreeColGameObjectTypes, then the specification must
 * be set before the type is de-serialized, otherwise the identifiers
 * can not be resolved.
 *
 * FreeColGameObjectTypes can be abstract.  Abstract types can be used
 * to derive other types, but can not be instantiated.  They will be
 * removed from the Specification after it has loaded completely.
 */
public abstract class FreeColGameObjectType extends FreeColObject
    implements Named {

    /** The default index of Modifiers provided by this type. */
    private int modifierIndex = 100;

    /** Whether the type is abstract, or can be instantiated. */
    private boolean abstractType;

    /**
     * The features of this game object type.  Feature containers are
     * created on demand.
     */
    private FeatureContainer featureContainer = null;

    // Do not serialize below.

    /**
     * The index imposes a total ordering consistent with equals on
     * each class extending FreeColGameObjectType, but this ordering
     * is nothing but the order in which the objects of the respective
     * class were defined.  It is guaranteed to remain stable only for
     * a particular revision of a particular specification.
     */
    private int index = -1;
    

    /**
     * Deliberately empty constructor.
     */
    protected FreeColGameObjectType() {}

    /**
     * Create a simple FreeColGameObjectType without a specification.
     *
     * @param id The object identifier.
     */
    public FreeColGameObjectType(String id) {
        this(id, null);
    }

    /**
     * Create a FreeColGameObjectType with a given specification but
     * no object identifier.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public FreeColGameObjectType(Specification specification) {
        this(null, specification);
    }

    /**
     * Create a FreeColGameObjectType with a given identifier and
     * specification.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
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
        if (featureContainer == null) featureContainer = new FeatureContainer();
        return featureContainer;
    }

    /**
     * Gets the index of this FreeColGameObjectType.
     *
     * The index imposes a total ordering consistent with equals on
     * each class extending FreeColGameObjectType, but this ordering
     * is nothing but the order in which the objects of the respective
     * class were defined.  It is guaranteed to remain stable only for
     * a particular revision of a particular specification.
     *
     * @return The game object index.
     */
    protected int getIndex() {
        return index;
    }

    /**
     * Sets the index of this FreeColGameObjectType.
     *
     * @param index The new index value.
     */
    protected final void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Gets a string suitable for looking up the description of
     * this object in {@link net.sf.freecol.client.gui.i18n.Messages}.
     *
     * @return A description key.
     */
    public final String getDescriptionKey() {
        return getId() + ".description";
    }

    /**
     * Gets the identifier of this object with the given prefix
     * removed if the id of the object starts with the prefix, and the
     * entire id otherwise.
     *
     * @param prefix The prefix to test.
     * @return An identifier.
     */
    public final String getSuffix(String prefix) {
        return (getId().startsWith(prefix)) ? getId().substring(prefix.length())
            : getId();
    }

    /**
     * Applies the given difficulty level to this
     * FreeColGameObjectType.  If the behaviour of a
     * FreeColGameObjectType depends on difficulty, it must override
     * this method.  This base form does nothing.
     *
     * @param difficulty The difficulty level to apply.
     */
    public void applyDifficultyLevel(OptionGroup difficulty) {
        // do nothing
    }

    /**
     * Get the modifier index value.  This is the priority with which
     * Modifiers provided by this type will be applied.
     *
     * @return The modifier index.
     */
    public final int getModifierIndex() {
        return modifierIndex;
    }

    /**
     * Get the index for the given Modifier.  By default, this returns
     * the type's modifier index.  Override this method if the type
     * should distinguish different modifier priorities.
     *
     * @param modifier The <code>Modifier</code> to check.
     * @return The modifier index.
     * @see BuildingType#getModifierIndex(Modifier)
     */
    protected int getModifierIndex(Modifier modifier) {
        return modifierIndex;
    }

    /**
     * Set the modifier index value.
     *
     * @param newModifierIndex The new modifier index.
     */
    public final void setModifierIndex(final int newModifierIndex) {
        this.modifierIndex = newModifierIndex;
    }

    /**
     * Is this an abstract type?
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isAbstractType() {
        return abstractType;
    }


    // Interface Named

    /**
     * Gets a string suitable for looking up the name of this
     * object in {@link net.sf.freecol.client.gui.i18n.Messages}.
     *
     * @return A message key.
     */
    public final String getNameKey() {
        return getId() + ".name";
    }


    // Serialization

    // We do not serialize index, so no INDEX_TAG.
    // We do not need to write the abstractType attribute, as once
    // the spec is read, all cases of abstractType==true are removed.
    private static final String ABSTRACT_TAG = "abstract";
    // Denotes deletion of a child element.
    protected static final String DELETE_TAG = "delete";
    // Denotes that this type extends another.
    protected static final String EXTENDS_TAG = "extends";
    // Denotes preservation of attributes and children.
    protected static final String PRESERVE_TAG = "preserve";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Ability ability : getSortedCopy(getAbilities())) {
            ability.toXML(out);
        }

        for (Modifier modifier : getSortedModifiers()) {
            modifier.toXML(out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        if (getId() == null) throw new XMLStreamException("Null id");

        abstractType = getAttribute(in, ABSTRACT_TAG, false);
    }

    /**
     * Should this game object type clear containers on read?
     * Usually true, but not if this type is extending another one.
     *
     * @return True if the containers should be cleared.
     */
    protected boolean readShouldClearContainers(XMLStreamReader in) {
        return !hasAttribute(in, EXTENDS_TAG)
            && !hasAttribute(in, PRESERVE_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (featureContainer != null && readShouldClearContainers(in)) {
            // Clear containers, but not if extending existing containers.
            featureContainer.clear();
        }

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (Ability.getXMLElementTagName().equals(tag)) {
            if (getAttribute(in, DELETE_TAG, false)) {
                removeAbilities(readId(in));
                closeTag(in, DELETE_TAG);

            } else {
                Ability ability = new Ability(in, spec); // Closes the element
                if (ability.getSource() == null) ability.setSource(this);
                addAbility(ability);
                spec.addAbility(ability);
            }

        } else if (Modifier.getXMLElementTagName().equals(tag)) {
            if (getAttribute(in, DELETE_TAG, false)) {
                removeModifiers(readId(in));
                closeTag(in, Modifier.getXMLElementTagName());

            } else {
                Modifier modifier = new Modifier(in, spec);// Closes the element
                if (modifier.getSource() == null) modifier.setSource(this);
                if (modifier.getIndex() < 0) {
                    modifier.setIndex(getModifierIndex(modifier));
                }
                addModifier(modifier);
                spec.addModifier(modifier);
            }

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getId();
    }
}
