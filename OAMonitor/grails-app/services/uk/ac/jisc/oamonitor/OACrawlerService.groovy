package uk.ac.jisc.oamonitor

import grails.transaction.Transactional
import groovy.util.logging.Log4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method


//http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-uri-request.html

@Transactional @Log4j
class OACrawlerService {

    def titleLookupService

    def getRecordsSince(es_endpoint, from_timestamp, closure)
    {
        println("inside getRecordsSince")
        def http
        if (es_endpoint==null || es_endpoint.isEmpty())
           throw new RuntimeException("Elasticsearch URL not passed "+es_endpoint)
        else
            http = new HTTPBuilder(es_endpoint)

        Date latest;
        http.request(Method.GET,ContentType.JSON) { req, json->
            if (req.status==200)
            {
                println "OK"
                println json
                json.responseData.results.hits.each {
                    def presentDate = new Date(it.date)
                    if(latest==null)
                        latest = presentDate
                    else if (latest < new Date(it._source.last_updated))
                        latest = new Date(it._source.last_updated)

                    closure(it)
                }
            } else
                println "Bad request"
        }
    }

    //closure passed into record processing to create a record should it not exist already
    def doaj_processing_closure = { record ->
        String title         = record._source.bibjson.title
        Map authors          = record._source.author
        String journalTitle  = record._source.journal.ti
        String publisherName = record._source.journal.publisher
        Map identifier       = record._source.identifier
        String type          = record._source._type
        //def identifier = Identifier.lookupOrCreateCanonicalIdentifier(issn, title) //could return null

        TitleInstance titleInstance
        switch (type.toLowerCase())
        {
            case "journal":
                titleLookupService.find(title,publisherName,identifier,null) //Look for journals that article appear in and link
                break
            case "article":
                titleInstance  = titleLookupService.find(title,publisherName,identifier,null) //Look for journals that article appear in and link
                Article article              = new Article(title: title)

                if (titleInstance)
                    Appearence.create(article, titleInstance) //add to link table i.e. m:n

                Set<Person> authorsList      = new TreeSet<Person>() //returned or created person objects i.e. authors
                Set eliminatingORCIDs        = new TreeSet() // Set of potential lists to be flattened to elimiate

                authors.each {key, value ->
                //Person currentAuthor = Person.createOrLookupAuthor(value, identifier, titleInstance, title)
                    def (currentAuthor, potentialORCIDs) = Person.createOrLookupAuthor(value, identifier, eliminatingORCIDs)
                    currentAuthor.addToArticles(article)
                    if (!potentialORCIDs)
                        eliminatingORCIDs.addAll(potentialORCIDs)
                    authorsList.add(currentAuthor) //lookup or create author
                }
                if (authorsList.size() > 0)
                    article.setAuthors(authorsList) ////link author to article
                else
                {
                    throw new RuntimeException("Problem with authors, which have been created from JSON :\t"+authors + "\tSet Contents: "+authorsList)
                    log.debug("Problem with authors, which have been created from JSON :\t"+authors + "\tSet Contents: "+authorsList)
                }
                if (!eliminatingORCIDs.isEmpty())
                {
                    Set<Person> removeSet = new TreeSet<Person>()
                    eliminatingORCIDs.each { id->
                        Person authorToRemove = Person.findByOrcids(new Identifier(namespace: "orcid", value: id))
                        if (!authorToRemove)
                        {
                            removeSet.add(authorToRemove)
                            eliminatingORCIDs.remove(id)
                        }
                    }
                    if (!removeSet.isEmpty())
                    {
                        authorsList.removeAll(removeSet)
                        if (authorsList.size()==1 && eliminatingORCIDs.size()==1) // we can assume this person should have the remaining orcid
                            authorsList.first().addToOrcids(new Identifier(namespace: "orcid", value: eliminatingORCIDs.first()))
                        else if (authorsList>1 && eliminatingORCIDs.size()==1)
                        {
                            //to think about
                        }
                        else if (authorsList>1 && eliminatingORCIDs.size()>1)
                        {
                            //to think about
                        }
                    }

                }
                article.save(failOnError: true)
                break
            default:
                throw new RuntimeException("Problem with current record:\t"+record) //shouldn't happen, but is there "just in case"
                break
        }

    }


}
