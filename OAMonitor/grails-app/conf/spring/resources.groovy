// Place your Spring DSL code here

import grails.plugin.springsecurity.userdetails.GormUserDetailsService
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper

beans = {

  userDetailsService(GormUserDetailsService) {
    grailsApplication = ref('grailsApplication')
  }

  userDetailsByNameServiceWrapper(UserDetailsByNameServiceWrapper) {
    userDetailsService = ref('userDetailsService')
  }

  preAuthenticatedAuthenticationProvider(org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider) {
    preAuthenticatedUserDetailsService = ref('userDetailsByNameServiceWrapper')
  }

  securityContextPersistenceFilter(org.springframework.security.web.context.SecurityContextPersistenceFilter){
  }
}

