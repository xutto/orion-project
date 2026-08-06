package com.mac.orion.domain.model;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Se debe trabajar con este hostNode cuando se necesiten los datos del host(peer) propio porque
 * cuando se configura el hostNode se inyectan los protocolos y los protocolos usan inyección a su
 * vez, por lo que si se necesitan datos del host deben ser a partir de este objeto de dominio que
 * estará en el context para poder inyectarse correctamente
 */
@Builder(toBuilder = true)
@Getter
@ToString
public final class HostNode {

  private final String id;

  private final Set<Address> addresses;

}
