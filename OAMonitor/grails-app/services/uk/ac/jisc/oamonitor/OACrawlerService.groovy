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

@Transactional 
@Log4j
class OACrawlerService {

    def titleLookupService

    def harvestDOAJ() {
      log.debug("harvestDOAJ()");
      getRecordsSince('http://staging.doaj.cottagelabs.com/query/journal,article/_search', null, doaj_processing_closure, false)
    }

    /**
     * Crawler through Elasticsearch records
     * @param es_endpoint The Elasticsearch node to search through, must be valid Http URL
     * @param from_timestamp //Can be null, will auto-search using Crawler Domain for saved time instances
     * @param closure //This an be for additional processing of each record
     * @param debug //<code>true or false</code> for additional or removed functionality that is not needed should the case be
     * @throws RuntimeException invalid URLS, un-encountered logic i.e grey areas
     * @return
     */
    def getRecordsSince(es_endpoint, from_timestamp, closure, debug) {
        log.debug("getRecordsSince(${es_endpoint},${from_timestamp},closure,${debug})");
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

        log.debug("Collecting since ${latestDate}");

        if(!debug) {
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
        else {
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
        String journalTitle  = record._source.bibjson.journal?.title
        String publisherName = record._source.bibjson.journal?.publisher

        List identifiers = []
        record._source.bibjson.identifier.each { identifiers.add(['namespace':it.type, 'value':it.id]) }

        String type          = record._type

        TitleInstance journal

        switch (type.toLowerCase())
        {
            case "journal":
                titleLookupService.lookup(journalTitle,publisherName,identifiers,null) //Look for journals that article appear in and link
                break

            case "article":
                println("**Jornal** \t"+journalTitle + "\tpub name\t"+publisherName + "\tIndentifiers\t"+identifiers)
                if ( journalTitle ) {
                  // Don't use DOIs to lookup the journal (Might use a truncated form later)
                  def journal_identifiers = []
                  ['issn','eissn','pissn'].each { tp ->
                    if ( record._source.bibjson.identifier[tp] != null ) {
                      journal_identifiers.add('namespace':tp,'value':record._source.bibjson.identifier[tp])
                    }
                  }
                  if ( journal_identifiers.size() > 0 ) {
                    journal = titleLookupService.lookup(journalTitle,publisherName,journal_identifiers,null) 
                  }
                }

                Article article = new Article(title: title, continuingSeries: null, provenance: null, reference:null)
                if (!article.save()) {
                    article.errors.each {
                        log.error(it)
                    }
                }

                if (journal && article)
                   Appearence.create(article, journal) //add to link table i.e. m:n

                Set<Person> authorsList = new TreeSet<Person>() //returned or created person objects i.e. authors
                Set eliminatingORCIDs   = new TreeSet() // Set of potential lists to be flattened to elimiate

                authors.each {author ->

                    // See if we can identify a person based on identifiers
                    // II: The only instance of an orcid we have is in a record where it's embedded in the email as in
                    //     email: "ORCID: 0000-0001-5907-2795 stet@ukr.net" - We probably need to parse this somehow
                    def person = Person.lookupByIdentifierSet([
                                                             [namespace:'email',value:author.email],
                                                             [namespace:'orcid',value:author.orcid]
                                                           ])

                    def article_name = new AuthorName(
                                                      theArticle:article,
                                                      matchedPerson:person, 
                                                      fullname:author.name).save()
                    // def (currentAuthor, potentialORCIDs) = Person.createOrLookupAuthor(author.name, identifier)
                    // log.debug(article)
                    // log.debug("Current Author\t"+currentAuthor+"\tPotential ORCIDs :"+potentialORCIDs)

                    // if (!potentialORCIDs)
                    //     eliminatingORCIDs.addAll(potentialORCIDs)
                    // authorsList.add(currentAuthor) //lookup or create author
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
