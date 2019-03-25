/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The base class for all types defined by the specification. It can
 * be instantiated in order to provide a source for modifiers and
 * abilities that are provided by the code rather than defined in the
 * specification, such as the "artillery in the open" penalty.
 *
 * A FreeColSpecObjectType does not always need a reference to the
 * specification. However, if it has attributes or children that are
 * themselves FreeColSpecObjectTypes, then the specification must be
 * set before the type is de-serialized, otherwise the identifiers
 * therein can not be resolved.
 *
 * FreeColSpecObjectTypes can be abstract. Abstract types can be used
 * to derive other types, but can not be instantiated.  They will be
 * removed from the Specification after it has loaded completely.
 *
 * Many FreeColSpecObjectTypes have ids, but some leaf types do not
 * need them and there they may be omitted.
 */
public abstract class FreeColSpecObjectType extends FreeColSpecObject
    implements Named {

    /** Whether the type is abstract, or can be instantiated. */
    private boolean abstractType;

    /**
     * The features of this game object type.  Feature containers are
     * created on demand.
     */
    private FeatureContainer featureContainer = null;

    /**
     * Scopes that might limit the action of this object to certain
     * types of objects.
     */
    private ScopeContainer scopeContainer;

    // Do not serialize below.

    /**
     * The index imposes a total ordering consistent with equals on
     * each class extending FreeColSpecObjectType, but this ordering
     * is nothing but the order in which the objects of the respective
     * class were defined.  It is guaranteed to remain stable only for
     * a particular revision of a particular specification.
     */
    private int index = -1;
    

    /**
     * Deliberately empty constructor.
     */
    protected FreeColSpecObjectType() {
        super(null);
    }

    /**
     * Create a simple FreeColSpecObjectType without a specification.
     *
     * @param id The object identifier.
     */
    public FreeColSpecObjectType(String id) {
        this(id, null);
    }

    /**
     * Create a FreeColSpecObjectType with a given specification but
     * no object identifier.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public FreeColSpecObjectType(Specification specification) {
        this(null, specification);
    }

    /**
     * Create a FreeColSpecObjectType with a given identifier and
     * specification.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public FreeColSpecObjectType(String id, Specification specification) {
        super(specification);

        setId(id);
    }


    /**
     * Gets the index of this FreeColSpecObjectType.
     *
     * The index imposes a total ordering consistent with equals on
     * each class extending FreeColSpecObjectType, but this ordering
     * is nothing but the order in which the objects of the respective
     * class were defined.  It is guaranteed to remain stable only for
     * a particular revision of a particular specification.
     *
     * @return The game object index.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Sets the index of this FreeColSpecObjectType.
     *
     * @param index The new index value.
     */
    protected final void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Gets a string suitable for looking up the description of
     * this object in {@link net.sf.freecol.common.i18n.Messages}.
     *
     * @return A description key.
     */
    public final String getDescriptionKey() {
        return Messages.descriptionKey(getId());
    }

    /**
     * Is this an abstract type?
     *
     * @return True if this is an abstract game object type.
     */
    public final boolean isAbstractType() {
        return this.abstractType;
    }

    // Scope delegation

    public final List<Scope> getScopeList() {
        return ScopeContainer.getScopeList(this.scopeContainer);
    }

    public final Stream<Scope> getScopes() {
        return ScopeContainer.getScopes(this.scopeContainer);
    }

    public final void copyScopes(Collection<Scope> scopes) {
        this.scopeContainer = ScopeContainer.setScopes(this.scopeContainer, scopes);
    }

    public final void addScope(Scope scope) {
        this.scopeContainer = ScopeContainer.addScope(this.scopeContainer, scope);
    }

    public final void removeScope(Scope scope) {
        ScopeContainer.removeScope(this.scopeContainer, scope);
    }

    public boolean appliesTo(FreeColObject fco) {
        return ScopeContainer.scopeContainerAppliesTo(this.scopeContainer, fco);
    }

    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getNameKey() {
        return Messages.nameKey(getId());
    }

    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public final FeatureContainer getFeatureContainer() {
        if (this.featureContainer == null) {
            this.featureContainer = new FeatureContainer();
        }
        return this.featureContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        FreeColSpecObjectType o = copyInCast(other, FreeColSpecObjectType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.abstractType = o.isAbstractType();
        this.featureContainer.copy(o.getFeatureContainer());
        this.scopeContainer = ScopeContainer.setScopes(this.scopeContainer,
                                                       o.getScopeList());
        this.index = o.getIndex();
        return true;
    }


    // Serialization

    // We do not serialize index, so no INDEX_TAG.
    // We do not need to write the abstractType attribute, as once
    // the spec is read, all cases of abstractType==true are removed.
    private static final String ABSTRACT_TAG = "abstract";
    // Denotes deletion of a child.
    protected static final String DELETE_TAG = "delete";
    // Denotes that this type extends another.
    public static final String EXTENDS_TAG = "extends";
    // Denotes preservation of attributes and children.
    public static final String PRESERVE_TAG = "preserve";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Ability ability : sort(getAbilities())) ability.toXML(xw);

        for (Modifier modifier : getSortedModifiers()) modifier.toXML(xw);

        ScopeContainer.scopeContainerToXML(this.scopeContainer, xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.abstractType = xr.getAttribute(ABSTRACT_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            if (this.featureContainer != null) this.featureContainer.clear();
            ScopeContainer.clearScopes(this.scopeContainer);
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (Ability.TAG.equals(tag)) {
            if (xr.getAttribute(DELETE_TAG, false)) {
                removeAbilities(xr.readId());
                xr.closeTag(Ability.TAG);

            } else {
                Ability ability = new Ability(xr, spec); // Closes
                if (ability.getSource() == null) ability.setSource(this);
                addAbility(ability);
                spec.addAbility(ability);
            }

        } else if (Modifier.TAG.equals(tag)) {
            if (xr.getAttribute(DELETE_TAG, false)) {
                removeModifiers(xr.readId());
                xr.closeTag(Modifier.TAG);

            } else {
                Modifier modifier = new Modifier(xr, spec); // Closes
                if (modifier.getSource() == null) modifier.setSource(this);
                addModifier(modifier);
                spec.addModifier(modifier);
            }

        } else if (Scope.TAG.equals(tag)) {
            this.scopeContainer = ScopeContainer
                .addScope(this.scopeContainer, new Scope(xr));

        } else {
            super.readChild(xr);
        }
    }

    // getXMLTagName left to subclasses


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getId();
    }
}
