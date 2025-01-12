package com.tpeterb.securechatclient.component;

import com.tpeterb.securechatclient.module.AppModule;
import com.tpeterb.securechatclient.ui.ChatListActivity;
import com.tpeterb.securechatclient.ui.LoginActivity;
import com.tpeterb.securechatclient.ui.PersonalChatActivity;
import com.tpeterb.securechatclient.ui.RegisterActivity;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {AppModule.class})
@Singleton
public interface AppComponent {

    void inject(RegisterActivity registerActivity);

    void inject(LoginActivity loginActivity);

    void inject(ChatListActivity chatListActivity);

    void inject(PersonalChatActivity personalChatActivity);

}
