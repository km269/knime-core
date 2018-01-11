/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 */
package org.knime.expressions.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.node.ExecutionContext;

/**
 * Utility class to obtain the preferred converters between {@link DataType} and
 * Java {@link Class}. These converters can be used to convert {@link DataCell}
 * into Java objects, which in return are used as input for expressions.
 * Furthermore the result of the expressions can be converted into
 * {@link DataCell}.
 * 
 * Note: at the moment only the preferred converter will be returned. This may
 * be extended in the future as the number of factories may grow.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ExpressionConverterUtils {

	/*
	 * The data types for which converters are registered in both directions, i.e.
	 * from DataCell to Java objects and vice versa.
	 */
	private static DataType[] DATA_TYPES;

	/* Maps holding the converters. */
	private static HashMap<DataType, DataCellToJavaConverterFactory<?, ?>> KNIME_TO_JAVA_CONVERTER_MAP;
	private static HashMap<Class<?>, JavaToDataCellConverterFactory<?>> JAVA_TO_KNIME_CONVERTER_MAP;

	static {
		/* Retrieves the data types for which converters exist. */
		DATA_TYPES = JavaToDataCellConverterRegistry.getInstance().getAllDestinationTypes().stream()
				.filter(d -> d.getCellFactory(null).orElse(null) instanceof FromSimpleString)
				.sorted((a, b) -> a.getName().compareTo(b.getName())).toArray(DataType[]::new);

		KNIME_TO_JAVA_CONVERTER_MAP = new HashMap<>(DATA_TYPES.length);
		JAVA_TO_KNIME_CONVERTER_MAP = new HashMap<>(DATA_TYPES.length);

		/*
		 * Used to store only data types for which converters exist in both directions,
		 * i.e. to java objects with specific classes and from these java classes back.
		 */
		LinkedList<DataType> tempList = new LinkedList<>();

		for (DataType type : DATA_TYPES) {
			Iterator<DataCellToJavaConverterFactory<?, ?>> knimeToJavaIterator = DataCellToJavaConverterRegistry
					.getInstance().getFactoriesForSourceType(type).iterator();

			/* Gets for each type the preferred converter. */
			if (knimeToJavaIterator.hasNext()) {
				DataCellToJavaConverterFactory<?, ?> knimeToJavaConverter = knimeToJavaIterator.next();

				KNIME_TO_JAVA_CONVERTER_MAP.put(type, knimeToJavaConverter);

				Iterator<?> javaToKnimeIterator = JavaToDataCellConverterRegistry.getInstance()
						.getConverterFactories(knimeToJavaConverter.getDestinationType(), type).iterator();

				/* Gets for each class the preferred converter. */
				if (javaToKnimeIterator.hasNext()) {
					JavaToDataCellConverterFactory<?> javaToKnimeConverter = (JavaToDataCellConverterFactory<?>) javaToKnimeIterator
							.next();

					JAVA_TO_KNIME_CONVERTER_MAP.put(knimeToJavaConverter.getDestinationType(), javaToKnimeConverter);

					tempList.add(type);
				}
			}
		}

		/* Update the data types if converters don't exist in both directions for a specific type. */
		if (tempList.size() < DATA_TYPES.length) {
			DATA_TYPES = new DataType[tempList.size()];

			tempList.toArray(DATA_TYPES);
		}
	}

	/**
	 * 
	 * @return possible {@link DataType} for which converter to Java objects and
	 *         their inverse converter exist.
	 */
	public static DataType[] possibleTypes() {
		return DATA_TYPES;
	}

	/**
	 * 
	 * @param type
	 *            {@link DataType} that shall be returned by the expression.
	 * @return string containing the Java return type that shall be returned by the
	 *         script. Returns empty string if there is no preferred Java return
	 *         type mapped to the provided {@link DataType}.
	 */
	public static String extractJavaReturnString(DataType type) {
		if (!KNIME_TO_JAVA_CONVERTER_MAP.containsKey(type)) {
			return "";
		}
		return KNIME_TO_JAVA_CONVERTER_MAP.get(type).getDestinationType().getName();
	}

	/**
	 * 
	 * @param type
	 *            data type that shall be returned.
	 * @return string containing the java imports needed for the specific return
	 *         type.
	 */
	public static String getJavaImport(DataType type) {
		return "import " + KNIME_TO_JAVA_CONVERTER_MAP.get(type).getDestinationType().getName() + ";\n";
	}

	/**
	 * Returns the preferred Java to {@link DataCell} converter for the provided
	 * class.
	 * 
	 * @param javaClass
	 *            {@link Class} to convert from.
	 * @param context
	 *            {@link ExecutionContext} which may be used for creating the
	 *            {@link CellFactory}. You may use {@code null}.
	 * @return {@link JavaToDataCellConverter} converting the provided {@link Class}
	 *         to the preferred {@link DataCell}. Returns {@code null} if such a
	 *         converter or its reversing converter is not registered.
	 */
	public static JavaToDataCellConverter<?> getJavaToDataCellConverter(Class<?> javaClass, ExecutionContext context) {
		if (!JAVA_TO_KNIME_CONVERTER_MAP.containsKey(javaClass)) {
			return null;
		}

		return JAVA_TO_KNIME_CONVERTER_MAP.get(javaClass).create(context);
	}

	/**
	 * Returns the preferred {@link DataCell} to Java converted for the provided
	 * {@link DataType}.
	 * 
	 * @param dataType
	 *            {@link DataType} to convert from.
	 * @return {@link DataCellToJavaConverter} converting the provided
	 *         {@link DataType} to the preferred Java object. Returns {@code null}
	 *         if such a converter or its reversing converter is not registered.
	 */
	public static DataCellToJavaConverter<?, ?> getKnimeToJavaConverter(DataType dataType) {
		return KNIME_TO_JAVA_CONVERTER_MAP.get(dataType).create();
	}
}
