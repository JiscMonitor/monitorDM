package uk.ac.jisc.oamonitor

class Article extends KBComponent {

    // USE KBComponent name property
    // String title

    static hasMany = [
    ]

    static mappedBy = [
    ]

    //static hasOne = [leadAuthor:Person] //no way to tell from data

    //Equivalent to doing article.journals to return hasMany
    Set<TitleInstance> getTitleInstances() { //i.e. the journals 
        Appearence.findAllByArticle(this).collect { it.titleInstance }
    }

    //is the article connected to any journals etc
    boolean hasTitleInstance(TitleInstance titleInstance) {
        Appearence.countByArticleAndTitleInstance(this, titleInstance) > 0
    }

    Set<Appearence> getAppearances() {
        Appearence.findAllByArticle(this)
    }

    static constraints = {
        //leadAuthor nullable: true, blank: true //todo talk to Ian if we're to assume an author is or if this is needed at all
    }

}
