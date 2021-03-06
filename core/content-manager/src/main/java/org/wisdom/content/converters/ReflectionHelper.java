/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.content.converters;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Some utilities functions to extract type and generic metadata.
 */
public class ReflectionHelper {

    /**
     * Get the list of class-type pairs that represent the type arguments of a
     * {@link ParameterizedType parameterized} input type.
     * <p>
     * For any given {@link ClassTypePair#rawClass() class} part of each pair
     * in the returned list, following rules apply:
     * <ul>
     * <li>If a type argument is a class then the class is returned as raw class.</li>
     * <li>If the type argument is a generic array type and the generic component
     * type is a class then class of the array is returned as raw class.</li>
     * <li>If the type argument is a parameterized type and it's raw type is a
     * class then that class is returned as raw class.</li>
     * </ul>
     * If the {@code type} is not an instance of ParameterizedType an empty
     * list is returned.
     *
     * @param type parameterized type.
     * @return the list of class-type pairs representing the actual type arguments.
     * May be empty, but may never be {@code null}.
     * @throws IllegalArgumentException if any of the generic type arguments is
     *                                  not a class, or a generic array type, or the generic component type
     *                                  of the generic array type is not class, or not a parameterized type
     *                                  with a raw type that is not a class.
     */
    public static List<ClassTypePair> getTypeArgumentAndClass(final Type type) throws IllegalArgumentException {
        final Type[] types = getTypeArguments(type);
        if (types == null) {
            return Collections.emptyList();
        }

        List<ClassTypePair> list = new ArrayList<>();
        for (Type t : types) {
            list.add(new ClassTypePair(erasure(t), t));
        }
        return list;
    }

    /**
     * Get the {@link Class} representation of the given type.
     * <p>
     * This corresponds to the notion of the erasure in JSR-14.
     *
     * @param type type to provide the erasure for.
     * @return the given type's erasure.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> erasure(Type type) {
        return EraserVisitor.ERASER.visit(type);
    }

    /**
     * Get the type arguments for a parameterized type.
     * <p>
     * In case the type is not a {@link ParameterizedType parameterized type},
     * the method returns {@code null}.
     *
     * @param type parameterized type.
     * @return type arguments for a parameterized type, or {@code null} in case the input type is
     * not a parameterized type.
     */
    public static Type[] getTypeArguments(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        return ((ParameterizedType) type).getActualTypeArguments();
    }

    /**
     * Gets the default value as String for the primitive types.
     *
     * @param type the primitive type
     * @return the default value as String
     */
    public static String getPrimitiveDefault(Class type) {
        if (type == Boolean.class) {
            return "false";
        }
        if (type == Character.class) {
            return Character.toString((char) 0);
        }
        return "0";
    }

    /**
     * Gets a field object from the given class. This methods used in this order: {@link Class#getField(String)} and
     * {@link Class#getDeclaredField(String)}. If the found field is not accessible,
     * the accessibility is set to {@value true}. IF the field cannot be found, this method throws a {@link java.lang
     * .NoSuchFieldException}.
     *
     * @param clazz the class
     * @param name  the field's name
     * @return the field object
     * @throws NoSuchFieldException if the field cannot be found
     */
    public static Field getField(Class clazz, String name) throws NoSuchFieldException {
        Field field = null;
        try {
            field = clazz.getField(name);
        } catch (NoSuchFieldException e) {
            // The field is not public, next attempt.
        }

        if (field == null) {
            field = clazz.getDeclaredField(name);
        }

        // We have the field. If not found the previous lookup would have thrown an exception.
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }


}
