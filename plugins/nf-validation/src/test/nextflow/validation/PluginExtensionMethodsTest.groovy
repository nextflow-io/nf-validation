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
        def result = dsl_eval(SCRIPT_TEXT)

        then:
        true
    }

    def 'should validate parameters functions' () {
        given:
        def  SCRIPT_TEXT = '''
            include { validateParameters } from 'plugin/nf-validation'
            
            validateParameters(params)
        '''

        when:
        def result = dsl_eval(SCRIPT_TEXT)

        then:
        true
    }
}
