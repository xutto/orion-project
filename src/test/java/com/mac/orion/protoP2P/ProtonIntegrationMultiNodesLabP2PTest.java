package com.mac.orion.protoP2P;

import com.mac.orion.BaseManualTest;
import com.mac.orion.application.in.KadConnectorUseCase;
import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.application.in.UpdateRoutingTableUseCase;
import com.mac.orion.application.out.FileSharerDialerUseCase;
import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.application.service.DiscoveryResponderService;
import com.mac.orion.application.service.FileSharingConnectorService;
import com.mac.orion.application.service.FilesScanConectorService;
import com.mac.orion.application.service.KadConnectorService;
import com.mac.orion.application.service.KadDiscoveryProcessService;
import com.mac.orion.application.service.ScanFilesService;
import com.mac.orion.application.service.UpdateFileRoutingTableService;
import com.mac.orion.application.service.publisher.FilePublisherService;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Address;
import com.mac.orion.domain.model.HostNode;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.domain.scheduler.FileShareScheduler;
import com.mac.orion.domain.scheduler.FilesScanScheduler;
import com.mac.orion.domain.scheduler.PeerDiscoveryScheduler;
import com.mac.orion.infrastructure.mapper.BytesMapper;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.mapper.PeerMapperImpl;
import com.mac.orion.infrastructure.p2p.dial.FileSharerDialerAdapter;
import com.mac.orion.infrastructure.p2p.dial.KadDialerAdapter;
import com.mac.orion.infrastructure.p2p.factory.FileSharerProtocolFactory;
import com.mac.orion.infrastructure.p2p.factory.KadProtocolFactory;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.protocol.FileSharerProtocol;
import com.mac.orion.infrastructure.p2p.protocol.KadProtocol;
import com.mac.orion.infrastructure.persistence.adapter.FileIndexAdapter;
import com.mac.orion.infrastructure.persistence.adapter.FileRepositoryAdapter;
import com.mac.orion.infrastructure.persistence.repository.FilesRepository;
import io.libp2p.core.Host;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.protocol.Identify;
import io.libp2p.protocol.Ping;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.mac.orion.domain.dht.OperationsType.REMOVE;
import static com.mac.orion.domain.dht.OperationsType.SAVE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ProtonIntegrationMultiNodesLabP2PTest extends BaseManualTest {

  public static final String STRING_CONNECTION_PEER = "/ip4/0.0.0.0/tcp/";

  @Autowired
  private DiscoveryResponderService discoveryService;
  @Autowired
  private UpdateRoutingTableUseCase updateRoutingTableUseCase;
  @Autowired
  private PeerMapper peerMapper;
  @Autowired
  private RoutingTable routingTable;
  @Autowired
  private Host hostNode;
//  @Autowired
//  private KadProtocol kadProtocol;
  @Autowired
  private KadConnectorUseCase kadConnectorUseCase;
  @Autowired
  private BytesMapper bytesMapper;
  @Autowired
  private FilesMapper filesMapper;
  @Autowired
  private FilesScanConectorService filesScanConectorService;
  @Autowired
  private FilesRepository filesRepository;
  @Autowired
  private ScanFilesService scanFilesService;
  @Autowired
  private FileIndexAdapter fileIndexAdapter;
  @Autowired
  private KadDiscoveryUseCase kadDiscoveryUseCase;
  @Autowired
  private KadProtocolFactory kadProtocolFactory;
  @Autowired
  private FileSharerProtocolFactory fileSharerProtocolFactory;
  @Autowired
  private ProtocolFactoryCreator protocolFactoryCreator;
  @Autowired
  private Settings settings;

//  @Autowired
//  private PeerMapperImpl peerMapperImpl;


  @Test
  void givenHostNodeAWithHostNodeBPeerAddressInRoutingTable_WhenCallToHostNode2_ThenReturnWith0Closest()
      throws ExecutionException, InterruptedException, IOException {

    final List<HostCompositionSupport> hostCompositionSupports = initializeNodes(2);
    Thread.sleep(2000);

    final HostCompositionSupport firstCompo = hostCompositionSupports.getFirst();
    final HostCompositionSupport secondCompo = hostCompositionSupports.get(1);

    final Host hostNode2 = secondCompo.getHostNode();

    // get peer hostNode2
    final Peer buildPeer = getPeer(hostNode2);

    // add peer hostNode2 to hostNode1 routingTable
    firstCompo.getRoutingTable().publish(SAVE, buildPeer);

    final KadConnectorService kadConnectorService = obtainKadConnectorByRemoteHostCompo(firstCompo,
        firstCompo.getPort());

    // trigger the connection process of hostNode1 to hostNode2
    kadConnectorService.connectAnnouncePeer();

    // trigger the file sharing process
    final FileSharingConnectorService fileSharingConnectorServiceFirst = getFileSharingConnectorService(
        firstCompo);
    final FileSharingConnectorService fileSharingConnectorServiceSecond = getFileSharingConnectorService(
        secondCompo);
    final FilesScanConectorService filesScanConectorServiceFirst = getFilesScanConectorService(
        firstCompo);
    final FilesScanConectorService filesScanConectorServiceSecond = getFilesScanConectorService(
        secondCompo);
    fileSharingConnectorServiceFirst.connectFileSharing();
    fileSharingConnectorServiceSecond.connectFileSharing();
    filesScanConectorServiceFirst.connectFilesScan();
    filesScanConectorServiceSecond.connectFilesScan();

    // wait time test
    Thread.sleep(120_000);
  }


  @Test
  void given5RemotePeers_whenLocalNodeInitiatedDiscoveryProcess_thenFillOwnRoutingTableCorrectly()
      throws ExecutionException, InterruptedException, IOException {

    final List<HostCompositionSupport> hostCompositionSupports = initializeNodes(5);

    // START HOST NODE
//    hostNode.start().get();

    final Set<Host> allPeers = hostCompositionSupports.stream()
        .map(HostCompositionSupport::getHostNode)
        .collect(Collectors.toSet());

    //

    Thread.sleep(2000);
    hostCompositionSupports.forEach((hostCompositionSupport) -> {

      allPeers.stream()
          .filter(h -> !h.getPeerId().equals(hostCompositionSupport.getHostNode().getPeerId()))
          .map(h -> {
            final Set<Address> addresses = h.getNetwork().getTransports().getFirst()
                .listenAddresses()
                .stream().map(peerMapper::mapAddress).collect(
                    Collectors.toSet());
            return Peer.builder()
                .id(h.getPeerId().toString())
                .address(addresses)
                .build();
          })
          .forEach(p -> hostCompositionSupport.getRoutingTable().publish(SAVE,
              p)); // todo si haces getFirst se añade el primero coincidente haciendo que cada nodo solo tenga 1 id de otro remoto.
    });

    hostCompositionSupports.forEach((hostCompositionSupport) -> {
      log.info("routingTable of HOST: [{}]", hostCompositionSupport.getHostNode().getPeerId());
      hostCompositionSupport.getRoutingTable().getAllPeers()
          .forEach(p -> log.info("Peer in routingTable: {}", p.getId()));

      final FilesScanConectorService filesScanConectorService = getFilesScanConectorService(
          hostCompositionSupport);
      filesScanConectorService.connectFilesScan();
    });

    Thread.sleep(2000);
    final Host remoteNodeBootstrap = hostCompositionSupports.getFirst().getHostNode();
    final Peer targetPeer = getPeer(remoteNodeBootstrap);

    routingTable.publish(SAVE, targetPeer);

    // trigger the connection process of hostNode local to remotes
    kadConnectorUseCase.connectAnnouncePeer();

    Thread.sleep(120_000);
  }

  @Test
  void givenMultipleNodes_WhenActivatedDiscoveryProcess_thenMustConnectAllOfThem()
      throws ExecutionException, InterruptedException, IOException {

    // START HOST NODE LOCAL AS BOOTSTRAP
//    hostNode.start().get();

    // trigger the connection process of hostNode LOCAL AS BOOTSTRAP
//    kadConnectorUseCase.connectAnnouncePeer();

    int HOST_NODES_COUNT = 5;

    final List<HostCompositionSupport> hostCompositionSupportList = initializeNodes(
        HOST_NODES_COUNT);
    Thread.sleep(2000);

    hostCompositionSupportList.forEach((hostCompositionSupport) -> {

      final Peer targetPeer = getPeer(hostNode);

      hostCompositionSupport.getRoutingTable().publish(SAVE, targetPeer);

      final KadConnectorService kadConnectorService = obtainKadConnectorByRemoteHostCompo(
          hostCompositionSupport, hostCompositionSupport.getPort());

      // added pause connection, prevent chaos, but would work anyway.
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      // dispara el servicio de descubrimiento en cada uno
      kadConnectorService.connectAnnouncePeer();
    });

    Thread.sleep(60000);

  }

  @Test
  void initializerNodes_by_testBootstrapConnection()
      throws ExecutionException, InterruptedException, IOException {

    int HOST_NODES_COUNT = 5;
    final List<HostCompositionSupport> hostCompositionSupports = initializeNodes(HOST_NODES_COUNT);
    final Set<Host> allPeers = hostCompositionSupports.stream()
        .map(HostCompositionSupport::getHostNode)
        .collect(Collectors.toSet());
    Thread.sleep(2000);

    hostCompositionSupports.forEach((hostCompositionSupport) -> {

      allPeers.stream()
          .filter(h -> !h.getPeerId().equals(hostCompositionSupport.getHostNode().getPeerId()))
          .map(h -> {
            final Set<Address> addresses = h.getNetwork().getTransports().getFirst()
                .listenAddresses()
                .stream().map(peerMapper::mapAddress).collect(
                    Collectors.toSet());
            return Peer.builder()
                .id(h.getPeerId().toString())
                .address(addresses)
                .build();
          })
          .forEach(p -> hostCompositionSupport.getRoutingTable().publish(SAVE,
              p)); // todo si haces getFirst se añade el primero coincidente haciendo que cada nodo solo tenga 1 id de otro remoto.
      // dispara el servicio de descubrimiento en cada uno

      final KadConnectorService kadConnectorService = obtainKadConnectorByRemoteHostCompo(
          hostCompositionSupport, hostCompositionSupport.getPort());
      kadConnectorService.connectAnnouncePeer();
    });

    Thread.sleep(Integer.MAX_VALUE);


  }

  private List<HostCompositionSupport> initializeNodes(int hostNodesCount) throws
      ExecutionException, InterruptedException, IOException {

    final ArrayList<HostCompositionSupport> hostCompositionSupports = new ArrayList<>();
    int port = 5000;

    // create N hostnodes
    for (int i = 0; i < hostNodesCount; i++) {
      final HostCompositionSupport hostCompositionSupport = new HostCompositionSupport();

      port += i;

      Host hostNode = new HostBuilder()
          .protocol(
              new Ping(),
              new Identify()
          )
          .transport(TcpTransport::new)
          .secureChannel(NoiseXXSecureChannel::new)
          .muxer(StreamMuxerProtocol::getMplex)
          .listen(STRING_CONNECTION_PEER + port)
          .build();

      // start a fake node
      hostNode.start().get();
      log.info("HOST NODE ID: {}, PORT: {}", hostNode.getPeerId(), port);

      final String id = hostNode.getPeerId().toString();
//      final String address = hostNode.getNetwork().getTransports().getFirst().listenAddresses()
//          .getFirst().toString();
      final HostNode ownHostNode = HostNode.builder().id(id)
//          .address(address)
          .addresses(new HashSet<>())
          .build();

      final RoutingDataHashTableToTesting ownRoutingDataHashTable = RoutingDataHashTableToTesting.createNew(
          Set.of(SAVE, REMOVE),
          ownHostNode);

//      final Set<String> files = new HashSet<>();
//      files.add("inventHash-" + i);
      final LuceneManualTestConfiguration luceneManualTestConfiguration = new LuceneManualTestConfiguration();
      final FileIndexAdapter fia = new FileIndexAdapter(
          luceneManualTestConfiguration.getAnalyzer(),
          luceneManualTestConfiguration.getIndexWriter(),
          luceneManualTestConfiguration.getSearcherManager());
      final FileRoutingDataHashTableToTest ownFileRoutingTable =
          new FileRoutingDataHashTableToTest(fia);

      // BUILDING FILE SHARER PROTOCOL
      final FileSharerProtocol fileSharerProtocol = getFileShareProtocol(ownFileRoutingTable);
      hostNode.addProtocolHandler(fileSharerProtocol);

      // BUILDING CUSTOM REMOTE PROTOCOL
      final KadProtocol protocol = getKadProtocol(ownRoutingDataHashTable);

      hostNode.addProtocolHandler(protocol);

      // BUILDING COMPOSITE
      hostCompositionSupport.setHostNode(hostNode);
      hostCompositionSupport.setRoutingTable(ownRoutingDataHashTable);
//      hostCompositionSupport.setKadProtocolFactory(kadProtocolFactory);
      hostCompositionSupport.setPort(String.valueOf(port));
//      hostCompositionSupport.setFileSharerProtocolFactory(fileSharerProtocolFactory);
      hostCompositionSupport.setFileRoutingTable(ownFileRoutingTable);
      hostCompositionSupport.setProtocolFactoryCreator(protocolFactoryCreator);

      hostCompositionSupports.add(hostCompositionSupport);


    }

    return hostCompositionSupports;
  }

  private IndexWriter fileIndexWriterConfiguration() throws IOException {
    final Directory directory = new ByteBuffersDirectory(); // ram usage
    final Analyzer analyzer = new StandardAnalyzer();
    final IndexWriterConfig config = new IndexWriterConfig(analyzer);

    return new IndexWriter(directory, config);
  }

  @NotNull
  private KadProtocol getKadProtocol(RoutingDataHashTableToTesting ownRoutingDataHashTable) {
    final NodeConfigUseCase nodeConfig = mock(NodeConfigUseCase.class);
    when(nodeConfig.getLimitK()).thenReturn(20);
    final DiscoveryResponderService responderService = new DiscoveryResponderService(
        ownRoutingDataHashTable, nodeConfig);

    return new KadProtocol(
        new KadDiscoveryProcessService(responderService, updateRoutingTableUseCase),
        peerMapper);
  }

  @NotNull
  public FileSharerProtocol getFileShareProtocol(
      FileRoutingDataHashTableToTest fileRoutingDataHashTableToTest) {

    final UpdateFileRoutingTableService updateFileRoutingTableService = new UpdateFileRoutingTableService(
        fileRoutingDataHashTableToTest);

    return new FileSharerProtocol(filesMapper,
        updateFileRoutingTableService);
  }

  private FileSharingConnectorService getFileSharingConnectorService(
      HostCompositionSupport support) {

    final FileSharerDialerUseCase fileSharerDialerDialerAdapter = new FileSharerDialerAdapter(
        support.getFileRoutingTable(), support.getProtocolFactoryCreator(), support.getHostNode(),
        filesMapper);

    final FileShareScheduler fileShareScheduler = new FileShareScheduler(support.getRoutingTable(),
        fileSharerDialerDialerAdapter);
    return new FileSharingConnectorService(fileShareScheduler);
  }

  private FilesScanConectorService getFilesScanConectorService(HostCompositionSupport support) {
    final FileRepositoryAdapter fileRepositoryAdapter = new FileRepositoryAdapter(filesRepository,
        filesMapper/*, support.getRoutingTable()*/);

    final FilePublisherService filePublisherService = new FilePublisherService();
    final ScanFilesService scanFilesService = new ScanFilesService(fileRepositoryAdapter,
        filePublisherService, settings);
    // C:\Users\jaluque\Downloads\ORION-FILES\share2

    /*

    try { // ultra manual added folders to scan in dummy's
      final List<String> folders = new ArrayList<>();
      folders.add("C:\\Users\\jaluque\\Downloads\\ORION-FILES\\share2");
      Field scanFiles = ScanFilesService.class.getDeclaredField("scanFolders");
      scanFiles.setAccessible(true);
      scanFiles.set(scanFilesService, folders);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }


     */

    final FilesScanScheduler filesScanScheduler = new FilesScanScheduler(scanFilesService,
        filePublisherService, support.getFileRoutingTable(),
        support.getRoutingTable());
    return new FilesScanConectorService(filesScanScheduler);
  }


  private KadConnectorService obtainKadConnectorByRemoteHostCompo(HostCompositionSupport firstCompo,
      String port) {

    final NodeConfigUseCase nodeConfigUseCase = mock(NodeConfigUseCase.class);
    when(nodeConfigUseCase.getPort()).thenReturn(Integer.parseInt(port));

    final KadDialerAdapter kadDialerAdapter = new KadDialerAdapter(
        firstCompo.getProtocolFactoryCreator(), firstCompo.getHostNode(),
        firstCompo.getRoutingTable(), new PeerMapperImpl(), nodeConfigUseCase);

    return new KadConnectorService(
        new PeerDiscoveryScheduler(firstCompo.getRoutingTable(), kadDialerAdapter));
  }

  private Peer getPeer(Host hostNode2) {
    final Set<Address> addresses = hostNode2.getNetwork().getTransports().getFirst()
        .listenAddresses()
        .stream().map(peerMapper::mapAddress).collect(
            Collectors.toSet());
    return Peer.builder()
        .id(hostNode2.getPeerId().toString())
        .address(addresses)
        .build();
  }

}

//  @Test
// todo este tests está bien el problema es que con la nueva arquitectuira se disparan las llamadas desde la routingTable y no se pueden
//  hacer desde el kadConnectorUseCase a pelo, porque hay un PeerDiscoveryProcess que lo hace de forma automática y periódica
//  void givenMultipleNodesWhenSendMessageMustBuildHasTableCorrectly()
//      throws ExecutionException, InterruptedException {
//
//    int HOST_NODES_COUNT = 5;
//
//    final List<HostCompositionSupport> hostCompositionSupportList = initializeNodes(
//        HOST_NODES_COUNT);
//
//    Thread.sleep(2000);
//
//    // call host to REMOTE peers
//    hostCompositionSupportList.forEach((hostCompositionSupport) -> {
//
//      final Host targetHost = hostCompositionSupport.getHostNode();
//
//      final PeerId peerId = targetHost.getPeerId();
//      final String addresRaw = targetHost.getNetwork().getTransports().getFirst().listenAddresses()
//          .getFirst().toString();
//      try {
//        Thread.sleep(2000);
//      } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//      }
//      log.info("THE SENDER ID IS: {}", hostNode.getPeerId());
//
/// /      final Peer targetPeer = Peer.builder().id(peerId.toString()).address(addresRaw).build();
//
/// /      kadConnectorUseCase.connectAnnouncePeer();
//
//    });
//
//    // wait to end proccess
//    Thread.sleep(10000);
//
//    // call REMOTE PEERS to HOST
//    hostCompositionSupportList.forEach((hostCompositionSupport) -> {
//
//      try {
//        Thread.sleep(2000);
//      } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//      }
//
//      final PeerId peerId = hostNode.getPeerId();
//      final String addresRaw = hostNode.getNetwork().getTransports().getFirst().listenAddresses()
//          .getFirst().toString();
//
//      final KadDialerAdapter kadDialerAdapter = new KadDialerAdapter(
//          hostCompositionSupport.getKadProtocol(), hostCompositionSupport.getHostNode(),
//          hostCompositionSupport.getRoutingTable());
//      final KadConnectorService kadConnectorService = new KadConnectorService(
//          new PeerDiscoveryProcess(hostCompositionSupport.getRoutingTable(), kadDialerAdapter));
//
/// /      final Peer targetPeer = Peer.builder().id(peerId.toString()).address(addresRaw).build();
//
/// /      kadConnectorService.connectAnnouncePeer();
//
//    });
//
//    Thread.sleep(10000);
//  }



/*
  private PeerInfoDiscovery buildPeerInfo(Host hosNodeCaller) {
    final PeerData peer = PeerData.newBuilder()
        .setId(ByteString.copyFromUtf8(hosNodeCaller.getPeerId().toBase58()))
        .setAddress(ByteString.copyFromUtf8(
            hosNodeCaller.getNetwork().getTransports().getFirst().listenAddresses()
                .getFirst().toString()))
        .build();
    return PeerInfoDiscovery.newBuilder().setPeerInfo(peer).build();
  }
}
*/

/*
  private void callToPeer(final StreamPromise<? extends KadController> dialer,
      Host hostNodeCaller) {

    dialer.getController()
        .thenAccept(kadController -> kadController.send(buildPeerInfo(hostNodeCaller))
            .thenAccept(valid -> {
              log.info("Message sent successfully: {}", valid);
//              routingTableTest.getAllPeers() // todo esta routing table esta inyectada , funciona pero habría que ver como usar la que tiene cada nodo o algo asi
//                  .forEach(p -> log.info("Peer in routing host: {}", p.getId()));
            })
            .exceptionally(ex -> {
              log.error("Error sending message", ex);
              return null;
            }))
        .exceptionally(ex -> {
          log.error("Error getting controller", ex);
          return null;
        });


  }
*/



/*

    final Optional<ProtocolBinding<Object>> protocolBinding = peer01.getProtocols().stream()
        .filter(
            p -> p.getProtocolDescriptor().getAnnounceProtocols().contains("/discovery-self/1.0.0"))
        .findFirst();

    if (protocolBinding.isPresent()) {
      final ProtocolBinding<Object> binding = protocolBinding.get();
      final StreamPromise<? extends KadController> streamPromise =
          (StreamPromise<? extends KadController>) binding.dial(peer01, receiverPeerId,
              remoteAddrs);
      streamPromise.getController()
          .thenAccept(kadController -> {
            kadController.send(buildPeerInfo)
                .thenAccept(valid -> {
                  log.info("Message sent successfully: {}", valid);
                })
                .exceptionally(ex -> {
                  log.error("Error sending message", ex);
                  return null;
                });
          })
          .exceptionally(ex -> {
            log.error("Error getting controller", ex);
            return null;
          });


 */





    /*

    final Multiaddr hostAddress = host.getNetwork().getConnections().getFirst().localAddress();

    final Optional<ProtocolBinding<Object>> protocolBinding = host.getProtocols().stream()
        .filter(
            p -> p.getProtocolDescriptor().getAnnounceProtocols().contains("/discovery-self/1.0.0"))
        .findFirst();

    final PeerData peer = PeerData.newBuilder()
        .setId(ByteString.copyFromUtf8(host.getPeerId().toBase58()))
        .setAddress(ByteString.copyFromUtf8(
            host.getNetwork().getConnections().getFirst().localAddress().toString()))
        .build();
    final PeerInfoDiscovery buildPeerInfo = PeerInfoDiscovery.newBuilder().setPeerInfo(peer)
        .build();

    if (protocolBinding.isPresent()) {
      final ProtocolBinding<Object> binding = protocolBinding.get();
      final StreamPromise<? extends KadController> streamPromise =
          (StreamPromise<? extends KadController>) binding.dial(host, targetAddress);
      streamPromise.getController()
          .thenAccept(kadController -> {
            kadController.send(buildPeerInfo)
                .thenAccept(valid -> {
                  log.info("Message sent successfully: {}", valid);
                })
                .exceptionally(ex -> {
                  log.error("Error sending message", ex);
                  return null;
                });
          })
          .exceptionally(ex -> {
            log.error("Error getting controller", ex);
            return null;
          });
    }
    */
