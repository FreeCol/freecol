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

package net.sf.freecol.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * A class to allow access to the methods "fooType getFoo()" and
 * "void setFoo(fooType)" conventionally seen in objects.
 * Useful when Foo arrives as a run-time String, such as is the
 * case in serialization to/from XML representations.
 */
public class Introspector {

    /** The class whose field we are to operate on. */
    private final Class<?> theClass;

    /** The field whose get/set methods we wish to invoke. */
    private final String field;


    /**
     * Build a new Introspector for the specified class and field name.
     *
     * @param theClass The <code>Class</code> of interest.
     * @param field The field name within the class of interest.
     */
    public Introspector(Class<?> theClass, String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field may not be empty");
        }
        this.theClass = theClass;
        this.field = field;
    }


    /**
     * Get a get-method for this Introspector.
     *
     * @return A <code>Method</code> representing getField().
     */
    private Method getGetMethod() {
        String methodName = "get" + field.substring(0, 1).toUpperCase()
            + field.substring(1);

        try {
            return theClass.getMethod(methodName);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(theClass.getName()
                                               + "." + methodName, e);
        }
    }

    /**
     * Get a set-method for this Introspector.
     *
     * @param argType A <code>Class</code> that is the argument to
     *        the set-method
     * @return A <code>Method</code> representing setField().
     */
    private Method getSetMethod(Class<?> argType) {
        String methodName = "set" + field.substring(0, 1).toUpperCase()
            + field.substring(1);

        try {
            return theClass.getMethod(methodName, argType);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(theClass.getName()
                                               + "." + methodName, e);
        }
    }

    /**
     * Get the return type from a <code>Method</code>.
     *
     * @param method The <code>Method</code> to examine.
     * @return The method return type, or null on error.
     */
    private Class<?> getMethodReturnType(Method method) {
        Class<?> ret;

        try {
            ret = method.getReturnType();
        } catch (Exception e) {
            throw new IllegalArgumentException(theClass.getName()
                                               + "." + method.getName()
                                               + " return type.", e);
        }
        return ret;
    }

    /**
     * Get a function that converts to String from a given class.
     * We use Enum.name() for enums, and String.valueOf(argType) for the rest.
     *
     * @param argType A <code>Class</code> to find a converter for.
     * @return A conversion function, or null on error.
     */
    private Method getToStringConverter(Class<?> argType) {
        Method method;

        if (argType.isEnum()) {
            try {
                method = argType.getMethod("name");
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException(argType.getName()
                                                   + ".getMethod(name())", e);
            }
        } else {
            try {
                method = String.class.getMethod("valueOf", argType);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException("String.getMethod(valueOf("
                                                   + argType.getName() + "))", e);
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
     */
    private Method getFromStringConverter(Class<?> argType) {
        Method method;

        if (argType.isEnum()) {
            try {
                method = Enum.class.getMethod("valueOf", Class.class, String.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException("Enum.getMethod(valueOf(Class, String))", e);
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
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException(argType.getName()
                                                   + ".getMethod(valueOf(String))", e);
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
     */
    public String getter(Object obj) {
        Method getMethod = getGetMethod();
        Class<?> fieldType = getMethodReturnType(getMethod);

        if (fieldType == String.class) {
            try {
                return (String) getMethod.invoke(obj);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException(getMethod.getName()
                                                   + "(obj)", e);
            }
        } else {
            Object result = null;
            try {
                result = getMethod.invoke(obj);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException(getMethod.getName()
                                                   + "(obj)", e);
            }
            Method convertMethod = getToStringConverter(fieldType);
            if (Modifier.isStatic(convertMethod.getModifiers())) {
                try {
                    return (String) convertMethod.invoke(null, result);
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(null, result)", e);
                }
            } else {
                try {
                    return (String) convertMethod.invoke(result);
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(result)", e);
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
     */
    public void setter(Object obj, String value) {
        Method getMethod = getGetMethod();
        Class<?> fieldType = getMethodReturnType(getMethod);
        Method setMethod = getSetMethod(fieldType);

        if (fieldType == String.class) {
            try {
                setMethod.invoke(obj, value);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException(setMethod.getName()
                                                   + "(obj, " + value + ")", e);
            }
        } else {
            Method convertMethod = getFromStringConverter(fieldType);
            Object result = null;

            if (fieldType.isEnum()) {
                try {
                    result = convertMethod.invoke(null, fieldType, value);
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(null, " + fieldType.getName()
                                                       + ", " + value + ")", e);
                }
            } else {
                try {
                    result = convertMethod.invoke(null, value);
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    throw new IllegalArgumentException(convertMethod.getName()
                                                       + "(null, " + value + ")", e);
                }
            }
            try {
                setMethod.invoke(obj, result);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException(setMethod.getName()
                                                   + "(result)", e);
            }
        }
    }


    /**
     * Constructs a new instance of an object of a class specified by name,
     * with supplied parameters.
     *
     * @param tag The name of the class to instantiate.
     * @param types The argument types of the constructor to call.
     * @param params The parameters to call the constructor with.
     * @return The new object instance.
     * @exception IllegalArgumentException wraps all exceptional conditions.
     */
    public static Object instantiate(String tag, Class[] types,
                                     Object[] params) {
        Class<?> messageClass;
        try {
            messageClass = Class.forName(tag);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find class " + tag, e);
        }
        Constructor<?> constructor;
        try {
            constructor = messageClass.getDeclaredConstructor(types);
        } catch (NoSuchMethodException | SecurityException e) {
            String p = "Unable to find constructor " + tag + "(";
            for (Class type : types) p += " " + type;
            p += " )";
            throw new IllegalArgumentException(p, e);
        }
        Object instance;
        try {
            instance = constructor.newInstance(params);
        } catch (IllegalAccessException | IllegalArgumentException
                | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to construct " + tag, e);
        }
        return instance;
    }
}
