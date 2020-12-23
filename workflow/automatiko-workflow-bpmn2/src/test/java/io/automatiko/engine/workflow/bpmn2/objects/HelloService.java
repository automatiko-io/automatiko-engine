
package io.automatiko.engine.workflow.bpmn2.objects;

public class HelloService {

	public static String VALIDATE_STRING = null;

	public String hello(String name) {
		return "Hello " + name + "!";
	}

	public String helloEcho(String name) {
		return name;
	}

	public String validate(String value) {
		if (VALIDATE_STRING != null) {
			if (!VALIDATE_STRING.equals(value)) {
				throw new RuntimeException("Value does not match expected string: " + value);
			}
		}
		return value;
	}

	public String helloException(String name) {
		throw new RuntimeException("Hello Exception " + name + "!");
	}
}
