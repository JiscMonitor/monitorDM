package uk.ac.jisc.oamonitor

import groovy.util.logging.Log4j

@Log4j
class Person extends KBComponent {

  String fullname

  static hasMany = [
    articles: Article
  ]

  static constraints = {
    fullname    (nullable:true, blank:false, maxSize:2048)
  }

  static mapping = {
  }

  def lookupOrCreate(name, candidate_identifiers) {

    def result = lookupByIdentifier(candidate_identifiers)

    if ( result == null ) {
      result = new Person(fullname: name).save()
      candidate_identifiers.each { ci ->
        def id = Identifier.lookupOrCreateCanonicalIdentifier(ci.namespace, ci.value)
        result.ids.add(id)
      }
      result.save()
    }

    result
  }

  /**
   *  @params candidate_identifiers - A list of maps containing 2 entries: namespace and value = [ {namespace:'orcid',value:'xxx'},{namespace:'email',value:'y'} ]
   */
  def lookupByIdentifier(candidate_identifiers) {

    log.debug("lookupByIdentifier(${candidate_identifiers})");

    def result = null

    if ( candidate_identifiers.length > 0 ) {
      int ctr = 0
      // Work through candidate_identifiers and see if any match current people
      def base_query = "select p from Person as p join p.ids as ids where "
      def params = []

      candidate_identifiers.each { candidate_id ->
        if ( ( id != null ) && ( id.namespace != null ) && ( id.value != null ) ) {
          if ( ctr > 0 ) {
            base_query += ' OR'
          }
          base_query += " ( ids.value = ? AND ids.namespace.value = ? )"
          params.add(candidate_id.value, candidate_id.namespace)
          ctr++
        }
      }

      def qry_result = Person.executeQuery(base_query, params)
      if ( qry_result.size == 0 ) {
        log.debug("No matches");
      }
      else if ( qry_result.size == 1 ) {
        log.debug("Good match");
         result = qry_result[0]
      }
      else {
        def matched_person_objects = result.collect {it.id}
        throw new Exception("Set of identifiers ${candidate_identifiers} matches multiple People ${matched_person_objects}");
      }
    }

    result
  }
}
