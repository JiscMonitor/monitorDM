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
         <h1>Claim FQDN : ${org.name}</h1>

         <div class="well">
           <g:form controller="institution" action="claimFQDN" id="${params.id}" method="get">
             Name: <input type="text" name="q" value="${params.q}"/>
             <input type="submit"/>
           </g:form>
         </div>

         total:: ${totalHits}

         <table>
           <thead>
           </thead>
           <tbody>
             <g:each in="${hits}" var="h">
               <tr>
                 <td>${h.fqdn}</td>
                 <td>${h.institution?.name}</td>
                 <td><g:link action="claimFQDN" params="${params+[dn:h.id]}" class="btn btn-default">CLAIM</g:link></td>
               </tr>
             </g:each>
           </tbody>
         </table>

       </div>
     </div>


   </div>

  </body>
</html>

