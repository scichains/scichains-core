/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.executors.api;

import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.parameters.NoValidParameterException;
import net.algart.executors.api.parameters.Parameters;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

abstract class ParameterSetter {
    final Method method;
    private final int priority;
    final String parameterName;
    final Class<?> parameterType;

    ParameterSetter(Method method, Class<?> parameterType, int priority) {
        assert method != null;
        assert parameterType != null;
        this.method = method;
        this.priority = priority;
        this.parameterType = parameterType;
        final String parameterName = method.getName().substring("set".length());
        assert !parameterName.isEmpty();
        this.parameterName = parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
    }

    abstract Object getValue(Parameters properties);

    abstract ParameterValueType getControlValueType();

    void set(Executor executor) {
        try {
            final Object value = getValue(executor.parameters());
            Executor.LOG.log(System.Logger.Level.TRACE, () -> "    Setting property " + parameterName
                    + " to " + value + " by " + getClass().getSimpleName());
            method.invoke(executor, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Cannot call method \"" + method + "\" for executor " + executor);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new AssertionError("Setter method \"" + method
                        + "\" throws unexpected checked exception", e);
            }
        }
    }

    static Map<String, ParameterSetter> findSetters(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final Map<String, ParameterSetter> result = new LinkedHashMap<>();
        for (Method method : executor.getClass().getMethods()) {
            if (executor.skipStandardAutomaticParameters() && method.getDeclaringClass().equals(Executor.class)) {
                continue;
            }
            final ParameterSetter setter = getInstanceOrNull(method);
            if (setter != null) {
                final ParameterSetter previous = result.get(setter.parameterName);
                if (previous != null) {
                    // we have two suitable setters with different argument type
                    if (setter.priority < previous.priority) {
                        continue;
                    }
                }
                result.put(setter.parameterName, setter);
            }
        }
        return result;
    }

    private static ParameterSetter getInstanceOrNull(Method method) {
        Objects.requireNonNull(method);
        final String name = method.getName();
        if (!name.startsWith("set") || name.length() < 4 || !Character.isUpperCase(name.charAt(3))) {
            return null;
        }
        final Class<?> declaringClass = method.getDeclaringClass();
        if ((declaringClass == Executor.class || declaringClass == ExecutionBlock.class)
                && Executor.NON_SETTERS.contains(name)) {
            return null;
        }
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            return null;
        }
        if (parameterTypes[0].equals(boolean.class)) {
            return new BooleanSetter(method);
        } else if (parameterTypes[0].equals(int.class)) {
            return new IntSetter(method);
        } else if (parameterTypes[0].equals(long.class)) {
            return new LongSetter(method);
        } else if (parameterTypes[0].equals(float.class)) {
            return new FloatSetter(method);
        } else if (parameterTypes[0].equals(double.class)) {
            return new DoubleSetter(method);
        } else if (parameterTypes[0].equals(Integer.class)) {
            return new IntOrNullSetter(method);
        } else if (parameterTypes[0].equals(Long.class)) {
            return new LongOrNullSetter(method);
        } else if (parameterTypes[0].equals(Float.class)) {
            return new FloatOrNullSetter(method);
        } else if (parameterTypes[0].equals(Double.class)) {
            return new DoubleOrNullSetter(method);
        } else if (parameterTypes[0].equals(String.class)) {
            return new StringSetter(method);
        } else if (parameterTypes[0].equals(Color.class)) {
            return new ColorSetter(method);
        } else if (parameterTypes[0].isEnum()) {
            return new EnumSetter(method, parameterTypes[0]);
        } else {
            return null;
        }
    }

    static class BooleanSetter extends ParameterSetter {
        BooleanSetter(Method method) {
            super(method, boolean.class, 1);
        }

        @Override
        Object getValue(Parameters properties) {
            return properties.getBoolean(parameterName);
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.BOOLEAN;
        }
    }

    static class IntSetter extends ParameterSetter {
        IntSetter(Method method) {
            super(method, int.class, 2);
        }

        @Override
        Object getValue(Parameters properties) {
            return properties.getInteger(parameterName);
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.INT;
        }
    }

    static class LongSetter extends ParameterSetter {
        LongSetter(Method method) {
            super(method, long.class, 3);
        }

        @Override
        Object getValue(Parameters properties) {
            return properties.getLong(parameterName);
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.LONG;
        }
    }

    static class FloatSetter extends ParameterSetter {
        FloatSetter(Method method) {
            super(method, float.class, 4);
        }

        @Override
        Object getValue(Parameters properties) {
            return (float) properties.getDouble(parameterName);
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.FLOAT;
        }
    }

    static class DoubleSetter extends ParameterSetter {
        DoubleSetter(Method method) {
            super(method, double.class, 5);
        }

        @Override
        Object getValue(Parameters properties) {
            return properties.getDouble(parameterName);
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.DOUBLE;
        }
    }

    static class IntOrNullSetter extends ParameterSetter {
        IntOrNullSetter(Method method) {
            super(method, Integer.class, 11);
        }

        @Override
        Object getValue(Parameters properties) {
            String value = properties.getString(parameterName);
            value = value == null ? "" : value.trim();
            try {
                return value.isEmpty() ? null : Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new NoValidParameterException("Property \""
                        + parameterName + "\" is not a valid integer value: \"" + value + "\"");
            }
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.STRING;
        }
    }

    static class LongOrNullSetter extends ParameterSetter {
        LongOrNullSetter(Method method) {
            super(method, Integer.class, 12);
        }

        @Override
        Object getValue(Parameters properties) {
            String value = properties.getString(parameterName);
            value = value == null ? "" : value.trim();
            try {
                return value.isEmpty() ? null : Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new NoValidParameterException("Property \""
                        + parameterName + "\" is not a valid long integer value: \"" + value + "\"");
            }
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.STRING;
        }
    }

    static class FloatOrNullSetter extends ParameterSetter {
        FloatOrNullSetter(Method method) {
            super(method, Integer.class, 13);
        }

        @Override
        Object getValue(Parameters properties) {
            String value = properties.getString(parameterName);
            value = value == null ? "" : value.trim();
            try {
                return value.isEmpty() ? null : Float.valueOf(value);
            } catch (NumberFormatException e) {
                throw new NoValidParameterException("Property \""
                        + parameterName + "\" is not a valid float value: \"" + value + "\"");
            }
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.STRING;
        }
    }

    static class DoubleOrNullSetter extends ParameterSetter {
        DoubleOrNullSetter(Method method) {
            super(method, Integer.class, 14);
        }

        @Override
        Object getValue(Parameters properties) {
            String value = properties.getString(parameterName);
            value = value == null ? "" : value.trim();
            try {
                return value.isEmpty() ? null : Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new NoValidParameterException("Property \""
                        + parameterName + "\" is not a valid double value: \"" + value + "\"");
            }
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.STRING;
        }
    }

    static class StringSetter extends ParameterSetter {
        StringSetter(Method method) {
            super(method, String.class, 100);
        }

        @Override
        Object getValue(Parameters properties) {
            return properties.getString(parameterName, null);
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.STRING;
        }
    }

    static class ColorSetter extends ParameterSetter {
        ColorSetter(Method method) {
            super(method, Color.class, 50);
        }

        @Override
        Object getValue(Parameters properties) {
            final String color = properties.getString(parameterName, null);
            if (color == null) {
                return null;
            }
            try {
                return java.awt.Color.decode(color);
                // Note: we don't try to support alpha-channel here.
                // See comments inside ChannelOperation.decodeRGBA method.
            } catch (NumberFormatException e) {
                throw new NoValidParameterException("Property \"" + parameterName
                        + "\" is not a valid color value while calling " + method);
            }
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.STRING;
        }
    }

    static class EnumSetter extends ParameterSetter {
        @SuppressWarnings("rawtypes")
        private final Class<? extends Enum> enumClass;
        private final Method valueOfNameCustomMethod;

        EnumSetter(Method method, Class<?> enumClass) {
            super(method, enumClass, 10);
            assert enumClass.isEnum();
            assert Enum.class.isAssignableFrom(enumClass);
            this.enumClass = enumClass.asSubclass(Enum.class);
            checkPublicAccessToEnumClass(this.enumClass);
            Method valueOfNameCustomMethod = null;
            try {
                valueOfNameCustomMethod = this.enumClass.getMethod(
                        Executor.ENUM_VALUE_OF_NAME_CUSTOM_METHOD, String.class);
                final int methodModifiers = valueOfNameCustomMethod.getModifiers();
                if (!(Modifier.isStatic(methodModifiers) && Modifier.isPublic(methodModifiers))) {
                    throw new IllegalStateException("Method " + valueOfNameCustomMethod + " must be public static");
                }
            } catch (NoSuchMethodException ignored) {
            }
            this.valueOfNameCustomMethod = valueOfNameCustomMethod;
        }

        @Override
        @SuppressWarnings("unchecked")
        Object getValue(Parameters properties) {
            final String enumName = properties.getString(parameterName);
            if (valueOfNameCustomMethod != null) {
                try {
                    return valueOfNameCustomMethod.invoke(null, enumName);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Cannot use " + valueOfNameCustomMethod + " in " + method, e);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof IllegalArgumentException) {
                        throw new NoValidParameterException(
                                "Cannot find enum " + enumName + " while calling " + method, e.getCause());
                    } else {
                        throw new IllegalArgumentException(
                                "Cannot use " + valueOfNameCustomMethod + " in " + method, e);
                    }
                }
            }
            try {
                return Enum.valueOf(enumClass, enumName);
            } catch (IllegalArgumentException e) {
                throw new NoValidParameterException("Cannot find enum " + enumName + " while calling " + method, e);
            }
        }

        @Override
        ParameterValueType getControlValueType() {
            return ParameterValueType.ENUM_STRING;
            // Note: EnumSetter is used only for String controls.
            // For comparison, enum control for the int type does not require Java enum in "setXxx" method;
            // it uses usual Java int (setXxx(int)).
        }

        // Note: although ParameterSetter can still work even for non-public enum classes
        // (via Enum.valueOf call), this is a bad idea to provide such classes: the user
        // will not be able to access to enum constants using standard Java syntax.
        // Therefore, we require that both the enum class and its containing class
        // (if it exists) MUST be public.
        private static void checkPublicAccessToEnumClass(Class<?> enumClass) {
            if (!(Modifier.isPublic(enumClass.getModifiers()))) {
                throw new IllegalStateException("Enum " + enumClass + " must be public");
            }
            final Class<?> enclosingClass = enumClass.getEnclosingClass();
            if (enclosingClass != null && !(Modifier.isPublic(enclosingClass.getModifiers()))) {
                throw new IllegalStateException("Enum " + enumClass + " is an inner class of the " +
                        enclosingClass + ", which must be public");
            }
        }
    }
}
