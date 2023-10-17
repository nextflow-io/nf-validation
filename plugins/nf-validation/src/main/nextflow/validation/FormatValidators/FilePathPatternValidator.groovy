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
        ArrayList files = Nextflow.file(subject) as ArrayList
        ArrayList errors = []
        for( file : files ) {
            if (file.isDirectory()) {
                errors.add("'${subject}' is not a file, but a directory" as String)
            }
        }
        if(errors.size > 0) {
            return Optional.of(errors.join('\n'))
        }
        return Optional.empty()
    }
}