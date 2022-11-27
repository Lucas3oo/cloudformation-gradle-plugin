# cloudformation-gradle-plugin
Gradle plugin for create/update and delete AWS Cloudformation stacks.
The parameters for the Cloudformation template can be managed per environment and kept in
for instance Java properties files. Values in the Java properties files will be "interpolated"
according to Groovy's evaluation. That is, properties can reference other properties and Groovy/Java functions.


## Usage

### Apply to your project

Apply the plugin to your project.

```groovy
plugins {
  id 'se.solrike.cloudformation' version '1.0.0-beta.3'
}
```
Gradle 7.0 or later must be used.


### Quick start
The tasks have to be created. Minimal example on how to create a task that creates a stack.

```groovy
plugins {
  id 'se.solrike.cloudformation' version '1.0.0-beta.3'
}
task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  parameters = [ s3BucketName : 's3-bucket4711']
  stackName = 's3-buckets'
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

## The plugin provides three task
The plugin provides three tasks that all need to be created manually. They will not be created when the plugin is
applied.

* CreateOrUpdateStackTask
* DeleteStackTask
* PrintEnviromentParametersTask

## How the CreateOrUpdateStackTask works
The usual credential and region chain is used to find the credentials and the region to use.

The task will first check if the stack already exists and if it does it will update the existing stack.

Parameters for the stack is passed in as `Map` to the task. The map can in turn be inlined or be read from a Java properties file or any other file that can be deserialised into a map.

Stack parameter names in the Cloudformation template is usually in PascalCase but in Gradle you usually use camelCase. So the task will convert the parameter names to be in PascalCase for you.

If a parameter's value has Groovy's notation for string interpolation those will be evaluated (expanded). That means a parameter's value can reference another parameter or even a Groovy/Java function. See example 3 below.

The task can also filters out specific parameters given a name prefix. See example 2.1 below.

The task will inline the Cloudformation template in the request to AWS. So it will not for instance store it in an S3 bucket. This means that the template can't be bigger than 51,200 bytes.

The task will also create the following tags on the stack:
* TemplateName - with the file name of the template
* TemplateGitVersion - If the Gradle project is using GIT then the last commit info for the template is added.
* CreatedBy/UpdatedBy - The local OS user's userID that executes the task.



## How the DeleteStackTask works
The task will delete a stack. Typically define a task like this:

```groovy
task deleteS3Stack(type: se.solrike.cloudformation.DeleteStackTask) {
  stackName = "s3-buckets"
}
```

## How the PrintEnviromentParametersTask works
This task is mostly interested in order to debug a complex setup. It will take a map of parameters and
resolve all the values and then print those on the console. Here the map of parameters is read from a Java properties file.

```groovy
task printEnv(type: se.solrike.cloudformation.PrintEnviromentParametersTask) {
  description = 'Print the resolved parameters for the environment. ' +
      'Specify the enviroment to use with -Penv=<my-env>.'
  parameters = project.objects.mapProperty(String, String).convention(project.provider({
                  Properties props = new Properties()
                  file("environments/${env}.properties").withInputStream { props.load(it) }
                  props
                }))
  // pass in classloader so custom Groovy classes can be referenced in the env's properties file
  parentClassLoader = getClass().getClassLoader()
}
```


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
  stackName = 's3-buckets'
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
slrk.deploy.s3.s3BucketName: my-bucket-for-stage25
# Some property for another template
slrk.deploy.sam.handler: se.solrike.cloud.serverless.StreamLambdaHandler::handleRequest
```


```groovy
task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  group = 'AWS'
  description = 'Create S3 buckets using Cloudformation template. ' +
      'Specify the environment to use with -Penv=<my-env>.'
  parameterPrefix = 'slrk.deploy.s3.' // only include properties which begins with 'slrk.deploy.s3.'
  parameters = project.objects.mapProperty(String, String).convention(project.provider({
                  Properties props = new Properties()
                  file("environments/${env}.properties").withInputStream { props.load(it) }
                  props
                }))
  stackName = project.objects.property(String).convention(project.provider( {"s3-buckets-${env}"} ))
  templateFileName = 'aws-cloudformation/aws-s3-buckets.yaml'
}
```

### Example 3
In a large complex setup there are more than one template and consequently a lot more stack parameters. Sometimes you need to have the same value as input to several stacks and following DRY you need to be able to have variables in the properties files where you have the stacks parameters.

The following example has two stacks which shares an S3 bucket. One creates it and the other consumes it. So the bucket name needs to be as a parameter to both of them. Also the a DB password is needed and you don't want to have that as clear text in the environment's properties files. The example is simply base 64 encoding the password but in real life it shall be encrypted of course.

Properties file:

```java
# Java properties file for stage 25.
# Values are "interpolated" (evaluated as they where Groovy script) so it means those can reference
# other parameter and also be functions.

# common props
slrk.deploy.env.name: ${env}

# Properties for the S3 template
slrk.deploy.s3.s3BucketName: my-bucket-for-${slrk.deploy.env.name}

# Some property for another template
slrk.deploy.sam.handler: se.solrike.cloud.serverless.StreamLambdaHandler::handleRequest
slrk.deploy.sam.s3BucketName: ${slrk.deploy.s3.s3BucketName}

# DB user and password
slrk.deploy.db.user: application-user
slrk.deploy.db.password: ${MyUtils.decode('bXlub3Rzb3NlY3JldHBhc3N3b3JkCg==')}
```

Task to deploy and Groovy code in build.gradle to support the task.

```groovy
Properties loadProperties(String env) {
  Properties props = new Properties()
  file("environments/${env}.properties").withInputStream { props.load(it) }
  // so that the property 'env' can be referenced in the env's properties file
  props.setProperty('env', env)
  return props
}

class MyUtils {
  static String decode(String text) {
    return new String(Base64.getDecoder().decode(text)).trim()
  }
}

task deployS3Stack(type: se.solrike.cloudformation.CreateOrUpdateStackTask) {
  group = 'AWS'
  description = 'Create S3 buckets using Cloudformation template. ' +
      'Specify the enviroment to use with -Penv=<my-env>.'
  parameterPrefix = 'slrk.deploy.s3.' // only include properties which begins with 'slrk.deploy.s3.'
  parameters = project.objects.mapProperty(String, String).convention(project.provider({loadProperties(env)}))
  // pass in classloader so the MyUtils class above can be referenced in the env's properties file
  parentClassLoader = getClass().getClassLoader()
  stackName = project.objects.property(String).convention(project.provider( {"s3-buckets-${env}"} ))
  templateFileName = 'aws-cloudformation/aws-s3-buckets.yaml'
}
```





## Release notes
### 1.0.0-beta.3
Then the stack is ready any outputs form the stack will be listed on the console.

### 1.0.0-beta.2
* Supports Groovy string interpolation of values for the stack parameters.
* Added new task; `PrintEnviromentParametersTask`.


### 1.0.0-beta.1
Tasks for create/update a stack and delete a stack.

### Future
Improvements that might be implemented are:
* Logging of stack creation events
