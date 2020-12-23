package io.automatiko.engine.workflow.compiler.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.workflow.compiler.util.ClassUtils;

public class ClassUtilsTest {

	@Test
	public void testJavaLangTypeWithoutPackage() {

		Class<?> clazz = ClassUtils.constructClass("Boolean");

		assertThat(clazz).isEqualTo(Boolean.class);
	}

	@Test
	public void testJavaLangTypeWithPackage() {

		Class<?> clazz = ClassUtils.constructClass("java.lang.Boolean");

		assertThat(clazz).isEqualTo(Boolean.class);
	}

	@Test
	public void testCollectionWithoutGenerics() {

		Class<?> clazz = ClassUtils.constructClass("java.util.List");

		assertThat(clazz).isEqualTo(List.class);
	}

	@Test
	public void testCollectionWithGenerics() {

		Class<?> clazz = ClassUtils.constructClass("java.util.List<String>");

		assertThat(clazz).isEqualTo(List.class);
	}
}
