class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/institution" ( controller:'institution', action:'index' )
        "/institution/$id" ( controller:'institution', action:'show' )
        "/institution/$id/claimFQDN" ( controller:'institution', action:'claimFQDN' )

        "/"(controller:'welcome', action:'index');

        "500"(view:'/error')
	}
}
