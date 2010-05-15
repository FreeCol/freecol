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

package net.sf.freecol.common.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * A class to allow access to the methods "fooType getFoo()" and
 * "void setFoo(fooType)" conventionally seen in objects.
 * Useful when Foo arrives as a run-time String, such as is the
 * case in serialization to/from XML representations.
 */
public class Introspector {

    /**
     * The class whose field we are to operate on.
     */
    private Class<?> theClass;

    /**
     * The field whose get/set methods we wish to invoke.
     */
    private String field;

    /**
     * Build a new Introspector for the specified class and field name.
     *
     * @param theClass The <code>Class</code> of interest.
     * @param field The field name within the class of interest.
     * @throws IllegalArgumentException
     */
    public Introspector(Class<?> theClass, String field)
        throws IllegalArgumentException {
        if (field == null || field.length() == 0) {
            throw new IllegalArgumentException("Field may not be empty");
        }
        this.theClass = theClass;
        this.field = field;
    }

    /**
     * Get a get-method for this Introspector.
     *
     * @return A <code>Method</code> representing getField().
     * @throws IllegalArgumentException
     */
    private Method getGetMethod()
        throws IllegalArgumentException {
        String methodName = "get" + field.substring(0, 1).toUpperCase()
            + field.substring(1);

        try {
            return theClass.getMethod(methodName);
        } catch (Exception e) {
            throw new IllegalArgumentException(theClass.getName()
                                               + "." + methodName
                                               + ": " + e.toString());
        }
    }

    /**
     * Get a set-method for this Introspector.
     *
     * @param argType A <code>Class</code> that is the argument to
     *        the set-method
     * @return A <code>Method</code> representing setField().
     * @throws IllegalArgumentException
     */
    private Method getSetMethod(Class<?> argType)
        throws IllegalArgumentException {
        String methodName = "set" + field.substring(0, 1).toUpperCase()
            + field.substring(1);

        try {
            return theClass.getMethod(methodName, argType);
        } catch (Exception e) {
            throw new IllegalArgumentException(theClass.getName()
                                               + "." + methodName
                                               + ": " + e.toString());
        }
    }

    /**
     * Get the return type from a <code>Method</code>.
     *
     * @param method The <code>Method</code> to examine.
     * @return The method return type, or null on error.
     * @throws IllegalArgumentException
     */
    private Class<?> getMethodReturnType(Method method)
        throws IllegalArgumentException {
        Class<?> ret;

        try {
            ret = method.getReturnType();
        } catch (Exception e) {
            throw new IllegalArgumentException(theClass.getName()
                                               + "." + method.getName()
                                               + " return type: "
                                               + e.toString());
        }
        return ret;
    }

    /**
     * Get a function that converts to String from a given class.
     * We use Enum.name() for enums, and String.valueOf(argType) for the rest.
     *
     * @param argType A <code>Class</code> to find a converter for.
     * @return A conversion function, or null on error.
     * @throws IllegalArgumentException
     */
    private Method getToStringConverter(Class<?> argType)
        throws IllegalArgumentException {
        Method method;

        if (argType.isEnum()) {
            try {
                method = argType.getMethod("name");
            } catch (Exception e) {
                throw new IllegalArgumentException(argType.getName()
                                                   + ".getMethod(name()): "
                                                   + e.toString());
            }
        } else {
            try {
                method = String.class.getMethod("valueOf", argType);
            } catch (Exception e) {
                throw new IllegalArgumentException("String.getMethod(valueOf("
                                                   + argType.getName()
                                                   + ")): " + e.toString());
            }
        }
        return method;
    }

    /**
     * Get a function that converts from String to a given class.
     * We use Enum.valueOf(Class, String) for enums, and
     * argType.valueOf(String) for the rest, having first dodged
     * the primitive types.
     *
     * @param argType A <code>Class</code> to find a converter for.
     * @return A conversion function, or null on error.
     * @throws IllegalArgumentExcpetion
     */
    private Method getFromStringConverter(Class<?> argType)
        throws IllegalArgumentException {
        Method method;

        if (argType.isEnum()) {
            try {
                method = Enum.class.getMethod("valueOf", Class.class, String.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Enum.getMethod(valueOf(Class, String)): "
                                                   + e.toString());
            }
        } else {
            if (argType.isPrimitive()) {
                if (argType == Integer.TYPE) {
                    argType = Integer.class;
                } else if (argType == Boolean.TYPE) {
                    argType = Boolean.class;
                } else if (argType == Float.TYPE) {
                    argType = Float.class;
                } else if (argType == Double.TYPE) {
                    argType = Double.class;
                } else if (argType == Character.TYPE) {
                    argType = Character.class;
                } else {
                    throw new IllegalArgumentException("Need compatible class for primitive " + argType.getName());
                }
            }
            try {
                method = argType.getMethod("valueOf", String.class);
            } catch (Exception e) {
                throw new IllegalArgumentException(argType.getName()
                                                   + ".getMethod(valueOf(String)): "
                                                   + e.toString());
            }
        }
        return method;
    }

    /**
     * Invoke the get-method for this Introspector.
     *
     * @param obj An <code>Object</code> (really of type theClass)
     *        whose get-method is to be invoked.
     * @return A <code>String</code> containing the result of invoking
     *         the get-method.
     * @throws IllegalArgumentException
     */
    public String getter(Object obj)
        throws IllegalArgumentException {
        Method getMethod = getGetMethod();
        Class<?> fieldType = getMethodReturnType(getMethod);

        if (fieldType == String.class) {
            try {
                return (String) getMethod.invoke(obj);
            } catch (Exception e) {
                throw new IllegalArgumentException(getMethod.getName()
                                                   + "(obj)): "
                                                   + e.toString());
            }
        } else {
            Object result = null;
            try {
                result = getMethod.invoke(obj);
            } catch (Exception e) {
                throw new IllegalArgumentException(getMethod.getName()
                                                   + "(obj): "
                                                   + e.toString());
            }
            Method convertMethod = getToStringConverter(fieldType);
            if (Modifier.isStatic(convertMethod.getModifiers())) {
                try {
                    return (String) convertMethod.invoke(null, result);
                } catch (Exception e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(null, result): "
                                                       + e.toString());
                }
            } else {
                try {
                    return (String) convertMethod.invoke(result);
                } catch (Exception e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(result): "
                                                       + e.toString());
                }
            }
        }
    }

    /**
     * Invoke the set-method provided by this Introspector.
     *
     * @param obj An <code>Object</code> (really of type theClass)
     *        whose set-method is to be invoked.
     * @param value A <code>String</code> containing the value to be set.
     * @throws IllegalArgumentException
     */
    public void setter(Object obj, String value)
        throws IllegalArgumentException {
        Method getMethod = getGetMethod();
        Class<?> fieldType = getMethodReturnType(getMethod);
        Method setMethod = getSetMethod(fieldType);

        if (fieldType == String.class) {
            try {
                setMethod.invoke(obj, value);
            } catch (Exception e) {
                throw new IllegalArgumentException(setMethod.getName()
                                                   + "(obj, " + value + ")): "
                                                   + e.toString());
            }
        } else {
            Method convertMethod = getFromStringConverter(fieldType);
            Object result = null;

            if (fieldType.isEnum()) {
                try {
                    result = convertMethod.invoke(null, fieldType, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(null, " + fieldType.getName()
                                                       + ", " + value + "):"
                                                       + e.toString());
                }
            } else {
                try {
                    result = convertMethod.invoke(null, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(null, " + value + "):"
                                                       + e.toString());
                }
            }
            try {
                setMethod.invoke(obj, result);
            } catch (Exception e) {
                throw new IllegalArgumentException(setMethod.getName()
                                                   + "(result): "
                                                   + e.toString());
            }
        }
    }
}
