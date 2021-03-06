package uk.ac.jisc.oamonitor

import javax.persistence.Transient

class Org extends KBComponent {

  RefdataValue mission

  static manyByCombo = [
    children    : Org,
  ]

  static hasByCombo = [
    parent          :  Org,
    'previous'         :  Org,
    successor         :  Org
  ]

  static mappedByCombo = [
    children        : 'parent',
  ]

  //  static mappedBy = [
  //    ids: 'component',
  //  ]

  static hasMany = [
    roles: RefdataValue,
  ]

  static mapping = {
    //         id column:'org_id'
    //    version column:'org_version'
    mission column:'org_mission_fk_rv'
  }

  static constraints = {
    mission(nullable:true, blank:true)
  }

  //  @Transient
  //  def getPermissableCombos() {
  //  [
  //  ]
  //  }

  static def refdataFind(params) {
    def result = [];
    def ql = null;
    ql = Org.findAllByNameIlike("${params.q}%",params)

    if ( ql ) {
      ql.each { t ->
        result.add([id:"${t.class.name}:${t.id}",text:"${t.name}"])
      }
    }

    result
  }

  static Org lookupUsingComponentIdOrAlternate(ids) {
    def located_org = null

    switch (ids) {

      case List :

      // Assume [identifierType : "", identifierValue : "" ] format.
      // See if we can locate the item using any of the custom identifiers.
        ids.each { ci ->

          // We've already located an org for this identifier, the new identifier should be new (And therefore added to this org) or
          // resolve to this org. If it resolves to some other org, then there is a conflict and we fail!
          located_org = lookupByIO(ci.identifierType,ci.identifierValue)
          if (located_org) return located_org
        }
        break
      case Identifier :
        located_org = lookupByIO(
        ids.ns.ns,
        ids.value
        )
        break
    }
    located_org
  }

  @Override
  public String getNiceName() {
    return "Organization";
  }
  
  @Transient
  static def oaiConfig = [
    id:'orgs',
    textDescription:'Organization repository for GOKb',
    query:" from Org as o where o.status.value != 'Deleted'"
  ]

  /**
   *  Render this package as OAI_dc
   */
  @Transient
  def toOaiDcXml(builder, attr) {
    builder.'dc'(attr) {
      'dc:title' (name)
    }
  }

  /**
   *  Render this package as GoKBXML
   */
  @Transient
  def toGoKBXml(builder, attr) {
    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    def publishes = getPublishedTitles()
    def issues = getIssuedTitles()
    def provides = getProvidedPackages()
    def identifiers = getIds()
    
    builder.'gokb' (attr) {
      builder.'org' (['id':(id)]) {
        builder.'name' (name)
        if (identifiers) {
          builder.'identifiers' {
            identifiers.each { tid ->
              builder.'identifier' (['namespace':tid.namespace.value], tid.value) 
            }
          }
        }
        
        if (publishes) {
          'publishedTitles' {
            publishes.each { title ->
              builder.'title' (['id':title.id]) {
                builder.'name' (title.name)
                builder.'identifiers' {
                  title.ids?.each { tid ->
                    builder.'identifier' (['namespace':tid.namespace.value], tid.value)
                  }
                }
              }
            }
          }
        }
        
        if (issues) {
          'issuedTitles' {
            issues.each { title ->
              builder.'title' (['id':title.id]) {
                builder.'name' (title.name)
                builder.'identifiers' {
                  title.ids?.each { tid ->
                    builder.'identifier' (['namespace':tid.namespace.value], tid.value)
                  }
                }
              }
            }
          }
        }
        
        if (provides) {
          'providedPackages' {
            provides.each { pkg ->
              builder.'package' (['id':pkg.id]) {
                builder.'name' (pkg.name)
                builder.'identifiers' {
                  pkg.ids?.each { tid ->
                    builder.'identifier' (['namespace':tid.namespace.value], tid.value)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
