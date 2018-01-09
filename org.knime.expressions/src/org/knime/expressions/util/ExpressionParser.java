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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.node.workflow.FlowVariable;
import org.scijava.plugins.scripting.groovy.GroovyScriptLanguage;
import org.scijava.script.ScriptLanguage;

/**
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class ExpressionParser {
	private final String MAIN_METHOD = "mainStart";
	private HashMap<String, ParsedExpression> m_expressionMap;
	private HashMap<String, ScriptEngine> m_expressionInvocableMap;

	/* TODO: Change exceptions to exceptions not parsed and parse exceptions. */
	public void parseExpressions(String[] columnNames, String... expressions) {
		if (m_expressionMap == null) {
			m_expressionMap = new HashMap<>(expressions.length);
		}

		/*
		 * Used to guarantee that the map does not contain expressions, which aren't
		 * used anymore.
		 */
		HashMap<String, ParsedExpression> tempExpressionMap = new HashMap<>(expressions.length);

		for (String expression : expressions) {

			/* Checks if the expression has been already parsed. */
			if (!m_expressionMap.containsKey(expression)) {

				/*
				 * Escape characters used to mark column names and flow variables in the
				 * expression
				 */
				String escapeColumn = ExpressionCompletionProvider.getEscapeColumnSymbol();
				String escapeFlowVariable = ExpressionCompletionProvider.getEscapeFlowVariableSymbol();

				/*
				 * Map flow variables/columns with their escape characters to their actual name.
				 */
				HashMap<String, String> flowVariableMap = new HashMap<>();
				HashMap<String, String> columnMap = new HashMap<>();

				/*
				 * Split expression into lines, so that we are able to parse line by line. This
				 * makes it easier concerning the handling of the end delimiter (e.g. if its not
				 * in the same line).
				 */
				String[] lines = StringUtils.split(expression, "\n");

				for (int i = 0; i < lines.length; i++) {
					int startIndex = 0;
					String line = lines[i];

					/*
					 * As long as we find a starting delimiter we have to search for an ending
					 * delimiter and check if the flow variable or column name exists.
					 */
					while ((startIndex = StringUtils.indexOf(line, escapeColumn, startIndex)) >= 0) {
						int nextIndex = StringUtils.indexOf(line, escapeColumn, startIndex + 1);

						if (nextIndex < 0) {
							throw new IllegalArgumentException(
									"No such column: " + StringUtils.substring(line, startIndex + 1) + " (at line " + i
											+ ") \n\n expression: \n" + expression);
						}

						if (nextIndex == startIndex + 1) {
							/*
							 * We found the start of a flow variable. NOTE: this assumes that the escape
							 * character is the same as for column names but occurs twice. Possible TODO
							 */
							nextIndex = StringUtils.indexOf(expression, escapeFlowVariable, nextIndex + 1);

							if (nextIndex < 0) {
								throw new IllegalArgumentException(
										"Invalid special identifier: " + StringUtils.substring(line, startIndex + 1)
												+ " (at line " + i + ")  \n\n expression: \n" + expression);
							}

							/* TODO: check if flow variable exists. */

							String foundVariable = StringUtils.substring(line, startIndex, nextIndex + 2);
							String flowVariable = StringUtils.substring(foundVariable, 2, foundVariable.length() - 2);

							flowVariableMap.put(foundVariable, flowVariable);

							/*
							 * Update startIndex for the next search so that we won't accidentally parse an
							 * ending delimiter as a starting delimiter.
							 */
							startIndex = nextIndex + 2;
						} else {
							/* Found a column name. */
							String foundColumn = StringUtils.substring(line, startIndex, nextIndex + 1);
							String column = StringUtils.substring(foundColumn, 1, foundColumn.length() - 1);

							if (!ArrayUtils.contains(columnNames, column)) {
								/* TODO: check previously added columns for derive field nodes. */
								throw new IllegalArgumentException(
										"Colum '" + column + "' is not known. \n\n expression: \n" + expression);
							}

							columnMap.put(foundColumn, column);
							startIndex = nextIndex + 1;
						}

					}
				}

				/*
				 * Store the used flow variables, column names, the original expression and the
				 * parsed expression (i.e. with removed delimiters) into the map.
				 */
				String[] flowVariables = new String[flowVariableMap.size()];
				String[] columns = new String[columnMap.size()];

				flowVariableMap.values().toArray(flowVariables);
				columnMap.values().toArray(columns);

				String parsedExpression = StringUtils.replace(expression, escapeColumn, "");

				tempExpressionMap.put(expression,
						new ParsedExpression(expression, parsedExpression, columns, flowVariables));
			} else {
				/* We already parsed the expression so just keep it. */
				tempExpressionMap.put(expression, m_expressionMap.get(expression));
			}
		}

		m_expressionMap = tempExpressionMap;
	}

	public void checkExpressions(String[] expressions, DataType[] returnTypes) {
		if (m_expressionMap == null) {
			throw new IllegalStateException("Expressions have do be parsed before checking.");
		} else if(expressions.length != returnTypes.length) {
			throw new IllegalStateException("Number of expressions ("+expressions.length+") does not match number of return types ("+returnTypes.length+").");
		}

		if (m_expressionInvocableMap == null) {
			m_expressionInvocableMap = new HashMap<>(expressions.length);
		}

		HashMap<String, ScriptEngine> tempInvocableMap = new HashMap<>(expressions.length);

		for (int i = 0; i < expressions.length; i++) {
			String expressionString = expressions[i];
			String returnString = returnTypes[i].getName();
			
			if (!m_expressionMap.containsKey(expressionString)) {
				throw new IllegalArgumentException(
						"The following expression has not been parsed: \n" + expressionString);
			}

			if (!m_expressionInvocableMap.containsKey(expressionString)) {

				ParsedExpression expression = m_expressionMap.get(expressionString);

				/*
				 * TODO: introduce statics for input and method name -> important to forbid the
				 * name of the main-method.
				 */
				String header = "//@INPUT " + ArrayUtils.toString(expression.getColumns(), "")
						+ ArrayUtils.toString(expression.getFlowVariables(), "");

				/* TODO: strip returnString down to basic type. */
				String script = header + "\n def "+returnString+" "+MAIN_METHOD+"(){" + expression.getParsedExpression() + "}";

				ScriptLanguage language = new GroovyScriptLanguage();
				ScriptEngine engine = language.getScriptEngine();

				try {
					engine.eval(script);

					tempInvocableMap.put(expressionString, engine);
				} catch (ScriptException e) {
					// TODO analyze exception and return better error, i.e. strip exception down to
					// line and error.
					e.printStackTrace();
				}
			} else {
				tempInvocableMap.put(expressionString, m_expressionInvocableMap.get(expressionString));
			}
		}

		m_expressionInvocableMap = tempInvocableMap;
	}

	public Object computeExpression(String expression, Object... input) {
		ScriptEngine engine = m_expressionInvocableMap.get(expression);
		String[] columns = m_expressionMap.get(expression).getColumns();

		for (int i = 0; i < columns.length; i++) {
			engine.put(columns[i], input[i]);
		}
		
		try {
			return ((Invocable) engine).invokeFunction("mainStart");
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Computation failed.");
		} catch (ScriptException e) {
			throw new IllegalStateException("Computation failed.");
		}
	}

	public String[] getUsedColumnNames(String expression) {
		if (!m_expressionMap.containsKey(expression)) {
			throw new IllegalStateException("Given Expression has not been parsed yet.");
		}

		return m_expressionMap.get(expression).getColumns();
	}
}
