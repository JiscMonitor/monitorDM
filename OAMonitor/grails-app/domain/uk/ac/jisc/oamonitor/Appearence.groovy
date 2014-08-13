package uk.ac.jisc.oamonitor

import org.apache.commons.lang.builder.HashCodeBuilder

//The link between an Article to a Journal etc i.e. like spring security cores UserRole
//An Article can appear in one of more Journals
// Serializable incase of Cache usage
class Appearence implements Serializable {

    Article article;
    TitleInstance titleInstance

    static constraints = {
        //don't want to add an appearence mutliple times, once only!
        titleInstance validator: { TitleInstance ti, Appearence a ->
            if (a.article == null) return
            boolean existing = false
            Appearence.withNewSession {
                existing = Appearence.exists(a.article.id, ti.id)
            }
            if (existing) {
                return 'Appearence.exists'
            }
        }
    }

    static mapping = {
        id composite: ['article', 'titleInstance']
        version false
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

    static Appearence get(long articleID, long titleInstanceId) {
        Appearence.where {
            article == Article.load(articleID) &&
                    titleInstance == TitleInstance.load(titleInstanceId)
        }.get()
    }

    static boolean exists(long articleID, long titleInstanceId) {
        Appearence.where {
            article == Article.load(articleID) &&
                    titleInstance == TitleInstance.load(titleInstanceId)
        }.count() > 0
    }

    static Appearence create(Article article, TitleInstance titleInstance, boolean flush = false) {
        def instance = new Appearence(article: article, titleInstance: titleInstance)
        instance.save(flush: flush, insert: true)
        instance
    }

    static boolean remove(Article a, TitleInstance t, boolean flush = false) {
        if (a == null || t == null) return false

        int rowCount = Appearence.where {
            article == Article.load(a.id) &&
                    titleInstance == TitleInstance.load(t.id)
        }.deleteAll()

        if (flush) { Appearence.withSession { it.flush() } }

        rowCount > 0
    }

}
