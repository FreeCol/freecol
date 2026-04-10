/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.common;

import java.util.Objects;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.StringTemplate;

/**
 * A translatable exception to be shown to the user.
 */
public final class FreeColUserMessageException extends RuntimeException {

    private final StringTemplate stringTemplate;
    
    
    /**
     * Build a new FreeCol specific exception with the given message.
     *
     * @param message The message for this exception.
     */
    public FreeColUserMessageException(StringTemplate stringTemplate) {
        super(Messages.message(Objects.requireNonNull(stringTemplate)));
        this.stringTemplate = stringTemplate;
    }

    /**
     * Build a new FreeCol specific exception with the given message.
     *
     * @param stringTemplate The message for this exception.
     * @param throwable The {@code Throwable} cause for the exception.
     */
    public FreeColUserMessageException(StringTemplate stringTemplate, Throwable throwable) {
        super(Messages.message(Objects.requireNonNull(stringTemplate)), throwable);
        this.stringTemplate = stringTemplate;
    }

    
    /**
     * Gets the <code>StringTemplate</code> for displaying the exception to the user.
     * @return The <code>StringTemplate</code>.
     */
    public StringTemplate getStringTemplate() {
        return stringTemplate;
    }
}
