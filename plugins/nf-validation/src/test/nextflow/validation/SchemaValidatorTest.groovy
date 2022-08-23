package nextflow.validation

import spock.lang.Specification
import groovy.json.JsonSlurper

import java.nio.file.Path

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class SchemaValidatorTest extends Specification {

    def 'should validate a schema' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [transcriptome: '/some/path']
        validator.validateParameters(params, schema)

        then:
        !validator.hasErrors()
        !validator.hasWarnings()
    }

    def 'should find unexpected params' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [xyz: '/some/path']
        validator.validateParameters(params, schema)

        then:
        validator.hasWarnings()
        validator.warnings == ['* --xyz: /some/path']
        and:
        !validator.hasErrors()
    }

    def 'should ignore unexpected param' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [xyz: '/some/path', schema_ignore_params: 'xyz']
        validator.validateParameters(params, schema)

        then:
        !validator.hasWarnings()
        !validator.hasErrors()
    }

    def 'should fail for unexpected param' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [xyz: '/some/path', fail_unrecognised_params: 'true']
        validator.validateParameters(params, schema)

        then:
        validator.hasErrors()
        validator.errors == ['* --xyz: /some/path']
        and:
        !validator.hasWarnings()
    }

    def 'should find validation errors' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [outdir: 10]
        validator.validateParameters(params, schema)

        then:
        validator.hasErrors()
        validator.errors == [ '* --outdir: expected type: String, found: Integer (10)' ]
        and:
        !validator.hasWarnings()
    }

    def 'should correctly validate duration and memory objects' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [max_memory: 10.GB, max_time: 10.d]
        validator.validateParameters(params, schema)

        then:
        !validator.hasErrors()
        !validator.hasWarnings()
    }

    def 'should find validation errors for enum' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [publish_dir_mode: "incorrect"]
        validator.validateParameters(params, schema)

        then:
        validator.hasErrors()
        validator.errors == [ "* --publish_dir_mode: 'incorrect' is not a valid choice (Available choices (5 of 6): symlink, rellink, link, copy, copyNoFollow, ... )" ]
    }

    def 'correct validation of integers' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [max_cpus: 12]
        validator.validateParameters(params, schema)

        then:
        !validator.hasErrors()
        !validator.hasWarnings()
    }

    def 'correct validation of numbers' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [generic_number: 0.43]
        validator.validateParameters(params, schema)

        then:
        !validator.hasWarnings()
        !validator.hasErrors()
    }

    def 'should fail because of incorrect integer' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [max_cpus: 1.2]
        validator.validateParameters(params, schema)

        then:
        validator.hasErrors()
        validator.errors == [ '* --max_cpus: expected type: Integer, found: BigDecimal (1.2)' ]
        !validator.hasWarnings()
    }

    def 'should fail because of wrong pattern' () {
        given:
        def validator = new SchemaValidator()

        when:
        def ClassLoader classLoader = getClass().getClassLoader()
        def String schema = (String) getClass().getResource("test_schema.json").getPath()
        def params = [max_memory: '10']
        validator.validateParameters(params, schema)

        then:
        validator.hasErrors()
        validator.errors == [ '* --max_memory: string [10] does not match pattern ^[\\d\\.]+\\s*.(K|M|G|T)?B$ (10)' ]
        !validator.hasWarnings()
    }
}
