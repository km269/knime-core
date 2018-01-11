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
 *
 */
package org.knime.expressions.util;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.FlowVariable;

/**
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class MultiExpressionCellFactory extends AbstractCellFactory {

	private final ExpressionParser m_expressionParser;
	private final String[] m_expressions;
	private final Map<String, Integer> m_columnNameIndexMap;
	private final ExecutionContext m_executionContext;
	private final Map<String, FlowVariable> m_flowVariableMap;

	private HashMap<String, DataCellToJavaConverter<?, ?>[]> m_dataCellToJavaConverterMap;
	@SuppressWarnings("rawtypes")
	private HashMap<String, JavaToDataCellConverter> m_javaToDataCellConverterMap;

	public MultiExpressionCellFactory(DataColumnSpec[] inSpec, String[] expressions,
			Map<String, Integer> columnNameIndexMap, DataType[] resultTypes, Map<String, FlowVariable> flowVariables,
			ExecutionContext exec) {
		super(inSpec);

		if (resultTypes.length != expressions.length) {
			throw new IllegalArgumentException(
					"Number of provided resulting data types is not equal to the number of provided expressions.");
		} else if (inSpec.length != expressions.length) {
			throw new IllegalArgumentException(
					"Number of data column specs is not equal to the number of provided expressions");
		}

		m_expressionParser = new ExpressionParser();
		m_expressions = expressions;
		m_columnNameIndexMap = columnNameIndexMap;
		m_flowVariableMap = flowVariables;
		m_executionContext = exec;

		m_dataCellToJavaConverterMap = new HashMap<>(expressions.length);
		m_javaToDataCellConverterMap = new HashMap<>(expressions.length);

		String[] columnNames = new String[columnNameIndexMap.size()];
		columnNameIndexMap.keySet().toArray(columnNames);

		m_expressionParser.parseExpressions(columnNames,
				ExpressionConverterUtils.extractFlowVariables(m_flowVariableMap), expressions);
		m_expressionParser.checkExpressions(expressions, resultTypes);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DataCell[] getCells(DataRow row) {
		DataCell[] returnCells = new DataCell[m_expressions.length];

		/* Outer loop to iterate over the expressions and execute them. */
		outerLoop: for (int j = 0; j < returnCells.length; j++) {
			String expression = m_expressions[j];

			String[] usedColumnNames = m_expressionParser.getUsedColumnNames(expression);
			Object[] cellData = new Object[usedColumnNames.length];

			/* Gets all converters that convert DataCells to Java Objects only once. */
			if (!m_dataCellToJavaConverterMap.containsKey(expression)) {
				DataCellToJavaConverter<?, ?>[] dataCellToJavaConverters = new DataCellToJavaConverter[usedColumnNames.length];

				for (int i = 0; i < dataCellToJavaConverters.length; i++) {
					DataCell cell = row.getCell(m_columnNameIndexMap.get(usedColumnNames[i]));
					dataCellToJavaConverters[i] = ExpressionConverterUtils.getDataCellToJavaConverter(cell.getType());
				}

				m_dataCellToJavaConverterMap.put(expression, dataCellToJavaConverters);
			}

			/* Get the input data used by the expression. */
			for (int i = 0; i < cellData.length; i++) {
				String column = usedColumnNames[i];
				DataCell cell = row.getCell(m_columnNameIndexMap.get(column));

				/*
				 * Check if one of the input cells is missing. If so, return a missing cell.
				 * TODO: let the script handle missing cells?
				 */
				if (cell.isMissing()) {
					returnCells[j] = DataType.getMissingCell();
					continue outerLoop;
				}

				try {
					cellData[i] = m_dataCellToJavaConverterMap.get(expression)[i].convertUnsafe(cell);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			/* Execute expression and convert the result into the specified data type. */
			Object result = m_expressionParser.computeExpression(expression,
					ExpressionConverterUtils.extractFlowVariables(m_flowVariableMap), cellData);

			if (!m_javaToDataCellConverterMap.containsKey(expression)) {
				m_javaToDataCellConverterMap.put(expression,
						ExpressionConverterUtils.getJavaToDataCellConverter(result.getClass(), m_executionContext));
			}

			try {
				returnCells[j] = m_javaToDataCellConverterMap.get(expression).convert(result);
			} catch (Exception e) {
				throw new IllegalStateException("Was not able to convert result into DataCell");
			}
		}

		return returnCells;
	}
}
