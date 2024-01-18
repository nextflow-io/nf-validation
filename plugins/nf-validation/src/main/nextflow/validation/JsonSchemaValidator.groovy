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
            def String location = error.getInstanceLocation()
            def String[] locationList = location.split("/").findAll { it != "" }
            def String fieldName = ""
            def String capValidationType = "${validationType[0].toUpperCase()}${validationType[1..-1]}"

            if (locationList.size() > 0 && isInteger(location[0]) && validationType == "field") {
                def Integer entryInteger = location[0] as Integer
                def String entryString = "Entry ${entryInteger + 1}" as String
                if(locationList.size() > 1) {
                    fieldName = "Error for '${locationList[1..-1].join("/")}'"
                } else {
                    fieldName = "${capValidationType} error"
                }
                this.errors.add("* ${entryString}: ${fieldName}: ${errorString}" as String)
            } else if (validationType == "parameter") {
                fieldName = locationList.join("/")
                if(fieldName != "") {
                    this.errors.add("* --${fieldName}: ${errorString}" as String)
                } else {
                    this.errors.add("* ${capValidationType} error: ${errorString}" as String)
                }
            } else {
                this.errors.add(errorString)
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