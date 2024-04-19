---
title: Configuration
description: Description of all possible configuration options for nf-schema
---

# Configuration

The plugin can be configured using several configuration options. These options have to be in the `validation` scope which means you can write them in two ways:

```groovy
validation.<option> = <value>
```

OR

```groovy
validation {
    <option1> = <value1>
    <option2> = <value2>
}
```

## monochromeLogs

This option can be used to turn of the colored logs from nf-validation. This can be useful if you run a Nextflow pipeline in an environment that doesn't support colored logging.

```groovy
validation.monochromeLogs = <true|false> // default: false
```

## lenientMode

This option can be used to make the type validation more lenient. In normal cases a value of `"12"` will fail if the type is an `integer`. This will succeed in lenient mode since that string can be cast to an `integer`.

```groovy
validation.lenientMode = <true|false> // default: false
```

## failUnrecognisedParams

By default the `validateParameters()` function will only give a warning if an unrecognised parameter has been given. This usually indicates that a typo has been made and can be easily overlooked when the plugin only emits a warning. You can turn this warning into an error with the `failUnrecognisedParams` option.

```groovy
validation.failUnrecognisedParams = <true|false> // default: false
```

## showHiddenParams

By default the parameters, that have the `"hidden": true` annotation in the JSON schema, will not be shown in the help message created by `paramsHelp()`. Turning on this option will make sure the hidden parameters are also shown.

```groovy
validation.showHiddenParams = <true|false> // default: false
```

## ignoreParams

This option can be used to turn off the validation for certain parameters. It takes a list of parameter names as input.

```groovy
validation.ignoreParams = ["param1", "param2"] // default: []
```

## defaultIgnoreParams

!!! warning

    This option should only be used by pipeline developers

This option does exactly the same as `validation.ignoreParams`, but provides pipeline developers with a way to set the default parameters that should be ignored. This way the pipeline users don't have to re-specify the default ignored parameters when using the `validation.ignoreParams` option.

```groovy
validation.defaultIgnoreParams = ["param1", "param2"] // default: []
```
