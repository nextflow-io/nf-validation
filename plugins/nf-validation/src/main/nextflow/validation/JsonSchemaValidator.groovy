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

        // for (annotation : result.getAnnotations()) {
        //     println(annotation.getAnnotation())
        //     println(annotation.getEvaluationPath())
        //     println(annotation.getInstanceLocation())
        //     println(annotation.getKeyword())
        //     println(annotation.getSchemaLocation())
        // }        


        // Validator validator = new Validator(true)
        // validator.validate(this.schema, input, validationError -> {
        //     // Fail on other errors than validation errors
        //     if(validationError !instanceof ValidationError) {
        //         // TODO handle this better
        //         log.error("* ${validationError.getMessage()}" as String)
        //         return
        //     }

        //     // Get the name of the parameter and determine if it is a list entry
        //     def Integer entry = 0
        //     def String name = ''
        //     def String[] uriSplit = validationError.getUri().toString().replaceFirst('#/', '').split('/')
        //     def String error = ''

        //     if (input instanceof Map) {
        //         name = uriSplit.size() > 0 ? uriSplit[0..-1].join('/') : ''
        //     }
        //     else if (input instanceof List) {
        //         entry = uriSplit[0].toInteger() + 1
        //         name = uriSplit.size() > 1 ? uriSplit[1..-1].join('/') : ''
        //     }

        //     // Create custom error messages
        //     if (validationError instanceof MissingPropertyError) {
        //         def String paramUri = validationError.getUri().toString()
        //         error = "Missing required ${validationType}: ${validationError.getProperty()}" as String
        //     }
        //     else if (validationError instanceof ValidationError) {
        //         def String paramUri = validationError.getUri().toString()
        //         if (name == '') {
        //             this.errors.add("${validationError.getMessage()}" as String)
        //             return
        //         }
        //         def String value = validationError.getObject()
        //         def String msg = validationError.getMessage()
        //         error = "Error for ${validationType} '${name}' (${value}): ${msg}" as String
        //     }

        //     // Add the error to the list
        //     if (entry > 0) {
        //         this.errors.add("* Entry ${entry}: ${error}" as String)
        //     }
        //     else {
        //         this.errors.add("* ${error}" as String)
        //     }
        // })
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