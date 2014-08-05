package oamonitor

import grails.converters.JSON
import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder


//http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-uri-request.html

@Transactional
class OACrawlerService {

    def getRecordsSince(es_endpoint, from_timestamp, closure)
    {
        boolean firstRun = false
        def http
        if (es_endpoint==null || es_endpoint.isEmpty())
           return "Elasticsearch URL not passed"
        else
            http = new HTTPBuilder(es_endpoint)

        Date latest;
        if (from_timestamp)
            firstRun = true

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

    def doaj_processing_closure = { record ->
        def title = record.title
        def issn = record.issn

        //if(!title exists in db using issn) // found - Great Not found - Create
    }


}
