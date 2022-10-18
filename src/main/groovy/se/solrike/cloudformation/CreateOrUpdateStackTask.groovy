package se.solrike.cloudformation

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import software.amazon.awssdk.core.waiters.WaiterResponse
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.Capability
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse
import software.amazon.awssdk.services.cloudformation.model.Parameter
import software.amazon.awssdk.services.cloudformation.model.Tag
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse

/**
 * Task to create/update AWS Cloudformation stack. If the stack already exists then the stack will be updated.
 * <p>
 * The task will use the default credentials and region lookup chain.
 *
 * @author Lucas Persson
 */
abstract class CreateOrUpdateStackTask extends DefaultTask {

  /**
   * In some cases, you must explicitly acknowledge that your stack template contains certain capabilities
   * in order for CloudFormation to create the stack.
   *
   * @see software.amazon.awssdk.services.cloudformation.model.CreateStackRequest.Builder#capabilities
   */
  @Input
  @Optional
  public abstract ListProperty<String> getCapabilities()

  /**
   * Enable enable termination protection of the stack. Default false.
   */
  @Input
  @Optional
  public abstract Property<Boolean> getEnableTerminationProtection()

  /**
   * The template parameters.
   * <p>
   * Parameter names will be converted to PascalCase-ish since that is what Cloudformation templates expects.
   */
  @Input
  public abstract MapProperty<String, String> getParameters()

  /**
   * Optionally specify a parameter name prefix to filter out the parameters to use.
   * <p>
   * The submitted parameter names will have the prefix string removed from the names.
   */
  @Input
  @Optional
  public abstract Property<String> getParameterPrefix()

  /**
   * Stack name.
   */
  @Input
  public abstract Property<String> getStackName()

  /**
   * The file path of the Cloudformation template.
   */
  @Input
  public abstract Property<String> getTemplateFileName()


  @Internal
  CloudFormationClient client


  @TaskAction
  void execute() {

    client = CloudFormationClient.builder().build()

    if (stackExists(getStackName().get())) {
      updateStack()
    }
    else {
      createStack()
    }
    println "Stack '${getStackName().get()}' is ready."
  }

  void createStack() {
    CreateStackRequest.Builder builder = CreateStackRequest.builder()
    builder.stackName(getStackName().get())
        .templateBody(createTemplateBody())
        .parameters(createParameterList(optinallyFilterParameters()))
        .tags(createTags('CreatedBy'))
        .enableTerminationProtection(getEnableTerminationProtection().getOrElse(false))

    if (getCapabilities().present) {
      builder.capabilities(createCapabilityList())
    }
    // do create the stack
    CreateStackResponse response = client.createStack((CreateStackRequest)builder.build())
    println response

    WaiterResponse<DescribeStacksResponse> waiterResponse = client.waiter().waitUntilStackCreateComplete {
      it.stackName(getStackName().get())
    }
    //    def outputs = waiterResponse.matched().response().get().stacks().get(0).outputs()
    //    println outputs
  }

  void updateStack() {
    UpdateStackRequest.Builder builder = UpdateStackRequest.builder()
    builder.stackName(getStackName().get())
        .templateBody(createTemplateBody())
        .parameters(createParameterList(optinallyFilterParameters()))
        .tags(createTags('UpdatedBy'))

    if (getCapabilities().present) {
      builder.capabilities(createCapabilityList())
    }
    // do update the stack
    UpdateStackResponse response = client.updateStack((UpdateStackRequest)builder.build())
    println response

    client.waiter().waitUntilStackUpdateComplete {
      it.stackName(getStackName().get())
    }
  }

  Map<String, String> optinallyFilterParameters() {
    Map<String, String> parameters = getParameters().get()
    if (getParameterPrefix().present) {
      String prefix = getParameterPrefix().get()
      parameters = parameters.findAll {
        it.key.startsWith(prefix)
      }.collectEntries { key, value ->
        [(key.replaceAll(prefix,'')):(value)]
      }
    }
    return parameters
  }

  String createTemplateBody() {
    return new File(getTemplateFileName().get()).text
  }

  List<Tag> createTags(String createByOrUpdateByTag) {
    return [
      Tag.builder().key(createByOrUpdateByTag).value(System.getProperty('user.name')).build(),
      Tag.builder().key('TemplateName').value(getTemplateFileName().get()).build(),
      Tag.builder()
      .key('TemplateGitVersion')
      .value(Utils.getGitVersionOfFile(project.projectDir, getTemplateFileName().get())).build()
    ]
  }

  /**
   * Parameter names will be converted to PascalCase-ish since that is what Cloudformation templates expects.
   */
  Collection<Parameter> createParameterList(Map<String,String> parameterMap) {
    return parameterMap.collect { name, value ->
      Parameter.builder().parameterKey(name.capitalize()).parameterValue(value).build()
    }
  }

  Collection<Capability> createCapabilityList() {
    return getCapabilities().get().collect {
      Capability.fromValue(it)
    }
  }


  // There isn't any SDK call that can check if the stack exists or not.
  // So try get info of the stack and if that fails the assumption is that is then doesn't exists.
  boolean stackExists(String stackName) {
    try {
      DescribeStacksResponse stack = client.describeStacks {
        it.stackName(getStackName().get())
      }
      return true
    }
    catch (CloudFormationException e) {
      return false
    }
  }
}
