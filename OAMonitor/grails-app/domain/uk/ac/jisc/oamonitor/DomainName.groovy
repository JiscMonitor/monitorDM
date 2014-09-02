package uk.ac.jisc.oamonitor


class DomainName {

    /* domain name - lower case */
    String fqdn

    /* IF this fqdn belongs to an academic institution, create the link here */
    Org institution

    static constraints = {
        fqdn unique: true, nullable: false, blank: false
        institution nullable: true
    }
}
