package se.solrike.cloudformation

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
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
 * Task to delete AWS Cloudformation stack.
 * <p>
 * The task will use the default credentials and region lookup chain.
 *
 * @author Lucas Persson
 */
abstract class DeleteStackTask extends DefaultTask {

  /**
   * Stack name.
   */
  @Input
  public abstract Property<String> getStackName()

  @Internal
  CloudFormationClient client


  @TaskAction
  void execute() {
    client = CloudFormationClient.builder().build()
    deleteStack()
    println "Stack '${getStackName().get()}' is deleted."
  }

  void deleteStack() {
    DeleteStackResponse response = client.deleteStack {
      it.stackName(getStackName().get())
    }
    println response

    WaiterResponse<DescribeStacksResponse> waiterResponse = client.waiter().waitUntilStackDeleteComplete {
      it.stackName(getStackName().get())
    }
  }
}
