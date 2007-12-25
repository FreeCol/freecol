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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.client.gui.i18n.Messages;

public class FeatureContainer implements Features {

    private Map<String, Feature> features = new HashMap<String, Feature>();

    /**
     * Returns a copy of this Type's features.
     *
     * @return a <code>List</code> value
     */
    public List<Feature> getFeatures() {
        return new ArrayList<Feature>(features.values());
    }

    /**
     * Returns true if this Player has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return features.containsKey(id) && 
            features.get(id) instanceof Ability &&
            ((Ability) features.get(id)).getValue();
    }

    /**
     * Get the <code>Ability</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Ability</code> value
     */
    public final Ability getAbility(String id) {
        return (Ability) features.get(id);
    }

    /**
     * Get the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public final Modifier getModifier(String id) {
        return (Modifier) features.get(id);
    }

    /**
     * Add the given Feature to the set of Features present. If the
     * Feature given can not be combined with a Feature with the same
     * ID already present, the old Feature will be replaced.
     *
     * @param feature a <code>Feature</code> value
     */
    public void addFeature(Feature feature) {
        if (feature == null) {
            return;
        }
        Feature oldValue = features.get(feature.getId());
        if (oldValue instanceof Modifier && feature instanceof Modifier) {
            features.put(feature.getId(), Modifier.combine((Modifier) oldValue, (Modifier) feature));
        } else if (oldValue instanceof Ability && feature instanceof Ability) {
            features.put(feature.getId(), Ability.combine((Ability) oldValue, (Ability) feature));
        } else {
            features.put(feature.getId(), feature);
        }
    }

    /**
     * Removes and returns a Feature from this feature set.
     *
     * @param oldFeature a <code>Feature</code> value
     * @return a <code>Feature</code> value
     */
    public Feature removeFeature(Feature oldFeature) {
        Feature feature = features.get(oldFeature.getId());
        if (feature == null) {
            return null;
        } else if (feature == oldFeature) {
            features.remove(feature.getId());
        } else if (feature instanceof Modifier && oldFeature instanceof Modifier) {
            ((Modifier) feature).remove((Modifier) oldFeature);
        } else if (feature instanceof Ability && oldFeature instanceof Ability) {
            ((Ability) feature).remove((Ability) oldFeature);
        }
        return null;
    }
}
