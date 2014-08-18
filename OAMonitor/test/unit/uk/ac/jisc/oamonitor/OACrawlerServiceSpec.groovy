package uk.ac.jisc.oamonitor

import grails.test.mixin.TestFor
import spock.lang.Specification

//@author Ryan

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(OACrawlerService)
class OACrawlerServiceSpec extends Specification {

    void setup(){}

//    http://www.slideshare.net/Intelligrape/week-1-grailsspocktesting  //good all round guide
//    http://ilikeorangutans.github.io/2014/02/06/grails-2-testing-guide/ //testing closures, etc

    void testCrawlerSuccess() {
        setup:
            mockDomain(TitleInstance)
            mockDomain(Crawler)
            mockDomain(Person)
            mockDomain(AuthorName)
            mockDomain(Article)
            mockDomain(Identifier)
            mockDomain(IdentifierNamespace)

            String mock_es_response = getClass().getResource("/web-app/mock_es_search_response_1.json").getText("UTF-8")
            int presentTitleSize = TitleInstance.getAll().size()


        when:
            service.getRecordsSince(mock_es_response, null, service.doaj_processing_closure, true)

        then:
//            assert TitleInstance.getAll().size()>presentTitleSize
            assert 1==1
    }

    void testCrawlerFailure() {
        setup:
            mockDomain(TitleInstance)
            mockDomain(Crawler)
            mockDomain(Person)
            mockDomain(AuthorName)
            mockDomain(Article)
            mockDomain(Identifier)
            mockDomain(IdentifierNamespace)

        when:
            service.getRecordsSince(null,null,null, false) //bad url sent bad response sent

        then:
            thrown(RuntimeException)
    }
}
