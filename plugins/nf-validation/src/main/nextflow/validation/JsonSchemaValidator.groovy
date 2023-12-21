package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaException
import net.jimblackler.jsonschemafriend.SchemaStore
import net.jimblackler.jsonschemafriend.Validator
import net.jimblackler.jsonschemafriend.MissingPropertyError
import net.jimblackler.jsonschemafriend.DependencyError
import net.jimblackler.jsonschemafriend.ValidationError
import org.json.JSONObject
import org.json.JSONArray

import java.util.regex.Pattern
import java.util.regex.Matcher

@Slf4j
@CompileStatic
public class JsonSchemaValidator {

    private static Schema schema
    private static List<String> errors = []
    private static Pattern uriPattern = Pattern.compile('^#/(\\d*)?/?(.*)$')

    JsonSchemaValidator(String schemaString) {
        SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
        this.schema = schemaStore.loadSchemaJson(schemaString) // Load the schema.
    }

    private static validateObject(Object input, String validationType) {
        Validator validator = new Validator(true)
        validator.validate(this.schema, input, validationError -> {
            // Fail on other errors than validation errors
            if(validationError !instanceof ValidationError) {
                // TODO handle this better
                log.error("* ${validationError.getMessage()}" as String)
                return
            }

            // Get the name of the parameter and determine if it is a list entry
            def Integer entry = 0
            def String name = ''
            def String[] uriSplit = validationError.getUri().toString().replaceFirst('#/', '').split('/')
            def String error = ''

            if (input instanceof Map) {
                name = uriSplit.size() > 0 ? uriSplit[0..-1].join('/') : ''
            }
            else if (input instanceof List) {
                entry = uriSplit[0].toInteger() + 1
                name = uriSplit.size() > 1 ? uriSplit[1..-1].join('/') : ''
            }

            // Create custom error messages
            if (validationError instanceof MissingPropertyError) {
                def String paramUri = validationError.getUri().toString()
                error = "Missing required ${validationType}: ${validationError.getProperty()}" as String
            }
            else if (validationError instanceof ValidationError) {
                def String paramUri = validationError.getUri().toString()
                if (name == '') {
                    this.errors.add("${validationError.getMessage()}" as String)
                    return
                }
                def String value = validationError.getObject()
                def String msg = validationError.getMessage()
                error = "Error for ${validationType} '${name}' (${value}): ${msg}" as String
            }

            // Add the error to the list
            if (entry > 0) {
                this.errors.add("* Entry ${entry}: ${error}" as String)
            }
            else {
                this.errors.add("* ${error}" as String)
            }
        })
    }

    public static List<String> validate(JSONArray input, String validationType) {
        List<Map<String,Object>> inputList = input.collect { entry ->
            JSONObject jsonEntry = (JSONObject) entry
            jsonEntry.toMap()
        }
        this.validateObject(inputList, validationType)
        return this.errors
    }

    public static List<String> validate(JSONObject input, String validationType) {
        this.validateObject(input.toMap(), validationType)
        return this.errors
    }
}