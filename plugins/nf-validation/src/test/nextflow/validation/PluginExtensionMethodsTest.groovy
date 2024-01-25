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

    // 
    // Params validation tests
    //

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
            
            validateParameters(parameters_schema: 'src/testResources/nextflow_schema.json', monochrome_logs: params.monochrome_logs)
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* Missing required parameter(s): input, outdir

"""
        !stdout
    }

    def 'should validate a schema with no arguments' () {
        given:
        def schema_source = new File('src/testResources/nextflow_schema.json')
        def schema_dest   = new File('nextflow_schema.json')
        schema_dest << schema_source.text

        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters()
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

        cleanup:
        schema_dest.delete()
    }

    def 'should validate a schema - CSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'should validate a schema - TSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.tsv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'should validate a schema - YAML' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.yaml'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'should validate a schema - JSON' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.json'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'should validate a schema with failures - CSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/wrong.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.csv): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded'"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): Sample name must be provided and cannot contain spaces"
        !stdout
    }

    def 'should validate a schema with failures - TSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/wrong.tsv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.tsv): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded'"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): Sample name must be provided and cannot contain spaces"
        !stdout
    }

    def 'should validate a schema with failures - YAML' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/wrong.yaml'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.yaml): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded'"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): Sample name must be provided and cannot contain spaces"
        !stdout
    }

    def 'should validate a schema with failures - JSON' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/wrong.json'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.json): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded'"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): Sample name must be provided and cannot contain spaces"
        !stdout
    }

    def 'should find unexpected params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            params.validationSchemaIgnoreParams = 'xyz'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            params.validationFailUnrecognisedParams = true
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema', monochrome_logs: params.monochrome_logs)
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
            params.input = 'src/testResources/correct.csv'
            params.outdir = 10
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema', monochrome_logs: params.monochrome_logs)
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* --outdir (10): Value is [integer] but should be [string]

"""
        !stdout
    }

    def 'should correctly validate duration and memory objects' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_memory = '10.GB'
            params.max_time = '10.day'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'correct validation of integers' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_cpus = 12
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'correct validation of file-path-pattern - glob' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.glob = 'src/testResources/*.csv'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'correct validation of file-path-pattern - single file' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.glob = 'src/testResources/correct.csv'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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
            params.input = 'src/testResources/correct.csv'
            params.outdir = 1
            params.validationLenientMode = true
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_cpus = 1.2
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema', monochrome_logs: params.monochrome_logs)
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --max_cpus (1.2): Value is [number] but should be [integer]\n\n"
        !stdout
    }

    //
    // --help argument tests
    //

    def 'should print a help message' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            include { paramsHelp } from 'plugin/nf-validation'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
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
            params.validationShowHiddenParams = true
            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
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
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
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
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
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

    //
    // Summary of params tests
    //

    def 'should print params summary' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-validation'
            
            def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
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

    // 
    // Samplesheet validation tests
    //

    def 'should validate a schema from an input file' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.input = 'src/testResources/samplesheet.csv'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema')
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

    def 'should fail because of wrong file pattern' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = 'src/testResources/samplesheet_wrong_pattern.csv'
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(parameters_schema: '$schema', monochrome_logs: params.monochrome_logs)
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* --input (src/testResources/samplesheet_wrong_pattern.csv): Validation of file failed:
\t-> Entry 1: Error for field 'fastq_1' (test1_fastq1.txt): FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'
\t-> Entry 2: Error for field 'fastq_1' (test2_fastq1.txt): FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'

"""
        !stdout
    }

    def 'should fail because of missing required value' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def  SCRIPT_TEXT = """
            params.monochrome_logs = true
            params.input = 'src/testResources/samplesheet_no_required.csv'
            include { validateParameters } from 'plugin/nf-validation'

            validateParameters(parameters_schema: '$schema', monochrome_logs: params.monochrome_logs)
        """

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --input (src/testResources/samplesheet_no_required.csv): Validation of file failed:
\t-> Entry 1: Missing required field(s): sample
\t-> Entry 2: Missing required field(s): strandedness, sample

'''
        !stdout
    }
}
