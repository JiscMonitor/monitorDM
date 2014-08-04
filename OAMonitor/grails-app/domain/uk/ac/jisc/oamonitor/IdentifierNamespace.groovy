package uk.ac.jisc.oamonitor

import uk.ac.jisc.oamonitor.ClassUtils

class IdentifierNamespace {

  String value

  static mapping = {
    value column:'idns_value'
  }

  static constraints = {
    value (nullable:true, blank:false)
  }

  @Override
  public boolean equals(Object obj) {
    if (obj != null) {

      def dep = ClassUtils.deproxy(obj)
      if (dep instanceof IdentifierNamespace) {
        // Check the value attributes.
        return (this.value == dep.value)
      }
    }
    return false
  }
}
