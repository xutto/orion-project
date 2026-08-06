package com.mac.orion.infrastructure.mapper;

import com.mac.orion.domain.model.SearchResource;
import com.mac.orion.domain.model.SearchResult;
import com.mac.orion.infrastructure.p2p.model.Search.SearchInfoRequest;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchFilesResult;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {PeerMapper.class, BytesMapper.class, FilesMapper.class})
public interface SearchMapper {


  @Mapping(target = "id", source = "id", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "snippet", source = "snippet", qualifiedByName = "stringToByteSTR")
  SearchInfoRequest mapSearchInfoFromSearchResource(SearchResource searchResource);

  @Mapping(target = "id", source = "id", qualifiedByName = "byteSTRToString")
  @Mapping(target = "snippet", source = "snippet", qualifiedByName = "byteSTRToString")
  SearchResource mapSearchResourceFromSearchInfo(SearchInfoRequest searchInfoRequest);

  @Mapping(target = "id", source = "id", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "filesList", source = "files")
  SearchFilesResult mapSearchFilesResultFromSearchResource(SearchResult searchResult);

  @Mapping(target = "id", source = "id", qualifiedByName = "byteSTRToString")
  @Mapping(target = "files", source = "filesList")
  SearchResult mapSearchResourceFromSearchFilesResult(SearchFilesResult searchFilesResult);

}
