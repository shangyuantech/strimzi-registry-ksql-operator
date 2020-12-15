package io.strimzi.operator.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

public class ValidationUtils {

    public static void checkStringEmpty(final String str, final String errorMessage) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void checkArgument(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

//    public static boolean isCollectSame(Collection<?> a, Collection<?> b) {
//        if (a == null && b != null) {
//            return false;
//        } else if (a == null) {
//            return true;
//        } else {
//            return CollectionUtils.isEqualCollection(a, b);
//        }
//    }

    public static <T> boolean isObjectSame(final T a, final T b) {
        if (a == null && b != null) {
            return false;
        } else if (a == null) {
            return true;
        } else {
            if (a instanceof Collection<?>) {
                return CollectionUtils.isEqualCollection((Collection<?>) a, (Collection<?>) b);
            } else {
                return a.equals(b);
            }
        }
    }
}
