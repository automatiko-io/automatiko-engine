
package io.automatiko.engine.workflow.base.instance.impl.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import io.automatiko.engine.api.runtime.process.WorkItem;

/**
 * 
 */
public class UIWorkItemHandlerDialog extends JDialog {

	private static final long serialVersionUID = 510l;

	private Map<String, Object> results = new HashMap<String, Object>();
	private UIWorkItemHandler handler;
	private WorkItem workItem;
	private JTextField resultNameTextField;
	private JTextField resultValueTextField;
	private JButton addResultButton;
	private JButton completeButton;
	private JButton abortButton;

	public UIWorkItemHandlerDialog(UIWorkItemHandler handler, WorkItem workItem) {
		super(handler, "Execute Work Item", true);
		this.handler = handler;
		this.workItem = workItem;
		setSize(new Dimension(400, 300));
		initializeComponent();
	}

	private void initializeComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		getRootPane().setLayout(new BorderLayout());
		getRootPane().add(panel, BorderLayout.CENTER);

		JTextArea params = new JTextArea();
		params.setText(getParameters());
		params.setEditable(false);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 5;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(params, c);

		JLabel resultName = new JLabel("Result");
		c = new GridBagConstraints();
		c.gridy = 1;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(resultName, c);
		resultNameTextField = new JTextField();
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0.3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(resultNameTextField, c);

		JLabel resultValue = new JLabel("Value");
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(resultValue, c);
		resultValueTextField = new JTextField();
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 1;
		c.weightx = 0.7;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(resultValueTextField, c);

		addResultButton = new JButton("Add");
		addResultButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				addResult();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 1;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(addResultButton, c);

		completeButton = new JButton("Complete");
		completeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				complete();
			}
		});
		c = new GridBagConstraints();
		c.gridy = 2;
		c.weightx = 1;
		c.gridwidth = 4;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(completeButton, c);

		abortButton = new JButton("Abort");
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				abort();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 2;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(abortButton, c);
	}

	private String getParameters() {
		String result = "";
		if (workItem.getParameters() != null) {
			for (Iterator<Map.Entry<String, Object>> iterator = workItem.getParameters().entrySet().iterator(); iterator
					.hasNext();) {
				Map.Entry<String, Object> entry = iterator.next();
				result += entry.getKey() + " = " + entry.getValue() + "\n";
			}
		}
		return result;
	}

	private void addResult() {
		results.put(resultNameTextField.getText(), resultValueTextField.getText());
		resultNameTextField.setText("");
		resultValueTextField.setText("");
	}

	private void complete() {
		handler.complete(workItem, results);
		dispose();
	}

	private void abort() {
		handler.abort(workItem);
		dispose();
	}
}
