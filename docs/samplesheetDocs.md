# Samplesheet JSON schema

This page will describe the custom function that are implemented in this plugin for samplesheet validation and conversion. All functionality from JSON schema draft 4-7 is supported. For these functions, see the [official JSON schema docs](https://json-schema.org/).

An example samplesheet can be found [here](../plugins/nf-validation/src/testResources/schema_input.json).

## Properties
All fields should be present in the `properties` section. These should be in the order you want for the output channel e.g. for this the example schema the output channel will contain following fields in that exact order:
| name | type | contents |
|------|------|----------|
| meta | List | This will contain all values that are flagged with `meta` in the samplesheet. In the example this will contain the `string1`, `string2`, `integer1`, `integer2`, `boolean1` and `boolean2` values converted to their correct type (as specified in the samplesheet) |
| string | String | The value given in `string` as a String type |
| number | Integer/Number | The value given in `integer` as an Integer type |
| boolean | Boolean | The value given in `boolean` as a Boolean type |
| file | Nextflow.File | The value given in `file` as a Nextflow File type |
| directory | Nextflow.File | The value given in `directory` as a Nextflow type |
| uniqueField | String | The value given in `uniqueField` as a String type, which needs to be unique across all entries in the samplesheet |
| uniqueDependentField | Integer | The value given in `uniqueDependentField` as an Integer type, which needs to be unique in combination with `uniqueField` across all entries |
| nonExistingField | String | The value given in `nonExistingField` as a String type, which will default to `itDoesExist` if no value has been given here. |

A real use example of this will look like this when printed with `.view()`:
```
[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, path/to/test.txt, path/to/testDir, unique1, 1, itDoesExist]
[[string1:value, string2:value, integer1:5, integer2:5, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], itDoesExist]
[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], unique2, 1, itDoesExist]
[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, path/to/test.txt, path/to/testDir, unique3, 1, itDoesExist]

```

### Global parameters
These schema specifications can be used on any type of input:
<table>
    <tr style="font-weight: bold">
        <td> Parameter </td><td> Type </td><td> Description </td><td> Example </td>
    </tr>
    <tr>
        <td> meta </td>
        <td> List </td>
        <td> The current field will be considered a meta value when this parameter is present. This parameter should contain a list of the meta fields to assign this value to. The default is no meta for each field. </td>
        <td> 

```json
"field": {
    "meta": ["id","sample"]
}
``` 
will convert the `field` value to a meta value, resulting in the channel `[[id:value, sample:value]...]` 
See [here](../plugins/nf-validation/src/testResources/schema_input.json#L8-22) for an example in the samplesheet.
        </td>
    </tr>
    <tr>
        <td> unique </td>
        <td> Boolean or List </td>
        <td> Whether or not the field should contain a unique value over the entire samplesheet. A list can also be given to this parameter. This list should contain other field names that should be unique in combination with the current field. The default is `false`. </td>
        <td>

```json
"field1": {
    "unique":true
},
"field2": {
    "unique": ["field1"]
}
```
`field1` needs to be unique in this example. `field2` needs to be unique in combination with `field1`. So for a samplesheet like this:
```csv
field1,field2
value1,value2
value1,value3
value1,value2
```
both checks will fail. `field1` isn't unique since `value1` has been found more than once. `field2` isn't unique in combination with `field1` because the `value1,value2` combination has been found more than once.
See [here](../plugins/nf-validation/src/testResources/schema_input.json#L42-49) for an example in the samplesheet.
        </td>
    </tr>
    <tr>
        <td> deprecated </td>
        <td> Boolean </td>
        <td> A boolean variable stating that the field is deprecated and will be removed in the nearby future. This will throw a warning to the user that the current field is deprecated. The default value is `false` </td>
        <td>
```json
"field": {
    "deprecated": true
}
```
will show a warning stating that the use of `field` is deprecated: `The 'field' field is deprecated and will no longer be used in the future. Please check the official documentation of the pipeline for more information.`
        </td>
    </tr>
    <tr>
        <td> dependentRequired </td>
        <td> List </td>
        <td> A list containing names of other fields. The validator will check if these fields are filled in and throw an error if they aren't, but only when the field `dependentRequired` belongs to is filled in. </td>
        <td>

```json
"field1": {
    "dependentRequired": ["field2"]
},
"field2": {}
```
will check if `field2` is given when `field1` has a value. So for example:
```csv
field1,field2
value1,value2
value1,
,value2
```
The first row will pass the check because both fields are set. The second row will fail because `field1` is set, but `field2` isn't and `field1` is dependent on `field2`. The third row will pass the check because `field1` isn't set.
See [here](../plugins/nf-validation/src/testResources/schema_input.json#L8-22) for an example in the samplesheet.
        </td>
    </tr>
</table>

### Formats

Formats can be used to check `string` values for certain properties.

Following table shows all additional formats implemented in this plugin. These formats will also be converted to the correct type.

| Format | Description |
|-----------|-------------|
| file-path | Automatically checks if the file exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input |
| directory-path | Automatically checks if the directory exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input. This is currently synonymous for `file-path`. |

You can use the formats like this:
```json
"field": {
    "type":"string",
    "format":"file-path"
}
```
See [here](../plugins/nf-validation/src/testResources/schema_input.json#L33-41) for an example in the samplesheet.

