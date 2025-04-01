package com.tpeterb.securechatclient.security.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tpeterb.securechatclient.security.model.ServerDigitalSignaturePublicKeyResponseDTO;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Slf4j
@Singleton
public class ServerPublicKeyApiServiceProvider {

    private final ServerPublicKeyApiService serverPublicKeyApiService;

    @Inject
    public ServerPublicKeyApiServiceProvider(ServerPublicKeyApiService serverPublicKeyApiService) {
        this.serverPublicKeyApiService = serverPublicKeyApiService;
    }

    public LiveData<ServerDigitalSignaturePublicKeyResponseDTO> retrieveServerPublicKeyForDigitalSignatures() {
        MutableLiveData<ServerDigitalSignaturePublicKeyResponseDTO> liveData = new MutableLiveData<>();
        Call<ServerDigitalSignaturePublicKeyResponseDTO> call = serverPublicKeyApiService.retrieveServerDigitalSignaturePublicKey();
        call.enqueue(new Callback<ServerDigitalSignaturePublicKeyResponseDTO>() {
            @Override
            public void onResponse(Call<ServerDigitalSignaturePublicKeyResponseDTO> call, Response<ServerDigitalSignaturePublicKeyResponseDTO> response) {
                if (Objects.nonNull(response.body()) && response.isSuccessful()) {
                    log.info("Successfully obtained server's public key for digital signatures! Response = {}", response.body());
                    liveData.postValue(response.body());
                } else {
                    log.error("Failed to fetch server's digital signature public key!");
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<ServerDigitalSignaturePublicKeyResponseDTO> call, Throwable throwable) {
                if (Objects.nonNull(throwable)) {
                    log.error("Retrieval of server's public key for digital signatures failed, reason: {}", throwable.getMessage());
                    liveData.postValue(null);
                } else {
                    log.error("Retrieval of server's public key for digital signatures failed");
                    liveData.postValue(null);
                }
            }
        });
        return liveData;
    }

}
