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

package net.sf.freecol.client.gui.panel;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import net.sf.freecol.common.i18n.Messages;
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

    /** The decimal format to use for Modifiers. */
    private static final DecimalFormat modifierFormat
        = new DecimalFormat("0.00");


    public static final String getUnknownValue() {
        return Messages.message("modifierFormat.unknown");
    }

    public static final String format(double value) {
        return (value == Modifier.UNKNOWN)
            ? getUnknownValue()
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
        if (source == null) return getUnknownValue();

        String result = null;
        if (result == null && source instanceof Nameable) {
            result = ((Nameable)source).getName();
            if (result != null && result.isEmpty()) result = null;
        }
        if (result == null && source instanceof Named) {
            result = Messages.getName((Named)source);
            if (result != null && result.isEmpty()) result = null;
        }
        if (result == null) result = Messages.getName(source.getId());
        return result;
    }

    public static JLabel[] getModifierLabels(Modifier modifier,
                                             FreeColGameObjectType fcgot,
                                             Turn turn) {
        String sourceName = getSourceName(modifier.getSource());
        if (fcgot != null && modifier.appliesTo(fcgot)) {
            sourceName += " (" + Messages.getName(fcgot) + ")";
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
        return Messages.getName(feature) + ":"
            + ((!feature.hasScope()) ? ""
                : feature.getScopes().stream().map(scope -> {
                        String k = scope.getKey();
                        if (k == null) {
                            k = "";
                        } else {
                            k = Messages.getName(k);
                            if (scope.isMatchNegated()) k = "!" + k;
                        }
                        return k;
                    }).collect(Collectors.joining()));
    }

    public static String getModifierAsString(Modifier modifier) {
        return Arrays.stream(getModifierStrings(modifier))
            .filter(s -> s != null).collect(Collectors.joining());
    }
}
