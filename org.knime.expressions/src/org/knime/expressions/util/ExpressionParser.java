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
import java.util.Iterator;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.scijava.plugins.scripting.groovy.GroovyScriptLanguage;
import org.scijava.script.ScriptLanguage;

/**
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class ExpressionParser {
	private final static String[] SPECIAL_CHARACTERS = new String[] { ",", ".", ";", ":", "-", "#", "'", "+", "*", "~",
			"`", "´", "=", "}", "]", ")", "[", "(", "/", "{", "&", "%", "$", "\\", "§", "³", "²", "!", "\"", "^", "°",
			"<", ">", "|" };
	private final String MAIN_METHOD = "mainStart";
	private HashMap<String, ParsedExpression> m_expressionMap;
	private HashMap<String, ScriptEngine> m_expressionInvocableMap;

	/* TODO: Change exceptions to exceptions not parsed and parse exceptions. */

	/*
	 * TODO: special characters like '.' etc in column name => may yield problems.
	 */

	/**
	 * Parses the expression(s) and replaces the usages of column names by their
	 * actual names. Parsing already parsed expressions will do nothing but omit
	 * expressions, which aren't provided as input.
	 * 
	 * @param columnNames
	 *            all available column names.
	 * @param expressions
	 *            that shall be parsed.
	 */
	public void parseExpressions(String[] columnNames, String... expressions) {
		this.parseExpressions(columnNames, null, expressions);
	}

	/**
	 * Parses the expression and replaces the usages of flow variables and column
	 * names by their actual names. Parsing already parsed expressions will do
	 * nothing but omit expressions, which aren't provided as input.
	 * 
	 * @param columnNames
	 *            all available column names.
	 * @param flowVariableMap
	 *            map containing the {@link FlowVariable}s.
	 * @param expressions
	 *            that shall be parsed.
	 */
	public void parseExpressions(String[] columnNames, Map<String, Object> flowVariableMap, String... expressions) {
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
				String escapeColumnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
				String escapeColumnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();
				String escapeFlowVariableStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
				String escapeFlowVariableEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();

				/*
				 * Map flow variables/columns with their escape characters to their actual name.
				 */
				HashMap<String, String> variableMap = new HashMap<>();
				HashMap<String, String> columnMap = new HashMap<>();

				/*
				 * booleans to determine if ROWID, ROWINDEX, or ROWCOUNT is used in the
				 * expression.
				 */
				boolean containsROWID = false;
				boolean containsROWINDEX = false;
				boolean containsROWCOUNT = false;

				/*
				 * Split expression into lines, so that we are able to parse line by line. This
				 * makes it easier concerning the handling of the end delimiter (e.g. if its not
				 * in the same line).
				 */
				String[] lines = StringUtils.split(expression, "\n");

				/* Search for used column names using the escape characters. */
				/*
				 * TODO: inlined variables. Problem here: we check if column exist => for
				 * inlined variables only possible at run time.
				 */
				for (int i = 0; i < lines.length; i++) {
					int startIndex = 0;
					String line = lines[i];

					/* Check if ROWID, ROWINDEX, or ROWCOUNT is used in the expression. */
					containsROWID = containsROWID
							|| StringUtils.contains(line, ExpressionCompletionProvider.getEscapeExpressionStartSymbol()
									+ Expression.ROWID + ExpressionCompletionProvider.getEscapeExpressionEndSymbol());

					containsROWINDEX = containsROWINDEX || StringUtils.contains(line,
							ExpressionCompletionProvider.getEscapeExpressionStartSymbol() + Expression.ROWINDEX
									+ ExpressionCompletionProvider.getEscapeExpressionEndSymbol());

					containsROWCOUNT = containsROWCOUNT || StringUtils.contains(line,
							ExpressionCompletionProvider.getEscapeExpressionStartSymbol() + Expression.ROWCOUNT
									+ ExpressionCompletionProvider.getEscapeExpressionEndSymbol());

					/* Continue until we've read all start delimiters. */
					while ((startIndex = StringUtils.indexOf(line, escapeColumnStart, startIndex)) >= 0) {
						int endIndex = StringUtils.indexOf(line, escapeColumnEnd, startIndex + 1);

						if (endIndex < 0) {
							throw new IllegalArgumentException(
									"No such column: " + StringUtils.substring(line, startIndex + 1) + " (at line " + i
											+ ") \n\n expression: \n" + expression);
						}

						/* Found a column name. */
						String foundColumn = StringUtils.substring(line, startIndex,
								endIndex + escapeColumnEnd.length());
						String column = StringUtils.substring(foundColumn, escapeColumnStart.length(),
								foundColumn.length() - escapeColumnEnd.length());

						if (!ArrayUtils.contains(columnNames, column)) {
							/*
							 * TODO: check previously added columns for derive field nodes. Rather do that
							 * in the model itself.
							 */
							throw new IllegalArgumentException("Colum '" + column + "' in line "
									+ " is not known. \n\nexpression:\n" + expression);
						}

						columnMap.put(foundColumn, column);

						/* Update startIndex in case the end escape is the same as the start escape */
						startIndex = endIndex + escapeColumnEnd.length();
					}

					startIndex = 0;

					/* Continue until we've read all start delimiters. */
					while ((startIndex = StringUtils.indexOf(line, escapeFlowVariableStart, startIndex)) >= 0) {
						int endIndex = StringUtils.indexOf(line, escapeFlowVariableEnd, startIndex + 1);

						if (endIndex < 0) {
							throw new IllegalArgumentException(
									"No such column: " + StringUtils.substring(line, startIndex + 1) + " (at line " + i
											+ ") \n\n expression: \n" + expression);
						}

						String foundVariable = StringUtils.substring(line, startIndex,
								endIndex + escapeFlowVariableEnd.length());
						String flowVariable = StringUtils.substring(foundVariable, escapeColumnStart.length(),
								foundVariable.length() - escapeFlowVariableEnd.length());

						if (flowVariableMap == null || !flowVariableMap.containsKey(flowVariable)) {
							throw new IllegalArgumentException("Flow variable '" + flowVariable + "' in line " + (i + 1)
									+ " is not known. \n\nexpression:\n" + expression);
						}

						variableMap.put(foundVariable, flowVariable);

						/* Update startIndex in case the end escape is the same as the start escape */
						startIndex = endIndex + escapeFlowVariableEnd.length();
					}
				}

				/*
				 * Store the used flow variables, column names, the original expression and the
				 * parsed expression (i.e. with removed delimiters) into the map. Additionally
				 * match the found columns/variables with their actual name so that we are able
				 * to exchange these in the expression (and get rid of the escape delimiter).
				 */
				String[] foundVariables = new String[variableMap.size()];
				String[] foundColumns = new String[columnMap.size()];
				String[] flowVariables = new String[variableMap.size()];
				String[] columns = new String[columnMap.size()];

				Iterator<String> iter = variableMap.keySet().iterator();
				int i = 0;
				while (iter.hasNext()) {
					foundVariables[i] = iter.next();
					flowVariables[i] = variableMap.get(foundVariables[i]);
					i++;
				}

				iter = columnMap.keySet().iterator();
				i = 0;
				while (iter.hasNext()) {
					foundColumns[i] = iter.next();
					columns[i] = columnMap.get(foundColumns[i]);
					i++;
				}

				String parsedExpression = StringUtils.replaceEach(expression,
						ArrayUtils.addAll(foundColumns, foundVariables),
						ArrayUtils.addAll(replaceSpecialCharacters(columns), replaceSpecialCharacters(flowVariables)));

				tempExpressionMap.put(expression,
						new ParsedExpression(expression, parsedExpression, columns, replaceSpecialCharacters(columns),
								flowVariables, replaceSpecialCharacters(flowVariables), containsROWID, containsROWINDEX,
								containsROWCOUNT));
			} else {
				/* We already parsed the expression so just keep it. */
				tempExpressionMap.put(expression, m_expressionMap.get(expression));
			}
		}

		m_expressionMap = tempExpressionMap;
	}

	/**
	 * Checks the expression and appends necessary to evaluate the expression.
	 * Checking already checked expressions will do nothing but uncheck the
	 * expressions that aren't provided as input. Note that an expression has to be
	 * parsed prior to checking it
	 * ({@link #parseExpressions(String[], Map, String...)}).
	 * 
	 * @param expressions
	 *            that shall be parsed.
	 * @param returnTypes
	 *            array of {@link DataType} describing the expected return types of
	 *            the expressions.
	 */
	public void checkExpressions(String[] expressions, DataType[] returnTypes) {
		if (m_expressionMap == null) {
			throw new IllegalStateException("Expressions have do be parsed before checking.");
		} else if (expressions.length != returnTypes.length) {
			throw new IllegalStateException("Number of expressions (" + expressions.length
					+ ") does not match number of return types (" + returnTypes.length + ").");
		}

		if (m_expressionInvocableMap == null) {
			m_expressionInvocableMap = new HashMap<>(expressions.length);
		}

		HashMap<String, ScriptEngine> tempEngineMap = new HashMap<>(expressions.length);

		for (int i = 0; i < expressions.length; i++) {
			String expressionString = expressions[i];
			String returnString = ExpressionConverterUtils.extractJavaReturnString(returnTypes[i]);
			String importString = ExpressionConverterUtils.getJavaImport(returnTypes[i]);

			if (!m_expressionMap.containsKey(expressionString)) {
				throw new IllegalArgumentException(
						"The following expression has not been parsed: \n" + expressionString);
			}

			if (!m_expressionInvocableMap.containsKey(expressionString)) {

				ParsedExpression expression = m_expressionMap.get(expressionString);

				/*
				 * Scijava wants '//@INPUT variableName, ...' to define input variables.
				 * Otherwise during evaluation it doesn't know where they come from.
				 */
				String header = "//@INPUT " + ArrayUtils.toString(expression.getParsedColumns(), "")
						+ ArrayUtils.toString(expression.getParsedFlowVariables(), "") + "\n";

				/*
				 * Wrap the expression in own 'main' method so that we are able to invoke it
				 * later on instead of always re-evaluating the script.
				 */
				String script = header + importString + "\n def " + returnString + " " + MAIN_METHOD + "(){"
						+ expression.getParsedExpression() + "}";

				/* Get the used script language and script engine. */
				ScriptLanguage language = new GroovyScriptLanguage();
				ScriptEngine engine = language.getScriptEngine();

				/*
				 * Evaluates the script and caches the engine. Using this engine as an Invocable
				 * we are able to invoke the method rather than re-evaluating the script each
				 * time.
				 */
				try {
					engine.eval(script);

					tempEngineMap.put(expressionString, engine);
				} catch (ScriptException e) {
					throw new IllegalStateException(e.toString());
				}
			} else {
				tempEngineMap.put(expressionString, m_expressionInvocableMap.get(expressionString));
			}
		}

		m_expressionInvocableMap = tempEngineMap;
	}

	/**
	 * Computes the given expression and returns the result of the computation. The
	 * expression has to be parsed ({@link #parseExpressions(String[], String...)})
	 * and checked ({@link #checkExpressions(String[], DataType[])}) prior to
	 * evaluation.
	 * 
	 * @param expression
	 *            that shall be computed.
	 * @param input
	 *            the inputs used by the expression.
	 * @return result of the computation.
	 */
	public Object computeExpression(String expression, Object input) {
		return this.computeExpression(expression, null, input);
	}

	/**
	 * Computes the given expression and returns the result of the computation. The
	 * expression has to be parsed
	 * ({@link #parseExpressions(String[], Map, String...)}) and checked
	 * ({@link #checkExpressions(String[], DataType[])}) prior to evaluation.
	 * 
	 * @param expression
	 *            that shall be computed.
	 * @param flowVariableMap
	 *            mapping from flow variable names to the actual
	 *            {@link FlowVariable}s.
	 * @param input
	 *            the inputs used by the expression. If no input names have been
	 *            found in the expression, this parameter will be ignored.
	 * @return result of the computation.
	 */
	public Object computeExpression(String expression, Map<String, ?> flowVariableMap, Object... input) {
		ScriptEngine engine = m_expressionInvocableMap.get(expression);
		String[] columns = m_expressionMap.get(expression).getParsedColumns();
		String[] parsedFlowVariables = m_expressionMap.get(expression).getParsedFlowVariables();
		String[] originalFlowVariables = m_expressionMap.get(expression).getOriginalFlowVariables();

		/*
		 * Checks if we have the right number of input and flow variables if necessary.
		 */
		if (input == null && columns != null) {
			throw new IllegalArgumentException("The input parameters are missing.");
		} else if (columns.length != 0 && columns.length != input.length) {
			throw new IllegalArgumentException("The number of input parameters (" + input.length
					+ ") does not match the expected number (" + columns.length + ")");
		}
		if (parsedFlowVariables.length != 0 && flowVariableMap == null) {
			throw new IllegalArgumentException("No flow variables have been provided.");
		}

		/* Provide the input (columns) to the expression. */
		for (int i = 0; i < columns.length; i++) {
			engine.put(columns[i], input[i]);
		}

		/* Provide the input for the used flow variables. */
		for (int i = 0; i < originalFlowVariables.length; i++) {
			String flowVariable = originalFlowVariables[i];
			if (!flowVariableMap.containsKey(flowVariable)) {
				throw new IllegalStateException("The flow variable '" + flowVariable + "' cannot be found.");
			}

			engine.put(parsedFlowVariables[i], flowVariableMap.get(flowVariable));
		}

		/*
		 * Invoke the previously defined 'main' function. This is faster than
		 * engine.eval(...)
		 */
		try {
			return ((Invocable) engine).invokeFunction("mainStart");
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Computation failed: " + e.toString());
		} catch (ScriptException e) {
			throw new IllegalStateException("Computation failed: " + e.toString());
		}
	}

	/**
	 * Returns the column names used in the given expression.
	 * 
	 * @param expression
	 *            the expression for which column names shall be returned.
	 * @return the column names.
	 */
	public String[] getUsedColumnNames(String expression) {
		if (!m_expressionMap.containsKey(expression)) {
			throw new IllegalStateException("Given Expression has not been parsed yet.");
		}

		return m_expressionMap.get(expression).getOriginalColumns();
	}

	/**
	 * Replaces the special characters in such a way that variable names only
	 * consists of letters, numbers and "_". Also guarantees that the variable name
	 * starts with "_x" instead of a number x.
	 * 
	 * @param names
	 *            variable names that shall be pre-processed.
	 * @return valid variable names where special characters have been replaced.
	 */
	private String[] replaceSpecialCharacters(String[] names) {
		String[] replacedNames = new String[names.length];

		String[] specialCharacterReplacement = new String[SPECIAL_CHARACTERS.length];

		for (int i = 0; i < SPECIAL_CHARACTERS.length; i++) {
			specialCharacterReplacement[i] = "sc_" + i;
		}

		for (int i = 0; i < names.length; i++) {
			replacedNames[i] = StringUtils.replaceEach(names[i], SPECIAL_CHARACTERS, specialCharacterReplacement);

			if (Character.isDigit(replacedNames[i].charAt(0))) {
				replacedNames[i] = "_" + replacedNames;
			}
		}

		return replacedNames;
	}
}
