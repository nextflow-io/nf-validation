package nextflow.validation

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow
import nextflow.Global
import groovy.json.JsonGenerator
import org.json.JSONArray

import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.Files

@Slf4j
class SchemaEvaluator implements Evaluator {
    // Evaluate the file using the given schema

    private final String schema

    SchemaEvaluator(String schema) {
        this.schema = schema
    }

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()

        // Actual validation logic
        def Path file = Nextflow.file(value)
        // Don't validate if the file does not exist or is a directory
        if(!file.exists() && !file.isDirectory()) {
            return Evaluator.Result.success()
        }

        def String baseDir = Global.getSession().baseDir
        def String schemaFull = Utils.getSchemaPath(baseDir, this.schema)
        def List<Map> fileMaps = Utils.fileToMaps(file, this.schema, baseDir)
        def String schemaContents = Files.readString( Path.of(schemaFull) )
        def validator = new JsonSchemaValidator(schemaContents)

        // Remove all null values from JSON object
        // and convert the groovy object to a JSONArray
        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        def JSONArray arrayJSON = new JSONArray(jsonGenerator.toJson(fileMaps))

        def List<String> validationErrors = validator.validate(arrayJSON)
        if (validationErrors) {
            def List<String> errors = ["Validation of file failed:"] + validationErrors.collect { "\t${it}" as String}
            return Evaluator.Result.failure(errors.join("\n"))
        }

        log.debug("Validation of file '${value}' passed!")
        return Evaluator.Result.success()
    }

}