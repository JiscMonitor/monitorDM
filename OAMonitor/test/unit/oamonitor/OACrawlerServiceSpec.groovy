package oamonitor

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(OACrawlerService)
class OACrawlerServiceSpec extends Specification {


    void setUp(){
    }

    void testSomethingBad(){
        OACrawlerService.getRecordsSince()
    }

    void testSomethingGood(){
        OACrawlerService.getRecordsSince()
    }

    void "test something"() {
      URL mock_es_response_1 = getClass().getResource('mock_es_search_response_1.json')
    }
}
