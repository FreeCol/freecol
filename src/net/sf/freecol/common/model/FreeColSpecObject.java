/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.Introspector;


/**
 * A FreeColObject that also contains a Specification.
 */
public abstract class FreeColSpecObject extends FreeColObject {

    protected static final Logger logger = Logger.getLogger(FreeColSpecObject.class.getName());

    /** The <code>Specification</code> this object uses, which may be null. */
    private Specification specification;


    /**
     * Create a new specification-object.
     *
     * @param specification The <code>Specification</code> to use.
     */
    public FreeColSpecObject(Specification specification) {
        this.specification = specification;
    }


    /**
     * Instantiate a FreeCol specification object.
     *
     * @param <T> The actual instance type.
     * @param spec The <code>Specification</code> to use in the constructor.
     * @param returnClass The expected class of the object.
     * @return The new spec object, or null on error.
     */
    public static <T extends FreeColObject> T newInstance(Specification spec,
        Class<T> returnClass) {
        try {
            return Introspector.instantiate(returnClass,
                new Class[] { Specification.class },
                new Object[] { spec });
        } catch (Introspector.IntrospectorException ex) {
            logger.log(Level.WARNING, "newInstance failure", ex);
        }
        return null;
    }
        
    /**
     * Get the specification.  It may be null.
     *
     * @return The <code>Specification</code> used by this object.
     */
    @Override
    public Specification getSpecification() {
        return this.specification;
    }

    /**
     * Sets the specification for this object. 
     *
     * @param specification The <code>Specification</code> to use.
     */
    @Override
    protected void setSpecification(Specification specification) {
        this.specification = specification;
    }
}
