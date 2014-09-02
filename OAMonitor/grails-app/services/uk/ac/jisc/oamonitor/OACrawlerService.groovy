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
class OACrawlerService {

    def titleLookupService

    def harvestDOAJ() {
      log.debug("harvestDOAJ()");
      getRecordsSince('http://staging.doaj.cottagelabs.com/query/journal,article/_search', null, doaj_processing_closure, false)
      log.debug("harvestDOAJ() exiting");
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
        // def sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
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

        def highestTimestampSeen = Crawler.withCriteria {
            projections {
                max 'latestCrawled' //most recently crawled time date
            }
        }
        highestTimestampSeen = highestTimestampSeen.first()
        def highestIdSeen = null

        log.debug("Collecting since ${highestTimestampSeen}");

        if(!debug) {


          def query = null
          if ( highestTimestampSeen == null ) {
            query="*"
          }
          else {
            query="last_updated:[\"${highestTimestampSeen.toString()}\" TO *]"
          }

          def processed = 1;
          def from = 0;
          def sz = 50;

          while( processed > 0 ) {
            log.debug("Enter DOAJ batch processing section");
            processed = 0;

            // http://staging.doaj.cottagelabs.com/query/journal,article/_search?q=last_updated:[%222014-08-07T21:30:39Z%22%20TO%20*]

            log.debug("Query for q:${query} from:${from} sz:${sz}");
            log.debug("http request.....");

            http.request(Method.GET,ContentType.JSON) { req->
              // uri.path = 
              uri.query = [
                 q:query,
                 sort:'last_updated',
                 sort:'_id',
                 from:from,
                 size:sz
              ]

              from += sz

              response.success = { resp, json ->
                log.debug("request OK ${json}")
                assert resp.statusLine.statusCode == 200

                json.hits?.hits?.each { record ->

                  log.debug("Processing record");

                  DateTime currentRecordTimestamp = new DateTime(record._source.last_updated, DateTimeZone.UTC)

                  if((highestTimestampSeen==null)||(highestTimestampSeen<currentRecordTimestamp)) {
                    highestTimestampSeen = currentRecordTimestamp
                    log.debug("update highest timestamp to ${highestTimestampSeen}");
                  }

                  if ((highestIdSeen==null)||(highestIdSeen<record._id)) {
                    highestIdSeen = record._id
                    log.debug("update highest record_id to ${highestIdSeen}");
                  }

                  KBComponent.withNewTransaction { status ->
                    closure(record)
                    processed++
                    log.debug("closure completed (processed count: ${processed}");
                  }
                }
              }

              response.failure = { resp ->
                log.error('request failed')
                assert resp.status >= 400
              }
            }

            log.debug("Procesed ${processed} records.. nonzero == get next batch");
          }
        }
        else {
          //Test env code to pass JSON example Elasticsearch files
          def json = new JsonSlurper().parseText(es_endpoint)
          json.hits.hits.each { record ->
            closure(record)
          }
        }

      log.debug("getRecordsSince exiting");
    }

    //closure passed into record processing to create a record should it not exist already, record using gpath/slurped JSON
    def doaj_processing_closure = { record ->

        // N.B. For ARTICLES - this is the title of the article, for JOURNALS this is the title of the journal
        String title         = record._source.bibjson.title
        ArrayList authors    = record._source.bibjson.author
        String journalTitle  = record._source.bibjson.journal?.title
        String publisherName = record._source.bibjson.journal?.publisher

        List identifiers = []
        record._source.bibjson.identifier.each { identifiers.add(['namespace':it.type, 'value':it.id]) }

        String type          = record._type

        TitleInstance journal

        log.debug("bibjson identifiers: ${record._source.bibjson.identifier}");
        log.debug("identifiers: ${identifiers}");

        switch (type.toLowerCase()) {
            case "journal":
                log.debug("**JOURNAL** \t"+title + "\tpub name\t"+publisherName + "\tIdentifiers\t"+identifiers)
                titleLookupService.lookup(title,publisherName,identifiers,null) //Look for journals that article appear in and link
                break

            case "article":
                log.debug("**ARTICLE** \t"+title + "\tpub name\t"+publisherName + "\tIdentifiers\t"+identifiers)

                if ( journalTitle ) {
                  // Don't use DOIs to lookup the journal (Might use a truncated form later)
                  def journal_identifiers = []
                  record._source.bibjson.identifier.each {
                    if ( ['issn','eissn','pissn'].contains( it.type ) ) {
                      journal_identifiers.add(['namespace':it.type,'value':it.id])
                    }
                  }

                  if ( journal_identifiers.size() > 0 ) {
                    journal = titleLookupService.lookup(journalTitle,publisherName,journal_identifiers,null) 
                  }
                }

                Article article = new Article(name: title)
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
                    def person = Person.lookupByIdentifierSet([ [namespace:'email',value:author.email], [namespace:'orcid',value:author.orcid] ])

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
                article.save(failOnError: true, flush:true)
                break

            default:
                throw new RuntimeException("Problem with current record:\t"+record) //shouldn't happen, but is there "just in case"
                break
        }
    }


}
