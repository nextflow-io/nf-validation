package nextflow.validation

import nextflow.Global
import nextflow.Session
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.SchemaParsingContext
import dev.harrel.jsonschema.JsonNode

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

class CustomEvaluatorFactory implements EvaluatorFactory {

    private Boolean lenientMode
    private String baseDir

    CustomEvaluatorFactory() {
        def Session session = Global.getSession()
        this.lenientMode = session.params.validationLenientMode ?: false
        this.baseDir = session.baseDir.toString()
    }

    @Override
    public Optional<Evaluator> create(SchemaParsingContext ctx, String fieldName, JsonNode schemaNode) {
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
        } else if (fieldName == "schema" && schemaNode.isString()) {
            return Optional.of(new SchemaEvaluator(schemaNode.asString(), this.baseDir))
        } else if (fieldName == "uniqueEntries" && schemaNode.isArray()) {
            return Optional.of(new UniqueEntriesEvaluator(schemaNode.asArray()))
        } else if (fieldName == "type" && (schemaNode.isString() || schemaNode.isArray()) && lenientMode) {
            return Optional.of(new LenientTypeEvaluator(schemaNode))
        }

        return Optional.empty()
    }
}