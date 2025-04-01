package com.tpeterb.securechatclient.security.service;

import static com.tpeterb.securechatclient.constants.Constants.HTTP_STATUS_CODE_FOR_FAILED_KEY_EXCHANGE;
import static com.tpeterb.securechatclient.constants.Constants.NEW_KEY_EXCHANGE_SIGNALING_GENERAL_ENDPOINT;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.FAILED_SIGNATURE_VERIFICATION_ON_SERVER_SIDE;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.GENERAL_FAILURE;
import static com.tpeterb.securechatclient.security.model.KeyExchangeResult.SUCCESS;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tpeterb.securechatclient.messages.registry.StompSubscriptionRegistry;
import com.tpeterb.securechatclient.messages.service.StompSubscriptionService;
import com.tpeterb.securechatclient.security.model.EllipticCurveKeyExchangeDTO;
import com.tpeterb.securechatclient.security.model.KeyExchangeResult;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
@Slf4j
public class ServerKeyExchangeApiServiceProvider {

    private final ServerKeyExchangeApiService serverKeyExchangeApiService;

    @Inject
    public ServerKeyExchangeApiServiceProvider(ServerKeyExchangeApiService serverKeyExchangeApiService) {
        this.serverKeyExchangeApiService = serverKeyExchangeApiService;
    }

    public LiveData<Pair<EllipticCurveKeyExchangeDTO, KeyExchangeResult>> initiateEllipticCurveDiffieHellmanKeyExchange(EllipticCurveKeyExchangeDTO ellipticCurveKeyExchangeDTO) {
        log.info("Elliptic curve key exchange request to send = {}", ellipticCurveKeyExchangeDTO);
        MutableLiveData<Pair<EllipticCurveKeyExchangeDTO, KeyExchangeResult>> liveData = new MutableLiveData<>();
        Call<EllipticCurveKeyExchangeDTO> keyExchangeCall = serverKeyExchangeApiService.initiateEllipticCurveDiffieHellmanKeyExchange(ellipticCurveKeyExchangeDTO);
        log.info("ECDH DTO JUST BEFORE SENDING: isBeforeLogin = {}", ellipticCurveKeyExchangeDTO.isBeforeLogin());
        keyExchangeCall.enqueue(new Callback<EllipticCurveKeyExchangeDTO>() {
            @Override
            public void onResponse(Call<EllipticCurveKeyExchangeDTO> call, Response<EllipticCurveKeyExchangeDTO> response) {
                if (response.isSuccessful() && Objects.nonNull(response.body())) {
                    log.info("ECDH key exchange was successful with server, reveived the response!");
                    log.info("ECDH RESPONSE BODY = {}", response.body());
                    liveData.postValue(Pair.create(response.body(), SUCCESS));
                } else if (Objects.isNull(response.body()) && response.code() == HTTP_STATUS_CODE_FOR_FAILED_KEY_EXCHANGE) {
                    log.error("Signature verification of ECDH ephemeral public key on server side failed!");
                    liveData.postValue(Pair.create(null, FAILED_SIGNATURE_VERIFICATION_ON_SERVER_SIDE));
                } else {
                    log.error("Failed to do ECDH key exchange with server, response code = {}, message = {}", response.code(), response.message());
                    liveData.postValue(Pair.create(null, GENERAL_FAILURE));
                }
            }

            @Override
            public void onFailure(Call<EllipticCurveKeyExchangeDTO> call, Throwable throwable) {
                log.error("Failed to do ECDH key exchange with server, reason = {}", throwable.getMessage());
                liveData.postValue(Pair.create(null, GENERAL_FAILURE));
            }
        });
        return liveData;
    }

}
