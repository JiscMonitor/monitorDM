package uk.ac.jisc.oamonitor


/* todo: Rename to ArticleAuthor */
class AuthorName {


    /* The article this ArticleAuthor relates to */
    Article theArticle

    /* The name as it appeared */
    String fullname

    /* Any matched person */
    Person matchedPerson

    /* Attached to what org */
    DomainName domainName

    /* Attached to what org */
    Org institution

    /* Author, Research Assistant, Corresponding Author, etc, etc */
    RefdataValue role

    static constraints = {
        fullname unique: false, nullable: false, blank: false
        matchedPerson nullable: true
        institution nullable: true
        domainName nullable: true
        role nullable: true
    }
}
