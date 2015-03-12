// messages.thrift
// Defines the scheme for the messages
// University of Neuchatel, 2013
// Paradime Project

namespace java org.powerapi.bitwatts.reporter.thrift

struct Message {
  1: map<string,string> content
}
