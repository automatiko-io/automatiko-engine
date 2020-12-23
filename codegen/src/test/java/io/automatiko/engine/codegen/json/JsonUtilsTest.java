package io.automatiko.engine.codegen.json;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.automatiko.engine.codegen.json.JsonUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonUtilsTest {

	@Test
	public void testMerge() {
		ObjectMapper mapper = new ObjectMapper();

		JsonNode node1 = createJson(mapper, createJson(mapper, "numbers", Arrays.asList(1, 2, 3)));
		JsonNode node2 = createJson(mapper, createJson(mapper, "numbers", Arrays.asList(4, 5, 6)));
		JsonNode node3 = createJson(mapper, mapper.createObjectNode().put("number", 1));
		JsonNode node4 = createJson(mapper, mapper.createObjectNode().put("boolean", false));
		JsonNode node5 = createJson(mapper, mapper.createObjectNode().put("string", "javier"));

		JsonNode result = mapper.createObjectNode();
		JsonUtils.merge(node1, result);
		JsonUtils.merge(node2, result);
		JsonUtils.merge(node3, result);
		JsonUtils.merge(node4, result);
		JsonUtils.merge(node5, result);

		assertEquals(1, result.size());
		JsonNode merged = result.get("merged");
		assertEquals(4, merged.size());
		JsonNode numbers = merged.get("numbers");
		assertTrue(numbers instanceof ArrayNode);
		ArrayNode numbersNode = (ArrayNode) numbers;
		assertEquals(4, numbersNode.get(0).asInt());
		assertEquals(5, numbersNode.get(1).asInt());
		assertEquals(6, numbersNode.get(2).asInt());
		assertEquals(false, merged.get("boolean").asBoolean());
		assertEquals("javier", merged.get("string").asText());
		assertEquals(1, merged.get("number").asInt());
	}

	private JsonNode createJson(ObjectMapper mapper, String name, Collection<Integer> integers) {
		ArrayNode node = mapper.createArrayNode();
		for (int i : integers) {
			node.add(i);
		}
		return mapper.createObjectNode().set(name, node);
	}

	private JsonNode createJson(ObjectMapper mapper, JsonNode node) {
		return mapper.createObjectNode().set("merged", node);
	}
}
