package nextflow.validation

import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.SchemaParsingContext
import dev.harrel.jsonschema.JsonNode

class CustomEvaluatorFactory implements EvaluatorFactory {
    @Override
    public Optional<Evaluator> create(SchemaParsingContext ctx, String fieldName, JsonNode schemaNode) {
        // Format evaluators
        if (fieldName == "format" && schemaNode.isString()) {
            def String schemaString = schemaNode.asString()
            switch (schemaString) {
                case "directory-path":
                return Optional.of(new FormatDirectoryPathEvaluator())
                case "file-path":
                return Optional.of(new FormatFilePathEvaluator())
                case "path":
                return Optional.of(new FormatPathEvaluator())
                case "file-path-pattern":
                return Optional.of(new FormatFilePathPatternEvaluator())
            }
        } else if (fieldName == "exists" && schemaNode.isBoolean()) {
            return Optional.of(new ExistsEvaluator(schemaNode.asBoolean()))
        }
        return Optional.empty();
    }
}