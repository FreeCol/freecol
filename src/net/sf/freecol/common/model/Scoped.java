/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import java.util.List;
import java.util.stream.Stream;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * How scopes are handled.
 */
public interface Scoped {

    /**
     * Get the scopes applicable to this effect.
     *
     * @return A list of {@code Scope}s.
     */
    public List<Scope> getScopeList();

    /**
     * Get the scopes applicable to this effect as a stream.
     *
     * @return A stream of {@code Scope}s.
     */
    public Stream<Scope> getScopes();
    
    /**
     * Set the scopes for this object.
     *
     * @param scopes A list of new {@code Scope}s.
     */
    public void setScopes(List<Scope> scopes);

    /**
     * Add a scope.
     *
     * @param scope The {@code Scope} to add.
     */
    public void addScope(Scope scope);

    /**
     * Does at least one of this effect's scopes apply to an object.
     *
     * @param object The {@code FreeColObject} to check.
     * @return True if this effect applies.
     */
    default boolean appliesTo(FreeColObject object) {
        List<Scope> scopes = getScopeList();
        return (scopes == null || scopes.isEmpty()) ? true
            : any(scopes, s -> s.appliesTo(object));
    }
}
