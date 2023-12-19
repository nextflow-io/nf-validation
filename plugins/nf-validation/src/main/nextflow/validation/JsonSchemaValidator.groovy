package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaException
import net.jimblackler.jsonschemafriend.MissingPropertyError
import net.jimblackler.jsonschemafriend.SchemaStore
import net.jimblackler.jsonschemafriend.Validator
import org.json.JSONObject
import org.json.JSONArray

@Slf4j
@CompileStatic
public class JsonSchemaValidator {

    private static Schema schema
    private static List<String> errors = []

    JsonSchemaValidator(String schemaString) {
        SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
        this.schema = schemaStore.loadSchemaJson(schemaString) // Load the schema.
    }

    public static List<String> validateObject(JSONObject input) {
        Validator validator = new Validator()
        validator.validate(this.schema, input.toMap(), validationError -> {
            if(validationError instanceof SchemaException) {
                // TODO handle this better
                log.error("* ${validationError.getMessage()}" as String)
            }
            else if (validationError instanceof MissingPropertyError) {
                this.errors.add("* Missing required parameter: --${validationError.getProperty()}" as String)
            } else {
                // TODO write custom error messages for other types of errors
                this.errors.add("* ${validationError}" as String)
            }
        })
        return this.errors
    }

    public static List<String> validateArray(JSONArray input) {
        Validator validator = new Validator()
        Integer entryCount = 0
        input.forEach { entry ->
            entryCount++
            JSONObject jsonEntry = (JSONObject) entry
            validator.validate(this.schema, jsonEntry.toMap(), validationError -> {
                if(validationError instanceof SchemaException) {
                    // TODO handle this better
                    log.error("* ${validationError.getMessage()}" as String)
                }
                else if (validationError instanceof MissingPropertyError) {
                    this.errors.add("* Entry ${entryCount}: Missing required field: ${validationError.getProperty()}" as String)
                } else {
                    // TODO write custom error messages for other types of errors
                    this.errors.add("* ${validationError}" as String)
                }
            })
        }
        return this.errors
    }
}