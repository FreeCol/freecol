/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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


public class FeatureContainer {

    private Map<String, Set<Ability>> abilities = new HashMap<String, Set<Ability>>();
    private Map<String, Set<Modifier>> modifiers = new HashMap<String, Set<Modifier>>();

    private Specification specification;


    public FeatureContainer(Specification specification) {
        this.specification = specification;
    }

    public Set<Ability> getAbilities() {
        Set<Ability> result = new HashSet<Ability>();
        for (Set<Ability> abilitySet : abilities.values()) {
            result.addAll(abilitySet);
        }
        return result;
    }

    public Set<Modifier> getModifiers() {
        Set<Modifier> result = new HashSet<Modifier>();
        for (Set<Modifier> modifierSet : modifiers.values()) {
            result.addAll(modifierSet);
        }
        return result;
    }

    /**
     * Returns a Set of Abilities with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>Set<Feature></code> value
     */
    public Set<Ability> getAbilitySet(String id) {
        return getAbilitySet(id, null, null);
    }

    /**
     * Returns a Set of Abilities with the given ID which apply to the
     * given FreeColGameObjectType.
     *
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>Set<Feature></code> value
     */
    public Set<Ability> getAbilitySet(String id, FreeColGameObjectType objectType) {
        return getAbilitySet(id, objectType, null);
    }

    /**
     * Returns a Set of Abilities with the given ID which apply to the
     * given FreeColGameObjectType and Turn.
     *
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>Set<Feature></code> value
     */
    public Set<Ability> getAbilitySet(String id, FreeColGameObjectType objectType, Turn turn) {
        if (specification.getAbilities(id) == null) {
            throw new IllegalArgumentException("Unknown ability key: " + id);
        }
        Set<Ability> abilitySet = abilities.get(id);
        if (abilitySet == null) {
            return new HashSet<Ability>();
        } else {
            Set<Ability> result = new HashSet<Ability>();
            for (Ability ability : abilitySet) {
                if (ability.appliesTo(objectType, turn)) {
                    result.add(ability);
                }
            }
            return result;
        }
    }

    /**
     * Returns true if this Player has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return hasAbility(id, null, null);
    }

    /**
     * Returns true if this Player has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id, FreeColGameObjectType objectType) {
        return hasAbility(id, objectType, null);
    }

    /**
     * Returns true if this Player has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id, FreeColGameObjectType objectType, Turn turn) {
        if (specification.getAbilities(id) == null) {
            throw new IllegalArgumentException("Unknown ability key: " + id);
        }
        Set<Ability> abilitySet = abilities.get(id);
        if (abilitySet == null) {
            return false;
        } else {
            boolean foundApplicableAbility = false;
            for (Ability ability : abilitySet) {
                if (ability.appliesTo(objectType, turn)) {
                    if (ability.getValue()) {
                        foundApplicableAbility = true;
                    } else {
                        return false;
                    }
                }
            }
            return foundApplicableAbility;
        }
    }

    /**
     * Returns true if the given Set of Abilities is not empty and
     * does not contain any Abilities with the value false.
     *
     * @return a <code>boolean</code> value
     */
    public static boolean hasAbility(Set<Ability> abilitySet) {
        if (abilitySet.isEmpty()) {
            return false;
        } else {
            for (Ability ability : abilitySet) {
                if (!ability.getValue()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Returns a Set of Abilities with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>Set<Feature></code> value
     */
    public Set<Modifier> getModifierSet(String id) {
        return getModifierSet(id, null, null);
    }

    /**
     * Returns a Set of Abilities with the given ID which apply to the
     * given FreeColGameObjectType.
     *
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>Set<Feature></code> value
     */
    public Set<Modifier> getModifierSet(String id, FreeColGameObjectType objectType) {
        return getModifierSet(id, objectType, null);
    }

    /**
     * Returns a Set of Abilities with the given ID which apply to the
     * given FreeColGameObjectType and Turn.
     *
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>Set<Feature></code> value
     */
    public Set<Modifier> getModifierSet(String id, FreeColGameObjectType objectType, Turn turn) {
        if (specification.getModifiers(id) == null &&
            specification.getType(id) == null) {
            throw new IllegalArgumentException("Unknown modifier key: " + id);
        }
        Set<Modifier> modifierSet = modifiers.get(id);
        if (modifierSet == null) {
            return new HashSet<Modifier>();
        } else if (objectType == null) {
            return modifierSet;
        } else {
            Set<Modifier> result = new HashSet<Modifier>();
            for (Modifier modifier : modifierSet) {
                if (modifier.appliesTo(objectType, turn)) {
                    result.add(modifier);
                }
            }
            return result;
        }
    }

    /**
     * Applies a Set of Modifiers with the given ID to the given float
     * value.
     *
     * @param number a <code>float</code> value
     * @param id a <code>String</code> value
     */
    public float applyModifier(float number, String id) {
        return applyModifier(number, id, null, null);
    }

    /**
     * Applies a Set of Modifiers with the given ID which match the
     * given FreeColGameObjectType to the given float value.
     *
     * @param number a <code>float</code> value
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     */
    public float applyModifier(float number, String id, FreeColGameObjectType objectType) {
        return applyModifier(number, id, objectType, null);
    }

    /**
     * Applies a Set of Modifiers with the given ID which match the
     * given FreeColGameObjectType and Turn to the given float value.
     *
     * @param number a <code>float</code> value
     * @param id a <code>String</code> value
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     */
    public float applyModifier(float number, String id, FreeColGameObjectType objectType, Turn turn) {
        return applyModifierSet(number, turn, getModifierSet(id, objectType, turn));
    }

    /**
     * Applies a given Set of Modifiers to the given float value.
     *
     * @param number a <code>float</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>float</code> value
     */
    public static float applyModifiers(float number, Turn turn, List<Modifier> modifierSet) {
        if (modifierSet == null) {
            return number;
        }
        float result = number;
        for (Modifier modifier : modifierSet) {
            float value = modifier.getValue();
            if (value == Modifier.UNKNOWN) {
                return Modifier.UNKNOWN;
            }
            if (modifier.hasIncrement() && turn != null) {
                int diff = turn.getNumber() - modifier.getFirstTurn().getNumber();
                switch(modifier.getIncrementType()) {
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
            switch(modifier.getType()) {
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
     * Applies a given Set of Modifiers to the given float value.
     *
     * @param number a <code>float</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>float</code> value
     */
    public static float applyModifierSet(float number, Turn turn, Set<Modifier> modifierSet) {
        if (modifierSet == null) {
            return number;
        }
        float additive = 0, percentage = 0, multiplicative = 1;
        for (Modifier modifier : modifierSet) {
            float value = modifier.getValue();
            if (value == Modifier.UNKNOWN) {
                return Modifier.UNKNOWN;
            }
            if (modifier.hasIncrement() && turn != null) {
                int diff = turn.getNumber() - modifier.getFirstTurn().getNumber();
                switch(modifier.getIncrementType()) {
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
            switch(modifier.getType()) {
            case ADDITIVE:
                additive += value;
                break;
            case MULTIPLICATIVE:
                multiplicative *= value;
                break;
            case PERCENTAGE:
                percentage += value;
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
     * Add the given Ability to the set of Abilities present. If the
     * Ability given can not be combined with a Ability with the same
     * ID already present, the old Ability will be replaced.
     *
     * @param ability a <code>Ability</code> value
     * @return true if the Ability was added
     */
    public boolean addAbility(Ability ability) {
        if (ability == null) {
            return false;
        }
        Set<Ability> abilitySet = abilities.get(ability.getId());
        if (abilitySet == null) {
            abilitySet = new HashSet<Ability>();
            abilities.put(ability.getId(), abilitySet);
        }
        return abilitySet.add(ability);
    }

    /**
     * Add the given Modifier to the set of Modifiers present. If the
     * Modifier given can not be combined with a Modifier with the same
     * ID already present, the old Modifier will be replaced.
     *
     * @param modifier a <code>Modifier</code> value
     * @return true if the Modifier was added
     */
    public boolean addModifier(Modifier modifier) {
        if (modifier == null) {
            return false;
        }
        Set<Modifier> modifierSet = modifiers.get(modifier.getId());
        if (modifierSet == null) {
            modifierSet = new HashSet<Modifier>();
            modifiers.put(modifier.getId(), modifierSet);
        }
        return modifierSet.add(modifier);
    }

    /**
     * Removes and returns a Ability from this ability set.
     *
     * @param oldAbility a <code>Ability</code> value
     * @return a <code>Ability</code> value
     */
    public Ability removeAbility(Ability oldAbility) {
        if (oldAbility == null) {
            return null;
        } else {
            Set<Ability> abilitySet = abilities.get(oldAbility.getId());
            if (abilitySet == null) {
                return null;
            } else if (abilitySet.remove(oldAbility)) {
                return oldAbility;
            } else {
                return null;
            }
        }
    }

    /**
     * Describe <code>removeAbilities</code> method here.
     *
     * @param id a <code>String</code> value
     */
    public void removeAbilities(String id) {
        abilities.remove(id);
    }

    /**
     * Removes and returns a Modifier from this modifier set.
     *
     * @param oldModifier a <code>Modifier</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier removeModifier(Modifier oldModifier) {
        if (oldModifier == null) {
            return null;
        } else {
            Set<Modifier> modifierSet = modifiers.get(oldModifier.getId());
            if (modifierSet == null) {
                return null;
            } else if (modifierSet.remove(oldModifier)) {
                return oldModifier;
            } else {
                return null;
            }
        }
    }

    /**
     * Describe <code>removeModifiers</code> method here.
     *
     * @param id a <code>String</code> value
     */
    public void removeModifiers(String id) {
        modifiers.remove(id);
    }

    /**
     * Describe <code>add</code> method here.
     *
     * @param featureContainer a <code>FeatureContainer</code> value
     */
    public void add(FeatureContainer featureContainer) {
        for (Entry<String, Set<Ability>> entry : featureContainer.abilities.entrySet()) {
            Set<Ability> abilitySet = abilities.get(entry.getKey());
            if (abilitySet == null) {
                abilities.put(entry.getKey(), new HashSet<Ability>(entry.getValue()));
            } else {
                abilitySet.addAll(entry.getValue());
            }
        }
        for (Entry<String, Set<Modifier>> entry : featureContainer.modifiers.entrySet()) {
            Set<Modifier> modifierSet = modifiers.get(entry.getKey());
            if (modifierSet == null) {
                modifiers.put(entry.getKey(), new HashSet<Modifier>(entry.getValue()));
            } else {
                modifierSet.addAll(entry.getValue());
            }
        }
    }

    /**
     * Describe <code>remove</code> method here.
     *
     * @param featureContainer a <code>FeatureContainer</code> value
     */
    public void remove(FeatureContainer featureContainer) {
        for (Entry<String, Set<Ability>> entry : featureContainer.abilities.entrySet()) {
            Set<Ability> abilitySet = abilities.get(entry.getKey());
            if (abilitySet != null) {
                abilitySet.removeAll(entry.getValue());
            }
        }
        for (Entry<String, Set<Modifier>> entry : featureContainer.modifiers.entrySet()) {
            Set<Modifier> modifierSet = modifiers.get(entry.getKey());
            if (modifierSet != null) {
                modifierSet.removeAll(entry.getValue());
            }
        }
    }

    public boolean containsAbilityKey(String key) {
        return abilities.containsKey(key);
    }

    public boolean containsModifierKey(String key) {
        return modifiers.containsKey(key);
    }

    /**
     * Replaces the source field. This is necessary because objects
     * may inherit Features from other, abstract objects.
     *
     * @param oldSource a <code>FreeColGameObjectType</code> value
     * @param newSource a <code>FreeColGameObjectType</code> value
     */
    public void replaceSource(FreeColGameObjectType oldSource, FreeColGameObjectType newSource) {
        for (Ability ability : getAbilities()) {
            if (ability.getSource() == oldSource) {
                ability.setSource(newSource);
            }
        }
        for (Modifier modifier : getModifiers()) {
            if (modifier.getSource() == oldSource) {
                modifier.setSource(newSource);
            }
        }
    }

    /**
     * Debug helper.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[FeatureContainer [abilities");
        for (Ability ability : getAbilities()) {
            result.append(" " + ability.toString());
        }
        result.append("][modifiers");
        for (Modifier modifier : getModifiers()) {
            result.append(" " + modifier.toString());
        }
        result.append("]]");
        return result.toString();
    }
}
