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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;


public class FeatureContainer {

    private static final Logger logger = Logger.getLogger(FeatureContainer.class.getName());

    /** The abilities in the container. */
    private Map<String, Set<Ability>> abilities
        = new HashMap<String, Set<Ability>>();

    /** The modifiers in the container. */
    private Map<String, Set<Modifier>> modifiers
        = new HashMap<String, Set<Modifier>>();


    /**
     * Is an ability present in this container?
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the ability is present.
     */
    public static boolean hasAbility(FeatureContainer fc, String id,
                                     FreeColGameObjectType fcgot, Turn turn) {
        if (fc == null) return false;
        Set<Ability> abilitySet = fc.abilities.get(id);
        if (abilitySet == null) return false;
        for (Ability ability : abilitySet) {
            if (ability.appliesTo(fcgot, turn)
                && !ability.getValue()) return false;
        }
        return true;
    }

    /**
     * Is the given set of abilities non-empty and does not contain
     * any false-valued members.
     *
     * @return True if the ability set is `satisfied'.
     */
    public static boolean hasAbility(Set<Ability> abilitySet) {
        if (abilitySet.isEmpty()) return false;
        for (Ability ability : abilitySet) {
            if (!ability.getValue()) return false;
        }
        return true;
    }

    /**
     * Checks if this container contains a given ability key.
     *
     * @param fc The <code>FeatureContainer</code> to get abilities from.
     * @param key The key to check.
     * @return True if the key is present.
     */
    public static boolean containsAbilityKey(FeatureContainer fc, String key) {
        return (fc == null) ? false : fc.abilities.containsKey(key);
    }

    /**
     * Gets a copy of the abilities of a container.
     *
     * @param fc The <code>FeatureContainer</code> to get abilities from.
     * @return A set of abilities.
     */
    public static Set<Ability> getAbilities(FeatureContainer fc) {
        Set<Ability> result = new HashSet<Ability>();
        if (fc != null) {
            for (Set<Ability> abilitySet : fc.abilities.values()) {
                result.addAll(abilitySet);
            }
        }
        return result;
    }

    /**
     * Gets the set of abilities with the given Id from a container.
     *
     * @param fc The <code>FeatureContainer</code> to query.
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of abilities.
     */
    public static Set<Ability> getAbilitySet(FeatureContainer fc, String id,
                                             FreeColGameObjectType fcgot,
                                             Turn turn) {
        Set<Ability> result = new HashSet<Ability>();
        if (fc != null) {
            Set<Ability> abilitySet = fc.abilities.get(id);
            if (abilitySet != null) {
                for (Ability ability : abilitySet) {
                    if (ability.appliesTo(fcgot, turn)) result.add(ability);
                }
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
    public static boolean addAbility(FeatureContainer fc, Ability ability) {
        if (fc == null || ability == null) return false;
        Set<Ability> abilitySet = fc.abilities.get(ability.getId());
        if (abilitySet == null) {
            abilitySet = new HashSet<Ability>();
            fc.abilities.put(ability.getId(), abilitySet);
        }
        return abilitySet.add(ability);
    }

    /**
     * Remove the given ability from a container.
     *
     * @param fc The <code>FeatureContainer</code> to remove from.
     * @param ability An <code>Ability</code> to remove.
     * @return The ability removed.
     */
    public static Ability removeAbility(FeatureContainer fc, Ability ability) {
        if (fc == null || ability == null) return null;
        Set<Ability> abilitySet = fc.abilities.get(ability.getId());
        return (abilitySet == null || !abilitySet.remove(ability)) ? null
            : ability;
    }

    /**
     * Remove all abilities with a given Id.
     *
     * @param fc The <code>FeatureContainer</code> to remove abilities from.
     * @param id The id of the abilities to remove.
     */
    public static void removeAbilities(FeatureContainer fc, String id) {
        if (fc != null) fc.abilities.remove(id);
    }


    /**
     * Checks if this container contains a given modifier key.
     *
     * @param fc The <code>FeatureContainer</code> to get abilities from.
     * @param key The key to check.
     * @return True if the key is present.
     */
    public static boolean containsModifierKey(FeatureContainer fc, String key) {
        return (fc == null) ? false : fc.modifiers.containsKey(key);
    }

    /**
     * Gets a copy of the modifiers in a container.
     *
     * @param fc The <code>FeatureContainer</code> to get modifiers from.
     * @return A set of modifiers.
     */
    public static Set<Modifier> getModifiers(FeatureContainer fc) {
        Set<Modifier> result = new HashSet<Modifier>();
        if (fc != null) {
            for (Set<Modifier> modifierSet : fc.modifiers.values()) {
                result.addAll(modifierSet);
            }
        }
        return result;
    }

    /**
     * Gets the set of modifiers with the given Id from a container.
     *
     * @param fc The <code>FeatureContainer</code> to get modifiers from.
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public static Set<Modifier> getModifierSet(FeatureContainer fc, String id,
                                               FreeColGameObjectType fcgot,
                                               Turn turn) {
        return (fc == null) ? new HashSet<Modifier>()
            : fc.getModifierSet(id, fcgot, turn);
    }

    /**
     * Gets the set of modifiers with the given Id from this container.
     *
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifierSet(String id,
                                        FreeColGameObjectType fcgot,
                                        Turn turn) {
        Set<Modifier> result = new HashSet<Modifier>();
        Set<Modifier> modifierSet = modifiers.get(id);
        if (modifierSet != null) {
            if (fcgot == null) {
                result.addAll(modifierSet);
            } else {
                for (Modifier modifier : modifierSet) {
                    if (modifier.appliesTo(fcgot, turn)) result.add(modifier);
                }
            }
        }
        return result;
    }

    /**
     * Applies the modifiers with the given Id to the given number.
     *
     * @param fc The <code>FeatureContainer</code> to query.
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return The modified number.
     */
    public static float applyModifier(FeatureContainer fc,
                                      float number, String id,
                                      FreeColGameObjectType fcgot, Turn turn) {
        return (fc == null) ? number
            : fc.applyModifier(number, id, fcgot, turn);
    }

    /**
     * Applies the modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return The modified number.
     */
    public float applyModifier(float number, String id,
                               FreeColGameObjectType fcgot, Turn turn) {
        return applyModifierSet(number, turn,
                                getModifierSet(id, fcgot, turn));
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
                                       List<Modifier> modifierSet) {
        if (modifierSet == null || modifierSet.isEmpty()) return number;
        float result = number;
        for (Modifier modifier : modifierSet) {
            float value = modifier.getValue();
            if (value == Modifier.UNKNOWN) return Modifier.UNKNOWN;
            if (modifier.hasIncrement() && turn != null) {
                int diff = turn.getNumber()
                    - modifier.getFirstTurn().getNumber();
                switch (modifier.getIncrementType()) {
                case ADDITIVE:
                    value += modifier.getIncrement() * diff;
                    break;
                case MULTIPLICATIVE:
                    value *= modifier.getIncrement() * diff;
                    break;
                case PERCENTAGE:
                    value += (value * modifier.getIncrement() * diff) / 100;
                    break;
                }
            }
            switch (modifier.getType()) {
            case ADDITIVE:
                result += value;
                break;
            case MULTIPLICATIVE:
                result *= value;
                break;
            case PERCENTAGE:
                result += (result * value) / 100;
                break;
            }
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
                                         Set<Modifier> modifiers) {
        float additive = 0, percentage = 0, multiplicative = 1;
        for (Modifier modifier : modifiers) {
            float value = modifier.getValue();
            if (value == Modifier.UNKNOWN) return Modifier.UNKNOWN;
            if (modifier.hasIncrement() && turn != null) {
                int diff = turn.getNumber()
                    - modifier.getFirstTurn().getNumber();
                switch (modifier.getIncrementType()) {
                case ADDITIVE:
                    value += modifier.getIncrement() * diff;
                    break;
                case MULTIPLICATIVE:
                    value *= modifier.getIncrement() * diff;
                    break;
                case PERCENTAGE:
                    value += (value * modifier.getIncrement() * diff) / 100;
                    break;
                }
            }
            switch (modifier.getType()) {
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
     * @param fc The <code>FeatureContainer</code> to add to.
     * @param modifier The <code>Modifier</code> to add.
     * @return True if the modifier was added.
     */
    public static boolean addModifier(FeatureContainer fc,
                                      Modifier modifier) {
        if (fc == null || modifier == null) return false;
        Set<Modifier> modifierSet = fc.modifiers.get(modifier.getId());
        if (modifierSet == null) {
            modifierSet = new HashSet<Modifier>();
            fc.modifiers.put(modifier.getId(), modifierSet);
        }
        return modifierSet.add(modifier);
    }

    /**
     * Removes a modifier from a container.
     *
     * @param fc The <code>FeatureContainer</code> to remove from.
     * @param modifier The <code>Modifier</code> to remove.
     * @return The modifier removed.
     */
    public static Modifier removeModifier(FeatureContainer fc,
                                          Modifier modifier) {
        if (fc == null || modifier == null) return null;
        Set<Modifier> modifierSet = fc.modifiers.get(modifier.getId());
        return (modifierSet == null || !modifierSet.remove(modifier)) ? null
            : modifier;
    }

    /**
     * Removes all modifiers with a given Id.
     *
     * @param fc The <code>FeatureContainer</code> to remove from.
     * @param id The Id of the modifiers to remove.
     */
    public static void removeModifiers(FeatureContainer fc, String id) {
        if (fc != null) fc.modifiers.remove(id);
    }


    /**
     * Adds all the features in an object to this object.
     *
     * @param fc The <code>FreeColObject</code> to add features from.
     * @param fco The <code>FreeColObject</code> to add features from.
     */
    public static void addFeatures(FeatureContainer fc, FreeColObject fco) {
        FeatureContainer c = fco.getFeatureContainer();
        if (fc != null && c != null) {
            for (Entry<String, Set<Ability>> entry : c.abilities.entrySet()) {
                Set<Ability> abilitySet = fc.abilities.get(entry.getKey());
                if (abilitySet == null) {
                    fc.abilities.put(entry.getKey(),
                                     new HashSet<Ability>(entry.getValue()));
                } else {
                    abilitySet.addAll(entry.getValue());
                }
            }
            for (Entry<String, Set<Modifier>> entry : c.modifiers.entrySet()) {
                Set<Modifier> modifierSet = fc.modifiers.get(entry.getKey());
                if (modifierSet == null) {
                    fc.modifiers.put(entry.getKey(),
                                     new HashSet<Modifier>(entry.getValue()));
                } else {
                    modifierSet.addAll(entry.getValue());
                }
            }
        }
    }

    /**
     * Removes all the features in an object from a container.
     *
     * @param fc The <code>FeatureContainer</code> to remove features from.
     * @param fco The <code>FreeColObject</code> to find features to remove
     *     in.
     */
    public static void removeFeatures(FeatureContainer fc, FreeColObject fco) {
        FeatureContainer c = fco.getFeatureContainer();
        if (fc != null && c != null) {
            for (Entry<String, Set<Ability>> entry : c.abilities.entrySet()) {
                Set<Ability> abilitySet = fc.abilities.get(entry.getKey());
                if (abilitySet != null) {
                    abilitySet.removeAll(entry.getValue());
                }
            }
            for (Entry<String, Set<Modifier>> entry : c.modifiers.entrySet()) {
                Set<Modifier> modifierSet = fc.modifiers.get(entry.getKey());
                if (modifierSet != null) {
                    modifierSet.removeAll(entry.getValue());
                }
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
        for (Ability ability : getAbilities(this)) {
            if (ability.getSource() == oldSource) {
                removeAbility(this, ability);
                Ability newAbility = new Ability(ability);
                newAbility.setSource(newSource);
                addAbility(this, newAbility);
            }
        }
        for (Modifier modifier : getModifiers(this)) {
            if (modifier.getSource() == oldSource) {
                removeModifier(this, modifier);
                Modifier newModifier = new Modifier(modifier);
                newModifier.setSource(newSource);
                addModifier(this, newModifier);
            }
        }
    }

    /**
     * Debug helper.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[FeatureContainer [abilities");
        for (Ability ability : getAbilities(this)) {
            result.append(" " + ability.toString());
        }
        result.append("][modifiers");
        for (Modifier modifier : getModifiers(this)) {
            result.append(" " + modifier.toString());
        }
        result.append("]]");
        return result.toString();
    }
}
