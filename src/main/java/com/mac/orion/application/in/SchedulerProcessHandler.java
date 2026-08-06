package com.mac.orion.application.in;

public interface SchedulerProcessHandler {

  default void startProcess(Object arg){
    throw new  UnsupportedOperationException("Method startProcess is not implemented");
  }

  default void stopProcess(Object arg){
    throw new  UnsupportedOperationException("Method stopProcess is not implemented");
  }

  default void startProcess(){
    throw new  UnsupportedOperationException("Method startProcess is not implemented");
  }

  default void stopProcess(){
    throw new  UnsupportedOperationException("Method stopProcess is not implemented");
  }
}
