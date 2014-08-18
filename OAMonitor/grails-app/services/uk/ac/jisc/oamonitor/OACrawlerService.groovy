package uk.ac.jisc.oamonitor

import grails.plugin.jodatime.binding.DateTimeConverter
import grails.transaction.Transactional
import grails.util.Environment
import groovy.json.JsonSlurper
import groovy.util.logging.Log4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import java.text.SimpleDateFormat


//http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-uri-request.html

@Transactional @Log4j
class OACrawlerService {

    def titleLookupService

    /**
     * Crawler through Elasticsearch records
     * @param es_endpoint The Elasticsearch node to search through, must be valid Http URL
     * @param from_timestamp //Can be null, will auto-search using Crawler Domain for saved time instances
     * @param closure //This an be for additional processing of each record
     * @param debug //<code>true or false</code> for additional or removed functionality that is not needed should the case be
     * @throws RuntimeException invalid URLS, un-encountered logic i.e grey areas
     * @return
     */
    def getRecordsSince(es_endpoint, from_timestamp, closure, debug)
    {
        def http
        if (es_endpoint) {
            try {
                http = new HTTPBuilder(es_endpoint)
            } catch(URISyntaxException e) {
                if (!debug)
                    throw new RuntimeException("Elasticsearch URL not passed "+es_endpoint)
            }
        }
        else
            throw new RuntimeException("Elasticsearch URL not passed "+es_endpoint)


        def latestDate = Crawler.withCriteria {
            projections {
                max 'latestCrawled' //most recently crawled time date
            }
        }
        latestDate = latestDate.first()

        if(!debug)
        {
            http.request(Method.GET,ContentType.JSON) { req->
                response.success = { resp, json ->
                    println 'request OK'
                    assert resp.statusLine.statusCode == 200

                    json.hits.hits.each { record ->
                        DateTime presentDate = new DateTime(record._source.last_updated, DateTimeZone.UTC)
                        if(latestDate==null)
                            latestDate = presentDate
                        else if (latestDate < presentDate)
                            latestDate = presentDate  //keep a track of latest date found

                        closure(record)
                    }
                }

                response.failure = { resp ->
                    println 'request failed'
                    assert resp.status >= 400
                }
            }
        }
        else
        {
            //Test env code to pass JSON example Elasticsearch files
            def json = new JsonSlurper().parseText(es_endpoint)
            json.hits.hits.each { record ->
                closure(record)
            }
        }
    }

    //closure passed into record processing to create a record should it not exist already, record using gpath/slurped JSON
    def doaj_processing_closure = { record ->
        String title         = record._source.bibjson.title
        ArrayList authors    = record._source.bibjson.author
        String journalTitle  = record._source.bibjson.journal.title
        String publisherName = record._source.bibjson.journal.publisher
        Map identifier = new HashMap() //add identifiers to a map for further processing
        record._source.bibjson.identifier.each {identifier.put(it.type, it.id)}
        String type          = record._type
        //def identifier = Identifier.lookupOrCreateCanonicalIdentifier(issn, title) //could return null

        TitleInstance titleInstance
        switch (type.toLowerCase())
        {
            case "journal":
                titleLookupService.find(journalTitle,publisherName,identifier,null) //Look for journals that article appear in and link
                break
            case "article":
                println("Jornal \t"+journalTitle + "\tpub name\t"+publisherName + "\tIndentifiers\t"+identifier)
//                titleInstance           = titleLookupService.find(journalTitle,publisherName,identifier) //Look for journals that article appear in and link

                Article article         = new Article(title: title, continuingSeries: null, provenance: null, reference:null)
                if (!article.save()) {
                    article.errors.each {
                        println(it) //todo Sort these strange validation errors on save about the inherited properties: "reference", "provenance", and "continuingSeries"
                    }
                }
                if (titleInstance && article)
                    Appearence.create(article, titleInstance) //add to link table i.e. m:n

                Set<Person> authorsList = new TreeSet<Person>() //returned or created person objects i.e. authors
                Set eliminatingORCIDs   = new TreeSet() // Set of potential lists to be flattened to elimiate

                authors.each {author ->
                    def (currentAuthor, potentialORCIDs) = Person.createOrLookupAuthor(author.name, identifier)
                    println(article)
                    println("Current Author\t"+currentAuthor+"\tPotential ORCIDs :"+potentialORCIDs)
//                    currentAuthor.addToArticles(article) //commented out temp to debug other issues

                    if (!potentialORCIDs)
                        eliminatingORCIDs.addAll(potentialORCIDs)
                    authorsList.add(currentAuthor) //lookup or create author
                }

                if (authorsList.size() > 0)
                    article.setAuthors(authorsList) ////link author to article
                else
                {
                    log.debug("Problem with authors, which have been created from JSON :\t"+authors + "\tSet Contents: "+authorsList)
                    throw new RuntimeException("Problem with authors, which have been created from JSON :\t"+authors + "\tSet Contents: "+authorsList)
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
