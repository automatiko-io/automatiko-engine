
package io.automatik.engine.codegen.process.persistence.proto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import io.automatik.engine.codegen.data.Person;
import io.automatik.engine.codegen.data.PersonWithAddress;
import io.automatik.engine.codegen.data.PersonWithAddresses;
import io.automatik.engine.codegen.data.PersonWithList;
import io.automatik.engine.codegen.process.persistence.MarshallerGenerator;
import io.automatik.engine.codegen.process.persistence.proto.Proto;
import io.automatik.engine.codegen.process.persistence.proto.ProtoGenerator;
import io.automatik.engine.codegen.process.persistence.proto.ReflectionProtoGenerator;

public class MarshallerGeneratorTest {

	private ProtoGenerator<Class<?>> generator = new ReflectionProtoGenerator();

	@Test
	public void testPersonMarshallers() throws Exception {

		Proto proto = generator.generate("org.kie.kogito.test", Collections.singleton(Person.class));
		assertThat(proto).isNotNull();
		assertThat(proto.getMessages()).hasSize(1);

		MarshallerGenerator marshallerGenerator = new MarshallerGenerator(this.getClass().getClassLoader());

		List<CompilationUnit> classes = marshallerGenerator.generate(proto.toString());
		assertThat(classes).isNotNull();
		assertThat(classes).hasSize(1);

		Optional<ClassOrInterfaceDeclaration> marshallerClass = classes.get(0)
				.getClassByName("PersonMessageMarshaller");
		assertThat(marshallerClass).isPresent();
	}

	@Test
	public void testPersonWithListMarshallers() throws Exception {

		Proto proto = generator.generate("org.kie.kogito.test", Collections.singleton(PersonWithList.class));
		assertThat(proto).isNotNull();
		assertThat(proto.getMessages()).hasSize(1);

		System.out.println(proto.getMessages());

		MarshallerGenerator marshallerGenerator = new MarshallerGenerator(this.getClass().getClassLoader());

		List<CompilationUnit> classes = marshallerGenerator.generate(proto.toString());
		assertThat(classes).isNotNull();
		assertThat(classes).hasSize(1);

		Optional<ClassOrInterfaceDeclaration> marshallerClass = classes.get(0)
				.getClassByName("PersonWithListMessageMarshaller");
		assertThat(marshallerClass).isPresent();
	}

	@Test
	public void testPersonWithAdressMarshallers() throws Exception {

		Proto proto = generator.generate("org.kie.kogito.test", Collections.singleton(PersonWithAddress.class));
		assertThat(proto).isNotNull();
		assertThat(proto.getMessages()).hasSize(2);

		MarshallerGenerator marshallerGenerator = new MarshallerGenerator(this.getClass().getClassLoader());

		List<CompilationUnit> classes = marshallerGenerator.generate(proto.toString());
		assertThat(classes).isNotNull();
		assertThat(classes).hasSize(2);

		Optional<ClassOrInterfaceDeclaration> marshallerClass = classes.get(0)
				.getClassByName("AddressMessageMarshaller");
		assertThat(marshallerClass).isPresent();
		marshallerClass = classes.get(1).getClassByName("PersonWithAddressMessageMarshaller");
		assertThat(marshallerClass).isPresent();
	}

	@Test
	public void testPersonWithAdressesMarshallers() throws Exception {

		Proto proto = generator.generate("org.kie.kogito.test", Collections.singleton(PersonWithAddresses.class));
		assertThat(proto).isNotNull();
		assertThat(proto.getMessages()).hasSize(2);

		System.out.println(proto.getMessages());

		MarshallerGenerator marshallerGenerator = new MarshallerGenerator(this.getClass().getClassLoader());

		List<CompilationUnit> classes = marshallerGenerator.generate(proto.toString());
		assertThat(classes).isNotNull();
		assertThat(classes).hasSize(2);

		Optional<ClassOrInterfaceDeclaration> marshallerClass = classes.get(0)
				.getClassByName("AddressMessageMarshaller");
		assertThat(marshallerClass).isPresent();
		marshallerClass = classes.get(1).getClassByName("PersonWithAddressesMessageMarshaller");
		assertThat(marshallerClass).isPresent();
	}
}
