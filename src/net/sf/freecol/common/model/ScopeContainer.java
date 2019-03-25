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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Scope;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * How scopes are handled.
 */
public class ScopeContainer {

    /** Cached empty scope stream. */
    private static final Stream<Scope> noStream = Stream.<Scope>empty();
    /** Cached empty scope list. */
    private static final List<Scope> noList = Collections.<Scope>emptyList();
    /** Cached standard scope comparator. */
    private static final Comparator<Scope> scopeComparator
        = new Comparator<Scope>() {
                public int compare(Scope s1, Scope s2) {
                    return Long.compare(s1.hashCode(), s2.hashCode());
                }
            };

    /** The scopes. */
    private List<Scope> scopes;


    /**
     * Trivial constructor.
     */
    public ScopeContainer() {
        this.scopes = null;
    }
    

    /**
     * Is this container empty?
     *
     * @return True if no scopes are present.
     */
    public final boolean isEmpty() {
        return this.scopes == null || this.scopes.isEmpty();
    }

    /**
     * Get the scopes applicable to this effect.
     *
     * @return A list of {@code Scope}s.
     */
    public final List<Scope> getList() {
        return (this.isEmpty()) ? noList : this.scopes;
    }

    /**
     * Get the scopes applicable to this effect as a stream.
     *
     * @return A stream of {@code Scope}s.
     */
    public final Stream<Scope> get() {
        return (this.isEmpty()) ? noStream : this.scopes.stream();
    }
    
    /**
     * Set the scopes for this object.
     *
     * @param scopes A list of new {@code Scope}s.
     */
    public final void set(List<Scope> scopes) {
        if (this.scopes == null || scopes == null) {
            this.scopes = scopes;
        } else {
            this.clear();
            this.addAll(scopes);
        }
    }

    /**
     * Clear the scopes.
     */
    public final void clear() {
        if (this.scopes != null) this.scopes.clear();
    }
    
    /**
     * Add a scope.
     *
     * @param scope The {@code Scope} to add.
     */
    public final void add(Scope scope) {
        if (scope == null) return;
        if (this.scopes == null) this.scopes = new ArrayList<>();
        this.scopes.add(scope);
    }

    /**
     * Add all ths scopes from a collection.
     *
     * @param c The {@code Collection} to add from.
     */
    public final void addAll(Collection<Scope> c) {
        if (c == null || c.isEmpty()) return;
        if (this.scopes == null) this.scopes = new ArrayList<>();
        this.scopes.addAll(c);
    }
        
    /**
     * Add all the scopes in another scope container.
     *
     * @param other The other {@code ScopeContainer}.
     */
    public final void addAll(ScopeContainer other) {
        if (other != null) this.addAll(other.scopes);
    }
    
    /**
     * Remove a scope.
     *
     * @param scope The {@code Scope} to remove.
     */
    public final void remove(Scope scope) {
        if (this.scopes != null) this.scopes.remove(scope);
    }

    /**
     * Sort the scopes.
     *
     * @param comp The {@code Comparator} that defines the ordering.
     */
    public final void sort(Comparator<Scope> comp) {
        if (this.scopes != null) this.scopes.sort(comp);
    }

    /**
     * Does this scope container apply to the given object?
     * Trivially true if there are no actual scopes present,
     * otherwise at least one of the scopes must apply directly.
     *
     * @param object The {@code FreeColObject} to check.
     * @return True if this effect applies.
     */
    public boolean appliesTo(FreeColObject object) {
        return this.isEmpty() || any(this.scopes, s -> s.appliesTo(object));
    }

    /**
     * Write the scopes to a stream.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.scopes == null) return;
        this.sort(scopeComparator);
        for (Scope scope : this.scopes) scope.toXML(xw);
    }

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof ScopeContainer) {
            ScopeContainer sc = (ScopeContainer)o;
            if (this.isEmpty() != sc.isEmpty()) return false;
            if (this.isEmpty()) return true;
            List<Scope> sl1 = new ArrayList<>(this.scopes);
            List<Scope> sl2 = new ArrayList<>(sc.scopes);
            sl1.sort(scopeComparator);
            sl2.sort(scopeComparator);
            while (!sl1.isEmpty()) {
                if (sl2.isEmpty()) return false;
                Scope s1 = sl1.remove(0);
                Scope s2 = sl2.remove(0);
                if (!s1.equals(s2)) return false;
            }
            return sl2.isEmpty();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        for (Scope s : this.getList()) hash += 37 * s.hashCode();
        return hash;
    }

    
    // Useful wrappers that allow us to lazily create scope containers

    public static boolean isScopeContainerEmpty(ScopeContainer sc) {
        return sc == null || sc.isEmpty();
    }

    public static List<Scope> getScopeList(ScopeContainer sc) {
        return (sc == null) ? noList : sc.getList();
    }

    public static Stream<Scope> getScopes(ScopeContainer sc) {
        return (sc == null) ? noStream : sc.get();
    }
    
    public static ScopeContainer addScope(ScopeContainer sc, Scope scope) {
        if (sc == null) sc = new ScopeContainer();
        sc.add(scope);
        return sc;
    }

    public static void removeScope(ScopeContainer sc, Scope scope) {
        if (sc != null) sc.remove(scope);
    }
    
    public static ScopeContainer setScopes(ScopeContainer sc,
                                           Collection<Scope> c) {
        if (sc == null) {
            if (c == null || c.isEmpty()) return null;
            sc = new ScopeContainer();
        } else {
            sc.clear();
        }
        sc.addAll(c);
        return sc;
    }            
        
    public static void clearScopes(ScopeContainer sc) {
        if (sc != null) sc.clear();
    }

    public static boolean equalScopes(ScopeContainer sc1, ScopeContainer sc2) {
        return (sc1 == null) ? sc2 == null
            : (sc2 == null) ? false
            : sc1.equals(sc2);
    }
    
    public static boolean scopeContainerAppliesTo(ScopeContainer sc,
                                                  FreeColObject fco) {
        return sc == null || sc.appliesTo(fco);
    }

    public static void scopeContainerToXML(ScopeContainer sc,
                                           FreeColXMLWriter xw)
        throws XMLStreamException {
        if (sc != null) sc.toXML(xw);
    }
}
