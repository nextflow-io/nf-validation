package nextflow.validation

import java.nio.file.Path

import org.everit.json.schema.FormatValidator
import nextflow.Nextflow

public class FilePathExistsValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String subject) {
        Path file = Nextflow.file(subject) as Path  
        if (!file.exists()) {
            return Optional.of("the file '${subject}' does not exist" as String)
        }
        else if (file.isDirectory()) {
            return Optional.of("'${subject}' is not a file" as String)
        }
        return Optional.empty()
    }
}