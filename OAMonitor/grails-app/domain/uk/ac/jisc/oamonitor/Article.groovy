package uk.ac.jisc.oamonitor

class Article extends TitleInstance {

    String title

    static belongsTo = [Person] //todo discuss with ian

    static hasMany = [
            authors:Person
    ]

    //static hasOne = [leadAuthor:Person] //no way to tell from data

    //Equivalent to doing article.journals to return hasMany
    Set<TitleInstance> getTitleInstances() //i.e. the journals
    {
        Appearence.findAllByArticle(this).collect { it.titleInstance }
    }

    //is the article connected to any journals etc
    boolean hasTitleInstance(TitleInstance titleInstance)
    {
        Appearence.countByArticleAndTitleInstance(this, titleInstance) > 0
    }

    static constraints = {
        title nullable: false, blank: false
        //leadAuthor nullable: true, blank: true //todo talk to Ian if we're to assume an author is or if this is needed at all
    }

}
