
package io.automatiko.engine.workflow.process.core.node;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;

/**
 * Default implementation of a join.
 * 
 */
public class Join extends NodeImpl {

	public static final int TYPE_UNDEFINED = 0;
	/**
	 * The outgoing connection of a join of this type is triggered when all its
	 * incoming connections have been triggered.
	 */
	public static final int TYPE_AND = 1;
	/**
	 * The outgoing connection of a join of this type is triggered when one of its
	 * incoming connections has been triggered.
	 */
	public static final int TYPE_XOR = 2;
	/**
	 * The outgoing connection of a join of this type is triggered when one of its
	 * incoming connections has been triggered. It then waits until all other
	 * incoming connections have been triggered before allowing
	 */
	public static final int TYPE_DISCRIMINATOR = 3;
	/**
	 * The outgoing connection of a join of this type is triggered when n of its
	 * incoming connections have been triggered.
	 */
	public static final int TYPE_N_OF_M = 4;

	public static final int TYPE_OR = 5;

	private static final long serialVersionUID = 510l;

	private int type;
	private String n;

	public Join() {
		this.type = TYPE_UNDEFINED;
	}

	public void setType(final int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public void setN(String n) {
		this.n = n;
	}

	public String getN() {
		return n;
	}

	public void validateAddIncomingConnection(final String type, final Connection connection) {
		super.validateAddIncomingConnection(type, connection);
		if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
					+ ", " + connection.getTo().getName() + "] only accepts default incoming connection type!");
		}
	}

	public void validateAddOutgoingConnection(final String type, final Connection connection) {
		super.validateAddOutgoingConnection(type, connection);
		if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException(
					"This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
							+ connection.getFrom().getName() + "] only accepts default outgoing connection type!");
		}
		if (getTo() != null) {
			throw new IllegalArgumentException(
					"This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
							+ connection.getFrom().getName() + "] cannot have more than one outgoing connection!");
		}
	}

}
