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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.jetbrains.annotations.Unmodifiable;

import io.github.classgraph.base.internal.path.PathSyntax;
import io.github.classgraph.base.internal.utils.Assert;

/**
 * Implementation of ResourceList.
 */
final class ResourceListImpl extends UnmodifiableList<Resource> implements ResourceList {
    /** An unmodifiable empty {@link ResourceListImpl}. */
    static final ResourceList EMPTY_LIST = new ResourceListImpl(List.of());

    /**
     * Constructor.
     *
     * @param resources
     *            the elements of the list
     */
    ResourceListImpl(final List<Resource> resources) {
        super(resources);
    }

    /**
     * Factory. This list claims the given list rather than copying it, so the caller must not use the list
     * again after handing it over. Resources are listed in the order they were found on the classpath and module
     * path, so unlike an {@link InfoList}, a {@link ResourceListImpl} is never sorted.
     *
     * @param resources
     *            the elements of the list
     */
    static ResourceListImpl wrap(final List<Resource> resources) {
        return new ResourceListImpl(resources);
    }

    /**
     * Normalize a resource path given to a lookup method, so that it can be compared with
     * {@link Resource#getPath()}: any leading or trailing '/' is removed, empty and "." segments are removed, and
     * each ".." segment removes the segment before it, without going above the package root. A '!' is an ordinary
     * character, and the filesystem is not read. Every method that looks up a resource by path calls this, so that
     * they all accept the same spellings of a path.
     *
     * @param resourcePath
     *            the path to normalize.
     * @return the normalized path.
     */
    static String normalizeResourcePath(final String resourcePath) {
        return PathSyntax.sanitizeRelativePath(resourcePath, /* removeFinalSlash = */ true);
    }

    @Override
    public ResourceList get(final String resourcePath) {
        Assert.notNull(resourcePath, "resourcePath");
        return getNormalizedPath(normalizeResourcePath(resourcePath));
    }

    /**
     * Returns a list of all resources whose path is equal to a path that has already been normalized by
     * {@link #normalizeResourcePath(String)}, without normalizing it again.
     *
     * @param path
     *            the normalized path.
     * @return the resources in this list that have the given path, or the empty list if there are none.
     */
    ResourceList getNormalizedPath(final String path) {
        final List<Resource> matchingResources = new ArrayList<>(2);
        for (final Resource res : this) {
            if (res.getPath().equals(path)) {
                matchingResources.add(res);
            }
        }
        return matchingResources.isEmpty() ? EMPTY_LIST : ResourceListImpl.wrap(matchingResources);
    }

    // -------------------------------------------------------------------------------------------------------------

    @Unmodifiable
    @Override
    public List<String> getPaths() {
        final List<String> resourcePaths = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourcePaths.add(resource.getPath());
        }
        return Collections.unmodifiableList(resourcePaths);
    }

    @Unmodifiable
    @Override
    public List<String> getPathsRelativeToContainer() {
        final List<String> resourcePaths = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourcePaths.add(resource.getPathRelativeToContainer());
        }
        return Collections.unmodifiableList(resourcePaths);
    }

    @Unmodifiable
    @Override
    public List<URL> getURLs() {
        final List<URL> resourceURLs = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourceURLs.add(resource.getURL());
        }
        return Collections.unmodifiableList(resourceURLs);
    }

    @Unmodifiable
    @Override
    public List<URI> getURIs() {
        final List<URI> resourceURIs = new ArrayList<>(this.size());
        for (final Resource resource : this) {
            resourceURIs.add(resource.getURI());
        }
        return Collections.unmodifiableList(resourceURIs);
    }

    // -------------------------------------------------------------------------------------------------------------

    /** Returns true if a Resource is a classfile. */
    private static final Predicate<Resource> CLASSFILE_FILTER = resource -> ClassNames
            .isClassfilePath(resource.getPath());

    @Override
    public ResourceList classFilesOnly() {
        return filter(CLASSFILE_FILTER);
    }

    @Override
    public ResourceList nonClassFilesOnly() {
        return filter(CLASSFILE_FILTER.negate());
    }

    // -------------------------------------------------------------------------------------------------------------

    @Unmodifiable
    @Override
    public Map<String, ResourceList> asMap() {
        final Map<String, List<Resource>> pathToResources = new TreeMap<>();
        for (final Resource resource : this) {
            pathToResources.computeIfAbsent(resource.getPath(), _ -> new ArrayList<>(1)).add(resource);
        }
        final Map<String, ResourceList> pathToResourceList = new TreeMap<>();
        for (final Map.Entry<String, List<Resource>> ent : pathToResources.entrySet()) {
            pathToResourceList.put(ent.getKey(), ResourceListImpl.wrap(ent.getValue()));
        }
        return Collections.unmodifiableMap(pathToResourceList);
    }

    @Unmodifiable
    @Override
    public List<@Unmodifiable Entry<String, ResourceList>> findDuplicatePaths() {
        final List<Entry<String, ResourceList>> duplicatePaths = new ArrayList<>();
        // asMap() is sorted by path, and its entries are unmodifiable
        for (final Entry<String, ResourceList> pathAndResourceList : asMap().entrySet()) {
            if (pathAndResourceList.getValue().size() > 1) {
                duplicatePaths.add(pathAndResourceList);
            }
        }
        return Collections.unmodifiableList(duplicatePaths);
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public ResourceList filter(final Predicate<Resource> filter) {
        Assert.notNull(filter, "filter");
        final List<Resource> resourcesFiltered = new ArrayList<>();
        for (final Resource resource : this) {
            if (filter.test(resource)) {
                resourcesFiltered.add(resource);
            }
        }
        return new ResourceListImpl(resourcesFiltered);
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public ResourceList forEachByteArray(final ByteArrayConsumer byteArrayConsumer) throws IOException {
        Assert.notNull(byteArrayConsumer, "byteArrayConsumer");
        for (final Resource resource : this) {
            // load() has closed the resource by the time it returns, so there is nothing left here to close.
            // Closing the resource anyway would close it for whoever has it open, if load() threw because the
            // resource was already open, or if the consumer reopened it.
            byteArrayConsumer.accept(resource, resource.load());
        }
        return this;
    }

    @Override
    public ResourceList forEachByteArrayIgnoringIOException(final ByteArrayConsumer byteArrayConsumer) {
        Assert.notNull(byteArrayConsumer, "byteArrayConsumer");
        for (final Resource resource : this) {
            try {
                byteArrayConsumer.accept(resource, resource.load());
            } catch (final IOException _) {
                // Ignore
            }
        }
        return this;
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public ResourceList forEachInputStream(final InputStreamConsumer inputStreamConsumer) throws IOException {
        Assert.notNull(inputStreamConsumer, "inputStreamConsumer");
        for (final Resource resource : this) {
            // Open the resource before the try block, so that it is only closed if this method opened it. If open()
            // throws, the resource is either open for someone else, or open() has left it closed.
            final var inputStream = resource.open();
            try (resource) {
                inputStreamConsumer.accept(resource, inputStream);
            }
        }
        return this;
    }

    @Override
    public ResourceList forEachInputStreamIgnoringIOException(final InputStreamConsumer inputStreamConsumer) {
        Assert.notNull(inputStreamConsumer, "inputStreamConsumer");
        for (final Resource resource : this) {
            try {
                final var inputStream = resource.open();
                try (resource) {
                    inputStreamConsumer.accept(resource, inputStream);
                }
            } catch (final IOException _) {
                // Ignore
            }
        }
        return this;
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public ResourceList forEachByteBuffer(final ByteBufferConsumer byteBufferConsumer) throws IOException {
        Assert.notNull(byteBufferConsumer, "byteBufferConsumer");
        for (final Resource resource : this) {
            // Read the resource before the try block, so that it is only closed if this method opened it. If read()
            // throws, the resource is either open for someone else, or read() has left it closed.
            final var closeableByteBuffer = resource.read();
            try (resource) {
                // The buffer is only valid until the resource is closed, which is why the consumer is called with
                // the plain ByteBuffer rather than something it could close early
                byteBufferConsumer.accept(resource, Objects.requireNonNull(closeableByteBuffer.getByteBuffer()));
            }
        }
        return this;
    }

    @Override
    public ResourceList forEachByteBufferIgnoringIOException(final ByteBufferConsumer byteBufferConsumer) {
        Assert.notNull(byteBufferConsumer, "byteBufferConsumer");
        for (final Resource resource : this) {
            try {
                final var closeableByteBuffer = resource.read();
                try (resource) {
                    byteBufferConsumer.accept(resource,
                            Objects.requireNonNull(closeableByteBuffer.getByteBuffer()));
                }
            } catch (final IOException _) {
                // Ignore
            }
        }
        return this;
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public void close() {
        for (final Resource resource : this) {
            resource.close();
        }
    }
}
