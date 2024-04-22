package nextflow.validation

import java.nio.file.Path
import groovy.util.logging.Slf4j

import org.everit.json.schema.FormatValidator
import nextflow.Nextflow

@Slf4j
public class FilePathValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String subject) {
        if (subject.startsWith('s3://')) {
            log.debug("S3 paths are not supported by 'FilePathValidator': '${subject}'")
            return Optional.empty()
        }
        Path file = Nextflow.file(subject) as Path  
        if (file.isDirectory()) {
            return Optional.of("'${subject}' is not a file, but a directory" as String)
        }
        return Optional.empty()
    }
}