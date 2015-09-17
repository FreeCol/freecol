/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * The FreeCol root class.  Maintains an identifier, and an optional link
 * to the specification this object uses.
 */
public abstract class FreeColObject
    implements Comparable<FreeColObject>, ObjectWithId {

    protected static final Logger logger = Logger.getLogger(FreeColObject.class.getName());

    public static final int INFINITY = Integer.MAX_VALUE;
    public static final int UNDEFINED = Integer.MIN_VALUE;


    /** The unique identifier of an object. */
    private String id;

    /** The <code>Specification</code> this object uses, which may be null. */
    private Specification specification;

    /** An optional property change container, allocated on demand. */
    private PropertyChangeSupport pcs = null;


    /**
     * Get the object unique identifier.
     *
     * @return The identifier.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Set the object identifier.
     *
     * @param newId The new object identifier.
     */
    public void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Version of setId() for FreeColGameObject to override and intern the
     * id into the enclosing game.  Just equivalent to setId() at the
     * FreeColObject level.
     *
     * @param newId The new object identifier.
     */
    public void internId(final String newId) {
        setId(newId);
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
        return (getId().startsWith(prefix))
            ? getId().substring(prefix.length())
            : getId();
    }

    /**
     * Gets the usual suffix of this object's identifier, that is
     * everything after the last '.'.
     *
     * @return The usual identifier suffix.
     */
    public final String getSuffix() {
        String id = getId();
        return (id == null) ? null : lastPart(id, ".");
    }

    /**
     * Get the specification.  It may be null.
     *
     * @return The <code>Specification</code> used by this object.
     */
    public Specification getSpecification() {
        return specification;
    }

    /**
     * Sets the specification for this object. 
     *
     * This method should only ever be used by the object's constructor.
     *
     * @param specification The <code>Specification</code> to use.
     */
    protected void setSpecification(Specification specification) {
        this.specification = specification;
    }


    // Identifier manipulation

    /**
     * Get the type part of the identifier.
     *
     * @return The type part of the identifier, or null on error.
     */
    public static String getIdType(String id) {
        if (id != null) {
            int col = id.indexOf(':');
            return (col >= 0) ? id.substring(0, col) : id;
        }
        return null;
    }

    /**
     * Get the type part of the identifier of this object.
     *
     * @return The type part of the identifier, or null on error.
     */
    public String getIdType() {
        return getIdType(getId());
    }

    /**
     * Gets the numeric part of the identifier.
     *
     * @return The numeric part of the identifier, or negative on error.
     */
    public int getIdNumber() {
        if (id != null) {
            int col = id.indexOf(':');
            if (col >= 0) {
                try {
                    return Integer.parseInt(id.substring(col + 1));
                } catch (NumberFormatException nfe) {}
            }
        }
        return -1;
    }

    /**
     * Compare two FreeColObjects by their identifiers.
     *
     * @param fco1 The first <code>FreeColObject</code> to compare.
     * @param fco2 The second <code>FreeColObject</code> to compare.
     * @return The comparison result.
     */
    public static int compareIds(FreeColObject fco1, FreeColObject fco2) {
        if (fco1 == null) {
            return (fco2 == null) ? 0 : -1;
        } else if (fco2 == null) {
            return 1;
        }
        String id1 = fco1.getId();
        String id2 = fco2.getId();
        if (id1 == null) {
            return (id2 == null) ? 0 : -1;
        } else if (id2 == null) {
            return 1;
        }
        int cmp = fco1.getIdType().compareTo(fco2.getIdType());
        if (cmp == 0) cmp = fco1.getIdNumber() - fco2.getIdNumber();
        if (cmp == 0) cmp = fco1.hashCode() - fco2.hashCode();
        return cmp;
    }

    /**
     * Base for Comparable implementations.
     *
     * @param other The other <code>FreeColObject</code> subclass to compare.
     * @return The comparison result.
     */
    @Override
    public int compareTo(FreeColObject other) {
        return compareIds(this, other);
    }

    /**
     * Sort a collection of <code>FreeColObject</code>s.
     *
     * @param c The <code>Collection</code> to sort.
     * @return A sorted copy of the collection.
     */
    public static <T extends FreeColObject> List<T> getSortedCopy(Collection<T> c) {
        return c.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Log a collection of <code>FreeColObject</code>s.
     *
     * @param c The <code>Collection</code> to log.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public static <T extends FreeColObject> void logFreeColObjects(Collection<T> c, LogBuilder lb) {
        lb.add("[");
        for (T t : c) lb.add(t.getSuffix(), " ");
        lb.shrink(" ");
        lb.add("]");
    }

    /**
     * Invoke a method for this object.
     *
     * @param methodName The name of the method.
     * @param returnClass The class of the return value.
     * @param defaultValue The default value.
     * @return The result of invoking the method, or the default value
     *     on failure.
     */
    protected <T> T invokeMethod(String methodName, Class<T> returnClass,
                                 T defaultValue) {
        if (methodName != null && returnClass != null) {
            try {
                Method method = getClass().getMethod(methodName);
                if (method != null) {
                    return returnClass.cast(method.invoke(this));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Invoke failed: " + methodName, e);
            }
        }
        return defaultValue;
    }

    
    // Property change support

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        if (pcs != null) {
            pcs.firePropertyChange(event);
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (pcs == null) {
            return new PropertyChangeListener[0];
        } else {
            return pcs.getPropertyChangeListeners();
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (pcs == null) {
            return new PropertyChangeListener[0];
        } else {
            return pcs.getPropertyChangeListeners(propertyName);
        }
    }

    public boolean hasListeners(String propertyName) {
        if (pcs == null) {
            return false;
        } else {
            return pcs.hasListeners(propertyName);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(propertyName, listener);
        }
    }


    // Feature container handling.

    /**
     * Gets the feature container for this object, if any.
     * None is provided here, but select subclasses will override.
     *
     * @return Null.
     */
    public FeatureContainer getFeatureContainer() {
        return null;
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id) {
        return hasAbility(id, null);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColGameObjectType fcgot) {
        return hasAbility(id, fcgot, null);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColGameObjectType fcgot,
                                    Turn turn) {
        return FeatureContainer.hasAbility(getAbilities(id, fcgot, turn));
    }

    /**
     * Checks if this object contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return !getAbilities(key, null, null).isEmpty();
    }

    /**
     * Gets a sorted copy of the abilities of this object.
     *
     * @return A list of abilities.
     */
    public final List<Ability> getSortedAbilities() {
        List<Ability> abilities = new ArrayList<>();
        abilities.addAll(getAbilities());
        Collections.sort(abilities);
        return abilities;
    }

    /**
     * Gets a copy of the abilities of this object.
     *
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilities() {
        return getAbilities(null);
    }

    /**
     * Gets the set of abilities with the given identifier from this object.
     *
     * @param id The object identifier.
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilities(String id) {
        return getAbilities(id, null);
    }

    /**
     * Gets the set of abilities with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilities(String id,
                                           FreeColGameObjectType fcgot) {
        return getAbilities(id, fcgot, null);
    }

    /**
     * Gets the set of abilities with the given identifier from this
     * object.  Subclasses with complex ability handling should
     * override this as all prior routines are derived from it.
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of abilities.
     */
    public Set<Ability> getAbilities(String id,
                                     FreeColGameObjectType fcgot,
                                     Turn turn) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? Collections.<Ability>emptySet()
            : fc.getAbilities(id, fcgot, turn);
    }

    /**
     * Add the given ability to this object.
     *
     * @param ability An <code>Ability</code> to add.
     * @return True if the ability was added.
     */
    public boolean addAbility(Ability ability) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? false : fc.addAbility(ability);
    }

    /**
     * Remove the given ability from this object.
     *
     * @param ability An <code>Ability</code> to remove.
     * @return The ability removed or null on failure.
     */
    public Ability removeAbility(Ability ability) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? null : fc.removeAbility(ability);
    }

    /**
     * Remove all abilities with a given identifier.
     *
     * @param id The object identifier.
     */
    public void removeAbilities(String id) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeAbilities(id);
    }


    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @return True if the modifier is present.
     */
    public final boolean hasModifier(String id) {
        return hasModifier(id, null);
    }

    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return True if the modifier is present.
     */
    public final boolean hasModifier(String id, FreeColGameObjectType fcgot) {
        return hasModifier(id, fcgot, null);
    }

    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the modifier is present.
     */
    public boolean hasModifier(String id, FreeColGameObjectType fcgot,
                               Turn turn) {
        return !getModifiers(id, fcgot, turn).isEmpty();
    }

    /**
     * Checks if this object contains a given modifier key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public final boolean containsModifierKey(String key) {
        Set<Modifier> set = getModifiers(key);
        return (set == null) ? false : !set.isEmpty();
    }

    /**
     * Gets a sorted copy of the modifiers of this object.
     *
     * @return A list of modifiers.
     */
    public final List<Modifier> getSortedModifiers() {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.addAll(getModifiers());
        Collections.sort(modifiers);
        return modifiers;
    }

    /**
     * Gets a copy of the modifiers of this object.
     *
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifiers() {
        return getModifiers(null);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifiers(String id) {
        return getModifiers(id, null);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifiers(String id,
                                            FreeColGameObjectType fcgot) {
        return getModifiers(id, fcgot, null);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * Subclasses with complex modifier handling may override this
     * routine.
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifiers(String id,
                                      FreeColGameObjectType fcgot,
                                      Turn turn) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? Collections.<Modifier>emptySet()
            : fc.getModifiers(id, fcgot, turn);
    }

    /**
     * Applies this objects modifiers with the given identifier to the
     * given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable <code>Turn</code>.
     * @param id The object identifier.
     * @return The modified number.
     */
    public final float applyModifiers(float number, Turn turn, String id) {
        return applyModifiers(number, turn, id, null);
    }

    /**
     * Applies this objects modifiers with the given identifier to the
     * given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable <code>Turn</code>.
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float applyModifiers(float number, Turn turn,
                                      String id, FreeColGameObjectType fcgot) {
        return applyModifiers(number, turn, getModifiers(id, fcgot, turn));
    }

    /**
     * Applies a collection of modifiers to the given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable <code>Turn</code>.
     * @param mods The <code>Modifier</code>s to apply.
     * @return The modified number.
     */
    public static final float applyModifiers(float number, Turn turn,
                                             Collection<Modifier> mods) {
        return FeatureContainer.applyModifiers(number, turn, mods);
    }

    /**
     * Add the given modifier to this object.
     *
     * @param modifier An <code>Modifier</code> to add.
     * @return True if the modifier was added.
     */
    public boolean addModifier(Modifier modifier) {
        FeatureContainer fc = getFeatureContainer();
        if (fc == null) return false;
        return fc.addModifier(modifier);
    }

    /**
     * Remove the given modifier from this object.
     *
     * @param modifier An <code>Modifier</code> to remove.
     * @return The modifier removed.
     */
    public Modifier removeModifier(Modifier modifier) {
        FeatureContainer fc = getFeatureContainer();
        if (fc == null) return null;
        return fc.removeModifier(modifier);
    }

    /**
     * Remove all abilities with a given identifier.
     *
     * @param id The object identifier.
     */
    public void removeModifiers(String id) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeModifiers(id);
    }


    /**
     * Adds all the features in an object to this object.
     *
     * @param fco The <code>FreeColObject</code> to add features from.
     */
    public void addFeatures(FreeColObject fco) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.addFeatures(fco);
    }

    /**
     * Removes all the features in an object from this object.
     *
     * @param fco The <code>FreeColObject</code> to find features to remove in.
     */
    public void removeFeatures(FreeColObject fco) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeFeatures(fco);
    }

    /**
     * Get the defence modifiers applicable to this object.
     *
     * @return A set of defence <code>Modifier</code>s.
     */
    public Set<Modifier> getDefenceModifiers() {
        return getModifiers(Modifier.DEFENCE);
    }


    // DOM handling.  Beware, this needs to go away.

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Document document) {
        return toXMLElement(document, WriteScope.toServer());
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param document The <code>Document</code>.
     * @param player The <code>Player</code> to write to.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Document document, Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Null player for toXMLElement(doc, player)");
        }
        return toXMLElement(document, WriteScope.toClient(player));
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param document The <code>Document</code>.
     * @param writeScope The <code>WriteScope</code> to apply.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Document document, WriteScope writeScope) {
        if (!writeScope.isValid()) {
            throw new IllegalStateException("Invalid write scope: "
                + writeScope);
        }
        return toXMLElement(document, writeScope, null);
    }

    /**
     * This method writes a partial XML-representation of this object to
     * an element using only the mandatory and specified fields.
     *
     * @param document The <code>Document</code>.
     * @param fields The fields to write.
     * @return An XML-representation of this object.
     */
    public Element toXMLElementPartial(Document document, String... fields) {
        return toXMLElement(document, WriteScope.toServer(), fields);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param document The <code>Document</code>.
     * @param writeScope The <code>WriteScope</code> to apply.
     * @param fields An array of field names, which if non-null
     *               indicates this should be a partial write.
     * @return An XML-representation of this object.
     */
    private Element toXMLElement(Document document, WriteScope writeScope,
                                 String[] fields) {
        StringWriter sw = new StringWriter();
        FreeColXMLWriter xw = null;
        try {
            xw = new FreeColXMLWriter(sw, writeScope);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter,", ioe);
            return null;
        }

        try {
            if (fields == null) {
                toXML(xw);
            } else {
                toXMLPartial(xw, fields);
            }
            xw.close();

            DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
            Document tempDocument = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));
                return (Element)document.importNode(tempDocument.getDocumentElement(), true);
            } catch (IOException|ParserConfigurationException|SAXException ex) {
                throw new RuntimeException("Parse fail", ex);
            }
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Error writing stream", e);
        }
    }

    // @compat 0.10.x
    /**
     * Version of readId(FreeColXMLReader) that reads from an element.
     *
     * To be replaced with just:
     *   element.getAttribute(FreeColObject.ID_ATTRIBUTE_TAG);
     *
     * @param element An element to read the id attribute from.
     * @return The identifier attribute value.
     */
    public static String readId(Element element) {
        String id = element.getAttribute(ID_ATTRIBUTE_TAG);
        if (id == null) id = element.getAttribute(ID_ATTRIBUTE);
        return id;
    }
    // end @compat

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param element An XML-element that will be used to initialize
     *      this object.
     */
    public void readFromXMLElement(Element element) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xmlTransformer = factory.newTransformer();
            StringWriter stringWriter = new StringWriter();
            xmlTransformer.transform(new DOMSource(element),
                                     new StreamResult(stringWriter));
            String xml = stringWriter.toString();
            try (
                FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml));
            ) {
                xr.nextTag();
                readFromXML(xr);
            } catch (XMLStreamException xe) {
                throw new IllegalStateException("XML failure", xe);
            }
        } catch (IOException|TransformerException ex) {
            throw new RuntimeException("Read failure", ex);
        }
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof FreeColObject) {
            FreeColObject fco = (FreeColObject)o;
            return Utils.equals(this.id, fco.id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Utils.hashCode(this.id);
    }


    // Serialization

    /** XML tag name for identifier attribute. */
    public static final String ID_ATTRIBUTE_TAG = "id";

    // @compat 0.10.x
    /** Obsolete identifier attribute. */
    public static final String ID_ATTRIBUTE = "ID";
    // end @compat

    /** XML tag name for array elements. */
    public static final String ARRAY_SIZE_TAG = "xLength";

    /** XML attribute tag to denote partial updates. */
    private static final String PARTIAL_ATTRIBUTE_TAG = "partial";
    // @compat 0.10.x
    private static final String OLD_PARTIAL_ATTRIBUTE_TAG = "PARTIAL";
    // end @compat

    /** XML tag name for value attributes, used in many places. */
    protected static final String VALUE_TAG = "value";


    /**
     * Debugging tool, dump object XML to System.err.
     */
    public void dumpObject() {
        save(System.err, WriteScope.toSave(), false);
    }

    /**
     * Debugging tool, dump collection XML to System.err.
     */
    public static <T extends FreeColObject> void dumpCollection(Collection<T> items) {
        System.err.println("[Collection begin ");
        for (T t : items) t.dumpObject();
        System.err.println(" collection end]");
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The <code>File</code> to write to.
     * @return True if the save proceeded without error.
     * @exception FileNotFoundException
     */
    public boolean save(File file) throws FileNotFoundException {
        return save(file, WriteScope.toSave());
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The <code>File</code> to write to.
     * @param scope The <code>WriteScope</code> to use.
     * @return True if the save proceeded without error.
     * @exception FileNotFoundException
     */
    public boolean save(File file, WriteScope scope) throws FileNotFoundException {
        return save(file, scope, false);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The <code>File</code> to write to.
     * @param scope The <code>WriteScope</code> to use.
     * @param pretty Attempt to indent the output nicely.
     * @return True if the save proceeded without error.
     * @exception FileNotFoundException
     */
    public boolean save(File file, WriteScope scope, boolean pretty) throws FileNotFoundException {
        try (
            FileOutputStream fos = new FileOutputStream(file);
        ) {
            return save(fos, scope, pretty);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FileOutputStream", ioe);
        }
        return false;
    }

    /**
     * Writes the object to the given output stream
     *
     * @param out The <code>OutputStream</code> to write to.
     * @param scope The <code>WriteScope</code> to use.
     * @param pretty Attempt to indent the output nicely.
     * @return True if the save proceeded without error.
     */
    public boolean save(OutputStream out, WriteScope scope, boolean pretty) {
        try (
            FreeColXMLWriter xw = new FreeColXMLWriter(out, scope, pretty);
        ) {
            xw.writeStartDocument("UTF-8", "1.0");

            this.toXML(xw);

            xw.writeEndDocument();

            xw.flush();

            return true;
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Exception writing object.", xse);

        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter.", ioe);
        }
        return false;
    }

    /**
     * Serialize this FreeColObject to a string.
     *
     * @param scope The write scope to use.
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize(WriteScope scope) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        try (
            FreeColXMLWriter xw = new FreeColXMLWriter(sw, scope);
        ) {
            this.toXML(xw);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter,", ioe);
            return null;
        }

        return sw.toString();
    }

    /**
     * Serialize this FreeColObject to a string.
     *
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize() throws XMLStreamException {
        return serialize(WriteScope.toServer());
    }

    /**
     * Copy a FreeColObject.
     *
     * The copied object and its internal descendents will be
     * identical to the original objects, but not present in the game.
     * Newly created objects will prefer to refer to other newly
     * created objects.  Thus if you copy a tile, an internal colony
     * on the tile will also be copied, and the copied tile will refer
     * to the copied colony and the copied colony refer to the copied
     * tile, but both will refer to the original uncopied owning player. 
     *
     * @param game The <code>Game</code> to add the object to.
     * @param returnClass The required object class.
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass) {
        T ret = null;
        try (
            FreeColXMLReader xr = new FreeColXMLReader(new StringReader(this.serialize()));
        ) {
            ret = xr.copy(game, returnClass);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to copy: " + getId(), e);
        }
        return ret;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * All attributes will be made visible.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     * @see #toXML(FreeColXMLWriter)
     */
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        toXML(xw, getXMLTagName());
    }

    /**
     * This method writes an XML-representation of this object with
     * a specified tag to the given stream.
     *
     * Almost all FreeColObjects end up calling these, and implementing
     * their own write{Attributes,Children} methods which begin by
     * calling their superclass.  This allows a clean nesting of the
     * serialization routines throughout the class hierarchy.
     *
     * All attributes will be made visible.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @param tag The tag to use.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        writeAttributes(xw);

        writeChildren(xw);

        xw.writeEndElement();
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * To be overridden if required by any object that has attributes
     * and uses the toXML(FreeColXMLWriter, String) call.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        if (getId() == null) {
            logger.warning("FreeColObject with null identifier: " + this);
        } else {
            xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        }
    }

    /**
     * Write the children of this object to a stream.
     *
     * To be overridden if required by any object that has children
     * and uses the toXML(FreeColXMLWriter, String) call.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        // do nothing
    }

    /**
     * This method writes a partial XML-representation of this object to
     * the given stream using only the mandatory and specified fields.
     *
     * All attributes are considered visible as this is
     * server-to-owner-client functionality, but it depends ultimately
     * on the presence of a getFieldName() method that returns a type
     * compatible with String.valueOf.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @param fields The fields to write.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public final void toXMLPartial(FreeColXMLWriter xw,
                                   String[] fields) throws XMLStreamException {
        final Class theClass = getClass();

        try {
            xw.writeStartElement(getXMLTagName());

            xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());

            xw.writeAttribute(PARTIAL_ATTRIBUTE_TAG, true);

            for (String field : fields) {
                Introspector intro = new Introspector(theClass, field);
                xw.writeAttribute(field, intro.getter(this));
            }

            xw.writeEndElement();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Partial write failed for "
                       + theClass.getName(), e);
        }
    }


    /**
     * Initializes this object from an XML-representation of this object,
     * unless the PARTIAL_ATTRIBUTE tag is present which indicates
     * a partial update of an existing object.
     *
     * @param xr The input stream with the XML.
     * @exception XMLStreamException if there are any problems reading
     *     the stream.
     */
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        if (xr.hasAttribute(PARTIAL_ATTRIBUTE_TAG)
            // @compat 0.10.x
            || xr.hasAttribute(OLD_PARTIAL_ATTRIBUTE_TAG)
            // end @compat
            ) {
            readFromXMLPartial(xr);
        } else {
            readAttributes(xr);

            readChildren(xr);
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        String newId = xr.readId();
        if (newId != null) {
            if (xr.shouldIntern()) {
                internId(newId);
            } else {
                setId(newId);
            }
        }
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        String tag = xr.getLocalName();
        if (tag == null) {
            throw new XMLStreamException("Parse error, null opening tag.");
        }
        int next = -1;
        for (;;) {
            try {
                next = xr.nextTag();
            } catch (XMLStreamException xse) {
                logger.log(Level.SEVERE, "nextTag failed at " + tag
                    + ", previous=" + next, xse);
            }
            if (next == XMLStreamConstants.END_ELEMENT) break;
            readChild(xr);
        }
        xr.expectTag(tag);
    }

    /**
     * Reads a single child object.  Subclasses must override to read
     * their enclosed elements.  This particular instance of the
     * routine always throws XMLStreamException because we should
     * never arrive here.  However it is very useful to always call
     * super.readChild() when an unexpected tag is encountered, as the
     * exception thrown here provides some useful debugging context.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        throw new XMLStreamException("In " + getXMLTagName()
            + ", unexpected tag " + xr.getLocalName()
            + ", at: " + xr.currentTag());
    }

    /**
     * Updates this object from an XML-representation of this object.
     *
     * All attributes are considered visible as this is
     * server-to-owner-client functionality.  It depends ultimately on
     * the presence of a setFieldName() method that takes a parameter
     * type T where T.valueOf(String) exists.
     *
     * @param xr The input stream with the XML.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXMLPartial(FreeColXMLReader xr) throws XMLStreamException {
        final Class theClass = getClass();
        final String tag = xr.getLocalName();
        int n = xr.getAttributeCount();

        internId(xr.readId());

        for (int i = 0; i < n; i++) {
            String name = xr.getAttributeLocalName(i);

            if (ID_ATTRIBUTE_TAG.equals(name)
                // @compat 0.10.x
                || ID_ATTRIBUTE.equals(name)
                // end @compat
                || PARTIAL_ATTRIBUTE_TAG.equals(name)) continue;

            try {
                Introspector intro = new Introspector(theClass, name);
                intro.setter(this, xr.getAttributeValue(i));

            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not set field " + name, e);
            }
        }

        xr.closeTag(tag);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + ":" + getId();
        //+ " (super hashcode: " + Integer.toHexString(super.hashCode()) + ")"
    }

    /**
     * Gets the tag name used to serialize this object, generally the
     * class name starting with a lower case letter.
     *
     * @return The tag name for this object.
     */
    public abstract String getXMLTagName();
        
    /**
     * Gets the tag name used to serialize this object, generally the
     * class name starting with a lower case letter.  This method
     * should be overridden by all subclasses that need to be
     * serialized if instances are expected to be read.
     *
     * @return <code>null</code>.
     */
    public static String getXMLElementTagName() {
        return null;
    }
}
