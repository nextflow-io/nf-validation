package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONPointer
import org.json.JSONPointerException
import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.FormatEvaluatorFactory
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.providers.OrgJsonNode

import java.util.regex.Pattern
import java.util.regex.Matcher

@Slf4j
@CompileStatic
public class JsonSchemaValidator {

    private static ValidatorFactory validator
    private static JSONObject schema
    private static Pattern uriPattern = Pattern.compile('^#/(\\d*)?/?(.*)$')

    JsonSchemaValidator(String schemaString) {
        this.schema = new JSONObject(schemaString)
        def JSONPointer schemaPointer = new JSONPointer("#/\$schema")
        def String draft = schemaPointer.queryFrom(this.schema)
        if(draft != "https://json-schema.org/draft/2020-12/schema") {
            log.error("""Failed to load the meta schema:
The used schema draft (${draft}) is not correct, please use \"https://json-schema.org/draft/2020-12/schema\" instead.
See here for more information: https://json-schema.org/specification#migrating-from-older-drafts
""")
            throw new SchemaValidationException("", [])
        }
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withDialect() // TODO define the dialect
            .withEvaluatorFactory(EvaluatorFactory.compose(new CustomEvaluatorFactory(), new FormatEvaluatorFactory()))
    }

    private static List<String> validateObject(JsonNode input, String validationType, Object rawJson) {
        def Validator.Result result = this.validator.validate(this.schema, input)
        def List<String> errors = []
        for (error : result.getErrors()) {
            def String errorString = error.getError()
            // Skip double error in the parameter schema
            if (errorString.startsWith("Value does not match against the schemas at indexes") && validationType == "parameter") {
                continue
            }

            def String instanceLocation = error.getInstanceLocation()
            def JSONPointer pointer = new JSONPointer(instanceLocation)
            def String value = pointer.queryFrom(rawJson)

            // Get the errorMessage if there is one
            def String schemaLocation = error.getSchemaLocation().replaceFirst(/^[^#]+/, "")
            def JSONPointer schemaPointer = new JSONPointer("${schemaLocation}/errorMessage")
            def String customError = ""
            try{
                customError = schemaPointer.queryFrom(this.schema) ?: ""
            } catch (JSONPointerException e) {
                customError = ""
            }

            // Change some error messages to make them more clear
            if (customError == "") {
                def String keyword = error.getKeyword()
                if (keyword == "required") {
                    def Matcher matcher = errorString =~ ~/\[\[([^\[\]]*)\]\]$/
                    def String missingKeywords = matcher.findAll().flatten().last()
                    customError = "Missing required ${validationType}(s): ${missingKeywords}"
                }
            }

            def String[] locationList = instanceLocation.split("/").findAll { it != "" }

            if (locationList.size() > 0 && Utils.isInteger(locationList[0]) && validationType == "field") {
                def Integer entryInteger = locationList[0] as Integer
                def String entryString = "Entry ${entryInteger + 1}" as String
                def String fieldError = ""
                if(locationList.size() > 1) {
                    fieldError = "Error for ${validationType} '${locationList[1..-1].join("/")}' (${value}): ${customError ?: errorString}"
                } else {
                    fieldError = "${customError ?: errorString}" as String
                }
                errors.add("-> ${entryString}: ${fieldError}" as String)
            } else if (validationType == "parameter") {
                def String fieldName = locationList.join("/")
                if(fieldName != "") {
                    errors.add("* --${fieldName} (${value}): ${customError ?: errorString}" as String)
                } else {
                    errors.add("* ${customError ?: errorString}" as String)
                }
            } else {
                errors.add("-> ${customError ?: errorString}" as String)
            }

        }
        return errors
    }

    public static List<String> validate(JSONArray input) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "field", input)
    }

    public static List<String> validate(JSONObject input) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "parameter", input)
    }
}