package com.example.smithy.markdown;

public final class MarkdownGenerator {
	private final Model model;
	private final ServiceShape service;
	private final FileManifest manifest;
	private final ObjectNode config;

	public MarkdownGenerator(PluginContext ctx) {
		this.model = ctx.getModel();
		this.config = ctx.getSettings();
		ShapeId serviceId = ShapeId.from(config.expectStringMember("service").getValue());
		this.service = model.expectShape(serviceId, ServiceShape.class);
		this.manifest = ctx.getFileManifest();
	}

	public void run() {
		StringBuilder out = new StringBuilder();
		writeServiceHeader(out);
		writeOperations(out);
		writeShapes(out);

		String filename = config.getStringMemberOrDefault("outputFile", "API.md");
		manifest.writeFile(filename, out.toString());
	}

	private void writeServiceHeader(StringBuilder out) {
		out.append("# ").append(service.getId().getName()).append("\n\n");
		service.getTrait(DocumentationTrait.class).ifPresent(t -> out.append(t.getValue()).append("\n\n"));
		out.append("Version: `").append(service.getVersion()).append("`\n\n");
	}

	private void writeOperations(StringBuilder out) {
		out.append("## Operations\n\n");
		TopDownIndex index = TopDownIndex.of(model);
		for (OperationShape op : new TreeSet<>(index.getContainedOperations(service))) {
			writeOperation(out, op);
		}
	}

	private void writeOperation(StringBuilder out, OperationShape op) {
		out.append("### ").append(op.getId().getName()).append("\n\n");

		op.getTrait(DocumentationTrait.class).ifPresent(t -> out.append(t.getValue()).append("\n\n"));

		op.getTrait(HttpTrait.class).ifPresent(http -> {
			out.append("**HTTP**: `").append(http.getMethod()).append(" ").append(http.getUri()).append("`\n\n");
		});

		StructureShape input = model.expectShape(op.getInputShape(), StructureShape.class);
		if (!input.getAllMembers().isEmpty()) {
			out.append("**Input**:\n\n");
			writeMembers(out, input);
		}

		StructureShape output = model.expectShape(op.getOutputShape(), StructureShape.class);
		if (!output.getAllMembers().isEmpty()) {
			out.append("**Output**:\n\n");
			writeMembers(out, output);
		}
	}

	private void writeMembers(StringBuilder out, StructureShape struct) {
		out.append("| Field | Type | Required | Description |\n");
		out.append("|-------|------|----------|-------------|\n");
		for (MemberShape m : struct.getAllMembers().values()) {
			String type = model.expectShape(m.getTarget()).getType().toString();
			boolean required = m.hasTrait(RequiredTrait.class);
			String docs = m.getTrait(DocumentationTrait.class).map(DocumentationTrait::getValue).orElse("");
			out.append("| `").append(m.getMemberName()).append("` | ").append(type).append(" | ")
					.append(required ? "yes" : "no").append(" | ").append(docs.replace("\n", " ")).append(" |\n");
		}
		out.append("\n");
	}
	// writeShapes() omitted for brevity — same pattern over structures, enums, etc.
}