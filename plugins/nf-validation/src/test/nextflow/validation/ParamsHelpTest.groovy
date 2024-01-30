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
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : jorgeaguileraseqera
 */
class ParamsHelpTest extends Dsl2Spec{

    @Rule
    OutputCapture capture = new OutputCapture()


    @Shared String pluginsMode

    Path root = Path.of('.').toAbsolutePath().normalize()
    Path getRoot() { this.root }
    String getRootString() { this.root.toString() }

    def setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
        def root = this.getRoot()
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
}