package se.solrike.cloudformation;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

/**
 * @author Lucas Persson
 */
public class CloudformationPlugin implements Plugin<Project> {

  private static final GradleVersion SUPPORTED_VERSION = GradleVersion.version("7.0");

  @Override
  public void apply(Project project) {
    verifyGradleVersion(GradleVersion.current());
  }

  protected void verifyGradleVersion(GradleVersion version) {
    if (version.compareTo(SUPPORTED_VERSION) < 0) {
      String message = String.format("Gradle version %s is unsupported. Please use %s or later.", version,
          SUPPORTED_VERSION);
      throw new IllegalArgumentException(message);
    }
  }

}
