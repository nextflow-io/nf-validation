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

@Slf4j
@CompileStatic
public class JsonSchemaValidator {

    private static ValidatorFactory validator
    private static String schema
    private static List<String> errors = []
    private static Pattern uriPattern = Pattern.compile('^#/(\\d*)?/?(.*)$')

    JsonSchemaValidator(String schemaString) {
        this.schema = schemaString
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withEvaluatorFactory(EvaluatorFactory.compose(customFactory, new FormatEvaluatorFactory()))
            // .withEvaluatorFactory() => exists keyword should be implemented here
    }

    private static validateObject(JsonNode input, String validationType) {
        def Validator.Result result = this.validator.validate(this.schema, input)
        for (error : result.getErrors()) {
            def String errorString = error.getError()
            // Skip double error in the parameter schema
            if (errorString.startsWith("Value does not match against the schemas at indexes") && validationType == "parameter") {
                continue
            }

            // Change some error messages to make them more clear
            def String keyword = error.getKeyword()
            def String customError = ""
            if (keyword == "required") {
                def Matcher matcher = errorString =~ ~/\[\[([^\[\]]*)\]\]$/
                def String missingKeywords = matcher.findAll().flatten().last()
                customError = "Missing required ${validationType}(s): ${missingKeywords}"
            }

            def String location = error.getInstanceLocation()
            def String[] locationList = location.split("/").findAll { it != "" }

            if (locationList.size() > 0 && isInteger(locationList[0]) && validationType == "field") {
                def Integer entryInteger = locationList[0] as Integer
                def String entryString = "Entry ${entryInteger + 1}" as String
                def String fieldError = ""
                if(locationList.size() > 1) {
                    fieldError = "Error for ${validationType} '${locationList[1..-1].join("/")}': ${customError ?: errorString}"
                } else {
                    fieldError = customError ?: errorString
                }
                this.errors.add("* ${entryString}: ${fieldError}" as String)
            } else if (validationType == "parameter") {
                def String fieldName = locationList.join("/")
                if(fieldName != "") {
                    this.errors.add("* --${fieldName}: ${customError ?: errorString}" as String)
                } else {
                    this.errors.add("* ${customError ?: errorString}" as String)
                }
            } else {
                this.errors.add(customError ?: errorString)
            }

        }
    }

    public static List<String> validate(JSONArray input) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        this.validateObject(jsonInput, "field")
        return this.errors
    }

    public static List<String> validate(JSONObject input) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        this.validateObject(jsonInput, "parameter")
        return this.errors
    }

    private static Boolean isInteger(String input) {
        try {
            input as Integer
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }
}