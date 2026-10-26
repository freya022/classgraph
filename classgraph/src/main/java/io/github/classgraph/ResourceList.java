/*
 * This file is part of ClassGraph.
 *
 * Author: Luke Hutchison (luke.hutch@gmail.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.jetbrains.annotations.Unmodifiable;

/**
 * An {@link AutoCloseable} list of {@link AutoCloseable} {@link Resource} objects. Closing the list closes every
 * {@link Resource} in the list, releasing any open file handles or memory mappings.
 *
 * <p>
 * Lists returned by the ClassGraph API are unmodifiable: their elements are fixed when the list is created, and
 * every method that would add, remove, replace or sort an element throws {@link UnsupportedOperationException}.
 * Copy the list if you need a modifiable version of it, e.g. {@code new ArrayList<>(list)}.
 */
public sealed interface ResourceList extends UnmodifiableListIntf<Resource>, RandomAccess, AutoCloseable permits ResourceListImpl {
    /**
     * Return an unmodifiable empty {@link ResourceList}.
     *
     * @return the unmodifiable empty {@link ResourceList}.
     */
    static ResourceList emptyList() {
        return ResourceListImpl.EMPTY_LIST;
    }

    /**
     * Create a new unmodifiable {@link ResourceListImpl} from a completed collection of {@link Resource} objects.
     * The collection is copied.
     *
     * @param resourceCollection
     *            the collection of {@link Resource} objects.
     */
    static ResourceList of(Collection<Resource> resourceCollection) {
        return new ResourceListImpl(new ArrayList<>(Objects.requireNonNull(resourceCollection, "resourceCollection must not be null")));
    }

    /**
     * Returns a list of all resources with the requested path. (There may be more than one resource with a given
     * path, from different classpath elements or modules, so this returns a {@link ResourceList} rather than a
     * single {@link Resource}.)
     *
     * @param resourcePath
     *            The path of a resource, relative to the package root. The path is normalized before it is matched,
     *            in the same way as by {@link ScanResult#getResourcesWithPath(String)}: any leading or trailing '/'
     *            is removed, empty and "." segments are removed, and each ".." segment removes the segment before
     *            it, without going above the package root. So {@code "/a/b.txt"}, {@code "a//b.txt"} and
     *            {@code "a/./b.txt"} all match a resource whose path is {@code "a/b.txt"}. Letter case is matched
     *            exactly.
     * @return A {@link ResourceList} of {@link Resource} objects in this list that have the given path (there may
     *         be more than one resource with a given path, from different classpath elements or modules, so this
     *         returns a {@link ResourceList} rather than a single {@link Resource}.) Returns the empty list if no
     *         resource is found with a matching path.
     */
    ResourceList get(String resourcePath);

    /**
     * Get the paths of all resources in this list relative to the package root.
     *
     * @return The paths of all resources in this list relative to the package root, by calling
     *         {@link Resource#getPath()} for each item in the list.
     */
    @Unmodifiable
    List<String> getPaths();

    /**
     * Get the paths of all resources in this list relative to the jarfile or directory that contains them.
     *
     * @return The paths of all resources in this list relative to the jarfile or directory that contains them, by
     *         calling {@link Resource#getPathRelativeToContainer()} for each item in the list.
     */
    @Unmodifiable
    List<String> getPathsRelativeToContainer();

    /**
     * Get the URLs of all resources in this list, by calling {@link Resource#getURL()} for each item in the list.
     *
     * @return The URLs of all resources in this list.
     * @throws IllegalStateException
     *             if any resource's URI could not be converted to a {@link URL}.
     */
    @Unmodifiable
    List<URL> getURLs();

    /**
     * Get the URIs of all resources in this list, by calling {@link Resource#getURI()} for each item in the list.
     *
     * @return The URIs of all resources in this list.
     * @throws IllegalStateException
     *             if any resource's {@link URI} could not be formed, for example for a resource obtained from a
     *             module that does not report where its resources are.
     */
    @Unmodifiable
    List<URI> getURIs();

    /**
     * Return a new {@link ResourceList} consisting of only the resources with the filename extension ".class".
     *
     * @return A new {@link ResourceList} consisting of only the resources with the filename extension ".class".
     */
    ResourceList classFilesOnly();

    /**
     * Return a new {@link ResourceList} consisting of non-classfile resources only.
     *
     * @return A new {@link ResourceList} consisting of only the resources that do not have the filename extension
     *         ".class".
     */
    ResourceList nonClassFilesOnly();

    /**
     * Return this {@link ResourceList} as a map from resource path (obtained from {@link Resource#getPath()}) to a
     * {@link ResourceList} of {@link Resource} objects that have that path.
     *
     * @return This {@link ResourceList} as a map from resource path (obtained from {@link Resource#getPath()}) to a
     *         {@link ResourceList} of {@link Resource} objects that have that path, sorted by resource path. Each
     *         map value lists the resources with that path in the order they appear in this list. The keys are
     *         exactly the strings returned by {@link Resource#getPath()}, which have no leading or trailing '/' and
     *         no empty, "." or ".." segments, and {@link Map#get(Object)} does not normalize the path it is given.
     *         To look up a path written in another form, use {@link #get(String)}.
     */
    @Unmodifiable
    Map<String, ResourceList> asMap();

    /**
     * Find duplicate resource paths within this {@link ResourceList}.
     *
     * @return A {@link List} of {@link Entry} objects for all resources in the classpath and/or module path that
     *         have a non-unique path (i.e. where there are at least two resources with the same path), sorted by
     *         path. The key of each returned {@link Entry} is the path (obtained from {@link Resource#getPath()}),
     *         and the value is a {@link ResourceList} of at least two unique {@link Resource} objects that have
     *         that path. The entries are unmodifiable.
     */
    @Unmodifiable
    List<@Unmodifiable Entry<String, ResourceList>> findDuplicatePaths();

    /**
     * Find the subset of the {@link Resource} objects in this list for which the given filter predicate is true.
     *
     * @param filter
     *            The filter to apply. Only the {@link Resource} objects for which the filter returns true are
     *            copied to the returned list.
     * @return The subset of the {@link Resource} objects in this list for which the given filter predicate is true.
     */
    ResourceList filter(Predicate<Resource> filter);

    /**
     * A {@link FunctionalInterface} for consuming the contents of a {@link Resource} as a byte array.
     */
    @FunctionalInterface
    interface ByteArrayConsumer {
        /**
         * Consume the complete content of a {@link Resource} as a byte array.
         *
         * @param resource
         *            The {@link Resource} used to load the byte array.
         * @param byteArray
         *            The complete content of the resource.
         * @throws IOException
         *             if an IO exception occurs.
         */
        void accept(final Resource resource, final byte[] byteArray) throws IOException;
    }

    /**
     * Fetch the content of each {@link Resource} in this {@link ResourceList} as a byte array by calling
     * {@link Resource#load()}, which closes the resource before it returns, then pass the byte array to the given
     * {@link ByteArrayConsumer}.
     *
     * <p>
     * A resource that is already open cannot be loaded, so this method throws {@link IllegalStateException} when it
     * reaches one. That resource is left open, so whatever opened it can still use it.
     *
     * @param byteArrayConsumer
     *            The {@link ByteArrayConsumer}.
     * @return this (for method chaining).
     * @throws IOException
     *             if loading any of the resources, or the consumer itself, throws {@link IOException}.
     */
    ResourceList forEachByteArray(ByteArrayConsumer byteArrayConsumer) throws IOException;

    /**
     * The same as {@link #forEachByteArray(ByteArrayConsumer)}, but if loading a resource, or the consumer itself,
     * throws {@link IOException}, that resource is silently skipped and the iteration continues.
     *
     * @param byteArrayConsumer
     *            The {@link ByteArrayConsumer}.
     * @return this (for method chaining).
     */
    ResourceList forEachByteArrayIgnoringIOException(ByteArrayConsumer byteArrayConsumer);

    /**
     * A {@link FunctionalInterface} for consuming the contents of a {@link Resource} as an {@link InputStream}.
     */
    @FunctionalInterface
    interface InputStreamConsumer {
        /**
         * Consume a {@link Resource} as an {@link InputStream}.
         *
         * @param resource
         *            The {@link Resource} used to open the {@link InputStream}.
         * @param inputStream
         *            The {@link InputStream} opened on the resource.
         * @throws IOException
         *             if an IO exception occurs.
         */
        void accept(final Resource resource, final InputStream inputStream) throws IOException;
    }

    /**
     * Fetch an {@link InputStream} for each {@link Resource} in this {@link ResourceList}, pass the
     * {@link InputStream} to the given {@link InputStreamConsumer}, then close the {@link InputStream} after the
     * {@link InputStreamConsumer} returns, by calling {@link Resource#close()}.
     *
     * <p>
     * A resource that is already open cannot be opened again, so this method throws {@link IllegalStateException}
     * when it reaches one. That resource is left open, so whatever opened it can still use it.
     *
     * @param inputStreamConsumer
     *            The {@link InputStreamConsumer}.
     * @return this (for method chaining).
     * @throws IOException
     *             if opening any of the resources, or the consumer itself, throws {@link IOException}.
     */
    ResourceList forEachInputStream(InputStreamConsumer inputStreamConsumer) throws IOException;

    /**
     * The same as {@link #forEachInputStream(InputStreamConsumer)}, but if opening a resource, or the consumer
     * itself, throws {@link IOException}, that resource is silently skipped and the iteration continues.
     *
     * @param inputStreamConsumer
     *            The {@link InputStreamConsumer}.
     * @return this (for method chaining).
     */
    ResourceList forEachInputStreamIgnoringIOException(InputStreamConsumer inputStreamConsumer);

    /**
     * A {@link FunctionalInterface} for consuming the contents of a {@link Resource} as a {@link ByteBuffer}.
     */
    @FunctionalInterface
    interface ByteBufferConsumer {
        /**
         * Consume a {@link Resource} as a {@link ByteBuffer}.
         *
         * @param resource
         *            The {@link Resource} whose content is reflected in the {@link ByteBuffer}.
         * @param byteBuffer
         *            The {@link ByteBuffer} mapped to the resource.
         * @throws IOException
         *             if an IO exception occurs.
         */
        void accept(final Resource resource, final ByteBuffer byteBuffer) throws IOException;
    }

    /**
     * Read each {@link Resource} in this {@link ResourceList} as a {@link ByteBuffer}, pass the {@link ByteBuffer}
     * to the given {@link ByteBufferConsumer}, then release the {@link ByteBuffer} after the
     * {@link ByteBufferConsumer} returns, by calling {@link Resource#close()}.
     *
     * <p>
     * A resource that is already open cannot be read, so this method throws {@link IllegalStateException} when it
     * reaches one. That resource is left open, so whatever opened it can still use it.
     *
     * @param byteBufferConsumer
     *            The {@link ByteBufferConsumer}.
     * @return this (for method chaining).
     * @throws IOException
     *             if reading any of the resources, or the consumer itself, throws {@link IOException}.
     */
    ResourceList forEachByteBuffer(ByteBufferConsumer byteBufferConsumer) throws IOException;

    /**
     * The same as {@link #forEachByteBuffer(ByteBufferConsumer)}, but if reading a resource, or the consumer
     * itself, throws {@link IOException}, that resource is silently skipped and the iteration continues.
     *
     * @param byteBufferConsumer
     *            The {@link ByteBufferConsumer}.
     * @return this (for method chaining).
     */
    ResourceList forEachByteBufferIgnoringIOException(ByteBufferConsumer byteBufferConsumer);

    /** Close all the {@link Resource} objects in this {@link ResourceList}. */
    @Override
    void close();
}
