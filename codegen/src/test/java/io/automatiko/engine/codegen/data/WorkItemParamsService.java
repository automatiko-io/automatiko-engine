
package io.automatiko.engine.codegen.data;

public class WorkItemParamsService {

	public Boolean negate(Boolean b) {
		System.out.println("Boolean: " + b);
		return !b;
	}

	public Integer incrementI(Integer i) {
		System.out.println("Integer: " + i++);
		return i;
	}

	public Float incrementF(Float f) {
		System.out.println("Float: " + f++);
		return f;
	}

	public String duplicate(String s) {
		System.out.println("String: " + s);
		return s + s;
	}
}
