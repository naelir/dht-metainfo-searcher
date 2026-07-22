package com.naelir.dht;

public interface ITask extends Runnable {

    void run();

    boolean resolved();

}