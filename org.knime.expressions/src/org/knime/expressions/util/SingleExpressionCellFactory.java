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
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A {@link SingleCellFactory} that produces {@link DataCell} based on a given
 * expression that is being parsed and executed for each row.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class SingleExpressionCellFactory extends SingleCellFactory {
	private final ExpressionParser m_expressionParser;
	private final String m_expression;
	private final Map<String, Integer> m_columnNameIndexMap;
	private final Map<String, FlowVariable> m_flowVariableMap;
	private final ExecutionContext m_executionContext;

	private DataCellToJavaConverter<?, ?>[] m_dataCellToJavaConverters;
	@SuppressWarnings("rawtypes")
	private JavaToDataCellConverter m_javaToDataCellConverter;

	/**
	 * Constructs a {@link CellFactory} adding a single column with the given
	 * {@link DataType} by parsing the expression once and executing it for every
	 * row.
	 * 
	 * @param inSpec
	 *            the original {@link DataColumnSpec}.
	 * @param expression
	 *            that shall be parsed and executed.
	 * @param columnNameIndexMap
	 *            mapping from the column names to their indices.
	 * @param resultType
	 *            {@link DataType} the resulting column will have.
	 * @param exec
	 *            current {@link ExecutionContext}.
	 */
	public SingleExpressionCellFactory(DataColumnSpec inSpec, String expression,
			Map<String, Integer> columnNameIndexMap, DataType resultType, ExecutionContext exec) {
		this(inSpec, expression, columnNameIndexMap, resultType, null, exec);
	}

	/**
	 * Constructs a {@link CellFactory} adding a single column with the given
	 * {@link DataType} by parsing the expression once and executing it for every
	 * row.
	 * 
	 * @param inSpec
	 *            the original {@link DataColumnSpec}.
	 * @param expression
	 *            that shall be parsed and executed.
	 * @param columnNameIndexMap
	 *            mapping from the column names to their indices.
	 * @param resultType
	 *            {@link DataType} the resulting column will have.
	 * @param flowVariables
	 *            mapping from flow variable names to the actual
	 *            {@link FlowVariable}.
	 * @param exec
	 *            current {@link ExecutionContext}.
	 */
	public SingleExpressionCellFactory(DataColumnSpec inSpec, String expression,
			Map<String, Integer> columnNameIndexMap, DataType resultType, Map<String, FlowVariable> flowVariables,
			ExecutionContext exec) {
		super(inSpec);

		m_expressionParser = new ExpressionParser();
		m_expression = expression;
		m_columnNameIndexMap = columnNameIndexMap;
		m_executionContext = exec;
		m_flowVariableMap = flowVariables;

		String[] columnNames = new String[columnNameIndexMap.size()];
		columnNameIndexMap.keySet().toArray(columnNames);

		m_expressionParser.parseExpressions(columnNames, ExpressionConverterUtils.extractFlowVariables(flowVariables),
				expression);
		m_expressionParser.checkExpressions(new String[] { expression }, new DataType[] { resultType });
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DataCell getCell(DataRow row) {
		String[] usedColumnNames = m_expressionParser.getUsedColumnNames(m_expression);
		Object[] cellData = new Object[usedColumnNames.length];

		/* Gets all converters that convert DataCells to Java Objects only once. */
		if (m_dataCellToJavaConverters == null) {
			m_dataCellToJavaConverters = new DataCellToJavaConverter[usedColumnNames.length];

			for (int i = 0; i < m_dataCellToJavaConverters.length; i++) {
				DataCell cell = row.getCell(m_columnNameIndexMap.get(usedColumnNames[i]));
				m_dataCellToJavaConverters[i] = ExpressionConverterUtils.getDataCellToJavaConverter(cell.getType());
			}
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
				return DataType.getMissingCell();
			}

			try {
				cellData[i] = m_dataCellToJavaConverters[i].convertUnsafe(cell);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/* Execute expression and convert the result into the specified data type. */
		Object result = m_expressionParser.computeExpression(m_expression,
				ExpressionConverterUtils.extractFlowVariables(m_flowVariableMap), cellData);

		if (m_javaToDataCellConverter == null) {
			m_javaToDataCellConverter = ExpressionConverterUtils.getJavaToDataCellConverter(result.getClass(),
					m_executionContext);
		}

		try {
			return m_javaToDataCellConverter.convert(result);
		} catch (Exception e) {
			throw new IllegalStateException("Was not able to convert result into DataCell");
		}
	}

}
