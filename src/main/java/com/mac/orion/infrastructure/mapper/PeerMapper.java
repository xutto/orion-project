package com.mac.orion.infrastructure.mapper;

import static com.mac.orion.domain.share.Constants.ADDRESS_SEPARATOR_REGEX;

import com.mac.orion.domain.model.Address;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.p2p.model.Discovery.DiscoveryRequest;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileManifest;
import com.mac.orion.infrastructure.p2p.model.Peer.PeerAddress;
import com.mac.orion.infrastructure.p2p.model.Peer.PeerData;
import com.mac.orion.infrastructure.p2p.model.Peer.PeerSharer;
import io.libp2p.core.multiformats.Multiaddr;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, uses = BytesMapper.class)
public interface PeerMapper {

  @Mapping(target = "id", source = "id", qualifiedByName = "byteSTRToString")
  @Mapping(target = "address", source = "addressList")
  Peer peerDataToPeer(PeerData PeerData);


  @Mapping(target = "id", source = "id", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "addressList", source = "address")
  PeerData peerToPeerData(Peer peer);

//  @Named("mapAddressListFromDomain")
//  Set<PeerAddress> mapAddressListFromDomain(Set<Address> domainAddresses);

  @Mapping(target = "ip", source = "ip", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "type", source = "type", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "transmission", source = "transmission", qualifiedByName = "stringToByteSTR")
  PeerAddress mapAddressFromDomain(Address domainAddress);

  @Mapping(target = "ip", source = "ip", qualifiedByName = "byteSTRToString")
  @Mapping(target = "type", source = "type", qualifiedByName = "byteSTRToString")
  @Mapping(target = "transmission", source = "transmission", qualifiedByName = "byteSTRToString")
  Address mapAddressFromModel(PeerAddress modelAddress);

  @Mapping(target = "id", source = "id", qualifiedByName = "byteSTRToString")
  @Mapping(target = "lastSeen", source = "lastSeen", qualifiedByName = "longToInstant")
  @Mapping(target = "address", source = "addressList")
  Peer fromPeerSharer(PeerSharer peerSharer);

  @Mapping(target = "id", source = "id", qualifiedByName = "stringToByteSTR")
  @Mapping(target = "lastSeen", source = "lastSeen", qualifiedByName = "instantToLong")
  @Mapping(target = "addressList", source = "address")
  PeerSharer toPeerSharer(Peer peer);

  @Mapping(target = "id", source = "id", qualifiedByName = "byteSTRToString")
  @Mapping(target = "address", source = "addressList")
  Peer mapPeerFromDiscoveryRequest(DiscoveryRequest discoveryRequest);

//  @Named("mapSharingPeersDomain")
//  default java.util.Collection<Manifest.PeerSharer> mapSharingPeersDomain(Map<String, Peer> peers) {
//    if (peers == null || peers.isEmpty()) {
//      return java.util.Collections.emptyList();
//    }
//    return peers.values().stream()
//        .map(this::toPeerSharer)
//        .collect(java.util.stream.Collectors.toList());
//  }

  @AfterMapping
  default void afterMappingPeersHashMapToCollectionModel(File source, @MappingTarget FileManifest.Builder target) {
    final List<Peer> peers = source.getPeers().values().stream().toList();
    final Set<PeerSharer> peerSharers = peers.stream().map(this::toPeerSharer)
        .collect(Collectors.toSet());
    target.addAllSharingPeers(peerSharers);
  }

  @Named("mapSharingPeers")
  default Map<String, Peer> mapSharingPeers(List<PeerSharer> sharingPeersList) {
    return sharingPeersList.stream()
        .map(this::fromPeerSharer)
        .collect(Collectors.toSet()).stream()
        .collect(Collectors.toConcurrentMap(Peer::getId, peer -> peer));
  }


  default Address mapAddressString(String multiaddrString, Integer port) {
    // /ip4/192.168.1.115/tcp/4001
    final String[] split = multiaddrString.split(ADDRESS_SEPARATOR_REGEX);
    final String ip = split[2];
//    final Integer port = Integer.parseInt(split[4]);
    final String type = split[1];
    final String transmissionProtocol = split[3];
    return Address.builder()
        .ip(ip)
        .port(port == null ? Integer.parseInt(split[4]) : port)
        .type(type)
        .transmission(transmissionProtocol)
        .build();
  }

  default Address mapAddress(Multiaddr multiaddr) {
    final String stringAddress = multiaddr.toString();
    return mapAddressString(stringAddress, null);
  }

  default Address mapAddressWithPort(Multiaddr multiaddr, Integer port) {
    final String stringAddress = multiaddr.toString();
    return mapAddressString(stringAddress, port);
  }
}
