package io.github.classgraph;

import java.util.List;
import kotlin.Metadata;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Compiled from a Kotlin equivalent so inheritors can behave like a Kotlin unmodifiable collection or view.
 *
 * @param <E>
 *            Type of element
 */
@Metadata(mv = { 2, 4, 0 }, k = 1, xi = 48, d1 = {
        "\u0000\f\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\b`\u0018\u0000*\u0004\b\u0000\u0010\u00012\b\u0012\u0004\u0012\u0002H\u00010\u0002¨\u0006\u0003À\u0006\u0003" },
        d2 = { "Lio/github/classgraph/internal/ReadOnlyList;", "E", "", "Sources of classgraph (1).main" })
@Unmodifiable
interface ReadOnlyList<E> extends List<E> {
}
