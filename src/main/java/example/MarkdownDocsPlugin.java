package com.example.smithy.markdown;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;

public final class MarkdownDocsPlugin implements SmithyBuildPlugin {
	@Override
	public String getName() {
		return "markdown-docs";
	}

	@Override
	public void execute(PluginContext context) {
		new MarkdownGenerator(context).run();
	}
}