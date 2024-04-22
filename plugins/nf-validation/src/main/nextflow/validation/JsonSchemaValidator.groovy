package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import org.json.JSONObject
import org.json.JSONArray
import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.FormatEvaluatorFactory
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.providers.OrgJsonNode

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileStatic
public class JsonSchemaValidator {

    private static ValidatorFactory validator
    private static Pattern uriPattern = Pattern.compile('^#/(\\d*)?/?(.*)$')

    JsonSchemaValidator() {
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withDialect() // TODO define the dialect
            .withEvaluatorFactory(EvaluatorFactory.compose(new CustomEvaluatorFactory(), new FormatEvaluatorFactory()))
    }

    private static List<String> validateObject(JsonNode input, String validationType, Object rawJson, String schemaString) {
        def JSONObject schema = new JSONObject(schemaString)
        def String draft = Utils.getValueFromJson("#/\$schema", schema)
        if(draft != "https://json-schema.org/draft/2020-12/schema") {
            log.error("""Failed to load the meta schema:
The used schema draft (${draft}) is not correct, please use \"https://json-schema.org/draft/2020-12/schema\" instead.
See here for more information: https://json-schema.org/specification#migrating-from-older-drafts
""")
            throw new SchemaValidationException("", [])
        }
        
        def Validator.Result result = this.validator.validate(schema, input)
        def List<String> errors = []
        for (error : result.getErrors()) {
            def String errorString = error.getError()
            // Skip double error in the parameter schema
            if (errorString.startsWith("Value does not match against the schemas at indexes") && validationType == "parameter") {
                continue
            }

            def String instanceLocation = error.getInstanceLocation()
            def String value = Utils.getValueFromJson(instanceLocation, rawJson)

            // Get the custom errorMessage if there is one and the validation errors are not about the content of the file
            def String schemaLocation = error.getSchemaLocation().replaceFirst(/^[^#]+/, "")
            def String customError = ""
            if (!errorString.startsWith("Validation of file failed:")) {
                customError = Utils.getValueFromJson("${schemaLocation}/errorMessage", schema) as String
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

    public static List<String> validate(JSONArray input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "field", input, schemaString)
    }

    public static List<String> validate(JSONObject input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "parameter", input, schemaString)
    }
}