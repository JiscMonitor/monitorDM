package uk.ac.jisc.oamonitor

import javax.persistence.Transient

class Fund extends KBComponent {
  
  static hasByCombo = [
    org : Org,
  ]

  /**
   *  refdataFind generic pattern needed by inplace edit taglib to provide reference data to typedowns and other UI components.
   *  objects implementing this method can be easily located and listed / selected
   */
  static def refdataFind(params) {
    def result = [];
    def ql = null;
    ql = Office.findAllByNameIlike("${params.q}%",params)

    if ( ql ) {
      ql.each { t ->
      result.add([id:"${t.class.name}:${t.id}",text:"${t.name}"])
      }
    }

    result
  }

}
