package se.solrike.cloudformation

import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task to print the resolved parameters for an environment.
 * <p>
 * The task will first resolve parameters using Groovy's evaluation support.
 *
 * @author Lucas Persson
 */
abstract class PrintEnviromentParametersTask extends DefaultTask {


  /**
   * The parameters.
   */
  @Input
  public abstract MapProperty<String, String> getParameters()

  /**
   * The build script's class loader so that this task can find e.g. classes defined in the build.gradle.
   */
  @Input
  @Optional
  public abstract Property<ClassLoader> getParentClassLoader()


  @TaskAction
  void execute() {
    Map<String, String> resolvedParameters = ParameterResolver.resolve(getParameters().get(),
        getParentClassLoader().getOrElse(getClass().getClassLoader()))

    resolvedParameters.sort { it.key }.each {key, value ->
      println "$key : $value"
    }
  }
}
