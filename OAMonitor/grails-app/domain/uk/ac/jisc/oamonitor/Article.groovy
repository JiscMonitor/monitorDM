package uk.ac.jisc.oamonitor

class Article extends KBComponent {

    // USE KBComponent name property
    // String title

    static hasMany = [
    ]

    static mappedBy = [
    ]

    Set<Appearence> getAppearances() {
        def article_appearance_type = RefdataCategory.lookupOrCreate('Combo.Type','Appearence.Article')
        Appearence.executeQuery("select c.fromComponent from Combo as c where c.toComponent = :article and c.type = :type",
                                [article:this, type:article_appearance_type]);
    }

    static constraints = {
        //leadAuthor nullable: true, blank: true //todo talk to Ian if we're to assume an author is or if this is needed at all
    }

}
