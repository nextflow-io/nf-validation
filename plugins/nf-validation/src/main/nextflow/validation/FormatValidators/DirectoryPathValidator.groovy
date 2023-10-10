package nextflow.validation

import java.nio.file.Path
import groovy.util.logging.Slf4j

import org.everit.json.schema.FormatValidator
import nextflow.Nextflow

@Slf4j
public class DirectoryPathValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String subject) {
        if (subject.startsWith('s3://')) {
            log.debug("S3 paths are not supported by 'DirectoryPathValidator': '${subject}'")
            return Optional.empty()
        }
        Path file = Nextflow.file(subject) as Path
        if (file.exists() && !file.isDirectory()) {
            return Optional.of("'${subject}' is not a directory, but a file" as String)
        }
        return Optional.empty()
    }
}