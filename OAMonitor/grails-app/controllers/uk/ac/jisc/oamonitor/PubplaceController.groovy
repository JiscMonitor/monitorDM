package uk.ac.jisc.oamonitor

import grails.converters.*
import groovy.xml.MarkupBuilder
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import uk.ac.jisc.oamonitor.*


class PubplaceController {

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def index() {
    def result = [:]
    result.max = params.max ? Integer.parseInt(params.max) : 10
    result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

    def qry_params = []
    def base_qry = " from TitleInstance as o"

    if ( params.q ) {
      base_qry += " where lower(o.name) like ?"
      qry_params.add("%${params.q.toLowerCase()}%");
    }

    log.debug("base query: ${base_qry} ${qry_params}");

    result.totalHits = Org.executeQuery("select count(o) "+base_qry, qry_params )[0]
    result.hits = Org.executeQuery("select o ${base_qry}", qry_params, [max:result.max, offset:result.offset]);
    result
  }

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def show() {
    def result=[:]
    def ti_type = RefdataCategory.lookupOrCreate('Combo.Type','Appearence.TitleInstance')
    result.pubplace = TitleInstance.get(params.id)
    // result.works = Appearence.executeQuery("select a from Appearence as a where a.titleInstance = ?",[result.pubplace])
     
    result.appearances = Appearence.executeQuery("select c.fromComponent from Combo as c where c.fromComponent = :ti and c.type = :type",
                                              [ti:result.pubplace, type:ti_type])

    result
  }
}
