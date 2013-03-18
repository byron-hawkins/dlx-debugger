package org.hawkinssoftware.dlx;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DLXUtils {
	public static <E extends Enum<E>> IllegalArgumentException unknownEnumException(E e) {
		throw new IllegalArgumentException("Unknown enum value " + e);
	}

	public static IllegalArgumentException unknownSubtypeException(Object subtype, Class baseType) {
		throw new IllegalArgumentException("Unknown subtype " + subtype.getClass().getName() + " of type " + baseType.getName());
	}
}
