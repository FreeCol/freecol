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

package net.sf.freecol.client.gui;

import java.text.DecimalFormat;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FreeColSpecObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Turn;
import static net.sf.freecol.common.util.CollectionUtils.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class ModifierFormat {

    /** The decimal format to use for Modifiers. */
    private static final DecimalFormat modifierFormat
        = new DecimalFormat("0.00");


    public static final String getUnknownValue() {
        return Messages.message("modifierFormat.unknown");
    }

    public static final String format(float value) {
        return Modifier.isFloatKnown(value) ? modifierFormat.format(value)
            : getUnknownValue();
    }

    private static final String[] getModifierStrings(Modifier modifier) {
        return getModifierStrings(modifier.getValue(), modifier.getType());
    }

    private static final String[] getModifierStrings(float value,
                                                     ModifierType type) {
        String bonus = modifierFormat.format(value);
        if (!Modifier.isFloatKnown(value)) {
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

    @SuppressFBWarnings(value="NP_LOAD_OF_KNOWN_NULL_VALUE")
    private static String getSourceName(FreeColObject source) {
        if (source == null) return getUnknownValue();

        String result = null;
        if (result == null && source instanceof Nameable) {
            result = ((Nameable)source).getName();
            if (result != null && result.isEmpty()) result = null;
        }
        if (result == null && source instanceof Named) {
            result = Messages.getName((Named)source);
            if (result.isEmpty()) result = null;
        }
        if (result == null) result = Messages.getName(source.getId());
        return result;
    }

    public static JLabel[] getModifierLabels(Modifier modifier,
                                             FreeColSpecObjectType fcgot,
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
                : transform(feature.getScopes(), isNotNull(),
                            Scope::getFeatureString, Collectors.joining(",")));
    }

    public static String getModifierAsString(Modifier modifier) {
        return transform(getModifierStrings(modifier), isNotNull(),
                         Function.<String>identity(), Collectors.joining());
    }
}
