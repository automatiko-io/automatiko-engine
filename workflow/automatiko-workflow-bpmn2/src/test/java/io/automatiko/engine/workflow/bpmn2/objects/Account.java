
package io.automatiko.engine.workflow.bpmn2.objects;

public class Account {

	private Person person;

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@Override
	public String toString() {
		return "Account [person=" + person + "]";
	}

}
