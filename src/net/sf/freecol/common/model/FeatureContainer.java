/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;


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

    /** The abilities in the container. */
    private Map<String, Set<Ability>> abilities = null;

    /** The modifiers in the container. */
    private Map<String, Set<Modifier>> modifiers = null;


    /**
     * Have the abilities map been created?
     *
     * @return True if the abilities are present.
     */
    private synchronized boolean abilitiesPresent() {
        return abilities != null;
    }

    /**
     * On demand creation of the abilities map.
     */
    private synchronized void requireAbilities() {
        if (abilities == null) abilities = new HashMap<String, Set<Ability>>();
    }

    /**
     * Get a copy of the ability entry set.
     *
     * @return A copy of the ability entry set.
     */
    private Set<Entry<String, Set<Ability>>> getAbilityEntries() {
        if (!abilitiesPresent()) return null;
        synchronized (abilities) {
            return new HashSet<Entry<String,
                                     Set<Ability>>>(abilities.entrySet());
        }
    }

    /**
     * Have the modifiers map been created?
     *
     * @return True if the modifiers are present.
     */
    private synchronized boolean modifiersPresent() {
        return modifiers != null;
    }

    /**
     * On demand creation of the modifiers map.
     */
    private synchronized void requireModifiers() {
        if (modifiers == null) modifiers = new HashMap<String, Set<Modifier>>();
    }

    /**
     * Get a copy of the modifier entry set.
     *
     * @return A copy of the modifier entry set.
     */
    private Set<Entry<String, Set<Modifier>>> getModifierEntries() {
        if (!modifiersPresent()) return null;
        synchronized (modifiers) {
            return new HashSet<Entry<String,
                                     Set<Modifier>>>(modifiers.entrySet());
        }
    }


    /**
     * Is the given set of abilities non-empty and contains no
     * false-valued members?
     *
     * @return True if the ability set is `satisfied'.
     */
    public static boolean hasAbility(Set<Ability> abilitySet) {
        if (abilitySet == null || abilitySet.isEmpty()) return false;
        for (Ability ability : abilitySet) {
            if (!ability.getValue()) return false;
        }
        return true;
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
        return FeatureContainer.hasAbility(getAbilitySet(id, fcgot, turn));
    }

    /**
     * Checks if this container contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return !getAbilitySet(key, null, null).isEmpty();
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
    public Set<Ability> getAbilitySet(String id, FreeColGameObjectType fcgot,
                                      Turn turn) {
        Set<Ability> result = new HashSet<Ability>();
        if (!abilitiesPresent()) return result;

        synchronized (abilities) {
            Set<Ability> abilitySet;
            if (id == null) {
                abilitySet = new HashSet<Ability>();
                for (Set<Ability> aset : abilities.values()) {
                    abilitySet.addAll(aset);
                }
            } else {
                if ((abilitySet = abilities.get(id)) == null) return result;
            }

            for (Ability a : abilitySet) {
                if (a.appliesTo(fcgot, turn)) result.add(a);
            }
        }
        return result;
    }

    /**
     * Add the given ability to a container.
     *
     * @param fc The <code>FeatureContainer</code> to add to.
     * @param ability An <code>Ability</code> to add.
     * @return True if the Ability was added.
     */
    public boolean addAbility(Ability ability) {
        if (ability == null) return false;

        requireAbilities();
        synchronized (abilities) {
            Set<Ability> abilitySet = abilities.get(ability.getId());
            if (abilitySet == null) {
                abilitySet = new HashSet<Ability>();
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

        synchronized (abilities) {
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

        synchronized (abilities) {
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
    public Set<Modifier> getModifierSet(String id,
        FreeColGameObjectType fcgot, Turn turn) {
        Set<Modifier> result = new HashSet<Modifier>();
        if (!modifiersPresent()) return result;

        synchronized (modifiers) {
            Set<Modifier> modifierSet;
            if (id == null) {
                modifierSet = new HashSet<Modifier>();
                for (Set<Modifier> mset : modifiers.values()) {
                    modifierSet.addAll(mset);
                }
            } else {
                if ((modifierSet = modifiers.get(id)) == null) return result;
            }
            
            for (Modifier m : modifierSet) {
                if (m.appliesTo(fcgot, turn)) result.add(m);
            }
        }
        return result;
    }

    /**
     * Applies the modifiers with the given identifier to the given number.
     *
     * @param fc The <code>FeatureContainer</code> to query.
     * @param number The number to modify.
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return The modified number.
     */
    public static float applyModifier(FeatureContainer fc,
                                      float number, String id,
                                      FreeColGameObjectType fcgot, Turn turn) {
        return (fc == null || !fc.modifiersPresent()) ? number
            : fc.applyModifier(number, id, fcgot, turn);
    }

    /**
     * Applies the modifiers with the given identifier to the given number.
     *
     * @param number The number to modify.
     * @param id The object identifier.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return The modified number.
     */
    public float applyModifier(float number, String id,
                               FreeColGameObjectType fcgot, Turn turn) {
        return applyModifierSet(number, turn, getModifierSet(id, fcgot, turn));
    }

    /**
     * Applies a list of modifiers to the given float value.
     *
     * This routine is required for Col1-compatibility throughout the
     * production code, almost all other code should/does call
     * applyModifierSet().  The algorithms are quite similar, but different,
     * which is unsatisfactory, but appears to be necessary.
     *
     * @param number The number to modify.
     * @param turn An optional applicable <code>Turn</code>.
     * @param modifiers The <code>Modifier</code>s to apply.
     * @return The modified number.
     */
    public static float applyModifiers(float number, Turn turn,
                                       List<Modifier> mods) {
        if (mods == null || mods.isEmpty()) return number;
        Collections.sort(mods);
        float result = number;
        for (Modifier m : mods) {
            float value = m.getValue(turn);
            if (value == Modifier.UNKNOWN) return value;
            result = m.apply(result, value);
        }
        return result;
    }

    /**
     * Applies a set of modifiers to the given float value.
     *
     * Use this generally.  Only use applyModifiers in the production code.
     *
     * @param number The number to modify.
     * @param turn An optional applicable <code>Turn</code>.
     * @param modifiers The <code>Modifier</code>s to apply.
     * @return The modified number.
     */
    public static float applyModifierSet(float number, Turn turn,
                                         Set<Modifier> mods) {
        float additive = 0, percentage = 0, multiplicative = 1;
        for (Modifier m : mods) {
            float value = m.getValue(turn);
            if (value == Modifier.UNKNOWN) return Modifier.UNKNOWN;
            switch (m.getType()) {
            case ADDITIVE:
                additive += value;
                break;
            case MULTIPLICATIVE:
                multiplicative *= value;
                break;
            case PERCENTAGE:
                percentage += value;
                // If we want cumulative percentage modifiers:
                //   percentage += (percentage * value) / 100 + value;
                break;
            }
        }
        float result = number;
        result += additive;
        result *= multiplicative;
        result += (result * percentage) / 100;
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
        synchronized (modifiers) {
            Set<Modifier> modifierSet = modifiers.get(modifier.getId());
            if (modifierSet == null) {
                modifierSet = new HashSet<Modifier>();
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

        synchronized (modifiers) {
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

        synchronized (modifiers) {
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

        Set<Entry<String, Set<Ability>>> ca = c.getAbilityEntries();
        if (ca != null) {
            requireAbilities();
            synchronized (abilities) {
                for (Entry<String, Set<Ability>> entry : ca) {
                    Set<Ability> abilitySet = abilities.get(entry.getKey());
                    if (abilitySet == null) {
                        abilities.put(entry.getKey(),
                            new HashSet<Ability>(entry.getValue()));
                    } else {
                        abilitySet.addAll(entry.getValue());
                    }
                }
            }
        }

        Set<Entry<String, Set<Modifier>>> cm = c.getModifierEntries();
        if (cm != null) {
            requireModifiers();
            synchronized (modifiers) {
                for (Entry<String, Set<Modifier>> entry : cm) {
                    Set<Modifier> modifierSet = modifiers.get(entry.getKey());
                    if (modifierSet == null) {
                        modifiers.put(entry.getKey(),
                            new HashSet<Modifier>(entry.getValue()));
                    } else {
                        modifierSet.addAll(entry.getValue());
                    }
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

        Set<Entry<String, Set<Ability>>> ca = c.getAbilityEntries();
        if (ca != null && abilitiesPresent()) {
            synchronized (abilities) {
                for (Entry<String, Set<Ability>> entry : ca) {
                    Set<Ability> abilitySet = abilities.get(entry.getKey());
                    if (abilitySet == null) continue;
                    for (Ability a : new HashSet<Ability>(abilitySet)) {
                        if (a.getSource() == fco) {
                            abilitySet.remove(a);
System.err.println("REMOVED ABILITY " +a + " of " + fco + " from " + this);
                        }
                    }
                }
            }
        }

        Set<Entry<String, Set<Modifier>>> cm = c.getModifierEntries();
        if (cm != null && modifiersPresent()) {
            synchronized (modifiers) {
                for (Entry<String, Set<Modifier>> entry : cm) {
                    Set<Modifier> modifierSet = modifiers.get(entry.getKey());
                    if (modifierSet == null) continue;
                    for (Modifier m : new HashSet<Modifier>(modifierSet)) {
                        if (m.getSource() == fco) {
                            modifierSet.remove(m);
System.err.println("REMOVED MODIFIER " + m + " of " + fco + " from " + this);
                        }
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
            synchronized (abilities) {
                abilities.clear();
            }
        }
        if (modifiersPresent()) {
            synchronized (modifiers) {
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
        for (Ability ability : getAbilitySet(null, null, null)) {
            if (oldSource == null || ability.getSource() == oldSource) {
                removeAbility(ability);
                Ability newAbility = new Ability(ability);
                newAbility.setSource(newSource);
                addAbility(newAbility);
            }
        }

        for (Modifier modifier : getModifierSet(null, null, null)) {
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
        sb.append("[FeatureContainer [abilities");
        for (Ability ability : getAbilitySet(null, null, null)) {
            sb.append(" ").append(ability.toString());
        }
        sb.append("] [modifiers");
        for (Modifier modifier : getModifierSet(null, null, null)) {
            sb.append(" ").append(modifier.toString());
        }
        sb.append("]]");
        return sb.toString();
    }
}
