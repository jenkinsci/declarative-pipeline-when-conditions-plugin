## Additional When Conditions for Declarative Pipelines

This plugin provides additional `when` conditions for [Declarative Pipelines](https://plugins.jenkins.io/pipeline-model-definition).

### Conditions

#### stageHasRun

##### Arguments

* `stageName`: The name of a `stage`

True if a `stage` with this name has run or started running in this Pipeline, and 
has not been skipped.