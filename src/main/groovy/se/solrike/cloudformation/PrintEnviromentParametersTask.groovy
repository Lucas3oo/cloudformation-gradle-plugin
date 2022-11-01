package se.solrike.cloudformation

import org.gradle.api.DefaultTask
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
import software.amazon.awssdk.services.cloudformation.model.DeleteStackResponse
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse
import software.amazon.awssdk.services.cloudformation.model.Parameter
import software.amazon.awssdk.services.cloudformation.model.Tag
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse

/**
 * Task to print the resolved parameters for an environment.
 * <p>
 * The task will first resolve parameters using Groovy's evaluation support.
 *
 * @author Lucas Persson
 */
abstract class PrintEnviromentParametersTask extends DefaultTask {

  /**
   * The build script's class loader so that this task can find e.g. classes defined in the build.gradle.
   */
  @Input
  @Optional
  public abstract Property<ClassLoader> getParentClassLoader()

  /**
   * The parameters.
   */
  @Input
  public abstract MapProperty<String, String> getParameters()



  @TaskAction
  void execute() {
    Map resolvedParameters = ParameterResolver.resolve(getParameters().get(),
        getParentClassLoader().getOrElse(getClass().getClassLoader()))

    resolvedParameters.sort { it.key }.each {key, value ->
      println "$key : $value"
    }
  }
}
