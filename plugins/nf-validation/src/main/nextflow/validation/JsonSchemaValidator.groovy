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

@Slf4j
@CompileStatic
public class JsonSchemaValidator {

    private static Schema schema
    private static List<String> errors = []

    JsonSchemaValidator(String schemaString) {
        SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
        this.schema = schemaStore.loadSchemaJson(schemaString) // Load the schema.
    }

    public static List<String> validateObject(JSONObject input, String validationType, Integer entryCount) {
        Validator validator = new Validator(true)
        def String entryString = entryCount != -1 ? "Entry ${entryCount}: " : ""
        validator.validate(this.schema, input.toMap(), validationError -> {
            if(validationError instanceof SchemaException) {
                // TODO handle this better
                log.error("* ${validationError.getMessage()}" as String)
            }
            else if (validationError instanceof MissingPropertyError) {
                println(validationError.getMessage())
                this.errors.add("* ${entryString}Missing required ${validationType}: --${validationError.getProperty()}" as String)
            }
            else if (validationError instanceof ValidationError) {
                def String paramUri = validationError.getUri().toString()
                if (paramUri == '') {
                    this.errors.add("* ${entryString}${validationError.getMessage()}" as String)
                    return
                }
                def String param = paramUri.replaceFirst("#/", "")
                def String value = validationError.getObject()
                def String msg = validationError.getMessage()
                this.errors.add("* ${entryString}Error for ${validationType} '${param}' (${value}): ${msg}" as String)   
            } else {
                this.errors.add("* ${entryString}${validationError}" as String)
            }
        })
        return this.errors
    }

    public static List<String> validateArray(JSONArray input) {
        Integer entryCount = 0
        input.forEach { entry ->
            entryCount++
            JSONObject jsonEntry = (JSONObject) entry
            validateObject(jsonEntry, "field", entryCount)
        }
        return this.errors
    }
}