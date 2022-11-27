package se.solrike.cloudformation

import javax.annotation.Nullable

import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable

/**
 * Utilities for AWS SSM (Simple system management) like Parameter Store
 */
public class SsmUtils {

  public static void uploadParameters(Map<String, String> parameters, String excludeFilter,
      @Nullable String parameterPrefix = null) {
    SsmClient client = SsmClient.builder().build()

    parameters.findAll { !it.key.startsWith(excludeFilter) && it.value }.each { name, value ->
      client.putParameter {
        it.name(parameterPrefix ? "/$parameterPrefix/$name" : "/$name")
        it.value(value)
        it.overwrite(true)
        it.type(encryptParameter(name) ? ParameterType.SECURE_STRING : ParameterType.STRING)
      }
    }
  }

  public static void printParameters(String parameterPrefix) {
    SsmClient client = SsmClient.builder().build()
    List<String> parameters = []

    GetParametersByPathIterable iterable = client.getParametersByPathPaginator {
      it.path('/' + parameterPrefix)
      it.withDecryption(true)
    }
    iterable.forEach {
      it.parameters.each { parameters.add("$it.name : $it.value") }
    }
    parameters.sort().each { println it}
  }


  static boolean encryptParameter(String name) {
    return (name.endsWithIgnoreCase('password') || name.endsWithIgnoreCase('key'))
  }
}
