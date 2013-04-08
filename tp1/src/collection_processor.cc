#include <iostream>
#include <sstream>
#include <htmlcxx/html/ParserDom.h>
#include "collection_processor.h"
#include "utils.h"

using namespace htmlcxx;
using namespace std;

void CollectionProcessor::process() {
  HTML::ParserDom parser;

  HTML::ParserSax sax;

  while (document_source_->fetchNext()) {
    string url = document_source_->getUrl();
    indexer_->beginDocument(url);

    string html = document_source_->getText();
    
    tree<HTML::Node> dom = parser.parseTree(html);
    tree<HTML::Node>::iterator it = dom.begin();
    tree<HTML::Node>::iterator end = dom.end();

    for (; it != end; ++it) {
      
      if ((!it->isTag()) && (!it->isComment())) {
        string parentTag = dom.parent(it)->tagName();
        lowerString(parentTag);
        if (parentTag == "script" || parentTag == "style") {
          continue;
        }
        
        string block = it->text();
        string term;
        stringstream ss(block);
        while (ss >> term) {
          lowerString(term);
          indexer_->addTerm(stripSpecialChars(term));
        };
      }
    }
  }

  indexer_->end();
}

