# CmakeLists.txt in src/ricplib

include_directories(${TP1_SOURCE_DIR}/lib/ricp/src)

add_library(indexer utils string_tokenizer file_handler inverted_list indexer document_source collection_processor links_processor)

add_executable(indexp main)
target_link_libraries(indexp indexer htmlcxx RICPlib z)

add_executable(pr prmain)
target_link_libraries(pr indexer htmlcxx RICPlib z)

add_executable(queryp query)
target_link_libraries(queryp indexer htmlcxx RICPlib z)
