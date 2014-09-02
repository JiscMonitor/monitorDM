import java.util.regex.*


def emails = []
def input = "ORCID: 0000-0001-5907-2795 stet@ukr.net"

// Sometimes email address field is filled with other guff. Parse out all email addresses
Pattern p = Pattern.compile("(\\b[A-Z0-9._%+-]+)@([A-Z0-9.-]+\\.[A-Z]{2,4}\\b)", Pattern.CASE_INSENSITIVE);
Matcher matcher = p.matcher(input);
while(matcher.find()) {
  emails.add(matcher.group());
  println(matcher.group())
  println(matcher.group(1))
  println(matcher.group(2))
}


println(emails)
