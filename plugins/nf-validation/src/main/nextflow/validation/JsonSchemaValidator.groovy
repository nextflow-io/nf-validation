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
    private static Pattern uriPattern = Pattern.compile('^#/(\\d*)?/?(.*)$')

    JsonSchemaValidator(String schemaString) {
        this.schema = schemaString
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withDialect() // TODO define the dialect
            .withEvaluatorFactory(EvaluatorFactory.compose(new CustomEvaluatorFactory(), new FormatEvaluatorFactory()))
    }

    private static List<String> validateObject(JsonNode input, String validationType) {
        def Validator.Result result = this.validator.validate(this.schema, input)
        def List<String> errors = []
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

            if (locationList.size() > 0 && Utils.isInteger(locationList[0]) && validationType == "field") {
                def Integer entryInteger = locationList[0] as Integer
                def String entryString = "Entry ${entryInteger + 1}" as String
                def String fieldError = ""
                if(locationList.size() > 1) {
                    fieldError = "Error for ${validationType} '${locationList[1..-1].join("/")}': ${customError ?: errorString}"
                } else {
                    fieldError = customError ?: errorString
                }
                errors.add("* ${entryString}: ${fieldError}" as String)
            } else if (validationType == "parameter") {
                def String fieldName = locationList.join("/")
                if(fieldName != "") {
                    errors.add("* --${fieldName}: ${customError ?: errorString}" as String)
                } else {
                    errors.add("* ${customError ?: errorString}" as String)
                }
            } else {
                errors.add(customError ?: errorString)
            }

        }
        return errors
    }

    public static List<String> validate(JSONArray input) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "field")
    }

    public static List<String> validate(JSONObject input) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "parameter")
    }
}