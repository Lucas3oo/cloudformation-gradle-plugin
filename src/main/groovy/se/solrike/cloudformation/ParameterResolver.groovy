package se.solrike.cloudformation

/**
 * @author Lucas Persson
 */
public class ParameterResolver {

  public static Map<String, String> resolve(Map<String, String> parameters, ClassLoader parentClassLoader) {
    Binding binding = new Binding()

    Properties p = new Properties()
    p.putAll(parameters)
    // ConfigSlurper will create the properties as beans in case a dot notation is used
    ConfigObject conf = new ConfigSlurper().parse(p)
    conf.each { key, value ->
      binding.setVariable(key, value)
    }
    GroovyShell shell = new GroovyShell(parentClassLoader, binding)

    // multipass so nested references (in two hops) can be used.
    resolveValues(shell, binding)
    resolveValues(shell, binding)

    Map flatMap = [:]
    flatternMap(binding.getVariables(), flatMap, null)

    return flatMap
  }

  static void resolveValues(GroovyShell shell, Binding binding) {
    visitMap(binding.getVariables(), {
      it.value = shell.evaluate("\"$it.value\"")
    })
  }

  static void visitMap(Map map, Closure action) {
    map.each {
      if (it.value instanceof Map) {
        visitMap(it.value, action)
      }
      else {
        action.call(it)
      }
    }
  }

  // flattern the map and re-create the dot notation or the keys
  static void flatternMap(Map source, Map target, String prefix) {
    source.each {
      String key = prefix ? prefix + '.' + it.key : it.key
      if (it.value instanceof Map) {
        flatternMap(it.value, target, key)
      }
      else {
        target[key] = it.value
      }
    }
  }

}
