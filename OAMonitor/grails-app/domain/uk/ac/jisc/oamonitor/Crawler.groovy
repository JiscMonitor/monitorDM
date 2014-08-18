package uk.ac.jisc.oamonitor

import org.jadira.usertype.dateandtime.joda.PersistentDateTime
import org.joda.time.DateTime

class Crawler {

    DateTime latestCrawled
    Boolean wasSuccessful
    //    static hasMany = [crawledErrors: Exception]

    static constraints = {
        latestCrawled (nullable: false, empty:false, type: PersistentDateTime) //http://gpc.github.io/grails-joda-time/guide/persistence.html
        wasSuccessful (nullable: false, empty:false)
    }
}
