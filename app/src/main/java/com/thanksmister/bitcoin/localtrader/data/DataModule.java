/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.data;

import android.content.SharedPreferences;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.network.api.ApiModule;
import com.thanksmister.bitcoin.localtrader.network.api.BitcoinAverage;
import com.thanksmister.bitcoin.localtrader.network.api.BitfinexExchange;
import com.thanksmister.bitcoin.localtrader.network.api.BitstampExchange;
import com.thanksmister.bitcoin.localtrader.network.api.Coinbase;
import com.thanksmister.bitcoin.localtrader.network.services.ExchangeService;

import java.io.File;
import java.util.Arrays;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dpreference.DPreference;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

@Module(
        includes = {ApiModule.class},
        complete = false,
        library = true
)
public final class DataModule {

    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(BaseApplication app) {
        return app.getSharedPreferences("com.thanksmister.bitcoin.localtrader", MODE_PRIVATE);
    }

    @Provides
    @Singleton
    DPreference providePreferences(BaseApplication app) {
        return new DPreference(app.getApplicationContext(), "LocalTraderPref");
    }

    @Provides
    @Singleton
    ExchangeService provideExchangeService(SharedPreferences preferences, Coinbase coinbase, BitstampExchange bitstampExchange,
                                           BitfinexExchange bitfinexExchange, BitcoinAverage bitcoinAverage) {
        return new ExchangeService(preferences, coinbase, bitstampExchange, bitfinexExchange, bitcoinAverage);
    }

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient(BaseApplication app) {
        OkHttpClient client = new OkHttpClient();
        // Install an HTTP cache in the application cache directory.
        try {
            File cacheDir = new File(app.getCacheDir(), "http");
            Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
            client.setCache(cache);
            client.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
        } catch (Exception e) {
            Timber.e(e, "Unable to install disk cache.");
        }

        return client;
    }
}