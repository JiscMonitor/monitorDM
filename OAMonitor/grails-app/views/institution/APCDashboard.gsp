<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <title>${org.name} :: APC Dashboard</title>
  </head>
  <body>
   <div class="container">
     <div class="row">
       <div class="col-lg-12">
         <h1> ${org.name} APC Dashboard</h1>
         <div class="btn-group">
            <g:link controller="institution" action="claimFQDN" id="${params.id}" class="button btn btn-default">All</g:link>
          </div>
       </div>

       <table class="table table-striped">
         <thead>
           <tr><th colspan="9">Manuscript Title / Name of work</th></tr>
           <tr>
               <th>Published In</th>
               <th>Volume</th>
               <th>Issue</th>
               <th>Publication Date</th>
               <th>APC Status</th>
               <th>Funds</th>
               <th>APC Date</th>
               <th>Paid</th>
               <th>Currency</th>
           </tr>
         </thead>
         <tbody>
           <g:each in="${works}" var="w">
             <tr>
               <td colspan="9">
                 <g:link controller="work" action="show" id="${w.theArticle.id}">${w.theArticle.name}</g:link>
               </td>
             </tr>
             <g:each in="${w.theArticle.appearances}" var="a">
               <tr>
                 <td>&nbsp;&nbsp;--&gt;<g:link controller="pubplace" action="show" id="${a.titleInstance.id}">${a.titleInstance.name}</g:link></td>
                 <td>${a.volume?:'Unknown'}</td?
                 <td>${a.issue}</td>
                 <td></td>
                 <td></td>
                 <td></td>
                 <td></td>
                 <td></td>
                 <td></td>
                 <td></td>
               </tr>
             </g:each>
           </g:each>
         </tbody>
       </table>


     </div>
   </div>



  </body>
</html>

