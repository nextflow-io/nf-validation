package nextflow.validation

import java.nio.file.Path

import org.everit.json.schema.FormatValidator
import nextflow.Nextflow

public class PathValidator implements FormatValidator {

    @Override
    public Optional<String> validate(final String subject) {
        Path file = Nextflow.file(subject) as Path  
        return Optional.empty()
    }
}