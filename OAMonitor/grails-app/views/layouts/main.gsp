<!DOCTYPE html>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js"><!--<![endif]-->
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title><g:layoutTitle default="Monitor DM"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <script language="JavaScript">
      var monitorBaseUrl="<g:createLink controller='ajaxSupport' action='lookup'/>";
    </script>

    <asset:javascript src="application.js"/>
    <asset:stylesheet href="main.css"/>
    <asset:link rel="shortcut icon" href="favicon.ico" type="image/x-icon"/>

    <g:layoutHead/>
  </head>

  <body>

    <div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <g:link controller="home" action="index" class="navbar-brand">OA Monitor</g:link>
        </div>
        <div class="collapse navbar-collapse">
          <ul class="nav navbar-nav">
            <sec:ifLoggedIn>
              <sec:ifAnyGranted roles="ROLE_ADMIN">
                <li class="dropdown">
                  <a href="#" class="dropdown-toggle" data-toggle="dropdown">System Admin<b class="caret"></b></a>
                  <ul class="dropdown-menu">
                    <li><g:link controller="admin" action="triggerHarvest">Trigger Harvest</g:link></li>
                  </ul>
                </li>
              </sec:ifAnyGranted>

              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Monitor<b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li><g:link controller="institution" action="index">Institution Search</g:link></li>
                  <li><g:link controller="person" action="index">Person Search</g:link></li>
                  <li><g:link controller="pubplace" action="index">Publication Place Search</g:link></li>
                </ul>
              </li>
              <li><a href="#contact">Contact</a></li>
            </sec:ifLoggedIn>
          </ul>
          <ul class="nav navbar-nav pull-right">
            <sec:ifLoggedIn>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">${applicationContext.springSecurityService.currentUser?.display}<b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li><g:link controller="api">Api</g:link></li>
                  <li><g:link controller="profile">Profile</g:link></li>
                  <li><g:link controller="logout">Logout</g:link></li>
                </ul>
              </li>
            </sec:ifLoggedIn>
            <sec:ifNotLoggedIn>
              <li><g:link controller="login">Login</g:link></li>
              <li><g:link controller="register">Register</g:link></li>
            </sec:ifNotLoggedIn>
          </ul>
        </div><!--/.nav-collapse -->
      </div>
    </div>

    <g:layoutBody/>

    <g:if test="${grailsApplication.config.analytics?.code!=null}">
      <g:javascript>
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)})(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        ga('create', '${grailsApplication.config.analytics.code}', '${grailsApplication.config.analytics.host}');
        ga('send', 'pageview');
      </g:javascript>
    </g:if>

    </body>
</html>
