package org.knime.expressions.util;

import java.util.LinkedList;

public class ParsedExpression {
	private final String m_originalExpression;
	private final String m_parsedExpression;
	private final String[] m_flowVariables;
	private final String[] m_columns;
	
	public ParsedExpression(String originalExpression, String parsedExpression, String[] columns) {
		this(originalExpression, parsedExpression, columns, null);
	}
	
	public ParsedExpression(String originalExpression, String parsedExpression, String[] columns, String[] flowVariables) {
		m_originalExpression = originalExpression;
		m_parsedExpression = parsedExpression;
		m_flowVariables = flowVariables;
		m_columns = columns;
	}
	
	/**
	 * 
	 * @return {@code true} if the expression uses flow variables, otherwise {@code false}
	 */
	public boolean usesFlowVariables() {
		return m_flowVariables != null && m_flowVariables.length > 0;
	}
	
	/**
	 * 
	 * @return {@code true} if the expression uses specified columns, otherwise {@code false}
	 */
	public boolean usesColumns() {
		return m_columns != null && m_columns.length > 0;
	}
	
	/**
	 * 
	 * @return String containing the expression.
	 */
	public String getOriginalExpression() {
		return m_originalExpression;
	}
	
	/**
	 * 
	 * @return String containing the expression.
	 */
	public String getParsedExpression() {
		return m_parsedExpression;
	}

	/**
	 * 
	 * @return List containing the used flow variables.
	 */
	public String[] getFlowVariables() {
		return m_flowVariables;
	}

	/**
	 * 
	 * @return List containing the used columns.
	 */
	public String[] getColumns() {
		return m_columns;
	}
	
}
