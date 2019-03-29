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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The FreeCol root class.  Maintains an identifier, and an optional link
 * to the specification this object uses.
 *
 * All FreeColObjects are trivially sortable on the basis of their
 * identifiers as a consequence of the Comparable implementation here.
 * Do not override this any further, use explicit comparators if more fully
 * featured sorting is required.
 */
public abstract class FreeColObject
    implements Comparable<FreeColObject>, ObjectWithId {

    protected static final Logger logger = Logger.getLogger(FreeColObject.class.getName());

    /** Comparator by FCO identifier. */
    public static final Comparator<? super FreeColObject> fcoComparator
        = new Comparator<FreeColObject>() {
                public int compare(FreeColObject fco1, FreeColObject fco2) {
                    return FreeColObject.compareIds(fco1, fco2);
                }
            };


    /** Fallback class index. */
    protected static final int DEFAULT_CLASS_INDEX = 1000;


    /** The identifier of an object. */
    private String id;

    /** An optional property change container, allocated on demand. */
    private PropertyChangeSupport pcs = null;


    // Note: no constructors here.  There are some nasty cases where
    // we can not easily determine the identifier at construction
    // time, so it is better to just let things call setId() when
    // ready.  However we do have this utility.

    /**
     * Get the FreeColObject class corresponding to a class name.
     *
     * @param <T> A FreeColObject subclass.
     * @param name The class name.
     * @return The class, or null if none found.
     */
    @SuppressWarnings("unchecked")
    public static <T extends FreeColObject> Class<T> getFreeColObjectClassByName(String name) {
        final String type = "net.sf.freecol.common.model."
            + capitalize(name);
        final Class<T> c = (Class<T>)Introspector.getClassByName(type);
        if (c != null) return c;
        logger.warning("getFreeColObjectClass could not find: " + type);
        return null;
    }

    /**
     * Get the FreeColObject class for this object.
     *
     * @param <T> A FreeColObject subclass.
     * @return The class, or null on error.
     */
    @SuppressWarnings("unchecked")
    public <T extends FreeColObject> Class<T> getFreeColObjectClass() {
        return (Class<T>)this.getClass();
    }


    // Identifier handling

    /**
     * Get the object unique identifier.
     *
     * @return The identifier.
     */
    public String getId() {
        return this.id;
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
     * Does another FreeColObject have the same identifier?
     *
     * @param other The other {@code FreeColObject} to check.
     * @return True if the identifiers are non-null and equal.
     */
    public boolean idEquals(FreeColObject other) {
        return other != null && this.id != null
            && this.id.equals(other.getId());
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
     * Get the type part of the identifier.
     *
     * @param id The identifier to examine.
     * @return The type part of the identifier, or null on error.
     */
    public static String getIdTypeByName(String id) {
        if (id != null) {
            int col = id.lastIndexOf(':');
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
        return getIdTypeByName(getId());
    }

    /**
     * Gets the numeric part of the identifier.
     *
     * @return The numeric part of the identifier, or negative on error.
     */
    public int getIdNumber() {
        if (id != null) {
            int col = id.lastIndexOf(':');
            if (col >= 0) {
                String s = id.substring(col + 1);
                // @compat 0.11.6
                // AI used to generate ids with <thing>:am<number>
                // which changed to <thing>:am:<number>
                if (s.startsWith("am")) s = s.substring(2);
                // end @compat 0.11.6
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException nfe) {}
            }
        }
        return -1;
    }

    /**
     * Compare two FreeColObjects by their identifiers.
     *
     * @param fco1 The first {@code FreeColObject} to compare.
     * @param fco2 The second {@code FreeColObject} to compare.
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
        return (cmp > 0) ? 1 : (cmp < 0) ? -1
            : Utils.compareTo(fco1.getIdNumber(), fco2.getIdNumber());
    }

    /**
     * Accessor for the class index.
     *
     * @return The class index used by
     *      {@link net.sf.freecol.client.ClientOptions}.
     */
    public int getClassIndex () {
        return DEFAULT_CLASS_INDEX;
    }

    /**
     * Get the class index, handling null and non-FCO objects.
     *
     * @param o The object to examine.
     * @return The class index.
     */
    public static int getObjectClassIndex(Object o) {
        return (o instanceof FreeColObject) ? ((FreeColObject)o).getClassIndex()
            : DEFAULT_CLASS_INDEX;
    }
    

    // Specification handling.
    //
    // Base FreeColObjects do not contain a Specification, but
    // FreeColSpecObjects do, and FreeColGameObjects have access to
    // the Specification in the Game.  Noop implementations here, to
    // be overridden by subclasses.
    
    /**
     * Get the specification.
     *
     * @return The {@code Specification} used by this object.
     */
    public Specification getSpecification() {
        return null;
    }

    /**
     * Sets the specification for this object. 
     *
     * @param specification The {@code Specification} to use.
     */
    protected void setSpecification(@SuppressWarnings("unused")
                                    Specification specification) {}


    // Game handling.
    // Base FreeColObjects do not contain a Game, but several subclasses
    // (like FreeColGameObject) do.
    
    /**
     * Gets the game this object belongs to.
     *
     * @return The {@code Game} this object belongs to.
     */
    public Game getGame() {
        return null;
    }

    /**
     * Sets the game object this object belongs to.
     *
     * @param game The {@code Game} to set.
     */
    public void setGame(@SuppressWarnings("unused") Game game) {}
        

    // Property change support

    protected PropertyChangeSupport getPropertyChangeSupport() {
        return this.pcs;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (this.pcs == null) this.pcs = new PropertyChangeSupport(this);
        this.pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (this.pcs == null) this.pcs = new PropertyChangeSupport(this);
        this.pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (this.pcs != null) {
            this.pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (this.pcs != null) {
            this.pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (this.pcs != null) {
            this.pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        if (this.pcs != null) {
            this.pcs.firePropertyChange(event);
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (this.pcs != null) {
            this.pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (this.pcs != null) {
            this.pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (this.pcs != null) {
            this.pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return (this.pcs == null) ? new PropertyChangeListener[0]
            : this.pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return (this.pcs == null) ? new PropertyChangeListener[0]
            : this.pcs.getPropertyChangeListeners(propertyName);
    }

    public boolean hasListeners(String propertyName) {
        return (this.pcs == null) ? false : this.pcs.hasListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (this.pcs != null) {
            this.pcs.removePropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (this.pcs != null) {
            this.pcs.removePropertyChangeListener(propertyName, listener);
        }
    }


    // Feature container support
    //
    // Base FreeColObjects do not directly implement a feature container,
    // but some subclasses provide them.  As long as getFeatureContainer()
    // works, these routines should too.

    /**
     * Gets the feature container for this object, if any.
     *
     * @return The {@code FeatureContainer} for this object.
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
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColSpecObjectType fcgot) {
        return hasAbility(id, fcgot, null);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColSpecObjectType fcgot,
                                    Turn turn) {
        return FeatureContainer.allAbilities(getAbilities(id, fcgot, turn));
    }

    /**
     * Checks if this object contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return first(getAbilities(key, null, null)) != null;
    }

    /**
     * Gets a sorted copy of the abilities of this object.
     *
     * @return A list of abilities.
     */
    public final List<Ability> getSortedAbilities() {
        return sort(getAbilities());
    }

    /**
     * Gets a copy of the abilities of this object.
     *
     * @return A stream of abilities.
     */
    public final Stream<Ability> getAbilities() {
        return getAbilities(null);
    }

    /**
     * Gets the set of abilities with the given identifier from this object.
     *
     * @param id The object identifier.
     * @return A stream of abilities.
     */
    public final Stream<Ability> getAbilities(String id) {
        return getAbilities(id, null);
    }

    /**
     * Gets the set of abilities with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @return A stream of abilities.
     */
    public final Stream<Ability> getAbilities(String id,
                                              FreeColSpecObjectType fcgot) {
        return getAbilities(id, fcgot, null);
    }

    /**
     * Gets the set of abilities with the given identifier from this
     * object.  Subclasses with complex ability handling should
     * override this as all prior routines are derived from it.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return A set of abilities.
     */
    public Stream<Ability> getAbilities(String id,
                                        FreeColSpecObjectType fcgot,
                                        Turn turn) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? Stream.<Ability>empty()
            : fc.getAbilities(id, fcgot, turn);
    }

    /**
     * Add the given ability to this object.
     *
     * @param ability An {@code Ability} to add.
     * @return True if the ability was added.
     */
    public boolean addAbility(Ability ability) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? false : fc.addAbility(ability);
    }

    /**
     * Remove the given ability from this object.
     *
     * @param ability An {@code Ability} to remove.
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
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @return True if the modifier is present.
     */
    public final boolean hasModifier(String id, FreeColSpecObjectType fcgot) {
        return hasModifier(id, fcgot, null);
    }

    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return True if the modifier is present.
     */
    public boolean hasModifier(String id, FreeColSpecObjectType fcgot,
                               Turn turn) {
        return any(getModifiers(id, fcgot, turn));
    }

    /**
     * Checks if this object contains a given modifier key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public final boolean containsModifierKey(String key) {
        return any(getModifiers(key));
    }

    /**
     * Gets a sorted copy of the modifiers of this object.
     *
     * @return A list of modifiers.
     */
    public final List<Modifier> getSortedModifiers() {
        return sort(getModifiers(), Modifier.ascendingModifierIndexComparator);
    }

    /**
     * Gets a copy of the modifiers of this object.
     *
     * @return A set of modifiers.
     */
    public final Stream<Modifier> getModifiers() {
        return getModifiers(null);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     * @return A set of modifiers.
     */
    public final Stream<Modifier> getModifiers(String id) {
        return getModifiers(id, null);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @return A set of modifiers.
     */
    public final Stream<Modifier> getModifiers(String id,
                                               FreeColSpecObjectType fcgot) {
        return getModifiers(id, fcgot, null);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * Subclasses with complex modifier handling may override this
     * routine.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return A set of modifiers.
     */
    public Stream<Modifier> getModifiers(String id,
                                         FreeColSpecObjectType fcgot,
                                         Turn turn) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? Stream.<Modifier>empty()
            : fc.getModifiers(id, fcgot, turn);
    }

    /**
     * Applies this objects modifiers with the given identifier to the
     * given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param id The object identifier.
     * @return The modified number.
     */
    public final float apply(float number, Turn turn, String id) {
        return apply(number, turn, id, null);
    }

    /**
     * Applies this objects modifiers with the given identifier to the
     * given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float apply(float number, Turn turn, String id,
                             FreeColSpecObjectType fcgot) {
        return applyModifiers(number, turn, getModifiers(id, fcgot, turn));
    }

    /**
     * Applies a stream of modifiers to the given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param mods The {@code Modifier}s to apply.
     * @return The modified number.
     */
    public static final float applyModifiers(float number, Turn turn,
                                             Stream<Modifier> mods) {
        return FeatureContainer.applyModifiers(number, turn, mods);
    }

    /**
     * Applies a collection of modifiers to the given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param mods The {@code Modifier}s to apply.
     * @return The modified number.
     */
    public static final float applyModifiers(float number, Turn turn,
                                             Collection<Modifier> mods) {
        return FeatureContainer.applyModifiers(number, turn, mods);
    }

    /**
     * Add the given modifier to this object.
     *
     * @param modifier An {@code Modifier} to add.
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
     * @param modifier An {@code Modifier} to remove.
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
     * @param fco The {@code FreeColObject} to add features from.
     */
    public void addFeatures(FreeColObject fco) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.addFeatures(fco);
    }

    /**
     * Removes all the features in an object from this object.
     *
     * @param fco The {@code FreeColObject} to find features to remove in.
     */
    public void removeFeatures(FreeColObject fco) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeFeatures(fco);
    }

    /**
     * Get the defence modifiers applicable to this object.
     *
     * @return A list of defence {@code Modifier}s.
     */
    public List<Modifier> getDefenceModifiers() {
        return toList(getModifiers(Modifier.DEFENCE));
    }


    // Comparison support

    /**
     * Base for Comparable implementations.
     *
     * @param other The other {@code FreeColObject} subclass to compare.
     * @return The comparison result.
     */
    @Override
    public int compareTo(FreeColObject other) {
        return compareIds(this, other);
    }


    // Miscellaneous routines

    /**
     * Log a collection of {@code FreeColObject}s.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to log.
     * @param lb A {@code LogBuilder} to log to.
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
     * @param <T> The actual return type.
     * @param methodName The name of the method.
     * @param returnClass The expected return class.
     * @param defaultValue The default value.
     * @return The result of invoking the method, or the default value
     *     on failure.
     */
    protected <T> T invokeMethod(String methodName, Class<T> returnClass,
                                 T defaultValue) {
        if (methodName != null && returnClass != null) {
            try {
                return Introspector.invokeMethod(this, methodName, returnClass);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Invoke failed: " + methodName, ex);
            }
        }
        return defaultValue;
    }

    /**
     * Get an object to display when showing the user messages for this object.
     *
     * Example: If this object is a Building, the object to display will be the
     * BuildingType.
     *
     * @return A suitable {@code FreeColObject} to display, defaults to this.
     */
    public FreeColObject getDisplayObject() {
        return this;
    }
           
    
    // Serialization

    /** XML tag name for identifier attribute. */
    public static final String ID_ATTRIBUTE_TAG = "id";

    /** XML tag name for array elements. */
    public static final String ARRAY_SIZE_TAG = "xLength";

    /** XML attribute tag to denote partial updates. */
    public static final String PARTIAL_ATTRIBUTE_TAG = "partial";

    /** XML tag name for value attributes, used in many places. */
    protected static final String VALUE_TAG = "value";


    /**
     * Debugging tool, dump object XML to System.err.
     */
    public void dumpObject() {
        save(System.err, WriteScope.toSave(), false);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The {@code File} to write to.
     * @return True if the save proceeded without error.
     */
    public boolean save(File file) {
        return save(file, WriteScope.toSave());
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The {@code File} to write to.
     * @param scope The {@code WriteScope} to use.
     * @return True if the save proceeded without error.
     */
    public boolean save(File file, WriteScope scope) {
        return save(file, scope, false);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The {@code File} to write to.
     * @param scope The {@code WriteScope} to use.
     * @param pretty Attempt to indent the output nicely.
     * @return True if the save proceeded without error.
     */
    public boolean save(File file, WriteScope scope, boolean pretty) {
        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            return save(fos, scope, pretty);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating output stream", ioe);
        }
        return false;
    }

    /**
     * Writes the object to the given output stream
     *
     * @param out The {@code OutputStream} to write to.
     * @param scope The {@code WriteScope} to use.
     * @param pretty Attempt to indent the output nicely.
     * @return True if the save proceeded without error.
     */
    public boolean save(OutputStream out, WriteScope scope, boolean pretty) {
        boolean ret = false;
        if (scope == null) scope = FreeColXMLWriter.WriteScope.toSave();
        try (
            FreeColXMLWriter xw = new FreeColXMLWriter(out, scope, pretty);
        ) {
            xw.writeStartDocument("UTF-8", "1.0");

            this.toXML(xw);

            xw.writeEndDocument();

            ret = true;
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Exception writing object.", xse);

        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter.", ioe);
        }
        return ret;
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
     * Serialize this FreeColObject to a string for a target player.
     *
     * @param player The {@code Player} to serialize the object to.
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize(Player player) throws XMLStreamException {
        return serialize(WriteScope.toClient(player));
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
        return serialize(scope, null);
    }

    /**
     * Serialize this FreeColObject to a string, possibly partially.
     *
     * @param scope The write scope to use.
     * @param fields A list of field names, which if non-null indicates this
     *     should be a partial write.
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize(WriteScope scope, List<String> fields)
        throws XMLStreamException {
        StringWriter sw = new StringWriter();
        try (
            FreeColXMLWriter xw = new FreeColXMLWriter(sw, scope);
        ) {
            if (fields == null) {
                this.toXML(xw);
            } else {
                this.toXMLPartial(xw, fields);
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter,", ioe);
            return null;
        }

        return sw.toString();
    }

    /**
     * Copy a FreeColObject.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to add the object to.
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game) {
        @SuppressWarnings("unchecked") Class<T> returnClass
            = (Class<T>)this.getClass();
        return this.copy(game, returnClass);
    }

    /**
     * Copy a FreeColObject for a player.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to add the object to.
     * @param player The {@code Player} to copy for,
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game, Player player) {
        @SuppressWarnings("unchecked") Class<T> returnClass
            = (Class<T>)this.getClass();
        return this.copy(game, returnClass, player);
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
     * @param <T> The actual return type.
     * @param game The {@code Game} to add the object to.
     * @param returnClass The expected return class.
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass) {
        T ret = null;
        try (FreeColXMLReader xr
            = new FreeColXMLReader(new StringReader(this.serialize()))) {
            ret = xr.copy(game, returnClass);
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Copy of " + getId()
                + " to " + returnClass.getName() + "failed", xse);
        }
        return ret;
    }

    /**
     * Copy a FreeColObject for a target player.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to add the object to.
     * @param returnClass The expected return class.
     * @param player The {@code Player} that will see the result.
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass,
        Player player) {
        T ret = null;
        try (FreeColXMLReader xr
            = new FreeColXMLReader(new StringReader(this.serialize(player)))) {
            ret = xr.copy(game, returnClass);
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Copy of " + getId()
                + " to " + returnClass.getName()
                + " for " + player.getId() + "failed", xse);
        }
        return ret;
    }

    /**
     * Copy another FreeColObject into this one if it is compatible.
     *
     * @param <T> The {@code FreeColObject} subclass of the object to copy in.
     * @param other The other object.
     * @return True if the copy in is succesful.
     */
    public <T extends FreeColObject> boolean copyIn(T other) {
        FreeColObject fco = copyInCast(other, FreeColObject.class);
        if (fco == null) return false;
        for (PropertyChangeListener pcl : fco.getPropertyChangeListeners()) {
            fco.removePropertyChangeListener(pcl);
            this.addPropertyChangeListener(pcl);
        }
        return true;
    } 

    /**
     * If another object can be copied into this one, 
     *
     * @param <T> The {@code FreeColObject} subclass of the object to copy in.
     * @param other The {@code FreeColObject} to copy in.
     * @param <R> The {@code FreeColObject} subclass to copy in to.
     * @param returnClass The return class.
     * @return The other object cast to the return class, or null
     *     if incompatible in any way.
     */
    protected <T extends FreeColObject, R extends FreeColObject> R
        copyInCast(T other, Class<R> returnClass) {
        if (other == null) return (R)null;
        if (!idEquals(other)) {
            logger.warning("copyInCast " + other.getId() + " onto " + getId());
            return (R)null;
        }
        try {
            return returnClass.cast(other);
        } catch (ClassCastException cce) {}
        return (R)null;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * All attributes will be made visible.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
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
     * @param xw The {@code FreeColXMLWriter} to write to.
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
     * @param xw The {@code FreeColXMLWriter} to write to.
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
     * @param xw The {@code FreeColXMLWriter} to write to.
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
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @param fields The fields to write.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public final void toXMLPartial(FreeColXMLWriter xw,
                                   String[] fields) throws XMLStreamException {
        final Class theClass = getClass();

        xw.writeStartElement(getXMLTagName());

        xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());

        xw.writeAttribute(PARTIAL_ATTRIBUTE_TAG, true);

        for (String field : fields) {
            Introspector intro = new Introspector(theClass, field);
            try {
                String value = intro.getter(this);
                xw.writeAttribute(field, value);
            } catch (Introspector.IntrospectorException ie) {
                logger.log(Level.WARNING, "Failed to write field: " + field, ie);
            }
        }

        xw.writeEndElement();
    }

    /**
     * Simpler version of toXMLPartial, where the fields are pre-filled
     * with key,value pairs.  We thus no longer need call the introspector.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @param fields The fields to write.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public final void toXMLPartial(FreeColXMLWriter xw,
                                   List<String> fields) throws XMLStreamException {
        final Class theClass = getClass();

        try {
            xw.writeStartElement(getXMLTagName());

            xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());

            xw.writeAttribute(PARTIAL_ATTRIBUTE_TAG, true);

            int n = fields.size();
            for (int i = 0; i < n-1; i += 2) {
                xw.writeAttribute(fields.get(i), fields.get(i+1));
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
        if (xr.hasAttribute(PARTIAL_ATTRIBUTE_TAG)) {
            readFromXMLPartial(xr);
        } else {
            readAttributes(xr);

            readChildren(xr);
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        String newId = xr.readId();
        if (newId != null) setId(newId);
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        String tag = xr.getLocalName();
        if (tag == null) {
            throw new XMLStreamException("Parse error, null opening tag: " + this);
        }
        try {
            while (xr.moreTags()) {
                readChild(xr);
            }
        } catch (XMLStreamException xse) {
            logger.log(Level.SEVERE, "nextTag failed at " + tag, xse);
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
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        xr.unexpectedTag(getXMLTagName());
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

        setId(xr.readId());

        for (int i = 0; i < n; i++) {
            String name = xr.getAttributeLocalName(i);

            if (ID_ATTRIBUTE_TAG.equals(name)
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
     * Make the standard array key.
     *
     * @param i The array index.
     * @return The array key.
     */
    public static String arrayKey(int i) {
        return "x" + String.valueOf(i);
    }

    /**
     * Get the serialization tag for this object.
     *
     * @return The tag.
     */
    public abstract String getXMLTagName();


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof FreeColObject) {
            FreeColObject other = (FreeColObject)o;
            return Utils.equals(this.id, other.id);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + ":" + getId();
        //+ " (super hashcode: " + Integer.toHexString(super.hashCode()) + ")"
    }
}
