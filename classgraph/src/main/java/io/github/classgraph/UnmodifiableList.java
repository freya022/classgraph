/*
 * This file is part of ClassGraph.
 *
 * Author: Luke Hutchison
 *
 * Hosted at: https://github.com/classgraph/classgraph
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Luke Hutchison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.classgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The unmodifiable list that the lists returned by the ClassGraph API are built on.
 *
 * @param <T>
 *            the element type
 */
abstract class UnmodifiableList</* Non-null */ T> implements ReadOnlyList<T>, RandomAccess {
    /** The elements of this list, as an unmodifiable view. */
    private final List<T> elements;

    /**
     * Constructor.
     *
     * <p>
     * This list takes ownership of the given list rather than copying it, so the caller must not use the list again
     * after handing it over. A constructor that is reachable from outside this package therefore has to make the
     * copy itself, since it cannot make that demand of its caller.
     *
     * <p>
     * Sorting is also the caller's job, for lists whose order would otherwise be undefined, so that a list is never
     * sorted twice.
     *
     * @param elements
     *            the elements of the list
     */
    UnmodifiableList(final List<T> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Delegation to the read-only list

    @Override
    public T get(final int index) {
        return elements.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return elements.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return elements.lastIndexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public <R> R[] toArray(R[] a) {
        return elements.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return elements.containsAll(c);
    }

    @Override
    public ListIterator<T> listIterator() {
        return elements.listIterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return elements.listIterator(index);
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return elements.subList(fromIndex, toIndex);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Mutators, has to be rejected

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Bulk mutators, which have to be rejected even when they would not change the list

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(final Predicate<? super T> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(final UnaryOperator<T> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sort(final Comparator<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    // -------------------------------------------------------------------------------------------------------------
    // equals / hashCode / toString

    @Override
    public boolean equals(Object o) {
        // Make sure this instance also matches against the object, as the underlying collection isn't aware of it
        return o == this || (o instanceof List<?> && elements.equals(o));
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
