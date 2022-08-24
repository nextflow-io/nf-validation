package nextflow.validation

import nextflow.Channel
import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import spock.lang.TempDir
import test.Dsl2Spec

import java.nio.file.Path
import java.nio.file.Files


/**
 * @author : jorge <jorge.aguilera@seqera.io>
 *
 */
class PluginExtensionMethodsTest extends Dsl2Spec{

    @TempDir
    @Shared
    Path folder

    @Shared String pluginsMode

    def setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
        def root = Path.of('.').toAbsolutePath().normalize()
        def manager = new TestPluginManager(root){
            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder(){
                    @Override
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/resources/main/META-INF/MANIFEST.MF')
                    }
                }
            }
        }
        Plugins.init(root, 'dev', manager)
    }

    def cleanup() {
        Plugins.stop()
        PluginExtensionProvider.reset()
        pluginsMode ? System.setProperty('pf4j.mode',pluginsMode) : System.clearProperty('pf4j.mode')
    }

    def 'should import functions' () {
        given:
        def  SCRIPT_TEXT = '''
            include { validateParameters } from 'plugin/nf-validation'
        '''

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should validate when no params' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, 'src/testResources/test_schema.json')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should validate a schema' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.transcriptome = '/some/path'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should find unexpected params' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should ignore unexpected param' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.xyz = '/some/path'
            params.schema_ignore_params = 'xyz'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should fail for unexpected param' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.xyz = '/some/path'
            params.fail_unrecognised_params = true
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n* --xyz: /some/path"
    }

    def 'should find validation errors' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.outdir = 10
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n* --outdir: expected type: String, found: Integer (10)"
    }

    def 'should correctly validate duration and memory objects' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.max_memory = 10.GB
            params.max_time = 10.d
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should find validation errors for enum' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.publish_dir_mode = 'incorrect'
            params.max_time = 10.d
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n* --publish_dir_mode: 'incorrect' is not a valid choice (Available choices (5 of 6): symlink, rellink, link, copy, copyNoFollow, ... )"
    }

    def 'correct validation of integers' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.max_cpus = 12
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'correct validation of numbers' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.generic_number = 0.43
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        noExceptionThrown()
    }

    def 'should fail because of incorrect integer' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.max_cpus = 1.2
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n* --max_cpus: expected type: Integer, found: BigDecimal (1.2)"
    }

    def 'should fail because of wrong pattern' () {
        given:
        def schema = Path.of('src/testResources/test_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.max_memory = '10'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(session, '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n* --max_memory: string [10] does not match pattern ^[\\d\\.]+\\s*.(K|M|G|T)?B$ (10)"
    }

}
