package uk.ac.jisc.oamonitor


/* todo: Rename to ArticleAuthor */
class AuthorName {


    /* The article this ArticleAuthor relates to */
    Article theArticle

    /* The name as it appeared */
    String fullname

    /* Any matched person */
    Person matchedPerson

    /* todo : add Boolean isCorrespondingAuthor - to identify primary author */

    static constraints = {
        fullname unique: false, nullable: false, blank: false
        matchedPerson nullable: true
        /* todo isCorrespondingAuthor mapping */
    }
}
