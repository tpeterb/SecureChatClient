package com.tpeterb.securechatclient.security.service;

import com.tpeterb.securechatclient.security.model.EllipticCurveKeyExchangeDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ServerKeyExchangeApiService {

    @POST("api/key-exchange")
    Call<EllipticCurveKeyExchangeDTO> initiateEllipticCurveDiffieHellmanKeyExchange(@Body EllipticCurveKeyExchangeDTO ellipticCurveKeyExchangeDTO);

}
