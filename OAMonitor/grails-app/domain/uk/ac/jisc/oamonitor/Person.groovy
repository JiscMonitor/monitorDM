package uk.ac.jisc.oamonitor

import groovy.util.logging.Log4j

@Log4j
class Person {

    String fullname

    static hasMany = [
        orcids:Identifier,
        pisin:Identifier,
        eisin:Identifier,
        articles: Article
    ]

    static constraints = {
        fullname    (nullable:true, blank:false, maxSize:2048)
        orcids      (unique: true, nullable: true)
        pisin       (unique: true, nullable: true)
        eisin       (unique: true, nullable: true)
    }

    /**
     * One author at a time, check as there could be multiple orcids or none, check the articles and link
     * @param authorName
     * @param identifiers i.e. ORCIDs, EISIN, PISIN
     * @return Person created or found plus any possible ORCIDs that are not matched
     */
    static def createOrLookupAuthor(String authorName, Map identifiers)
    {
        Person person;
        AuthorName author
        def possibleORCIDs

        if (!AuthorName.findByFullname(authorName)) //Person won't exist without having a name existing
        {
            author = new AuthorName(fullname: authorName)
            person = new Person(fullname: authorName)
            author.addToAuthorMatches(person)

            if (!author.save())
            {
                author.errors.each {
                    log.debug(it)
                }
            }

            identifiers.each {key, value ->
                switch (key.toLowerCase()) {
                    case "essin":
                        person.addToEisin(Identifier.lookupOrCreateCanonicalIdentifier(value))
                        break
                    case "pssin":
                        person.addToPisin(Identifier.lookupOrCreateCanonicalIdentifier(value))
                        break
                    case "orcid":
                        person.addToOrcids(Identifier.lookupOrCreateCanonicalIdentifier(value))
                        break
                    default:
                        println("Unknown key type of identifier:\t" + key)
                }
            }
            person.save(failOnError: true)
        }
        else //does exist
        {
            if (identifiers) //rely on ID's first then by name
            {
                identifiers.each {key, value ->
                    switch (key.toLowerCase()) {
                        case "essin":
                            person = Person.findByEisin(value)
                            if (person.fullname.equalsIgnoreCase(authorName)) //ensure identifier is for the author we're searching for
                                return true //exit closure
                            break
                        case "pssin":
                            person = Person.findByPisin(value)
                            if (person.fullname.equalsIgnoreCase(authorName))
                                return true
                            break
                        case "orcid":
                            person = Person.findByOrcids(value)
                            if (person.fullname.equalsIgnoreCase(authorName))
                                return true
                            else if (!person) {
                                possibleORCIDs.add(value) //no one owns this orcid, is a potential for this author or any of the others being processed to be thiers
                            }
                            break
                        default:
                            log.debug("Could find a user from supplied Identifiers\t"+identifiers)
                    }
                }
            } else
            {
                author = AuthorName.findByFullname(authorName)
                if (author.authorMatches > 1) //could have a problem here, multiple authors, neither of which have unique identifiers
                {
                    //todo ask Ian about this issue
                    //some kind of logic to make an educated guess ?!?!?!
                    //check each person to see if theyve contributed to a certain journal //would need to pass inall the authors from the article
                    //check to see if they've worked with certain authors to make better decision
                }
                else {
                    person = Person.findByFullname(authorName)
                    if (!person)
                    {
                        throw new RuntimeException("Unable to locate Author, which should exist"+authorName)
                        log.debug("Unable to locate Author, which should exist"+authorName)
                    }
                }
            }
        }
        return [person, possibleORCIDs]
    }

}
