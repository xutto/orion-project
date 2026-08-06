package com.mac.orion.infrastructure.mapper;

import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.transfer.FileAvailability;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileError;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.NextWindow;
import com.mac.orion.infrastructure.p2p.model.FileTransfer;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileManifest;
import com.mac.orion.infrastructure.persistence.entity.FileEntity;
import java.util.HashSet;
import java.util.Set;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, uses = {
    PeerMapper.class, BytesMapper.class}, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FilesMapper {

  @Mapping(target = "name", source = "names", qualifiedByName = "mapNamesToString")
  @Mapping(target = "hash", source = "hash", qualifiedByName = "mapHashToString")
  FileEntity toEntity(File file);

  @Mapping(target = "score", ignore = true) // entities cannot have a score
  @Mapping(target = "peers", expression = "java(new java.util.HashMap<>())") // entities cannot have peers
  @Mapping(target = "names", source = "name", qualifiedByName = "mapNamesFromString")
  @Mapping(target = "hash", source = "hash", qualifiedByName = "mapStringToHash")
  File toDomain(FileEntity fileEntity);

  // todo arreglar mapper tambien de los peers con lo nuevo
  @Mapping(target = "peers", source = "sharingPeersList", qualifiedByName = "mapSharingPeers")
  @Mapping(target = "hash.value", source = "hash", qualifiedByName = "byteSTRToString")
  @Mapping(target = "names", source = "namesList", qualifiedByName = "byteSTRToString")
  //todo ojo con los bytestream, igual hay que convertir a string
  File fileManifestToDomain(FileManifest fileManifest);

  @Mapping(target = "sharingPeersList", source = "peers", ignore = true)
  // mapping on afterMappingPeersHashMapToCollectionModel
  @Mapping(target = "hash", source = "hash.value", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "namesList", source = "names", qualifiedByName = "stringToByteSTR")
    //todo ojo con los bytestream, igual hay que convertir a string
  FileManifest domainToFileManifest(File file);


  // file transfer mappings -----------------------------------------------------------|

  @Mapping(target = "value", source = "value", qualifiedByName = "stringToByteSTR")
  FileTransfer.Hash hashToHashTransfer(Hash hash);

  @Mapping(target = "value", source = "value", qualifiedByName = "byteSTRToString")
  Hash hashTransferToHashDomain(FileTransfer.Hash hash);

  @Mapping(target = "availability", source = "availability", qualifiedByName = "bitSetToByteSTR")
  FileTransfer.FileAvailability availabilityToAvailabilityTransfer(FileAvailability availability);

  @Mapping(target = "availability", source = "availability", qualifiedByName = "byteSTRToBitSet")
  FileAvailability availabilityTransferToDomain(FileTransfer.FileAvailability availability);

  @Mapping(target = "data", source = "data", qualifiedByName = "bytesToByteSTR")
  FileTransfer.FileChunk chunkDomainToTransfer(FileChunk fileChunk);

  @Mapping(target = "data", source = "data", qualifiedByName = "byteSTRToBytes")
  FileChunk chunkTransferToChunkDomain(FileTransfer.FileChunk fileChunk);

  @Mapping(target = "message", source = "message", qualifiedByName = "byteSTRToString")
  @Mapping(target = "code", source = "code", qualifiedByName = "byteSTRToString")
  FileError fileErrorTransferToFileError(FileTransfer.FileError fileError);

  @Mapping(target = "message", source = "message", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "code", source = "code", qualifiedByName = "stringToByteSTR")
  FileTransfer.FileError fileErrorToFileErrorTransfer(FileError fileError);

  NextWindow nextWindowTransferToNextWindow(FileTransfer.NextWindow nextWindow);

  FileTransfer.NextWindow nextWindowToNextWindowTransfer(NextWindow nextWindow);


  FileTransferData fileTransferToDomain(FileTransfer.FileMessage fileMessage);

  FileTransfer.FileMessage domainToFileTransfer(FileTransferData fileTransferData);

  FileTransfer.FileFragment fileFragmentDomainToTransfer(FileFragment fileFragment);


  // file transfer mappings ---------------------------------------------------|


  @Named("mapHashToString")
  default String mapHashToString(Hash hash) {
    return hash.getValue();
  }

  @Named("mapStringToHash")
  default Hash mapStringToHash(String hash) {
    return Hash.builder()
        .value(hash)
        .build();
  }

  @Named("mapNamesToString")
  default String mapNamesToString(Set<String> names) {
    return names.stream().findFirst().orElse("");
  }

  @Named("mapNamesFromString")
  default Set<String> mapNamesFromString(String name) {
    final Set<String> names = new HashSet<>();
    names.add(name);
    return names;
  }

//  @Named("mapNamesFromByteString")
//  default Set<String> mapNamesFromByteString(List<ByteString> names) {
//    return names.stream().map(this::byteStrToString).collect(Collectors.toSet());
//  }

//  @Name("mapNamesFromStringList"  )
//  default List<ByteString> mapNamesFromStringList(Set<String> names){
//    names.stream().map()
//  }
}
