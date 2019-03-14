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

package net.sf.freecol.common.i18n;

import java.util.List;


/**
 * A grammatical relationship.
 */
public class Relation {

    int low, high, mod = 1;
    boolean negated = false;
    boolean integer = true;

    public Relation(List<String> tokens) {
        parse(tokens);
    }

    public Relation(int low, int high) {
        this.low = low;
        this.high = high;
    }

    /**
     * Sets the divisor for a modulo operation (defaults to 1).
     *
     * @param mod an {@code int} value
     */
    public void setMod(int mod) {
        this.mod = mod;
    }

    /**
     * Negates the return value of the relation (defaults to false).
     *
     * @param value a {@code boolean} value
     */
    public void setNegated(boolean value) {
        this.negated = value;
    }

    /**
     * Determines whether this relation only matches integers
     * (defaults to true).
     *
     * @param value a {@code boolean} value
     */
    public void setInteger(boolean value) {
        this.integer = value;
    }

    /**
     * Returns true if the given number matches this relation.
     *
     * @param number a {@code double} value
     * @return a {@code boolean} value
     */
    public boolean matches(double number) {
        double value = (mod == 1) ? number : number % mod;
        if (integer && Double.compare(value, Math.rint(value)) != 0) {
            return false;
        }
        return (low <= value && value <= high) != negated;
    }

    /**
     * Parses a list of string tokens.
     *
     * @param input a list of string tokens
     */
    private void parse(List<String> input) {
        String token = input.remove(0);
        if ("n".equals(token)) {
            token = input.remove(0);
        } else {
            throw new RuntimeException("Relation must start with 'n': " + token);
        }
        if ("mod".equals(token)) {
            mod = Integer.parseInt(input.remove(0));
            token = input.remove(0);
        }
        if ("not".equals(token)) {
            negated = true;
            token = input.remove(0);
        }
        if ("is".equals(token)) {
            token = input.remove(0);
            if ("not".equals(token)) {
                negated = true;
                token = input.remove(0);
            }
            low = high = Integer.parseInt(token);
        } else {
            if ("within".equals(token)) {
                integer = false;
            }
            low = Integer.parseInt(input.remove(0));
            high = Integer.parseInt(input.remove(0));
        }
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Relation) {
            Relation other = (Relation)o;
            return this.low == other.low && this.high == other.high
                && this.mod == other.mod
                && this.negated == other.negated
                && this.integer == other.integer;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + this.low;
        hash = 31 * hash + this.high;
        hash = 31 * hash + this.mod;
        hash = 31 * hash + ((this.negated) ? 1 : 0)
            + ((this.integer) ? 2 : 0);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("n ");
        if (mod != 1) {
            sb.append("mod ").append(mod).append(' ');
        }
        if (low == high) {
            sb.append("is ");
            if (negated) sb.append("not ");
            sb.append(low);
        } else {
            if (negated) sb.append("not ");
            if (!integer) sb.append("with");
            sb.append("in ").append(low).append("..").append(high);
        }
        return sb.toString();
    }
}
