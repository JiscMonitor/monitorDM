package uk.ac.jisc.oamonitor

import grails.converters.*
import groovy.xml.MarkupBuilder
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.commons.GrailsClassUtils


class AdminController {

  def executorService
  def OACrawlerService

  def index() { 
  }

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def triggerHarvest() {
    log.debug("triggerHarvest()");
    def future = executorService.submit({
      log.debug("doHarvest() about to call...");
      doHarvest()
      log.debug("doHarvest() completed...");
    } as java.util.concurrent.Callable)
    redirect(controller:'home',action:'index')
  }


  private void doHarvest() {
    try {
      log.debug("doHarvest()");
      OACrawlerService.harvestDOAJ()
      log.debug("Redirect to home--action");
    }
    catch ( Exception e ) {
      log.error("Problem",e);
    }
  }
}
