package example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Utilities for loading Smithy JSON AST models from the repo's aws-models
 * directory.
 */
public final class SmithyModels {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private SmithyModels() {
	}

	public record ServiceMapEntry(String repo_dir, String service_name) {
	}

	public record LoadedModel(String serviceName, ServiceModel model) {
	}

	/** Minimal equivalent to the Rust ServiceModel. Expand as needed. */
	public static final class ServiceModel {
		public final String serviceName;
		public final List<Operation> operations;
		public final Map<String, Shape> shapes;

		public ServiceModel(String serviceName, List<Operation> operations, Map<String, Shape> shapes) {
			this.serviceName = serviceName;
			this.operations = operations;
			this.shapes = shapes;
		}
	}

	public static final class Operation {
		public final String name;
		public final String inputShape; // nullable
		public final String outputShape; // nullable
		public final List<String> errorShapes;
		public final String httpMethod; // nullable
		public final String httpUri; // nullable
		public final Integer httpCode; // nullable

		public Operation(String name, String inputShape, String outputShape, List<String> errorShapes,
				String httpMethod, String httpUri, Integer httpCode) {
			this.name = name;
			this.inputShape = inputShape;
			this.outputShape = outputShape;
			this.errorShapes = errorShapes;
			this.httpMethod = httpMethod;
			this.httpUri = httpUri;
			this.httpCode = httpCode;
		}
	}

	/**
	 * Placeholder: keep raw JSON for shapes unless you need strongly-typed
	 * variants.
	 */
	public static final class Shape {
		public final String shapeId;
		public final String type; // e.g. "structure", "string", ...
		public final JsonNode traits; // raw traits JSON (optional)
		public final JsonNode definition; // raw full shape JSON

		public Shape(String shapeId, String type, JsonNode traits, JsonNode definition) {
			this.shapeId = shapeId;
			this.type = type;
			this.traits = traits;
			this.definition = definition;
		}
	}

	/** Java equivalent of Rust load_service_map(models_dir). */
	public static Map<String, ServiceMapEntry> loadServiceMap(Path modelsDir) throws IOException {
		Path p = modelsDir.resolve("service-map.json");
		String content = Files.readString(p);
		// JSON object: { "<model_key>": { "repo_dir": "...", "service_name": "..." },
		// ... }
		return MAPPER.readValue(content, new TypeReference<Map<String, ServiceMapEntry>>() {
		});
	}

	/**
	 * Java equivalent of Rust load_all_models(models_dir).
	 *
	 * @return list sorted by service name, like the Rust code.
	 */
	public static List<LoadedModel> loadAllModels(Path modelsDir) throws IOException {
		Map<String, ServiceMapEntry> serviceMap = loadServiceMap(modelsDir);
		List<LoadedModel> models = new ArrayList<>();

		for (Map.Entry<String, ServiceMapEntry> e : serviceMap.entrySet()) {
			String modelKey = e.getKey();
			ServiceMapEntry entry = e.getValue();

			Path modelPath = modelsDir.resolve(modelKey + ".json");
			if (!Files.exists(modelPath)) {
				System.err.printf("Warning: Model file not found for %s: %s%n", modelKey, modelPath);
				continue;
			}

			ServiceModel model = parseModel(modelPath);
			models.add(new LoadedModel(entry.service_name(), model));
		}

		models.sort(Comparator.comparing(LoadedModel::serviceName));
		return models;
	}

	/**
	 * Minimal parse_model equivalent: reads the JSON AST, checks smithy version,
	 * extracts: - service name (namespace before '#') - operations list (including
	 * resource operations if you implement that) - shapes map (kept raw here)
	 *
	 * This is intentionally close to the Rust structure, but simplified.
	 */
	public static ServiceModel parseModel(Path path) throws IOException {
		JsonNode root = MAPPER.readTree(Files.readString(path));

		String smithyVersion = Optional.ofNullable(root.get("smithy")).map(JsonNode::asText).orElse("unknown");
		if (!smithyVersion.startsWith("2.")) {
			throw new IllegalArgumentException("Unsupported Smithy version: " + smithyVersion);
		}

		JsonNode shapesNode = root.get("shapes");
		if (shapesNode == null || !shapesNode.isObject()) {
			throw new IllegalArgumentException("Missing 'shapes' in model");
		}

		// Find the service shape and collect operation targets.
		String serviceName = "";
		List<String> operationTargets = new ArrayList<>();

		Iterator<Map.Entry<String, JsonNode>> shapesIter = shapesNode.fields();
		while (shapesIter.hasNext()) {
			Map.Entry<String, JsonNode> shapeEntry = shapesIter.next();
			String shapeId = shapeEntry.getKey();
			JsonNode shapeDef = shapeEntry.getValue();

			String type = Optional.ofNullable(shapeDef.get("type")).map(JsonNode::asText).orElse(null);
			if ("service".equals(type)) {
				serviceName = shapeId.contains("#") ? shapeId.substring(0, shapeId.indexOf('#')) : shapeId;

				JsonNode ops = shapeDef.get("operations");
				if (ops != null && ops.isArray()) {
					for (JsonNode op : ops) {
						JsonNode target = op.get("target");
						if (target != null && target.isTextual()) {
							operationTargets.add(target.asText());
						}
					}
				}

				// NOTE: Rust also pulls ops from service resources recursively.
				// You can implement collectResourceOperations(...) similarly if needed.

				break;
			}
		}

		// Parse shapes (kept raw as Shape objects).
		Map<String, Shape> shapes = new HashMap<>();
		Iterator<Map.Entry<String, JsonNode>> allShapes = shapesNode.fields();
		while (allShapes.hasNext()) {
			Map.Entry<String, JsonNode> shapeEntry = allShapes.next();
			String shapeId = shapeEntry.getKey();
			JsonNode def = shapeEntry.getValue();
			String type = Optional.ofNullable(def.get("type")).map(JsonNode::asText).orElse(null);
			if (type == null)
				continue;

			JsonNode traits = def.get("traits");
			shapes.put(shapeId, new Shape(shapeId, type, traits, def));
		}

		// Build operation list
		List<Operation> operations = new ArrayList<>();
		for (String target : operationTargets) {
			JsonNode opDef = shapesNode.get(target);
			if (opDef == null || !opDef.isObject())
				continue;

			String name = target.contains("#") ? target.substring(target.lastIndexOf('#') + 1) : target;

			String inputShape = Optional.ofNullable(opDef.get("input")).map(n -> n.get("target"))
					.filter(JsonNode::isTextual).map(JsonNode::asText).orElse(null);

			String outputShape = Optional.ofNullable(opDef.get("output")).map(n -> n.get("target"))
					.filter(JsonNode::isTextual).map(JsonNode::asText).orElse(null);

			List<String> errorShapes = new ArrayList<>();
			JsonNode errors = opDef.get("errors");
			if (errors != null && errors.isArray()) {
				for (JsonNode err : errors) {
					JsonNode t = err.get("target");
					if (t != null && t.isTextual())
						errorShapes.add(t.asText());
				}
			}

			String httpMethod = null;
			String httpUri = null;
			Integer httpCode = null;

			JsonNode traits = opDef.get("traits");
			if (traits != null && traits.isObject()) {
				JsonNode http = traits.get("smithy.api#http");
				if (http != null && http.isObject()) {
					if (http.get("method") != null && http.get("method").isTextual()) {
						httpMethod = http.get("method").asText();
					}
					if (http.get("uri") != null && http.get("uri").isTextual()) {
						httpUri = http.get("uri").asText();
					}
					if (http.get("code") != null && http.get("code").canConvertToInt()) {
						httpCode = http.get("code").asInt();
					}
				}
			}

			operations.add(new Operation(name, inputShape, outputShape, errorShapes, httpMethod, httpUri, httpCode));
		}

		operations.sort(Comparator.comparing(op -> op.name));
		return new ServiceModel(serviceName, operations, shapes);
	}
}