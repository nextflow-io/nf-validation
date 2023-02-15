package nextflow.validation

import java.nio.file.Path

import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.junit.Rule
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import test.Dsl2Spec
import test.OutputCapture
/**
 * @author : jorge <jorge.aguilera@seqera.io>
 *
 */
class PluginExtensionMethodsTest extends Dsl2Spec{

    @Rule
    OutputCapture capture = new OutputCapture()


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
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate when no params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('src/testResources/nextflow_schema.json')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* Missing required parameter: --input\n* Missing required parameter: --outdir\n\n"
        !stdout
    }

    def 'should validate a schema' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should find unexpected params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        stdout.size() >= 1
        stdout.contains("* --xyz: /some/path")
    }

    def 'should ignore unexpected param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.xyz = '/some/path'
            params.schema_ignore_params = 'xyz'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should fail for unexpected param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.xyz = '/some/path'
            params.fail_unrecognised_params = true
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --xyz: /some/path\n\n"
        !stdout
    }

    def 'should find validation errors' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = '/some/path/input.csv'
            params.outdir = 10
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --outdir: expected type: String, found: Integer (10)\n\n"
        !stdout
    }

    def 'should correctly validate duration and memory objects' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.max_memory = '10.GB'
            params.max_time = '10.day'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should find validation errors for enum' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.publish_dir_mode = 'incorrect'
            params.max_time = '10.day'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --publish_dir_mode: 'incorrect' is not a valid choice (Available choices (5 of 6): symlink, rellink, link, copy, copyNoFollow, ... )\n\n"
        !stdout
    }

    def 'correct validation of integers' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.max_cpus = 12
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'correct validation of numbers with lenient mode' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.lenient_mode = true
            params.max_cpus = '4'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should fail because of incorrect integer' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.max_cpus = 1.2
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --max_cpus: expected type: Integer, found: BigDecimal (1.2)\n\n"
        !stdout
    }

    def 'should fail because of wrong pattern' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = '/some/path/input.csv'
            params.outdir = '/some/path'
            params.max_memory = '10'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters('$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:\n\n* --max_memory: string [10] does not match pattern ^\\d+(\\.\\d+)?\\.?\\s*(K|M|G|T)?B$ (10)\n\n'''
        !stdout
    }

    def 'should print a help message' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            include { paramsHelp } from 'plugin/nf-validation'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, '$schema')
            log.info help_msg
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Typical pipeline command:') ||
                    it.contains('nextflow run') ||
                    it.contains('Input/output options') ||
                    it.contains('--input') ||
                    it.contains('--outdir') ||
                    it.contains('--email') ||
                    it.contains('--multiqc_title') ||
                    it.contains('Reference genome options') ||
                    it.contains('--genome') ||
                    it.contains('--fasta') 
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 10
    }

    def 'should print a help message with argument options' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            include { paramsHelp } from 'plugin/nf-validation'
            params.show_hidden_params = true
            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, '$schema')
            log.info help_msg
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('publish_dir_mode') && 
                    it.contains('(accepted: symlink, rellink, link, copy, copyNoFollow') 
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 1
    }

    def 'should print a help message of one parameter' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            include { paramsHelp } from 'plugin/nf-validation'
            params.help = 'publish_dir_mode'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, '$schema')
            log.info help_msg
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('--publish_dir_mode') ||
                    it.contains('type       :') ||
                    it.contains('default    :') ||
                    it.contains('description:') ||
                    it.contains('help_text  :') ||
                    it.contains('fa_icon    :') || // fa_icon shouldn't be printed
                    it.contains('enum       :') ||
                    it.contains('hidden     :') 
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 7
    }

    def 'should fail when help param doesnt exist' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            include { paramsHelp } from 'plugin/nf-validation'
            params.help = 'no_exist'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, '$schema')
            log.info help_msg
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('--no_exist') ? it : null }

        then:
        def error = thrown(Exception)
        error.message == "Specified param 'no_exist' does not exist in JSON schema."
        !stdout
    }

    def 'should print params summary' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-validation'
            
            def summary_params = paramsSummaryLog(workflow, '$schema')
            log.info summary_params
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    it.contains('Core Nextflow options') ||
                    it.contains('runName') ||
                    it.contains('launchDir') ||
                    it.contains('workDir') ||
                    it.contains('projectDir') ||
                    it.contains('userName') ||
                    it.contains('profile') ||
                    it.contains('configFiles') ||
                    it.contains('Input/output options') ||
                    it.contains('outdir') 
                    ? it : null }
        
        then:
        noExceptionThrown()
        stdout.size() == 11
        stdout ==~ /.*\[0;34moutdir     : .\[0;32moutDir.*/
    }
}
