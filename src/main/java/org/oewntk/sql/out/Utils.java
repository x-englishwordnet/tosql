/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.sql.out;

import java.io.PrintStream;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class Utils
{
	private static final BinaryOperator<Integer> mergingKeepFunction = (existing, replacement) -> {
		if (existing.equals(replacement))
		{
			throw new IllegalArgumentException(existing + "," + replacement);
		}
		return existing;
	};

	private static final BinaryOperator<Integer> mergingReplaceFunction = (existing, replacement) -> {
		if (existing.equals(replacement))
		{
			throw new IllegalArgumentException(existing + "," + replacement);
		}
		return replacement;
	};

	// map factory

	/**
	 * Make NID
	 * To be used with objects that support equal and comparable
	 *
	 * @param stream must be distinct
	 * @param <T>    type of stream elements
	 * @return map if object-to-NID pairs
	 */
	public static <T> Map<T, Integer> makeNIDMap(final Stream<T> stream)
	{
		//final AtomicInteger index = new AtomicInteger();
		//index.set(0);
		final int[] i = {0};
		return stream //
				.sequential() //
				.sorted() //
				.peek(e -> ++i[0]) //
				.map(item -> new SimpleEntry<>(item, i[0] /* index.addAndGet(1) */)) //
				.collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue, (existing, replacement) -> {
					throw new IllegalArgumentException(existing + "," + replacement);
				}, TreeMap::new));
	}

	public static <T> void generateTable(final PrintStream ps, final String table, final String columns, final Map<Integer, T> byId, final Function<Entry<Integer, T>, String> toString)
	{
		// make object-to-nid map
		Stream<Entry<Integer, T>> stream = byId.entrySet().stream() //
				.sorted(Comparator.comparingInt(Entry::getKey));

		// insert map
		Printers.printInsert(ps, table, columns, stream, toString, false);
	}

	// escape

	public static String escape(final String str)
	{
		return str.replace("'", "''");
	}
}
