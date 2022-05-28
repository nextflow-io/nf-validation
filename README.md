# nf-validation 

This plugins implement a validation Nextlow pipeline parameters
based on [nf-core JSON schema](https://nf-co.re/pipeline_schema_builder).

## Get started 

To compile and run the tests use the following command: 


```
./gradlew check
```      


## Launch it with Nextflow 

[WORK IN PROGRESS]

To test with Nextflow for development purpose:

1. Clone the Nextflow repo into a sibling directory  

   ```
   cd .. && https://github.com/nextflow-io/nextflow
   ``` 

2. Append to the `settings.gradle` in this project the following line:

   ```
   includeBuild('../nextflow')
   ```                        
   
3. Compile the plugin code

   ```
   ./gradlew compileGroovy
   ```
   
4. run nextflow with this command:

    ```
    ./launch.sh run -plugins nf-validation <script/pipeline name> [pipeline params]
    ```


## Dependencies

* Java 11 or later
* https://github.com/everit-org/json-schema
