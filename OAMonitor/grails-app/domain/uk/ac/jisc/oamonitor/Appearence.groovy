package uk.ac.jisc.oamonitor

import org.apache.commons.lang.builder.HashCodeBuilder

//The link between an Article to a Journal etc i.e. like spring security cores UserRole
//An Article can appear in one of more Journals
// Serializable incase of Cache usage
class Appearence extends KBComponent implements Serializable {

    String volume
    String issue
    RefdataValue apcStatus
    Date dateDetected = new Date()

    static constraints = {
      dateDetected nullable:true
      volume nullable:true
      issue nullable:true
      apcStatus nullable:true, blank:false
    }

    static hasByCombo = [
      article            : Article,
      titleInstance      : TitleInstance,
      submittedBy        : Person,
    ]

    static manyByCombo = [
        grants : Grant
    ]

    static mapping = {
    }


    boolean equals(other) {
        if (!(other instanceof Appearence)) {
            return false
        }

        other.article?.id == article?.id &&
                other.titleInstance?.id == titleInstance?.id
    }

    int hashCode() {
        def builder = new HashCodeBuilder()
        if (article) builder.append(article.id)
        if (titleInstance) builder.append(titleInstance.id)
        builder.toHashCode()
    }

    static Appearence create(Article article, TitleInstance titleInstance, boolean flush = false) {
        def instance = new Appearence(article: article, titleInstance: titleInstance)
        instance.save(flush: flush, insert: true)
        instance
    }

}
