package uk.ac.jisc.oamonitor

class AuthorName {

    String fullname

    static hasMany = [authorMatches:Person]

    static constraints = {
        fullname unique: true, nullable: false, blank: false
    }
}
