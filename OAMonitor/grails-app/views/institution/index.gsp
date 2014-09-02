<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <title>JISC OA Monitor</title>
  </head>
  <body>

   <div class="container">
     <div class="row">
       <div class="col-lg-12">
         <h1>Institution Search</h1>
       </div>
     </div>
     <div class="row">
       <div class="col-lg-12">
         <div class="well">
           <g:form method="get">
             Name: <input type="text" name="q" value="${params.q}"/>
             <input type="submit"/>
           </g:form>
         </div>

         <g:if test="${hits}" >
           <div class="paginateButtons" style="text-align:center">
             <g:if test="${(params.offset != null)}">
               Showing Results ${params.int('offset') + 1} - ${(totalHits < (max + offset)) ? totalHits : max + offset} of ${totalHits}
             </g:if>
             <g:elseif test="${totalHits && totalHits > 0}">
               Showing Results 1 - ${totalHits < max ? totalHits : max} of ${totalHits}
             </g:elseif>
             <g:else>
               Showing ${totalHits} Results
             </g:else>
             <br/>
             <span><g:paginate controller="institution" action="index" params="${params}" next="Next" prev="Prev" total="${totalHits}" /></span>

           </div>


           <table class="table table-striped">
             <thead> 
               <tr>
                 <th>Org Name</th>
               </tr>
             </thead>
             <tbody>
               <g:each in="${hits}" var="o">
                 <tr>
                   <td><g:link controller="institution" action="show" id="${o.id}">${o.name}</g:link></td>
                 </tr>
               </g:each>
             </tbody>
           </table>
         </g:if>

       </div>
     </div>
   </div>

  </body>
</html>

