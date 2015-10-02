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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A container to hold abilities and modifiers for some FreeColObject-subclass.
 *
 * - FreeColGameObjectType, Europe, Player, Settlement are current
 *   implementors.
 *
 * - Building delegates some functionality to its type.
 *
 * - Unit fakes it by constructing a FeatureContainer on the fly.
 *
 * - FreeColObject itself implements a null version.
 */
public final class FeatureContainer {

    private static final Logger logger = Logger.getLogger(FeatureContainer.class.getName());

    /** Lock variables. */
    private final Object abilitiesLock = new Object();
    private final Object modifiersLock = new Object();

    /** The abilities in the container. */
    private Map<String, Set<Ability>> abilities = null;

    /** The modifiers in the container. */
    private Map<String, Set<Modifier>> modifiers = null;


    /**
     * Have the abilities map been created?
     *
     * @return True if the abilities are present.
     */
    private boolean abilitiesPresent() {
        synchronized (abilitiesLock) {
            return abilities != null;
        }
    }

    /**
     * On demand creation of the abilities map.
     */
    private void requireAbilities() {
        synchronized (abilitiesLock) {
            if (abilities == null) abilities = new HashMap<>();
        }
    }

    /**
     * Have the modifiers map been created?
     *
     * @return True if the modifiers are present.
     */
    private boolean modifiersPresent() {
        synchronized (modifiersLock) {
            return modifiers != null;
        }
    }

    /**
     * On demand creation of the modifiers map.
     */
    private synchronized void requireModifiers() {
        synchronized (modifiersLock) {
            if (modifiers == null) modifiers = new HashMap<>();
        }
    }


    /**
     * Is the given set of abilities non-empty and contains no
     * false-valued members?
     *
     * @return True if the ability set is `satisfied'.
     */
    public static boolean hasAbility(Set<Ability> abilitySet) {
        return (abilitySet == null || abilitySet.isEmpty())
            ? false
            : all(abilitySet, Ability::getValue);
    }

    /**
     * Is the given set of abilities non-empty and contains no
     * false-valued members?
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the ability is present.
     */
    public boolean hasAbility(String id, FreeColGameObjectType fcgot,
                              Turn turn) {
        return FeatureContainer.hasAbility(getAbilities(id, fcgot, turn));
    }

    /**
     * Checks if this container contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return !getAbilities(key, null, null).isEmpty();
    }

    /**
     * Gets the set of abilities with the given identifier from a
     * container.
     *
     * @param id The object identifier (null matches all).
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of abilities.
     */
    public Set<Ability> getAbilities(String id, FreeColGameObjectType fcgot,
                                     Turn turn) {
        Set<Ability> result = new HashSet<>();
        if (abilitiesPresent()) {
            synchronized (abilitiesLock) {
                if (id == null) {
                    for (Set<Ability> aset : abilities.values()) {
                        result.addAll(aset);
                    }
                } else {
                    Set<Ability> aset = abilities.get(id);
                    if (aset != null) result.addAll(aset);
                }
            }
            Iterator<Ability> it = result.iterator();
            while (it.hasNext()) {
                Ability a = it.next();
                if (!a.appliesTo(fcgot, turn)) it.remove();
            }
        }
        return result;
    }

    /**
     * Add the given ability to a container.
     *
     * @param ability An <code>Ability</code> to add.
     * @return True if the <code>Ability</code> was added.
     */
    public boolean addAbility(Ability ability) {
        if (ability == null) return false;

        requireAbilities();
        synchronized (abilitiesLock) {
            Set<Ability> abilitySet = abilities.get(ability.getId());
            if (abilitySet == null) {
                abilitySet = new HashSet<>();
                abilities.put(ability.getId(), abilitySet);
            }
            return abilitySet.add(ability);
        }
    }

    /**
     * Remove the given ability from a container.
     *
     * @param ability An <code>Ability</code> to remove.
     * @return The ability removed or null on failure.
     */
    public Ability removeAbility(Ability ability) {
        if (ability == null || !abilitiesPresent()) return null;

        synchronized (abilitiesLock) {
            Set<Ability> abilitySet = abilities.get(ability.getId());
            return (abilitySet == null
                || !abilitySet.remove(ability)) ? null
                : ability;
        }
    }

    /**
     * Remove all abilities with a given identifier.
     *
     * @param id The object identifier.
     */
    public void removeAbilities(String id) {
        if (!abilitiesPresent()) return;

        synchronized (abilitiesLock) {
            abilities.remove(id);
        }
    }


    /**
     * Gets the set of modifiers with the given identifier from this
     * container.
     *
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifiers(String id, FreeColGameObjectType fcgot,
                                      Turn turn) {
        Set<Modifier> result = new HashSet<>();
        if (modifiersPresent()) {
            synchronized (modifiersLock) {
                if (id == null) {
                    for (Set<Modifier> mset : modifiers.values()) {
                        result.addAll(mset);
                    }
                } else {
                    Set<Modifier> mset = modifiers.get(id);
                    if (mset != null) result.addAll(mset);
                }
            }
            Iterator<Modifier> it = result.iterator();
            while (it.hasNext()) {
                Modifier m = it.next();
                if (!m.appliesTo(fcgot, turn)) it.remove();
            }
        }
        return result;
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
     * Applies a collection of modifiers to the given float value.
     *
     * @param number The number to modify.
     * @param turn An optional applicable <code>Turn</code>.
     * @param mods The <code>Modifier</code>s to apply.
     * @return The modified number.
     */
    public static float applyModifiers(float number, Turn turn,
                                       Collection<Modifier> mods) {
        if (mods == null || mods.isEmpty()) return number;
        List<Modifier> modifiers = new ArrayList<>(mods);
        Collections.sort(modifiers);
        float result = number;
        for (Modifier m : modifiers) {
            float value = m.getValue(turn);
            if (value == Modifier.UNKNOWN) return value;
            result = m.apply(result, value);
        }
        return result;
    }

    /**
     * Adds a modifier to a container.
     *
     * @param modifier The <code>Modifier</code> to add.
     * @return True if the modifier was added.
     */
    public boolean addModifier(Modifier modifier) {
        if (modifier == null) return false;

        requireModifiers();
        synchronized (modifiersLock) {
            Set<Modifier> modifierSet = modifiers.get(modifier.getId());
            if (modifierSet == null) {
                modifierSet = new HashSet<>();
                modifiers.put(modifier.getId(), modifierSet);
            }
            return modifierSet.add(modifier);
        }
    }

    /**
     * Removes a modifier from a container.
     *
     * @param modifier The <code>Modifier</code> to remove.
     * @return The modifier removed.
     */
    public Modifier removeModifier(Modifier modifier) {
        if (modifier == null || !modifiersPresent()) return null;

        synchronized (modifiersLock) {
            Set<Modifier> modifierSet = modifiers.get(modifier.getId());
            return (modifierSet == null
                || !modifierSet.remove(modifier)) ? null
                : modifier;
        }
    }

    /**
     * Removes all modifiers with a given identifier.
     *
     * @param id The object identifier.
     */
    public void removeModifiers(String id) {
        if (!modifiersPresent()) return;

        synchronized (modifiersLock) {
            modifiers.remove(id);
        }
    }

    /**
     * Adds all the features in an object to this object.
     *
     * @param fco The <code>FreeColObject</code> to add features from.
     */
    public void addFeatures(FreeColObject fco) {
        FeatureContainer c = fco.getFeatureContainer();
        if (c == null) return;

        if (c.abilitiesPresent()) {
            requireAbilities();
            HashMap<String, Set<Ability>> ca;
            synchronized (c.abilitiesLock) {
                ca = new HashMap<>(c.abilities);
            }
            synchronized (abilitiesLock) {
                for (Entry<String, Set<Ability>> e : ca.entrySet()) {
                    Set<Ability> abilitySet = abilities.get(e.getKey());
                    if (abilitySet == null) {
                        abilitySet = new HashSet<>();
                        abilities.put(e.getKey(), abilitySet);
                    }
                    abilitySet.addAll(e.getValue());
                }
            }
        }

        if (c.modifiersPresent()) {
            requireModifiers();
            HashMap<String, Set<Modifier>> cm;
            synchronized (c.modifiersLock) {
                cm = new HashMap<>(c.modifiers);
            }
            synchronized (modifiersLock) {
                for (Entry<String, Set<Modifier>> e : cm.entrySet()) {
                    Set<Modifier> modifierSet = modifiers.get(e.getKey());
                    if (modifierSet == null) {
                        modifierSet = new HashSet<>();
                        modifiers.put(e.getKey(), modifierSet);
                    }
                    modifierSet.addAll(e.getValue());
                }
            }
        }
    }

    /**
     * Removes all the features in an object from a container.
     *
     * @param fco The <code>FreeColObject</code> to find features to remove
     *     in.
     */
    public void removeFeatures(FreeColObject fco) {
        FeatureContainer c = fco.getFeatureContainer();
        if (c == null) return;

        if (abilitiesPresent() && c.abilitiesPresent()) {
            Set<String> ca = new HashSet<>();
            synchronized (c.abilitiesLock) {
                ca.addAll(c.abilities.keySet());
            }
            synchronized (abilitiesLock) {
                for (String key : ca) {
                    Set<Ability> abilitySet = abilities.get(key);
                    if (abilitySet == null) continue;
                    for (Ability a : new HashSet<>(abilitySet)) {
                        if (a.getSource() == fco) abilitySet.remove(a);
                    }
                }
            }
        }

        if (modifiersPresent() && c.modifiersPresent()) {
            Set<String> cm = new HashSet<>();
            synchronized (c.modifiersLock) {
                cm.addAll(c.modifiers.keySet());
            }
            synchronized (modifiersLock) {
                for (String key : cm) {
                    Set<Modifier> modifierSet = modifiers.get(key);
                    if (modifierSet == null) continue;
                    for (Modifier m : new HashSet<>(modifierSet)) {
                        if (m.getSource() == fco) modifierSet.remove(m);
                    }
                }
            }
        }
    }

    /**
     * Clear this feature container.
     */
    public void clear() {
        if (abilitiesPresent()) {
            synchronized (abilitiesLock) {
                abilities.clear();
            }
        }
        if (modifiersPresent()) {
            synchronized (modifiersLock) {
                modifiers.clear();
            }
        }
    }

    /**
     * Replaces the source field. This is necessary because objects
     * may inherit Features from other abstract objects.
     *
     * @param oldSource The old source <code>FreeColGameObjectType</code>.
     * @param newSource The new source <code>FreeColGameObjectType</code>.
     */
    public void replaceSource(FreeColGameObjectType oldSource,
                              FreeColGameObjectType newSource) {
        for (Ability ability : getAbilities(null, null, null)) {
            if (oldSource == null || ability.getSource() == oldSource) {
                removeAbility(ability);
                Ability newAbility = new Ability(ability);
                newAbility.setSource(newSource);
                addAbility(newAbility);
            }
        }

        for (Modifier modifier : getModifiers(null, null, null)) {
            if (oldSource == null || modifier.getSource() == oldSource) {
                removeModifier(modifier);
                Modifier newModifier = new Modifier(modifier);
                newModifier.setSource(newSource);
                addModifier(newModifier);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("[FeatureContainer");
        Set<Ability> abilities = getAbilities(null, null, null);
        if (!abilities.isEmpty()) {
            sb.append(" [abilities");
            for (Ability ability : getAbilities(null, null, null)) {
                sb.append(" ").append(ability);
            }
            sb.append("]");
        }
        Set<Modifier> modifiers = getModifiers(null, null, null);
        if (!modifiers.isEmpty()) {
            sb.append(" [modifiers");
            for (Modifier modifier : getModifiers(null, null, null)) {
                sb.append(" ").append(modifier);
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
