package org.knime.expressions.util;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;

import org.knime.base.node.util.JSnippetPanel;
import org.knime.base.node.util.KnimeCompletionProvider;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.workflow.FlowVariable;

@SuppressWarnings("serial")
public class ExpressionPanel extends JSnippetPanel {

	/**
	 * See
	 * {@link JSnippetPanel#JSnippetPanel(ManipulatorProvider, KnimeCompletionProvider)}
	 */
	public ExpressionPanel(ManipulatorProvider manipulatorProvider, KnimeCompletionProvider completionProvider) {
		super(manipulatorProvider, completionProvider);
	}

	/**
	 * See
	 * {@link JSnippetPanel#JSnippetPanel(ManipulatorProvider, KnimeCompletionProvider, boolean)}
	 */
	public ExpressionPanel(ManipulatorProvider manipulatorProvider, KnimeCompletionProvider completionProvider,
			boolean showColumns) {
		super(manipulatorProvider, completionProvider, showColumns);
	}

	/**
	 * See
	 * {@link JSnippetPanel#JSnippetPanel(ManipulatorProvider, KnimeCompletionProvider, boolean, boolean)}
	 */
	public ExpressionPanel(ManipulatorProvider manipulatorProvider, KnimeCompletionProvider completionProvider,
			boolean showColumns, boolean showFlowVariables) {
		super(manipulatorProvider, completionProvider, showColumns, showFlowVariables);
	}

	@Override
	protected void initSubComponents() {
		MouseListener[] mouseListeners = this.getColList().getMouseListeners();

		if (mouseListeners.length > 0) {
			this.getColList().removeMouseListener(mouseListeners[mouseListeners.length - 1]);
		}

		KeyListener[] keyListeners = this.getColList().getKeyListeners();

		if (keyListeners.length > 0) {
			this.getColList().removeKeyListener(keyListeners[keyListeners.length - 1]);
		}

		this.getColList().addKeyListener(new KeyAdapter() {
			/** {@inheritDoc} */
			@Override
			public void keyTyped(final KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER) {
					Object selected = getColList().getSelectedValue();
					if (selected != null) {
						onSelectionInColumnList(selected);
					}
				}
			}
		});

		this.getColList().addMouseListener(new MouseAdapter() {
			/** {@inheritDoc} */
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					Object selected = getColList().getSelectedValue();
					if (selected != null) {
						onSelectionInColumnList(selected);
					}
				}
			}
		});

		mouseListeners = this.getFlowVarsList().getMouseListeners();

		if (mouseListeners.length > 0) {
			this.getFlowVarsList().removeMouseListener(mouseListeners[mouseListeners.length - 1]);
		}

		keyListeners = this.getFlowVarsList().getKeyListeners();

		if (keyListeners.length > 0) {
			this.getFlowVarsList().removeKeyListener(keyListeners[keyListeners.length - 1]);
		}

		this.getFlowVarsList().addKeyListener(new KeyAdapter() {
			/** {@inheritDoc} */
			@Override
			public void keyTyped(final KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER) {
					Object selected = getFlowVarsList().getSelectedValue();
					if (selected != null) {
						onSelectionInVariableList(selected);
					}
				}
			}
		});
		this.getFlowVarsList().addMouseListener(new MouseAdapter() {
			/** {@inheritDoc} */
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					Object selected = getFlowVarsList().getSelectedValue();
					if (selected != null) {
						onSelectionInVariableList(selected);
					}
				}
			}
		});

	}

	/**
	 * Listener method that is called when a double click or an enter key stroke
	 * occurred on the flow variable list.
	 * 
	 * @param selected
	 *            Object that has been selected.
	 */
	protected void onSelectionInVariableList(final Object selected) {
		if (selected instanceof FlowVariable) {
			FlowVariable v = (FlowVariable) selected;
			String enter = this.getCompletionProvider().escapeFlowVariableName(v.getName());
			this.getExpEdit().replaceSelection(enter);
			this.getFlowVarsList().clearSelection();
			this.getExpEdit().requestFocus();
		}
	}

	/**
	 * Listener method that is called when a double click or an enter key stroke
	 * occurred on the column list.
	 * 
	 * @param selected
	 *            Object that has been selected.
	 */
	protected void onSelectionInColumnList(final Object selected) {
		if (selected != null) {
			String enter;
			if (selected instanceof String) {
				enter = ExpressionCompletionProvider.getEscapeExpressionStartSymbol() + selected
						+ ExpressionCompletionProvider.getEscapeExpressionEndSymbol();
			} else {
				DataColumnSpec colSpec = (DataColumnSpec) selected;
				String name = colSpec.getName().replace("$", "\\$");
				enter = this.getCompletionProvider().escapeColumnName(name);
			}
			this.getExpEdit().replaceSelection(enter);
			this.getColList().clearSelection();
			this.getExpEdit().requestFocus();
		}
	}
}
