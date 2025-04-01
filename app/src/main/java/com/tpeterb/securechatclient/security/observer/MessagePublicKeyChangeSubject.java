package com.tpeterb.securechatclient.security.observer;

public interface MessagePublicKeyChangeSubject {

    void addSubscriber(MessagePublicKeyChangeObserver publicKeyChangeObserver);

    void removeSubscriber(MessagePublicKeyChangeObserver publicKeyChangeObserver);

    void notifyObservers();

}
