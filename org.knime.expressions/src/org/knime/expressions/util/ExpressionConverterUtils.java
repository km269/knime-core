package org.knime.expressions.util;

import java.util.HashMap;
import java.util.Iterator;

import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;

public class ExpressionConverterUtils {

	private static DataType[] DATA_TYPES;
	private static HashMap<DataType, DataCellToJavaConverterFactory<?, ?>> KNIME_TO_JAVA_CONVERTER_MAP;
	private static HashMap<Class<?>, JavaToDataCellConverterFactory<?>> JAVA_TO_KNIME_CONVERTER_MAP;

	static {
		DATA_TYPES = JavaToDataCellConverterRegistry.getInstance().getAllDestinationTypes().stream()
				.filter(d -> d.getCellFactory(null).orElse(null) instanceof FromSimpleString)
				.sorted((a, b) -> a.getName().compareTo(b.getName())).toArray(DataType[]::new);

		KNIME_TO_JAVA_CONVERTER_MAP = new HashMap<>(DATA_TYPES.length);
		JAVA_TO_KNIME_CONVERTER_MAP = new HashMap<>(DATA_TYPES.length);

		for (DataType type : DATA_TYPES) {
			Iterator<DataCellToJavaConverterFactory<?, ?>> iterator = DataCellToJavaConverterRegistry.getInstance()
					.getFactoriesForSourceType(type).iterator();

			if (iterator.hasNext()) {
				DataCellToJavaConverterFactory<?, ?> knimeToJavaConverter = iterator.next();

				KNIME_TO_JAVA_CONVERTER_MAP.put(type, knimeToJavaConverter);

				Iterator<?> iter2 = JavaToDataCellConverterRegistry.getInstance()
						.getConverterFactories(knimeToJavaConverter.getDestinationType(), type).iterator();

				if (iter2.hasNext()) {
					JavaToDataCellConverterFactory<?> javaToKnimeConverter = (JavaToDataCellConverterFactory<?>) iter2
							.next();

					JAVA_TO_KNIME_CONVERTER_MAP.put(knimeToJavaConverter.getDestinationType(), javaToKnimeConverter);
				} else {
					/* TODO: remove datatype */
				}
			} else {
				/* TODO: remove datatype */
			}
		}
	}

	/**
	 * 
	 * @return possible knime data types for which converter to java types exist.
	 */
	public static DataType[] possibleTypes() {
		return DATA_TYPES;
	}

	/**
	 * 
	 * @param type
	 *            data type that shall be returned.
	 * @return string containing the return type java would use.
	 */
	public static String extractJavaReturnString(DataType type) {
		return KNIME_TO_JAVA_CONVERTER_MAP.get(type).getDestinationType().getName();

		// return JAVA_RETURN_TYPES.get(type);
	}

	/**
	 * 
	 * @param type
	 *            data type that shall be returned.
	 * @return string containing the java imports needed for the specific return
	 *         type.
	 */
	public static String getJavaImport(DataType type) {
		return "import "+KNIME_TO_JAVA_CONVERTER_MAP.get(type).getDestinationType().getName()+";\n";
	}
}
