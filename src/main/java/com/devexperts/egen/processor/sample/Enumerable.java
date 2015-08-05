package com.devexperts.egen.processor.sample;

/*
 * #%L
 * EGEN - Externalizable implementation generator
 * %%
 * Copyright (C) 2014 - 2015 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.io.ObjectStreamException;
import java.io.Serializable;


public class Enumerable<E extends Enumerable<E>> implements Comparable<E>, Serializable {
    private static final long serialVersionUID = -1515597278681975018L;

    private final transient Class<E> uclass;
    private final int ordinal;
    private final transient String name;

    /**
     * Creates new enumerable with specified ordinal and name
     * and registers it in its uniqueness class. Ensures that
     * both ordinal and name are unique within its uniqueness class.
     *
     * @throws ClassCastException if uniqueness class is not a correct superclass.
     * @throws IllegalArgumentException if either ordinal or name is a duplicate.
     */
    @SuppressWarnings("unchecked")
    protected Enumerable(int ordinal, String name) {
        this.uclass = getStrictUniquenessClass();
        this.ordinal = ordinal;
        this.name = name;
    }

    /**
     * Returns uniqueness class of this enumerable.
     */
    public final Class<E> uclass() {
        return uclass;
    }

    /**
     * Returns ordinal of this enumerable.
     */
    public final int ordinal() {
        return ordinal;
    }

    /**
     * Returns ordinal of this enumerable.
     * @deprecated Use #ordinal()
     */
    public final int code() {
        return ordinal;
    }

    /**
     * Returns name of this enumerable.
     */
    public final String name() {
        return name;
    }

    /**
     * Overrides {@link Object#equals(Object obj)} for strict reference comparison.
     */
    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public final boolean equals(Object obj) {
        return this == obj;
    }

    /**
     * Overrides {@link Object#hashCode()} to return ordinal.
     */
    public final int hashCode() {
        return ordinal;
    }

    /**
     * Overrides {@link Object#toString()} and returns name.
     */
    public String toString() {
        return name;
    }

    /**
     * Compares this enumerable with the specified object for order.
     * By default, operates only within a single uniqueness class
     * and performs comparison of the involved enumerable ordinals.
     *
     * @throws ClassCastException if the specified object is not
     *         the enumerable with the same uniqueness class.
     * @throws IllegalStateException if either of enumerables is unregistered.
     */
    public int compareTo(E e) {
        if (this == e)
            return 0;
        if (uclass != e.uclass())
            throw new ClassCastException("Requires " + uclass + ", found " + e.uclass());
        if (ordinal == e.ordinal())
            throw new IllegalStateException("Unregistered enumerable detected.");
        return ordinal > e.ordinal() ? 1 : -1;
    }

    /**
     * Returns a set of all registered enumerable values for specified
     * uniqueness class. Returns an empty set if this class is unknown
     * or if no values have been registered yet.
     * <p>
     * The returned set is unmodifiable; nevertheless it could change
     * over time if new values become registered. Thus, it is highly
     * recommended to use it only after all values have been registered.
     * <p>
     * The returned set is neither cloneable nor serializable; if any
     * such operation is required, then some other set based on this
     * set need to be created.
     */


    /**
     * This method is a shortcut for <code>values(uclass).findByName(name)</code>.
     */
    public static <E extends Enumerable<? super E>> E findByName(Class<E> uclass, String name) {
        return null;
    }

    /**
     * This method is a shortcut for <code>values(uclass).findByName(name, def_value)</code>.
     */
    public static <E extends Enumerable<? super E>> E findByName(Class<E> uclass, String name, E def_value) {
        return null;
    }

    /**
     * This method is a shortcut for <code>values(uclass).findByOrdinal(ordinal)</code>.
     * @deprecated Use <code>values(uclass).findByOrdinal(ordinal)</code>
     */
    public static <E extends Enumerable<? super E>> E findByCode(Class<E> uclass, int ordinal) {
        return null;
    }

    /**
     * This method is a shortcut for <code>values(uclass).findByOrdinal(ordinal, def_value)</code>.
     * @deprecated Use <code>values(uclass).findByOrdinal(ordinal, def_value)</code>
     */
    public static <E extends Enumerable<? super E>> E findByCode(Class<E> uclass, int ordinal, E def_value) {
        return null;
    }

    // ========== Miscellaneous Stuff ==========

    /**
     * Overrides {@link Object#clone()} for strict disabling of cloning.
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "InstanceofIncompatibleInterface"})
    protected final Object clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable))
            throw new CloneNotSupportedException();
        throw new UnsupportedOperationException("Enumerable may not be cloned.");
    }

    /**
     * Resolves proper substitution after deserialization
     * based on uniqueness class and deserialized ordinal.
     */
    protected final Object readResolve() throws ObjectStreamException {
        // Note that only ordinal is (de)serializable and, thus, valid.
        // Convert runtime exceptions into proper I/O exceptions to indicate
        // unmarshalling error as this is more likely to be compatibility issue.
        return null;
    }

    /**
     * Returns uniqueness class of this enumerable if it passes required
     * checks. Uses {@link #getUniquenessClass()} for initial value.
     *
     * @throws ClassCastException if uniqueness class is not a correct superclass.
     */
    protected final Class<E> getStrictUniquenessClass() {
        Class<E> c = getUniquenessClass();
        if (c == null)
            throw new ClassCastException("Uniqueness class is null.");
        if (!c.isInstance(this))
            throw new ClassCastException("Uniqueness class is not a superclass: " + c);
        //looks like javac bug that requires cast
        //noinspection RedundantCast
        if (!Enumerable.class.isAssignableFrom(c) || (Class<?>)c == Enumerable.class)
            throw new ClassCastException("Uniqueness class is too generic: " + c);
        return c;
    }

    /**
     * Returns uniqueness class of this enumerable. The uniqueness class
     * must be either a class or a superclass of the enumerable instance
     * and it must be a subclass of the <code>Enumerable</code> class.
     * <p>
     * By default, it returns a class or a superclass of this instance
     * which is an immediate subclass of the <code>Enumerable</code> class.
     * Thus each new immediate subclass of the <code>Enumerable</code>
     * class creates new uniqueness class which includes all its subclasses.
     */
    @SuppressWarnings("unchecked")
    protected Class<E> getUniquenessClass() {
        Class<?> c = getClass();
        Class<?> s;
        while ((s = c.getSuperclass()) != Enumerable.class && s != null)
            c = s;
        return (Class<E>)c;
    }
}
