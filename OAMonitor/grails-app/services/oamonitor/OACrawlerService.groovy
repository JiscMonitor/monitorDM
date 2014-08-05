package oamonitor

import grails.converters.JSON
import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import uk.ac.jisc.oamonitor.Identifier
import uk.ac.jisc.oamonitor.IdentifierNamespace
import uk.ac.jisc.oamonitor.TitleInstance


//http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-uri-request.html

@Transactional
class OACrawlerService {

    def getRecordsSince(es_endpoint, from_timestamp, closure)
    {
        def http
        if (es_endpoint==null || es_endpoint.isEmpty())
           throw new RuntimeException("Elasticsearch URL not passed "+es_endpoint)
        else
            http = new HTTPBuilder(es_endpoint)

        Date latest;
        http.request(GET,JSON) { req, json->

            json.responseData.results.each {
                presentDate = new Date(it.date)
                if(latest==null)
                    latest = presentDate
                else if (latest < new Date(it.date))
                    latest = new Date(it.date)

                closure(it)
            }
        }
    }

    //closure passed into record processing to create a record should it not exist already
    def doaj_processing_closure = { record ->
        def title = record.title
        def issn = record.issn
        Identifier identifier = lookupOrCreateCanonicalIdentifier(issn, title)
        if (!identifier)
        {
            new Identifier(new IdentifierNamespace(issn), title)
        }
    }


}
