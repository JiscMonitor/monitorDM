package uk.ac.jisc.oamonitor

import grails.converters.*
import groovy.xml.MarkupBuilder
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import uk.ac.jisc.oamonitor.*


class InstitutionController {

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def index() { 
    def result = [:]
    result.max = params.max ? Integer.parseInt(params.max) : 10
    result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

    def qry_params = []
    def base_qry = " from Org as o"

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
    result.org = Org.get(params.id)
    result.domains = DomainName.findAllByInstitution(result.org)
    result.works = AuthorName.executeQuery("select a from AuthorName as a where a.institution = ? or a.domainName.institution = ?",[result.org,result.org])
    result
  }

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def claimFQDN() {
    def result=[:]
    result.org = Org.get(params.id)
    
    if ( params.dn != null ) {
      def domainname = DomainName.get(params.dn);
      if ( ( domainname ) && ( domainname.institution == null ) ) {
        domainname.institution = result.org
        domainname.save(flush:true)
      }
    }

    result.org = Org.get(params.id)
    result.hits = DomainName.executeQuery("select d from DomainName as d where d.fqdn like ?",["%${params.q}%"])
    result.totalHits = DomainName.executeQuery("select count(d) from DomainName as d where d.fqdn like ?",["%${params.q}%"])[0]
    result
  }
}
