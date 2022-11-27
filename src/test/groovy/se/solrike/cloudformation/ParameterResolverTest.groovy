/*
 *  Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.cloudformation

import static org.assertj.core.api.Assertions.*

import org.junit.jupiter.api.Test

/**
 *
 */
class ParameterResolverTest {

  @Test
  void testResolve() {
    // given a set of unresolved parameters
    Map<String, String> parameters = [
      'env' : 'stage25',
      'slrk.deploy.env.name': '${env}',
      'slrk.deploy.s3.s3BucketName' : 'my-bucket-for-${slrk.deploy.env.name}',
      'bigEnv' : '${se.solrike.cloudformation.MyUtils.toUpper(env)}']

    // when resolving those
    Map<String, String> resolvedParameters = ParameterResolver.resolve(parameters,
        getClass().getClassLoader())

    // then the resolved parameters shall contain 'my-bucket-for-stage25' value
    assertThat(resolvedParameters).contains(entry('slrk.deploy.s3.s3BucketName','my-bucket-for-stage25'))

    // and then the value which is a script shall be evaluated
    assertThat(resolvedParameters).contains(entry('bigEnv','STAGE25'))
  }
}

class MyUtils {
  static String toUpper(String text) {
    return text.toUpperCase()
  }
}

