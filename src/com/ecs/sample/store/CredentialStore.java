package com.ecs.sample.store;


public interface CredentialStore {
  public String[] read();
  public void write(String[] response);
  public void clearCredentials();
}
