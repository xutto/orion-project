package com.mac.orion.application.service;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.application.in.FileAllocatorUseCase;
import com.mac.orion.application.out.FilesPort;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Status;
import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.Sidecar;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAllocatorService implements FileAllocatorUseCase {

  public static final int FRAGMENT_SIZE = 8 * 1024 * 1024; // 8 MB TODO A CONFIG
  public static final int CHUNK_SIZE = 256 * 1024; // 256 KB TODO A CONFIG
  public static final String EXTENSION_SIDECAR_XUT = ".xut";
  public static final String EXTENSION_SIDECAR_SWAP = ".swap";
  public static final String EXTENSION_LOCKS = ".locks";
  public static final int VERSION_NUMBER_INITIAL = 1;
  public static final int SIDECAR_HEADER_VERSION = 1;
  public static final int SIDECAR_HEADER_FRAGMENTS = 4;
  public static final int SIDECAR_HEADER_PAYLOAD = 4;
  private static final ConcurrentHashMap<String, ReentrantReadWriteLock> sidecarLocks = new ConcurrentHashMap<>();

//  @Value("${orion.files.temp.folder}") // todo config
//  private String tempFolder;
//  @Value("${orion.files.download.folder}") // todo config
//  private String downloadFolder;

  private final Settings settings;
  private final FilesPort filesPort;


  @Override
  public File allocateFilePhysical(final File file) {

    final Path filePath = Path.of(settings.getDirectories().temp(), file.getHash().getValue());
    // save the file with status DOWNLOADING
    final File downloadFile = file.toBuilder().path(filePath.toString())
        .hash(file.getHash())
        .names(file.getNames())
        .size(file.getSize())
        .status(Status.DOWNLOADING)
        .build();
    filesPort.save(downloadFile); // todo ver si se puede salvar en el orchestrator mejor

    // Files.createDirectories(Path.of(tempFolder)); // todo ¿deberia existir por configuraciones previas?
    try (FileChannel ch = FileChannel.open(filePath,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      ch.truncate(file.getSize());
    } catch (IOException e) {
      log.error("Cannot allocate file: {}", filePath.getFileName(), e);
      throw new RuntimeException(e);
    }
    if (!Files.exists(filePath)) {
      log.error("File not allocated: {}", filePath.getFileName());
      return null;
    }
    return downloadFile;
  }


  @Override
  public byte[] getFileFragment(Hash hash, Integer index) {
    final File fileByHash = filesPort.findByHash(hash.getValue());
    final Path filePath = Path.of(fileByHash.getPath()); // TODO ESTO ESTA MAL?
    final Long fileSize = fileByHash.getSize();

    long offsetByte = (long) index * FRAGMENT_SIZE;
    if (offsetByte >= fileSize) {
      return new byte[0];
    }

    int toRead = (int) Math.min(FRAGMENT_SIZE, fileSize - offsetByte);

    try (FileChannel ch = FileChannel.open(filePath, StandardOpenOption.READ)) {
      ByteBuffer buffer = ByteBuffer.allocate(toRead);
      long pos = offsetByte;
      while (buffer.hasRemaining()) {
        int n = ch.read(buffer, pos);
        if (n == -1) {
          break;
        }
        pos += n;
      }
      buffer.flip();
      byte[] result = new byte[buffer.remaining()];
      buffer.get(result);
      return result;
    } catch (IOException e) {
      log.error("Cannot found fragment file: {}", filePath.getFileName(), e);
      throw new RuntimeException(e);
    }

  }

  @Override
  public boolean allocateFileLogical(final File file) {
    // todo implements logical allocation file
    return false;
  }

  @Override
  public boolean allocateSidecar(final File file) throws IOException {

    int totalFragments = getFragmetsBySize(file.getSize());
    final BitSet bitset = createClearBitSet(totalFragments);
    byte[] payload = bitset.toByteArray();
    ByteBuffer header = ByteBuffer.allocate(
            SIDECAR_HEADER_VERSION +
                SIDECAR_HEADER_FRAGMENTS +
                SIDECAR_HEADER_PAYLOAD)
        .order(ByteOrder.BIG_ENDIAN);
    header.put((byte) VERSION_NUMBER_INITIAL);            // version
    header.putInt(totalFragments); // total fragments
    header.putInt(payload.length);  // bitSet length in bytes

    Path sidecarPath = Path.of(settings.getDirectories().temp(), file.getHash().getValue() + EXTENSION_SIDECAR_XUT);

    Files.createDirectories(sidecarPath.getParent());
    // Writes safely: to temporary file and rename atomically
    Path tmpSwap = sidecarPath.resolveSibling(sidecarPath.getFileName() + EXTENSION_SIDECAR_SWAP);
    try (OutputStream out = Files.newOutputStream(tmpSwap,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      out.write(header.array());
      out.write(payload);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Files.move(tmpSwap, sidecarPath, StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
    return Files.exists(sidecarPath);
  }

  @Override
  public Sidecar loadSidecarBitsetUnsafe(Hash hash) throws IOException {

    final Path sidecarFilePath = Path.of(settings.getDirectories().temp(), hash.getValue() + EXTENSION_SIDECAR_XUT);
    if (!Files.exists(sidecarFilePath)) {
      throw new RuntimeException("Sidecar file not found: "
          + sidecarFilePath.getFileName()); // todo a custom exception , debe existir el bitSet en FS
    }
    try (InputStream in = Files.newInputStream(sidecarFilePath)) {
      int version = in.read();
      if (version < 0) {
        throw new RuntimeException("Sidecar version file not found: "
            + sidecarFilePath.getFileName()); // todo a custom exception , debe existir el bitSet en FS
      }
      final byte[] bytesLen = in.readNBytes(SIDECAR_HEADER_FRAGMENTS);
      final byte[] bytesPayload = in.readNBytes(SIDECAR_HEADER_PAYLOAD);

      // check header length
      if (bytesLen.length != SIDECAR_HEADER_FRAGMENTS
          || bytesPayload.length != SIDECAR_HEADER_PAYLOAD) {
        log.error("Sidecar file corrupted: {}", sidecarFilePath.getFileName());
        throw new IOException("Sidecar file corrupted: " + sidecarFilePath.getFileName());
      }

      final int totalFragments = ByteBuffer.wrap(bytesLen)
          .order(ByteOrder.BIG_ENDIAN).getInt();

      int lenPayload = ByteBuffer.wrap(bytesPayload).order(ByteOrder.BIG_ENDIAN).getInt();

      byte[] payload = in.readNBytes(lenPayload);
      final BitSet bitSet = BitSet.valueOf(payload);
      return Sidecar.builder()
          .version(version)
          .totalFragments(totalFragments)
          .data(bitSet)
          .build();
    }
  }


  @Override
  public Sidecar loadSidecarBitset(Hash hash) throws IOException {

    final ReadLock readLock = lockFor(hash).readLock();
    readLock.lock(); // lock when reading
    try {
      return loadSidecarBitsetUnsafe(hash);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void updateSidecar(Sidecar sidecar, Hash fileHash) throws IOException {

    final WriteLock writeLock = lockFor(fileHash).writeLock();
    writeLock.lock(); // lock when writing

    try {
      final Path sidecarFilePath = Path.of(settings.getDirectories().temp(), fileHash.getValue() + EXTENSION_SIDECAR_XUT);
      byte[] payload = sidecar.getData().toByteArray();
      ByteBuffer header = ByteBuffer.allocate(
              SIDECAR_HEADER_VERSION +
                  SIDECAR_HEADER_FRAGMENTS +
                  SIDECAR_HEADER_PAYLOAD)
          .order(ByteOrder.BIG_ENDIAN);
      header.put((byte) 1);            // todo NEEDS DEFINE version
      header.putInt(sidecar.getTotalFragments()); // total fragments
      header.putInt(payload.length);  // longitud del bitset en bytes

      // Escribe de forma segura: a archivo temporal y renombrar atómicamente
      UUID tmpUUID = UUID.randomUUID();
      Path tmpSwap = sidecarFilePath.resolveSibling(
          sidecarFilePath.getFileName() + EXTENSION_SIDECAR_SWAP + tmpUUID);
      try (OutputStream out = Files.newOutputStream(tmpSwap,
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        out.write(header.array());
        out.write(payload);
      }
      // todo fallo multihilo por culpa de windows y su nefasta gestion del FS, REFACTORIZAR A OTRO METODO
      int attempts = 0;
      IOException last = null;

      while (attempts < 13) { // ~200–500 ms total dependiendo del backoff
        try {
          Files.move(tmpSwap, sidecarFilePath, StandardCopyOption.REPLACE_EXISTING,
              StandardCopyOption.ATOMIC_MOVE); //todo  explore moveWithRetry
          return;
        } catch (IOException e) {
          log.warn("fail to update sidecar: {}, try again! ", sidecarFilePath.getFileName(), e);
          last = e;
          try {
            Thread.sleep(10L * (attempts + 1));
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
          attempts++;
        }
      }
      throw last;

    } finally {
      writeLock.unlock(); // unlock when finished writing
    }
  }

  @Override
  public boolean tryClaim(Hash hash, int fragmentIndex) {
    Path locksDirectory = Path.of(settings.getDirectories().temp(), hash.getValue() + EXTENSION_LOCKS);
    Path claim = locksDirectory.resolve(Integer.toString(fragmentIndex));
    try {
      Files.createDirectories(locksDirectory);
    } catch (IOException e) {
      log.error("Cannot create lock directory: {}", locksDirectory, e);
      throw new RuntimeException(e);
    }
    try (SeekableByteChannel ch = Files.newByteChannel(claim, StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE)) {
      return true; // creada → reclamada
    } catch (FileAlreadyExistsException e) {
      return false; // otra entidad ya la reclamó
    } catch (IOException e) {
      log.warn("Cannot create lock file: {}", claim.getFileName(), e);
      return false;
    }
  }

  @Override
  public void deleteClaim(Hash hash) {
    Path locksDirectory = Path.of(settings.getDirectories().temp(), hash.getValue() + EXTENSION_LOCKS);
    if (!Files.isDirectory(locksDirectory)) {
      return;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(locksDirectory)) {
      stream.forEach(f -> {
        try {
          Files.deleteIfExists(f);
        } catch (IOException e) {
          log.warn("Cannot delete lock file: {}", f, e);
        }
      });
    } catch (IOException e) {
      log.warn("Cannot list locks directory: {}", locksDirectory.toAbsolutePath(), e);
    }

    try {
      Files.deleteIfExists(locksDirectory);
    } catch (IOException e) {
      log.warn("Cannot delete locks directory: {}", locksDirectory.getFileName(), e);
    }
  }

  @Override
  public BitSet createClearBitSet(int fragments) {
    return new BitSet(fragments);
  }

  @Override
  public BitSet createFullBitSet(int fragments) {
    BitSet bitset = new BitSet(fragments);
    bitset.set(0, fragments);
    return bitset;
  }

  @Override
  public int getFragmetsBySize(Long size) {
    return (int) ((size + FRAGMENT_SIZE - 1L) / FRAGMENT_SIZE);
  }

  @Override
  public int getChunksBySize(int size) {
    return (size + CHUNK_SIZE - 1) / CHUNK_SIZE; // ceil
  }


  @Override
  public byte[] getChunkByFragment(byte[] fragment, int index) {
    if (fragment != null && fragment.length > 0) {

      if (index < 0 || index >= Math.ceil((double) fragment.length / CHUNK_SIZE)) {
        log.error("Index out of bounds: [{}]", index);
        throw new IllegalArgumentException("Index out of bounds");
      }

      int offset = index * CHUNK_SIZE;
      if (offset >= fragment.length) {
        return new byte[0]; // No data
      }
      int length = Math.min(CHUNK_SIZE, fragment.length - offset);
      byte[] chunk = new byte[length];
      ByteBuffer.wrap(fragment, offset, length).get(chunk);
      return chunk;
    } else {
      return new byte[0];
    }
  }


  @Override
  public ByteBuffer assembleFragment(ConcurrentHashMap<Integer, FileChunk> chunks,
      FileFragment fileFragmentTemplate) {

    // fragment size defined from host
    final Integer originFragmentSize = fileFragmentTemplate.getSize();

    // fragment definitive
    final ByteBuffer fragmentDataBuffer = ByteBuffer.allocate(originFragmentSize);

    // put chunks on fragment
    chunks.forEach((index, chunk) -> {
      byte[] data = chunk.getData();
      int offset = index * CHUNK_SIZE;        // dónde empieza este chunk en el fragmento

      // Acota a límites (último chunk/pieza puede ser más corto)
      if (offset >= originFragmentSize) {
        return;              // fuera de rango, ignorar con seguridad
      }
      int toPut = Math.min(data.length, originFragmentSize - offset);

      // Escribir sin alterar position/limit del buffer original
      ByteBuffer view = fragmentDataBuffer.duplicate();
      view.position(offset).limit(offset + toPut);
      view.put(data, 0, toPut);
    });

    return fragmentDataBuffer;
  }

  @Override
  // devuelve booleano (last or not) a partir del bitset que carga del FS
  public boolean writeFragmentToFile(FileFragment fileFragmentTemplate, ByteBuffer fragmentBuf,
      Hash fileHash) {

    final Integer fragmentIndex = fileFragmentTemplate.getIndex();
    final Path filePath = Path.of(settings.getDirectories().temp(), fileHash.getValue());
    long fragmenOffset = (long) fragmentIndex * FRAGMENT_SIZE;

    // Prepara una vista lista para lectura exactamente pieceLen bytes
    ByteBuffer src = fragmentBuf.duplicate();
    src.position(0);                 // comienza al inicio
    src.limit(fileFragmentTemplate.getSize());             // limita al tamaño real del fragmento

    try (FileChannel ch = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
      while (src.hasRemaining()) {
        final int writeBytes = ch.write(src, fragmenOffset);// escritura posicional
        fragmenOffset += writeBytes; // opcional: también puedes usar write en bucle sin ajustar offset
      }
      // Si necesitas durabilidad fuerte inmediata, fuerza metadatos+datos:
      ch.force(true);

      final WriteLock wl = lockFor(fileHash).writeLock();
      wl.lock();

      try {
        // 2) update bitset
        final Sidecar sidecar = loadSidecarBitsetUnsafe(fileHash);
        final BitSet bits = (BitSet) sidecar.getData().clone();
        // TODO CHECK IN THE FUTURE -> fragmentIndex < sidecar.totalFragments
        bits.set(fragmentIndex);
        final Sidecar updatedSidecar = sidecar.toBuilder().data(bits).build();
        updateSidecar(updatedSidecar, fileHash);
        log.debug("WRITE_FRAGMENT: [{}] nextClearBit: [{}], updatedSidecar-totalfragments: [{}]",
            fragmentIndex,
            bits.nextClearBit(0), sidecar.getTotalFragments());

        return bits.nextClearBit(0) >= sidecar.getTotalFragments();
      } finally {
        wl.unlock();
      }

    } catch (IOException e) {
      log.error("Cannot write fragment to file: {}", filePath.getFileName(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void finalizeFile(FileTransferData fileTransferData) throws IOException {

    // get db file data
    final File fileByHash = filesPort.findByHash(fileTransferData.getHash().getValue());
    final String primalName = fileByHash.getNames().stream().findFirst()
        .orElse(fileTransferData.getHash().getValue()); // if no names rename with hash
    final Path downloadFilePath = Path.of(settings.getDirectories().download(), primalName);

    // get tmpFilePath
    final Path fileTmpPath = Path.of(settings.getDirectories().temp(), fileTransferData.getHash().getValue());

    if (Files.exists(fileTmpPath)) {

      // CHECK MD5
      final String md5Checksum = FileProcessUtils.checksumMD5String(fileTmpPath);
      if (md5Checksum.equals(fileTransferData.getHash().getValue())) {

        // delete sidecar
        deleteSidecar(fileTransferData);

        // Copiar el fichero al destino (sobrescribe si ya existe) // todo mejorar esto
        Files.copy(fileTmpPath, downloadFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Borrar el fichero temporal original
        Files.delete(fileTmpPath);

        final File fileUpdated = fileByHash.toBuilder().status(Status.STORED).build();
        filesPort.save(fileUpdated);

        // Borrar el directorio de claims
        deleteClaim(fileTransferData.getHash());

      }

    }

    // find by md5 from DB
    // CHECK MD5
    // ASYNC
    // MOVE FILE TO DESTINATION FOLDER
    // UPDATE FILE DB (PATH & STATUS)

    // ASYNC
    // MOVE FILE TO DESTINATION FOLDER
    // UPDATE FILE DB (PATH & STATUS)

  }

  private void deleteSidecar(FileTransferData fileTransferData) {
    final WriteLock writeLock = lockFor(fileTransferData.getHash()).writeLock();
    final Path sidecarPAth = Path.of(settings.getDirectories().temp(),
        fileTransferData.getHash().getValue() + EXTENSION_SIDECAR_XUT);
    writeLock.lock();
    try {
      if (Files.exists(sidecarPAth)) {
        Files.delete(sidecarPAth);
      }
    } catch (IOException e) {
      log.error("Cannot delete sidecar file: {}", sidecarPAth.getFileName(), e);
    } finally {
      writeLock.unlock();
    }

  }

  private static ReentrantReadWriteLock lockFor(Hash hash) {
    // true = lock justo (prioridad a hilos esperando más tiempo)
    return sidecarLocks.computeIfAbsent(hash.getValue(), k -> new ReentrantReadWriteLock(true));
  }



/* todo implementar esta solucion
  // Escribir una pieza recibida en el offset correcto y marcarla en el .tmp
  public static void writePiece(File f, int pieceIndex, byte[] pieceData) throws IOException {
    // 1) Escribe la pieza en su posición
    long offset = (long) pieceIndex * PIECE_SIZE;
    try (FileChannel ch = FileChannel.open(dataPath(f), StandardOpenOption.WRITE)) {
      ch.write(ByteBuffer.wrap(pieceData), offset);
      // No cerramos con force() cada vez para rendimiento; hazlo cada N piezas si quieres durabilidad inmediata
    }

    // 2) Actualiza el bitset en el .tmp
    BitSet bits = loadBitset(tmpPath(f));
    bits.set(pieceIndex);
    saveBitset(tmpPath(f), bits);
  }
*/

/* todo implementar esta solucion

  // Cargar bitset desde .tmp; si no existe, bitset vacío
  static BitSet loadBitset(Path tmp) throws IOException {
    if (!Files.exists(tmp)) return new BitSet();
    try (InputStream in = Files.newInputStream(tmp)) {
      int version = in.read();
      if (version < 0) return new BitSet();
      byte[] lenBuf = in.readNBytes(4);
      int len = ByteBuffer.wrap(lenBuf).order(ByteOrder.BIG_ENDIAN).getInt();
      byte[] payload = in.readNBytes(len);
      return BitSet.valueOf(payload);
    }
  }
*/

//  private void saveBitset(Path tmp, BitSet bits) throws IOException {
//    byte[] payload = bits.toByteArray();
//    ByteBuffer header = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN);
//    header.put((byte)1);            // version
//    header.putInt(payload.length);  // longitud del bitset en bytes
//
//    // Escribe de forma segura: a archivo temporal y renombrar atómicamente
//    Path tmpSwap = tmp.resolveSibling(tmp.getFileName() + EXTENSION_TMP_SWAP);
//    try (OutputStream out = Files.newOutputStream(tmpSwap,
//        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
//      out.write(header.array());
//      out.write(payload);
//    }
//    Files.move(tmpSwap, tmp, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
//  }

  // Escribir una pieza recibida en el offset correcto y marcarla en el .tmp
//  public void writePiece(FileInfo f, int pieceIndex, byte[] pieceData) throws IOException {
//    // 1) Escribe la pieza en su posición
//    long offset = (long) pieceIndex * PIECE_SIZE;
//    try (FileChannel ch = FileChannel.open(dataPath(f), StandardOpenOption.WRITE)) {
//      ch.write(ByteBuffer.wrap(pieceData), offset);
//      // No cerramos con force() cada vez para rendimiento; hazlo cada N piezas si quieres durabilidad inmediata
//    }
//
//    // 2) Actualiza el bitset en el .tmp
//    BitSet bits = loadBitset(tmpPath(f));
//    bits.set(pieceIndex);
//    saveBitset(tmpPath(f), bits);
//  }


}
