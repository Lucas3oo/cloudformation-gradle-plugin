# cloudformation-gradle-plugin
Gradle plugin for create/update and delete AWS Cloudformation stacks.
The parameters for the Cloudformation template can be managed per environment and kept in
for instance Java properties files.


## Usage

### Apply to your project

Apply the plugin to your project.

```groovy
plugins {
  id 'se.solrike.cloudformation' version '1.0.0-beta.1'
}
```
Gradle 7.0 or later must be used.


### Quick start
The tasks have to be created. Minimal example on how to create a task that creates a stack.

```groovy
plugins {
  id 'se.solrike.cloudformation' version '1.0.0-beta.1'
}
task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  parameters = [ s3BucketName : 's3-bucket4711']
  stackName = "s3-buckets"
  templateFileName = 'aws-cloudformation/aws-s3-buckets.yaml'
}
```

The AWS Cloudformation template (minimal) under the folder `aws-cloudformation`  in the project looks like this:

```yaml
AWSTemplateFormatVersion: 2010-09-09
Parameters:
  S3BucketName:
    Type: String
Resources:
  S3Bucket:
    Type: AWS::S3::Bucket
    Properties:
      BucketName: !Ref S3BucketName
```

AWS credentials needs to be configured. E.g. environment variables or using `aws configure` CLI or Java system properties.


## How the CreateOrUpdateStackTask works
The usual credential and region chain is used to find the credentials and the region to use.

The task will first check if the stack already exists and if it does it will update the existing stack.

Stack parameter names in the Cloudformation template is usually in PascalCase but in Gradle you usually use camelCase. So the task will convert the parameter names to be in PascalCase for you.

The task can also filters out specific parameters given a name prefix. See below example.

The task will inline the Cloudformation template in the request to AWS. So it will not for instance store it in an S3 bucket. This means that the template can't be bigger than 51,200 bytes.

The task will also create the following tags on the stack:
* TemplateName - with the file name of the template
* TemplateGitVersion - If the Gradle project is using GIT then the last commit info for the template is added.
* CreatedBy/UpdatedBy - The local OS user's userID that executes the task.

## More advanced example
### Example 1
Some stacks need additional permission when they are created, called "capabilities".

When creating stacks for production you can add an extra safe guard against deletion, called "termination protection".

```groovy
task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  group = 'AWS'
  description = 'Create S3 buckets using Cloudformation template.'
  capabilities = [ 'CAPABILITY_IAM'] // default empty
  enableTerminationProtection = true // default false
  parameters = [ s3BucketName : 's3-bucket4711']
  stackName = "s3-buckets"
  templateFileName = 'aws-cloudformation/aws-s3-buckets.yaml'
}
```

### Example 2
If you have multiple environments then you want to re-use the template with different parameters.
Parameters for the Cloudformation template can be managed in Java properties files and the `java.util.Properties` instance can be used to configure the task.
Thanks to Gradle's provider/property and Groovy's closure concept which these tasks are using you can
delay the configuration of the task until it is executed. By that you can pass in properties to the task from command line or external resources.

Here the bucket name is instead specified in a Java properties file with the name of the environment the stack shall
be created for.

Properties file called `environments/stage25.properties`

```java
# Java properties file for stage 25
s3BucketName: my-bucket-for-stage25
```

The task definition which takes the name of the properties file as a command line argument.

```groovy
task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  group = 'AWS'
  description = 'Create S3 buckets using Cloudformation template. ' +
      'Specify the environment to use with -Penv=<my-env>.'
  parameters = project.objects.mapProperty(String, String).convention(project.provider({
                  Properties props = new Properties()
                  file("environments/${env}.properties").withInputStream { props.load(it) }
                  props
                }))
  stackName = project.objects.property(String).convention(project.provider( {"s3-buckets-${env}"} ))
  templateFileName = 'aws-cloudformation/aws-s3-buckets.yaml'
}
```

Run the task like:

```
./gradlew deployS3Stack -Penv=stage25
```

### Example 2.1
If you have all sorts of properties in the properties file you can filter out the ones that are applicable for the specific template using the parameter prefix attribute on the task. The parameter names will also be pruned of the prefix.

```java
# Java properties file for stage 25
# Properties for the S3 template
slrf.deploy.s3.s3BucketName: my-bucket-for-stage25
# Some property for another template
slrf.deploy.sam.handler: se.solrike.cloud.serverless.StreamLambdaHandler::handleRequest
```


```groovy
task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  group = 'AWS'
  description = 'Create S3 buckets using Cloudformation template. ' +
      'Specify the environment to use with -Penv=<my-env>.'
  parameters = project.objects.mapProperty(String, String).convention(project.provider({
                  Properties props = new Properties()
                  file("environments/${env}.properties").withInputStream { props.load(it) }
                  props
                }))
  parameterPrefix = 'slrf.deploy.s3.' // only include properties which begins with 'slrf.deploy.s3.'
  stackName = project.objects.property(String).convention(project.provider( {"s3-buckets-${env}"} ))
  templateFileName = 'aws-cloudformation/aws-s3-buckets.yaml'
}
```





## Release notes

### 1.0.0-beta.1
Tasks for create/update a stack and delete a stack.

### Future
Improvements that might be implemented are:
* Logging of stack creation events
