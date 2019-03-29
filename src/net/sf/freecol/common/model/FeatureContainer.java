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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A container to hold abilities and modifiers for some FreeColObject-subclass.
 *
 * - FreeColSpecObjectType, Europe, Player, Settlement are current
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

    protected Collection<Ability> getAbilityValues() {
        Set<Ability> ret = new HashSet<>();
        synchronized (abilitiesLock) {
            if (abilities != null) {
                for (Set<Ability> as : this.abilities.values()) ret.addAll(as);
            }
        }
        return ret;
    }
    protected Collection<Modifier> getModifierValues() {
        Set<Modifier> ret = new HashSet<>();
        synchronized (modifiersLock) {
            if (modifiers != null) {
                for (Set<Modifier> ms : this.modifiers.values()) ret.addAll(ms);
            }
        }
        return ret;
    }
    

    /**
     * Is the given set of abilities non-empty and contains no
     * false-valued members?
     *
     * @param abilities A stream of {@code Ability}s to check.
     * @return True if the abilities are `satisfied'.
     */
    public static boolean allAbilities(Stream<Ability> abilities) {
        boolean ret = false;
        for (Ability ability : iterable(abilities)) {
            if (!ability.getValue()) return false;
            ret = true;
        }
        return ret;
    }

    /**
     * Is the given set of abilities non-empty and contains no
     * false-valued members?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return True if the ability is present.
     */
    public boolean hasAbility(String id, FreeColSpecObjectType fcgot,
                              Turn turn) {
        return FeatureContainer.allAbilities(getAbilities(id, fcgot, turn));
    }

    /**
     * Checks if this container contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return first(getAbilities(key, null, null)) != null;
    }

    /**
     * Gets the set of abilities with the given identifier from a
     * container.
     *
     * @param id The object identifier (null matches all).
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return A stream of abilities.
     */
    public Stream<Ability> getAbilities(String id, FreeColSpecObjectType fcgot,
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
            removeInPlace(result, a -> !a.appliesTo(fcgot, turn));
        }
        return result.stream();
    }

    /**
     * Add the given ability to a container.
     *
     * @param ability An {@code Ability} to add.
     * @return True if the {@code Ability} was added.
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
     * @param ability An {@code Ability} to remove.
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
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return A stream of {@code Modifier}s.
     */
    public Stream<Modifier> getModifiers(String id,
                                         FreeColSpecObjectType fcgot,
                                         Turn turn) {
        if (!modifiersPresent()) return Stream.<Modifier>empty();
        Set<Modifier> mset = new HashSet<>();
        synchronized (modifiersLock) {
            if (id == null) {
                for (Set<Modifier> ms : modifiers.values()) mset.addAll(ms);
            } else {
                Set<Modifier> ms = modifiers.get(id);
                if (ms != null) mset.addAll(ms);
            }
        }
        removeInPlace(mset, m -> !m.appliesTo(fcgot, turn));
        return (mset.isEmpty()) ? Stream.<Modifier>empty() : mset.stream();
    }

    /**
     * Applies a collection of modifiers to the given float value.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param mods The {@code Modifier}s to apply.
     * @return The modified number.
     */
    public static float applyModifiers(float number, Turn turn,
                                       Collection<Modifier> mods) {
        return (mods == null || mods.isEmpty()) ? number
            : applyModifiersInternal(number, turn,
                sort(mods, Modifier.ascendingModifierIndexComparator));
    }

    /**
     * Applies a stream of modifiers to the given float value.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param mods The {@code Modifier}s to apply.
     * @return The modified number.
     */
    public static float applyModifiers(float number, Turn turn,
                                       Stream<Modifier> mods) {
        return (mods == null) ? number
            : applyModifiersInternal(number, turn,
                sort(mods, Modifier.ascendingModifierIndexComparator));
    }

    /**
     * Implement applyModifiers.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param mods The {@code Modifier}s to apply.
     * @return The modified number.
     */
    private static float applyModifiersInternal(float number, Turn turn,
                                                Collection<Modifier> mods) {
        float result = number;
        for (Modifier m : mods) {
            float value = m.getValue(turn);
            if (Float.compare(value, Modifier.UNKNOWN) == 0) {
                return value;
            }
            result = m.apply(result, value);
        }
        return result;
    }

    /**
     * Adds a modifier to a container.
     *
     * @param modifier The {@code Modifier} to add.
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
     * @param modifier The {@code Modifier} to remove.
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
     * @param fco The {@code FreeColObject} to add features from.
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
                forEachMapEntry(ca, e -> {
                        Set<Ability> abilitySet = abilities.get(e.getKey());
                        if (abilitySet == null) {
                            abilitySet = new HashSet<>();
                            abilities.put(e.getKey(), abilitySet);
                        }
                        abilitySet.addAll(e.getValue());
                    });
            }
        }

        if (c.modifiersPresent()) {
            requireModifiers();
            HashMap<String, Set<Modifier>> cm;
            synchronized (c.modifiersLock) {
                cm = new HashMap<>(c.modifiers);
            }
            synchronized (modifiersLock) {
                forEachMapEntry(cm, e -> {
                        Set<Modifier> modifierSet = modifiers.get(e.getKey());
                        if (modifierSet == null) {
                            modifierSet = new HashSet<>();
                            modifiers.put(e.getKey(), modifierSet);
                        }
                        modifierSet.addAll(e.getValue());
                    });
            }
        }
    }

    /**
     * Removes all the features in an object from a container.
     *
     * @param fco The {@code FreeColObject} to find features to remove
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
     * Copy anther feature container.
     *
     * @param other The other {@code FeatureContainer}.
     */
    public void copy(FeatureContainer other) {
        clear();
        if (other != null) {
            for (Ability a : other.getAbilityValues()) addAbility(a);
            for (Modifier m : other.getModifierValues()) addModifier(m);
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
     * @param oldSource The old source {@code FreeColSpecObjectType}.
     * @param newSource The new source {@code FreeColSpecObjectType}.
     */
    public void replaceSource(FreeColSpecObjectType oldSource,
                              FreeColSpecObjectType newSource) {
        for (Ability ability : transform(getAbilities(null, null, null),
                a -> oldSource == null || a.getSource() == oldSource)) {
            removeAbility(ability);
            Ability newAbility = new Ability(ability);
            newAbility.setSource(newSource);
            addAbility(newAbility);
        }

        for (Modifier modifier : transform(getModifiers(null, null, null),
                m -> oldSource == null || m.getSource() == oldSource)) {
            removeModifier(modifier);
            Modifier mod = Modifier.makeModifier(modifier);
            mod.setSource(newSource);
            addModifier(mod);
        }
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("[FeatureContainer");
        int siz = sb.length();
        for (Ability ability : iterable(getAbilities(null, null, null))) {
            sb.append(' ').append(ability);
        }
        if (sb.length() > siz) {
            sb.insert(siz, " [abilities");
            sb.append(']');
        }
        siz = sb.length();
        for (Modifier modifier : iterable(getModifiers(null, null, null))) {
            sb.append(' ').append(modifier);
        }
        if (sb.length() > siz) {
            sb.insert(siz, "[modifiers");
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}
