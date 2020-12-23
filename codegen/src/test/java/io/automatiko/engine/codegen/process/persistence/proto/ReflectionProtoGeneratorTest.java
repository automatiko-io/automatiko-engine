
package io.automatiko.engine.codegen.process.persistence.proto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.codegen.data.Person;
import io.automatiko.engine.codegen.data.PersonVarInfo;
import io.automatiko.engine.codegen.data.PersonWithAddress;
import io.automatiko.engine.codegen.data.PersonWithAddresses;
import io.automatiko.engine.codegen.data.PersonWithList;
import io.automatiko.engine.codegen.process.persistence.proto.Proto;
import io.automatiko.engine.codegen.process.persistence.proto.ProtoField;
import io.automatiko.engine.codegen.process.persistence.proto.ProtoGenerator;
import io.automatiko.engine.codegen.process.persistence.proto.ProtoMessage;
import io.automatiko.engine.codegen.process.persistence.proto.ReflectionProtoGenerator;

public class ReflectionProtoGeneratorTest {

	private ProtoGenerator<Class<?>> generator = new ReflectionProtoGenerator();

	@Test
	public void testPersonProtoFile() {

		Proto proto = generator.generate("io.automatiko.engine.test", Collections.singleton(Person.class));
		assertThat(proto).isNotNull();

		assertThat(proto.getPackageName()).isEqualTo("io.automatiko.engine.test");
		assertThat(proto.getSyntax()).isEqualTo("proto2");
		assertThat(proto.getMessages()).hasSize(1);

		ProtoMessage person = proto.getMessages().get(0);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("Person");
		assertThat(person.getJavaPackageOption()).isEqualTo("io.automatiko.engine.codegen.data");
		assertThat(person.getFields()).hasSize(3);

		ProtoField field = person.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("adult");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("age");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");
	}

	@Test
	public void testPersonWithAddressProtoFile() {

		Proto proto = generator.generate("io.automatiko.engine.test", Collections.singleton(PersonWithAddress.class));
		assertThat(proto).isNotNull();

		assertThat(proto.getPackageName()).isEqualTo("io.automatiko.engine.test");
		assertThat(proto.getSyntax()).isEqualTo("proto2");
		assertThat(proto.getMessages()).hasSize(2);

		ProtoMessage address = proto.getMessages().get(0);
		assertThat(address).isNotNull();
		assertThat(address.getName()).isEqualTo("Address");
		assertThat(address.getJavaPackageOption()).isEqualTo("io.automatiko.engine.codegen.data");
		assertThat(address.getFields()).hasSize(4);

		ProtoField field = address.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("city");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("country");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("street");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(3);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("zipCode");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		ProtoMessage person = proto.getMessages().get(1);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("PersonWithAddress");
		assertThat(person.getJavaPackageOption()).isEqualTo("io.automatiko.engine.codegen.data");
		assertThat(person.getFields()).hasSize(4);

		field = person.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("address");
		assertThat(field.getType()).isEqualTo("Address");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("adult");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("age");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(3);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");
	}

	@Test
	public void testPersonWithListProtoFile() {

		Proto proto = generator.generate("io.automatiko.engine.test", Collections.singleton(PersonWithList.class));
		assertThat(proto).isNotNull();

		assertThat(proto.getPackageName()).isEqualTo("io.automatiko.engine.test");
		assertThat(proto.getSyntax()).isEqualTo("proto2");
		assertThat(proto.getMessages()).hasSize(1);

		ProtoMessage address = proto.getMessages().get(0);
		assertThat(address).isNotNull();
		assertThat(address.getName()).isEqualTo("PersonWithList");
		assertThat(address.getJavaPackageOption()).isEqualTo("io.automatiko.engine.codegen.data");
		assertThat(address.getFields()).hasSize(7);

		ProtoField field = address.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("adult");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("age");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("booleanList");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("repeated");

		field = address.getFields().get(3);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("integerList");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("repeated");

		field = address.getFields().get(4);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("longList");
		assertThat(field.getType()).isEqualTo("int64");
		assertThat(field.getApplicability()).isEqualTo("repeated");

		field = address.getFields().get(5);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(6);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("stringList");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("repeated");
	}

	@Test
	public void testPersonWithAddressesProtoFile() {

		Proto proto = generator.generate("io.automatiko.engine.test", Collections.singleton(PersonWithAddresses.class));
		assertThat(proto).isNotNull();

		assertThat(proto.getPackageName()).isEqualTo("io.automatiko.engine.test");
		assertThat(proto.getSyntax()).isEqualTo("proto2");
		assertThat(proto.getMessages()).hasSize(2);

		ProtoMessage address = proto.getMessages().get(0);
		assertThat(address).isNotNull();
		assertThat(address.getName()).isEqualTo("Address");
		assertThat(address.getJavaPackageOption()).isEqualTo("io.automatiko.engine.codegen.data");
		assertThat(address.getFields()).hasSize(4);

		ProtoField field = address.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("city");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("country");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("street");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = address.getFields().get(3);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("zipCode");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");

		ProtoMessage person = proto.getMessages().get(1);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("PersonWithAddresses");
		assertThat(person.getJavaPackageOption()).isEqualTo("io.automatiko.engine.codegen.data");
		assertThat(person.getFields()).hasSize(4);

		field = person.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("addresses");
		assertThat(field.getType()).isEqualTo("Address");
		assertThat(field.getApplicability()).isEqualTo("repeated");

		field = person.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("adult");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("age");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("optional");

		field = person.getFields().get(3);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");
	}

	@Test
	public void testPersonAsModelProtoFile() {

		Proto proto = generator.generate("@Indexed", "@Field(store = Store.YES)", "io.automatiko.engine.test.persons",
				Person.class);
		assertThat(proto).isNotNull();

		assertThat(proto.getPackageName()).isEqualTo("io.automatiko.engine.test.persons");
		assertThat(proto.getSyntax()).isEqualTo("proto2");
		assertThat(proto.getMessages()).hasSize(1);

		ProtoMessage person = proto.getMessages().get(0);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("Person");
		assertThat(person.getComment()).isEqualTo("@Indexed");
		assertThat(person.getJavaPackageOption()).isEqualTo("io.automatiko.engine.test.persons");
		assertThat(person.getFields()).hasSize(3);

		ProtoField field = person.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("adult");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("optional");
		assertThat(field.getComment()).isEqualTo("@Field(store = Store.YES)");

		field = person.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("age");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("optional");
		assertThat(field.getComment()).isEqualTo("@Field(store = Store.YES)");

		field = person.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");
		assertThat(field.getComment()).isEqualTo("@Field(store = Store.YES)");
	}

	@Test
	public void testPersonWithVariableInfoAsModelProtoFile() {

		Proto proto = generator.generate("@Indexed", "@Field(store = Store.YES)", "io.automatiko.engine.test.persons",
				PersonVarInfo.class);
		assertThat(proto).isNotNull();

		assertThat(proto.getPackageName()).isEqualTo("io.automatiko.engine.test.persons");
		assertThat(proto.getSyntax()).isEqualTo("proto2");
		assertThat(proto.getMessages()).hasSize(1);

		ProtoMessage person = proto.getMessages().get(0);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("PersonVarInfo");
		assertThat(person.getComment()).isEqualTo("@Indexed");
		assertThat(person.getJavaPackageOption()).isEqualTo("io.automatiko.engine.test.persons");
		assertThat(person.getFields()).hasSize(3);

		ProtoField field = person.getFields().get(0);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("adult");
		assertThat(field.getType()).isEqualTo("bool");
		assertThat(field.getApplicability()).isEqualTo("optional");
		assertThat(field.getComment()).isEqualTo("@Field(store = Store.YES)");

		field = person.getFields().get(1);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("age");
		assertThat(field.getType()).isEqualTo("int32");
		assertThat(field.getApplicability()).isEqualTo("optional");
		assertThat(field.getComment()).isEqualTo("@Field(store = Store.YES)");

		field = person.getFields().get(2);
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("name");
		assertThat(field.getType()).isEqualTo("string");
		assertThat(field.getApplicability()).isEqualTo("optional");
		assertThat(field.getComment()).isEqualTo("@Field(store = Store.YES)\n @VariableInfo(tags=\"test\")");
	}
}
