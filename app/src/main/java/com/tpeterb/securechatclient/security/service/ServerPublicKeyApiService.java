package com.tpeterb.securechatclient.security.service;

import com.tpeterb.securechatclient.security.model.ServerDigitalSignaturePublicKeyResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ServerPublicKeyApiService {

    @GET("/api/key/public/signature")
    Call<ServerDigitalSignaturePublicKeyResponseDTO> retrieveServerDigitalSignaturePublicKey();

}
