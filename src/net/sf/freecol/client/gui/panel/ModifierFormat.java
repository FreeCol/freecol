/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.text.DecimalFormat;
import javax.swing.JLabel;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Turn;


public class ModifierFormat {

    /** Generic unknown result.  TODO: transfer to strings file. */
    private static final String UNKNOWN = "???";

    /** The decimal format to use for Modifiers. */
    private static final DecimalFormat modifierFormat
        = new DecimalFormat("0.00");


    public static final String format(double value) {
        return (value == Modifier.UNKNOWN) ? UNKNOWN
            : modifierFormat.format(value);
    }

    public static final String[] getModifierStrings(Modifier modifier) {
        return getModifierStrings(modifier.getValue(), modifier.getType());
    }

    public static final String[] getModifierStrings(float value,
                                                    ModifierType type) {
        String bonus = modifierFormat.format(value);
        if (value == Modifier.UNKNOWN) {
            return new String[] { " ", bonus, null };
        }
        String[] result = (value < 0)
            ? new String[] { "-", bonus.substring(1), null }
            : new String[] { "+", bonus, null };
        switch (type) {
        case MULTIPLICATIVE:
            // assumes multiplicative modifiers will never be negative
            result[0] = "\u00D7";
            break;
        case PERCENTAGE:
            result[2] = "%";
            break;
        default:
            break;
        }
        return result;
    }

    private static String getSourceName(FreeColObject source) {
        if (source == null) return UNKNOWN;

        String result = null;
        if (result == null && source instanceof Nameable) {
            result = ((Nameable)source).getName();
            if ("".equals(result)) result = null;
        }
        if (result == null && source instanceof Named) {
            result = Messages.message(((Named)source).getNameKey());
            if ("".equals(result)) result = null;
        }
        if (result == null) result = Messages.getName(source);
        return result;
    }

    public static JLabel[] getModifierLabels(Modifier modifier,
                                             FreeColGameObjectType fcgot,
                                             Turn turn) {
        String sourceName = getSourceName(modifier.getSource());
        if (fcgot != null) {
            for (Scope scope : modifier.getScopes()) {
                if (scope.appliesTo(fcgot)) {
                    sourceName += " (" + Messages.message(fcgot.getNameKey())
                        + ")";
                }
            }
        }
        float value = modifier.getValue(turn);
        String[] bonus = getModifierStrings(value, modifier.getType());
        JLabel[] result = new JLabel[3];
        result[0] = new JLabel(sourceName);
        result[1] = new JLabel(bonus[0] + bonus[1]);
        result[2] = (bonus[2] == null) ? null : new JLabel(bonus[2]);
        return result;
    }

    public static String getFeatureAsString(Feature feature) {
        String label = Messages.getName(feature) + ":";
        if (feature.hasScope()) {
            for (Scope scope : feature.getScopes()) {
                String key = null;
                if (scope.getType() != null) {
                    key = scope.getType();
                } else if (scope.getAbilityId() != null) {
                    key = scope.getAbilityId();
                } else if (scope.getMethodName() != null) {
                    key = "model.scope." + scope.getMethodName();
                }
                if (key != null) {
                    label += (scope.isMatchNegated() ? " !" : " ")
                        + Messages.message(key + ".name") + ",";
                }
            }
        }
        return label.substring(0, label.length() - 1);
    }

    public static String getModifierAsString(Modifier modifier) {
        String result = "";
        for (String string : getModifierStrings(modifier)) {
            if (string != null) {
                result += string;
            }
        }
        return result;
    }
}
