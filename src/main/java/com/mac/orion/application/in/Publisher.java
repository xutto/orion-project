package com.mac.orion.application.in;

import com.mac.orion.domain.dht.OperationsType;
import java.util.function.Consumer;

public interface Publisher<T> {

  void publish(OperationsType operation, T item);


  default void subscribe(OperationsType operation, Object other,  Consumer<T> consumer){
    throw new UnsupportedOperationException("operation not supported yet.");
  }

  default void subscribe(OperationsType operation, Consumer<T> consumer){
    throw new UnsupportedOperationException("operation not supported yet.");
  }


}
