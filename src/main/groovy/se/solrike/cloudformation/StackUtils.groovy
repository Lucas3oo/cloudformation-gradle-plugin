package se.solrike.cloudformation


import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse
import software.amazon.awssdk.services.cloudformation.model.Output

/**
 * Utilities for interact with AWS. E.g. Cloudformation stacks
 */
public class StackUtils {

  /**
   *  Resolve a AWS Cloudformation stack logical resource ID to a physical resource ID
   *
   *  @param stackName - stackname , e.g. 'my-lambda-stack-for-production'
   *  @param logicalResourceId - logical resource ID in the stack, e.g. 'ServerlessFunctionArn'
   *  @return physical resource ID, e.g. 'arn:aws:lambda:eu-north-1:12345:function:slrk-prod-books-api'
   */
  public static String resolveStackResource(String stackName, String logicalResourceId) {
    CloudFormationClient client = CloudFormationClient.builder().build()

    DescribeStackResourceResponse response = client.describeStackResource {
      it.stackName(stackName)
      it.logicalResourceId(logicalResourceId)
    }
    return response.stackResourceDetail.physicalResourceId
  }

  /**
   * Resolve an output of a stack using the output's key.
   *
   * @param stackName - stackname , e.g. 'my-lambda-stack-for-production'
   * @param key - e.g. 'LogGroupName'
   * @return the value for the key, e.g. '/aws/lambda/slrk-prod-books-api'
   *
   * @throws CloudFormationException - e.g. if the stack doesn't exists.
   * @throws IllegalArgumentException - if the key doesn't exists.
   */
  public static String resolveStackOutput(String stackName, String key) {
    CloudFormationClient client = CloudFormationClient.builder().build()

    DescribeStacksResponse response = client.describeStacks {
      it.stackName(stackName)
    }
    Output output = response.stacks().get(0).outputs().find { it.outputKey == key }
    if (output) {
      return output.outputValue
    }
    else {
      throw new IllegalArgumentException("Cloud not find output with key '$key'.")
    }
  }
}
