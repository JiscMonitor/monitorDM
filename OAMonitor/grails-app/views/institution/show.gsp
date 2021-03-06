<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <title>${org.name}</title>
  </head>
  <body>

   <div class="container">
     <div class="row">
       <div class="col-lg-12">
         <h1>${org.name}</h1>
       </div>
     </div>

     <div class="btn-group">
       <g:link controller="institution" action="claimFQDN" id="${params.id}" class="button btn btn-default">Claim FQDN</g:link>
       <g:link controller="institution" action="other" id="${params.id}" class="button btn btn-default">Action..</g:link>
       <g:link controller="institution" action="APCDashboard" id="${params.id}" class="button btn btn-default">APC Dashboard</g:link>
     </div>

     <div>
       <table class="table table-striped">
         <thead>
           <tr><th>Works</th></tr>
         </thead>
         <tbody>
           <g:each in="${works}" var="w">
             <tr>
               <td>( 
                    <g:if test="${w.matchedPerson.name != null}"> <g:link controller="person" action="show" id="${w.matchedPerson.id}">${w.fullname}</g:link></g:if>
                    <g:else>${w.fullname}</g:else>
                   )
                 <g:link controller="work" action="show" id="${w.theArticle.id}">${w.theArticle.name}</g:link> in <ul>
                 apps ${w.theArticle.appearances}
                 <g:each in="${w.theArticle.appearances}" var="a">
                   Published in 
                   <g:if test="${((a.volume != null) || (a.issue != null))}">volume ${a.volume?:'Unknown'} 
                        <g:if test="${a.issue != null}">issue ${a.issue}</g:if> of </g:if>
                   <g:link controller="pubplace" action="show" id="${a.titleInstance.id}">${a.titleInstance.name}</g:link> 
                   on XX (Detected on <g:formatDate date="${a.dateDetected}" format="yyyy-MM-dd"/>)
                   <span class="pull-right">--LicenseHere--</span>
                 </g:each>
                 </ul>
               </td>
             </tr>
           </g:each>
         </tbody>
       </table>

       <table class="table table-striped">
         <thead>
           <tr><th>Domains</th></tr>
         </thead>
         <tbody>
           <g:each in="${domains}" var="d">
             <tr>
               <td>${d}</td>
             </tr>
           </g:each>
         </tbody>
       </table>


     </div>

   </div>

  </body>
</html>

