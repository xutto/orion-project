package com.mac.orion.infrastructure.ui.nodes;

import java.util.HashSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NodesConfiguration {

  @Bean
  public ControllerNodes filesControllerNodes(){
      return new ControllerNodesImpl(new HashSet<>());
  }
  @Bean
  public ControllerNodes connectionControllerNodes(){
    return new ControllerNodesImpl(new HashSet<>());
  }

  @Bean
  public ControllerNodes homeControllerNodes(){
    return new ControllerNodesImpl(new HashSet<>());
  }

  @Bean
  public ControllerNodes searchControllerNodes(){
    return new ControllerNodesImpl(new HashSet<>());
  }

  @Bean
  public ControllerNodes searchResultControllerNodes(){
    return new ControllerNodesImpl(new HashSet<>());
  }

  @Bean
  public ControllerNodes transfersControllerNodes(){
    return new ControllerNodesImpl(new HashSet<>());
  }

  @Bean
  public ControllerNodes settingsControllerNodes(){
    return new ControllerNodesImpl(new HashSet<>());
  }

}
