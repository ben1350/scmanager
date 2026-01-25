package rest;

import io.quarkus.qute.TemplateExtension;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TemplateExtension
public class PaginationExtensions {

    /**
     * Supports: {page.plus(1)}
     * Used for the "Next" button link.
     */
    public static int plus(Integer value, int add) {
        return value + add;
    }

    /**
     * Supports: {page.minus(1)}
     * Used for the "Previous" button link.
     */
    public static int minus(Integer value, int sub) {
        return value - sub;
    }

    /**
     * Resolves the {1..totalPages} issue.
     * Usage in HTML: {#for i in totalPages.range}
     */
    public static List<Integer> range(Integer totalPages) {
        if (totalPages == null || totalPages < 1) {
            return List.of(1);
        }
        return IntStream.rangeClosed(1, totalPages)
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Logic-less CSS class toggling.
     * Usage: <li class="{page.is(i, 'active blue', 'waves-effect')}">
     */
    public static String is(Integer currentPage, int loopIndex, String trueClass, String falseClass) {
        return (currentPage != null && currentPage == loopIndex) ? trueClass : falseClass;
    }

    /**
     * Optional: Helper for boundary checks.
     * Usage: {#if page.isFirst} ... {/if}
     */
    public static boolean isFirst(Integer page) {
        return page != null && page <= 1;
    }

    /**
     * Optional: Helper for boundary checks.
     * Usage: {#if page.isLast(totalPages)} ... {/if}
     */
    public static boolean isLast(Integer page, int totalPages) {
        return page != null && page >= totalPages;
    }
}
