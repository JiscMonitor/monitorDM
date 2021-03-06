package uk.ac.jisc.oamonitor

import uk.ac.jisc.oamonitor.cred.*
import uk.ac.jisc.oamonitor.ClassUtils

class TitleLookupService {

  def grailsApplication
  def componentLookupService

  //  def find(title, issn, eissn) {
  //    find(title, issn, eissn, null, null)
  //  }

  private Map classOneMatch (def ids) {

    // log.debug("classOneMatch(${ids})");

    // Get the class 1 identifier namespaces.
    Set<String> class_one_ids = grailsApplication.config.identifiers.class_ones

    // Return the list of class 1 identifiers we have found or created, as well as the
    // list of matches
    def result = [
      "class_one" 	: false,
      "ids"			: [],
      "matches"		: [] as Set
    ]

    // Go through each of the class_one_ids and look for a match.
    ids.each { id_def ->

      if (id_def.namespace && id_def.value) {

        // id_def is map with keys 'type' and 'value'
        Identifier the_id = Identifier.lookupOrCreateCanonicalIdentifier(id_def.namespace, id_def.value)

        // log.debug("Processing ${id_def.namespace}, ${id_def.value}, ${the_id} ");
        the_id.refresh()

        // Add the id.
        result['ids'] << the_id

        // We only treat a component as a match if the matching Identifer
        // is a class 1 identifier.
        if (class_one_ids.contains(id_def.namespace)) {

          // Flag class one is present.
          result['class_one'] = true

          // If we find an ID then lookup the components.
          the_id.identifiedComponents.each { KBComponent c ->

            // Ensure we're not looking at a Hibernate Proxy class representation of the class
            KBComponent dproxied = ClassUtils.deproxy(c);

            // Only add if it's a title.
            if ( dproxied instanceof TitleInstance ) {
              result['matches'] << (dproxied as TitleInstance)
            }
          }
        }
      }
    }

    result
  }



  def lookup (title, publisher_name, identifiers, user = null) {

    // The TitleInstance
    TitleInstance the_title = null

    // Create the normalised title.
    String norm_title = GOKbTextUtils.normaliseString(title)

    // Lookup any class 1 identifier matches
    def results = classOneMatch (identifiers)

    // The matches.
    List< KBComponent> matches = results['matches'] as List

    switch (matches.size()) {
      case 0 :
        // No match behaviour.
        // log.debug ("Title class one identifier lookup yielded no matches.")

        // Check for presence of class one ID
        if (results['class_one']) {
          // log.debug ("One or more class 1 IDs supplied so must be a new TI.")

          // Create the new TI.
          the_title = new TitleInstance(name:title)

        } else {

          // No class 1s supplied we should try and find a match on the title string.
          // log.debug ("No class 1 ids supplied.")

          // Lookup using title string match only.
          the_title = attemptStringMatch (norm_title)

          if (the_title) {
            // log.debug("TI ${the_title} matched by name. Partial match")

            // Add the variant.
            the_title.addVariantTitle(title)

            // Raise a review request
            ReviewRequest.raise(
                the_title,
                "'${title}' added as a variant of '${the_title.name}'.",
                "No 1st class ID supplied but reasonable match was made on the title name.",
                user
                )

          } else {

            // log.debug("No TI could be matched by name. New TI, flag for review.")

            // Could not match on title either.
            // Create a new TI but attach a Review request to it.
            the_title = new TitleInstance(name:title)
          }
        }
        break;
      case 1 :
      // Single component match.
        // log.debug ("Title class one identifier lookup yielded a single match.")

        the_title = singleTIMatch(title, norm_title, matches[0], user)

        break;
      default :
      // Multiple matches.
        // log.debug ("Title class one identifier lookup yielded ${matches.size()} matches. This is a bad match. Ingest should skip this row.")
        break;
    }

    // If we have a title then lets set the publisher and ids... (Could be an article, so there maybe one to many publishers!)
    if (the_title) {

      // Add the publisher.
      addPublisher(publisher_name, the_title, user)

      // Add all the identifiers.
      LinkedHashSet id_set = []
      id_set.addAll(the_title.getIds())
      id_set.addAll(results['ids'])
      the_title.setIds(id_set)

      // Try and save the result now.
      if ( the_title.save(failOnError:true, flush:true) ) {
        // log.debug("Succesfully saved TI: ${the_title.name}")
      }
      else {
        the_title.errors.each { e ->
          log.error("Problem saving title: ${e}");
        }
      }
    }

    the_title
  }

  private TitleInstance addPublisher (String publisher_name, TitleInstance ti, user = null) {

    // Lookup our publisher.
    Org publisher = Org.findByName(publisher_name) ?: new Org(name:publisher_name).save();
    ti.changePublisher(publisher)
    

    ti
  }

  private TitleInstance attemptStringMatch (String norm_title) {

    // Default to return null.
    TitleInstance ti = null

    // Try and find a title by matching the norm string.
    // Default to the min threshold
    double best_distance = grailsApplication.config.cosine.good_threshold

    TitleInstance.list().each { TitleInstance t ->

      // Get the distance and then determine whether to add to the list or
      double distance = GOKbTextUtils.cosineSimilarity(norm_title, t.normname)
      if (distance >= best_distance) {
        ti = t
        best_distance = distance
      }

      t.variantNames?.each { vn ->
        distance = GOKbTextUtils.cosineSimilarity(norm_title, vn.normVariantName)
        if (distance >= best_distance) {
          ti = t
          best_distance = distance
        }
      }
    }

    // Return what we have found... If anything.
    ti
  }

  private TitleInstance singleTIMatch(String title, String norm_title, TitleInstance ti, User user) {

    // The threshold for a good match.
    double threshold = grailsApplication.config.cosine.good_threshold

    // Work out the distance between the 2 title strings.
    double distance = GOKbTextUtils.cosineSimilarity(ti.normname, norm_title)

    // Check the distance.
    switch (distance) {

      case 1 :

      // Do nothing just continue using the TI.
        // log.debug("Exact distance match for TI.")
        break

      case ( ti.variantNames.collect{GOKbTextUtils.cosineSimilarity(it.normVariantName, norm_title)>= threshold }.size() > 0 ) :
        log.debug("Existing variant title good match");
        // Good match on existing variant titles
        // log.debug("Good match for TI on variant.")
        break

      case {it >= threshold} :

      // Good match. Need to add as alternate name.
        // log.debug("Good distance match for TI. Add as variant.")
        ti.addVariantTitle(title)
        break

      default :
        // Bad match...
        log.debug("BAD MATCH:: ${title} -- ${ti.normname} / ${norm_title} == ${distance}");
        ti.addVariantTitle(title)
        break
    }

    ti
  }

}
