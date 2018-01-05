package org.knime.expressions.util;

import java.util.HashMap;

import javax.script.Invocable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ExpressionParser {
	private HashMap<String, ParsedExpression> m_expressionMap;
	private HashMap<String, Invocable> m_expressionInvocableMap;

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

//	public void checkExpressions(String... expressions) {
//		if (m_expressionMap == null) {
//			throw new IllegalStateException("Expressions have do be parsed before checking.");
//		}
//		
//		if(m_expressionInvocableMap == null) {
//			m_expressionInvocableMap = new HashMap<>(expressions.length);
//		}
//
//		HashMap<String, Invocable> tempInvocableMap = new HashMap<>(expressions.length);
//
//		for (String expressionString : expressions) {
//			if (!m_expressionMap.containsKey(expressionString)) {
//				throw new IllegalArgumentException(
//						"The following expression has not been parsed: \n" + expressionString);
//			}
//
//			if (!m_expressionInvocableMap.containsKey(expressionString)) {
//
//				ParsedExpression expression = m_expressionMap.get(expressionString);
//
//				/*
//				 * TODO: introduce statics for input and method name -> important to forbid the
//				 * name of the main-method.
//				 */
//				String header = "//@INPUT " + ArrayUtils.toString(expression.getColumns(), "")
//						+ ArrayUtils.toString(expression.getFlowVariables(), "");
//
//				String script = header + "\n def mainStart(){" + expression.getParsedExpression() + "}";
//
//				ScriptLanguage language = new GroovyScriptLanguage();
//				ScriptEngine engine = language.getScriptEngine();
//
//				try {
//					engine.eval(script);
//
//					tempInvocableMap.put(expressionString, (Invocable) engine);
//				} catch (ScriptException e) {
//					// TODO analyze exception and return better error, i.e. strip exception down to
//					// line and error.
//					e.printStackTrace();
//				}
//			} else {
//				tempInvocableMap.put(expressionString, m_expressionInvocableMap.get(expressionString));
//			}
//		}
//
//		m_expressionInvocableMap = tempInvocableMap;
//	}
//	
//	public void getReturnTypes(DataTableSpec inSpec, Map<String, FlowVariable> availableFlowVariables, String... expressions) {
//		if(m_expressionInvocableMap == null) {
//			checkExpressions(expressions);
//		}
//		
//		for(String expressionString : expressions) {
//			ParsedExpression expression = m_expressionMap.get(expressionString);
//			
//			for(String columnName : expression.getColumns()) {
//				/* TODO: create dummies that can be used to determine the outcome */
//				
//				
//			}
//		}
//	}
	
	
}
