package nextflow.validation

import java.nio.file.Path
import groovy.util.logging.Slf4j

import org.everit.json.schema.FormatValidator
import nextflow.Nextflow

@Slf4j
public class FilePathPatternValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String subject) {
        if (subject.startsWith('s3://')) {
            log.debug("S3 paths are not supported by 'FilePathPatternValidator': '${subject}'")
            return Optional.empty()
        }
        ArrayList files = Nextflow.files(subject)
        ArrayList errors = []

        if(files.size() == 0) {
            return Optional.of("No files were found using the globbing pattern '${subject}'" as String)
        }
        for( file : files ) {
            if (file.isDirectory()) {
                errors.add("'${file.toString()}' is not a file, but a directory" as String)
            }
        }
        if(errors.size() > 0) {
            return Optional.of(errors.join('\n'))
        }
        return Optional.empty()
    }
}